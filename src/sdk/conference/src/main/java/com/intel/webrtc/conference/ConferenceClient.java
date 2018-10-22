/*
 * Intel License Header Holder
 */
package com.intel.webrtc.conference;

import android.util.Log;

import com.intel.webrtc.base.ActionCallback;
import com.intel.webrtc.base.IcsError;
import com.intel.webrtc.base.LocalStream;
import com.intel.webrtc.base.MediaConstraints;
import com.intel.webrtc.base.PeerConnectionChannel;
import com.intel.webrtc.base.MediaConstraints.TrackKind;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.RTCStatsCollectorCallback;
import org.webrtc.RTCStatsReport;
import org.webrtc.SessionDescription;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.socket.client.Ack;

import static com.intel.webrtc.base.CheckCondition.DCHECK;
import static com.intel.webrtc.base.CheckCondition.RCHECK;

/**
 * ConferenceClient handles PeerConnection interactions between client and server.
 */
public final class ConferenceClient implements SignalingChannel.SignalingChannelObserver,
                                               PeerConnectionChannel.PeerConnectionChannelObserver {

    /**
     * Interface for observing conference client events.
     */
    public interface ConferenceClientObserver {
        /**
         * Called upon a RemoteStream gets added to the conference.
         *
         * @param remoteStream RemoteStream added.
         */
        void onStreamAdded(RemoteStream remoteStream);

        /**
         * Called upon a Participant joins the conference.
         *
         * @param participant Participant joins the conference.
         */
        void onParticipantJoined(Participant participant);

        /**
         * Called upon receiving a message.
         *
         * @param participantId id of the message sender.
         * @param message       message received.
         */
        void onMessageReceived(String participantId, String message);

        /**
         * Called upon server disconnected.
         */
        void onServerDisconnected();
    }

    private enum RoomStates {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    private static final String TAG = "ICS";
    private final ConferenceClientConfiguration configuration;
    //signalingChannel will be created upon join() and will be destructed upon leave().
    private SignalingChannel signalingChannel;
    private ConferenceInfo conferenceInfo;
    private ActionCallback<ConferenceInfo> joinCallback;
    private ExecutorService signalingExecutor = Executors.newSingleThreadExecutor();
    private ExecutorService callbackExecutor = Executors.newSingleThreadExecutor();
    //key: publication/subscription id.
    private ConcurrentHashMap<String, ConferencePeerConnectionChannel> pcChannels;
    //key: subscription id.
    private ConcurrentHashMap<String, ActionCallback<Subscription>> subCallbacks;
    private ConcurrentHashMap<String, ActionCallback<Publication>> pubCallbacks;
    private RoomStates roomStates;
    private final Object statusLock = new Object();
    private List<ConferenceClientObserver> observers;

    /**
     * Constructor for ConferenceClient.
     *
     * @param configuration ConferenceClientConfiguration for ConferenceClient
     */
    public ConferenceClient(ConferenceClientConfiguration configuration) {
        this.configuration = configuration;
        observers = Collections.synchronizedList(new ArrayList<ConferenceClientObserver>());
        pcChannels = new ConcurrentHashMap<>();
        subCallbacks = new ConcurrentHashMap<>();
        pubCallbacks = new ConcurrentHashMap<>();
        roomStates = RoomStates.DISCONNECTED;
    }

    /**
     * Add a ConferenceClientObserver.
     *
     * @param observer ConferenceClientObserver to be added.
     */
    public void addObserver(ConferenceClientObserver observer) {
        RCHECK(observer);
        observers.add(observer);
    }

    /**
     * Remove a ConferenceClientObserver.
     *
     * @param observer ConferenceClientObserver to be removed.
     */
    public void removeObserver(ConferenceClientObserver observer) {
        RCHECK(observer);
        observers.remove(observer);
    }

    /**
     * Get the ConferenceInfo of this ConferenceClient.
     *
     * @return current ConferenceInfo of this ConferenceClient.
     */
    public ConferenceInfo info() {
        return conferenceInfo;
    }

    /**
     * Join a conference specified by |token|.
     *
     * @param token    token issued by conference server (nuve).
     * @param callback ActionCallback.onSuccess will be invoked with the ConferenceInfo when
     *                 succeeds to join the conference room. Otherwise when fails to do so,
     *                 ActionCallback.onFailure will be invoked with the corresponding IcsError.
     */
    public void join(String token, ActionCallback<ConferenceInfo> callback) {
        if (!checkRoomStatus(RoomStates.DISCONNECTED)) {
            callback.onFailure(new IcsError("Wrong room status."));
            return;
        }
        DCHECK(signalingChannel == null);
        this.joinCallback = callback;
        signalingChannel = new SignalingChannel(token, this);
        changeRoomStatus(RoomStates.CONNECTING);
        signalingChannel.connect(configuration);
    }

    /**
     * Leave the conference.
     */
    public void leave() {
        if (checkRoomStatus(RoomStates.DISCONNECTED)) {
            Log.w(TAG, "Wrong room status when leave.");
            return;
        }
        DCHECK(signalingChannel);
        sendSignalingMessage("logout", null, new Ack() {
            @Override
            public void call(Object... args) {
                DCHECK(extractMsg(0, args).equals("ok"));
                signalingChannel.disconnect();
            }
        });
    }

    /**
     * Publish a LocalStream to the conference.
     *
     * @param localStream LocalStream to be published.
     * @param callback    ActionCallback.onSuccess will be invoked with the Publication when
     *                    succeeds to publish the LocalStream. Otherwise when fails to do so,
     *                    ActionCallback.onFailure will be invoked with the corresponding IcsError.
     */
    public void publish(LocalStream localStream, ActionCallback<Publication> callback) {
        publish(localStream, null, callback);
    }

    /**
     * Publish a LocalStream to the conference.
     *
     * @param localStream LocalStream to be published.
     * @param options     PublishOptions for publishing this LocalStream.
     * @param callback    ActionCallback.onSuccess will be invoked with the Publication when
     *                    succeeds to publish the LocalStream. Otherwise when fails to do so,
     *                    ActionCallback.onFailure will be invoked with the corresponding IcsError.
     */
    public void publish(final LocalStream localStream, final PublishOptions options,
                        final ActionCallback<Publication> callback) {
        if (!checkRoomStatus(RoomStates.CONNECTED)) {
            triggerCallback(callback, new IcsError("Wrong room status."));
            return;
        }
        RCHECK(localStream);

        Ack publishAck = new Ack() {
            @Override
            public void call(Object... args) {
                if (extractMsg(0, args).equals("ok")) {
                    try {
                        JSONObject result = (JSONObject) args[1];
                        ConferencePeerConnectionChannel pcChannel =
                                getPeerConnection(result.getString("id"), false, false);
                        if (callback != null) {
                            pubCallbacks.put(result.getString("id"), callback);
                        }
                        pcChannel.publish(localStream, options);
                    } catch (JSONException e) {
                        triggerCallback(callback, new IcsError(e.getMessage()));
                    }
                } else {
                    triggerCallback(callback, new IcsError(extractMsg(1, args)));
                }
            }
        };

        try {
            JSONObject mediaInfo = new JSONObject();

            if (localStream.hasVideo()) {
                JSONObject resolution = new JSONObject();
                resolution.put("width", localStream.resolutionWidth);
                resolution.put("height", localStream.resolutionHeight);

                JSONObject params = new JSONObject();
                params.put("resolution", resolution);
                params.put("framerate", localStream.frameRate);

                JSONObject video = new JSONObject();
                video.put("parameters", params);
                video.put("source", localStream.getStreamSourceInfo().videoSourceInfo.type);

                mediaInfo.put("video", video);
            } else {
                mediaInfo.put("video", false);
            }

            if (localStream.hasAudio()) {
                JSONObject audio = new JSONObject();
                audio.put("source", localStream.getStreamSourceInfo().audioSourceInfo.type);

                mediaInfo.put("audio", audio);
            } else {
                mediaInfo.put("audio", false);
            }

            JSONObject publishMsg = new JSONObject();
            publishMsg.put("media", mediaInfo);

            if (localStream.getAttributes() != null) {
                JSONObject attr = new JSONObject(localStream.getAttributes());
                publishMsg.put("attributes", attr);
            }

            sendSignalingMessage("publish", publishMsg, publishAck);

        } catch (JSONException e) {
            DCHECK(e);
        }
    }

    void unpublish(final String publicationId, final Publication publication) {
        if (!checkRoomStatus(RoomStates.CONNECTED)) {
            Log.w(TAG, "Wrong room status when unpublish.");
            return;
        }
        RCHECK(publicationId);

        try {
            JSONObject unpubMsg = new JSONObject();
            unpubMsg.put("id", publicationId);

            sendSignalingMessage("unpublish", unpubMsg, new Ack() {
                @Override
                public void call(Object... args) {
                    DCHECK(extractMsg(0, args).equals("ok"));
                    ConferencePeerConnectionChannel pcChannel = getPeerConnection(publicationId);
                    pcChannel.dispose();
                    pcChannels.remove(publicationId);
                    publication.onEnded();
                }
            });

        } catch (JSONException e) {
            DCHECK(false);
        }
    }

    /**
     * Subscribe a RemoteStream from the conference.
     *
     * @param remoteStream RemoteStream to be subscribed.
     * @param callback     ActionCallback.onSuccess will be invoked with the Subscription when
     *                     succeeds to subscribe the RemoteStream. Otherwise when fails to do so,
     *                     ActionCallback.onFailure will be invoked with the corresponding IcsError.
     */
    public void subscribe(RemoteStream remoteStream, ActionCallback<Subscription> callback) {
        subscribe(remoteStream, null, callback);
    }

    /**
     * Subscribe a RemoteStream from the conference.
     *
     * @param remoteStream RemoteStream to be subscribed.
     * @param options      SubscribeOptions for subscribing the RemoteStream.
     * @param callback     ActionCallback.onSuccess will be invoked with the Subscription when
     *                     succeeds to subscribe the RemoteStream. Otherwise when fails to do so,
     *                     ActionCallback.onFailure will be invoked with the corresponding IcsError.
     */
    public void subscribe(final RemoteStream remoteStream, final SubscribeOptions options,
                          final ActionCallback<Subscription> callback) {
        if (!checkRoomStatus(RoomStates.CONNECTED)) {
            triggerCallback(callback, new IcsError("Wrong room status."));
            return;
        }
        RCHECK(remoteStream);

        final boolean subVideo = options == null || options.videoOption != null;
        final boolean subAudio = options == null || options.audioOption != null;

        Ack subscribeAck = new Ack() {
            @Override
            public void call(Object... args) {
                if (extractMsg(0, args).equals("ok")) {
                    for (ConferencePeerConnectionChannel pcChannel : pcChannels.values()) {
                        if (pcChannel.stream.id().equals(remoteStream.id())) {
                            triggerCallback(callback,
                                            new IcsError("Remote stream has been subscribed."));
                            return;
                        }
                    }
                    JSONObject result = (JSONObject) args[1];
                    try {
                        ConferencePeerConnectionChannel pcChannel =
                                getPeerConnection(result.getString("id"), subVideo, subAudio);
                        if (callback != null) {
                            subCallbacks.put(result.getString("id"), callback);
                        }
                        pcChannel.subscribe(remoteStream, options);
                    } catch (JSONException e) {
                        triggerCallback(callback, new IcsError(e.getMessage()));
                    }
                } else {
                    triggerCallback(callback, new IcsError(extractMsg(1, args)));
                }
            }
        };

        try {
            JSONObject media = new JSONObject();
            if (subVideo) {
                JSONObject video = new JSONObject();
                video.put("from", remoteStream.id());
                if (options != null) {
                    video.put("parameters", options.videoOption.generateOptionsMsg());
                }
                media.put("video", video);
            } else {
                media.put("video", false);
            }
            if (subAudio) {
                JSONObject audio = new JSONObject();
                audio.put("from", remoteStream.id());
                media.put("audio", audio);
            } else {
                media.put("audio", false);
            }

            JSONObject subscribeMsg = new JSONObject();
            subscribeMsg.put("media", media);

            sendSignalingMessage("subscribe", subscribeMsg, subscribeAck);

        } catch (JSONException e) {
            DCHECK(e);
        }
    }

    void unsubscribe(final String subscriptionId, final Subscription subscription) {
        if (!checkRoomStatus(RoomStates.CONNECTED)) {
            Log.w(TAG, "Wrong room status when unsubscribe.");
            return;
        }
        RCHECK(subscriptionId);

        try {
            JSONObject unpubMsg = new JSONObject();
            unpubMsg.put("id", subscriptionId);

            sendSignalingMessage("unsubscribe", unpubMsg, new Ack() {
                @Override
                public void call(Object... args) {
                    if (pcChannels.containsKey(subscriptionId)) {
                        ConferencePeerConnectionChannel pcChannel = getPeerConnection(subscriptionId);
                        pcChannel.dispose();
                        pcChannels.remove(subscriptionId);
                        subscription.onEnded();
                    }
                }
            });

        } catch (JSONException e) {
            DCHECK(false);
        }
    }

    /**
     * Send a text message to all participants in the conference.
     *
     * @param message  message to be sent.
     * @param callback ActionCallback.onSuccess will be invoked succeeds to send the message.
     *                 Otherwise when fails to do so, ActionCallback.onFailure will be invoked
     *                 with the corresponding IcsError.
     */
    public void send(String message, ActionCallback<Void> callback) {
        send(null, message, callback);
    }

    /**
     * Send a text message to a specific participant in the conference.
     *
     * @param participantId id of Participant the message to be sent to.
     * @param message       message to be sent.
     * @param callback      ActionCallback.onSuccess will be invoked succeeds to send the message.
     *                      Otherwise when fails to do so, ActionCallback.onFailure will be invoked
     *                      with the corresponding IcsError.
     */
    public void send(String participantId, String message, final ActionCallback<Void> callback) {
        if (!checkRoomStatus(RoomStates.CONNECTED)) {
            triggerCallback(callback, new IcsError(0, "Wrong status"));
            return;
        }
        RCHECK(message);

        try {
            JSONObject sendMsg = new JSONObject();
            sendMsg.put("to", participantId == null ? "all" : participantId);
            sendMsg.put("message", message);

            sendSignalingMessage("text", sendMsg, new Ack() {
                @Override
                public void call(Object... args) {
                    if (extractMsg(0, args).equals("ok")) {
                        callbackExecutor.execute(new Runnable() {
                            @Override
                            public void run() {
                                if (callback != null) {
                                    callback.onSuccess(null);
                                }
                            }
                        });
                    } else {
                        triggerCallback(callback, new IcsError(extractMsg(1, args)));
                    }
                }
            });

        } catch (JSONException e) {
            DCHECK(false);
        }
    }

    void getStats(String id, final ActionCallback<RTCStatsReport> callback) {
        if (!pcChannels.containsKey(id)) {
            triggerCallback(callback, new IcsError(0, "Wrong state"));
            return;
        }
        ConferencePeerConnectionChannel pcChannel = getPeerConnection(id);
        pcChannel.getConnectionStats(new RTCStatsCollectorCallback() {
            @Override
            public void onStatsDelivered(RTCStatsReport rtcStatsReport) {
                if (callback != null) {
                    callback.onSuccess(rtcStatsReport);
                }
            }
        });
    }

    private void closeInternal() {
        for (String key : pcChannels.keySet()) {
            pcChannels.get(key).dispose();
        }
        pcChannels.clear();
        subCallbacks.clear();
        pubCallbacks.clear();
        signalingChannel = null;
        conferenceInfo = null;
        joinCallback = null;
    }

    private boolean checkRoomStatus(RoomStates roomStates) {
        synchronized (statusLock) {
            return this.roomStates == roomStates;
        }
    }

    private void changeRoomStatus(RoomStates roomStates) {
        synchronized (statusLock) {
            this.roomStates = roomStates;
        }
    }

    private ConferencePeerConnectionChannel getPeerConnection(String id) {
        DCHECK(pcChannels.containsKey(id));
        return getPeerConnection(id, true, true);
    }

    private ConferencePeerConnectionChannel getPeerConnection(String id, boolean enableVideo,
                                                              boolean enableAudio) {
        if (pcChannels.containsKey(id)) {
            return pcChannels.get(id);
        }
        ConferencePeerConnectionChannel pcChannel =
                new ConferencePeerConnectionChannel(id, configuration.rtcConfiguration,
                                                    enableVideo, enableAudio, this);
        pcChannels.put(id, pcChannel);
        return pcChannel;
    }

    <T> void triggerCallback(final ActionCallback<T> callback, final IcsError error) {
        DCHECK(callbackExecutor);
        if (callback == null) {
            return;
        }
        callbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                callback.onFailure(error);
            }
        });
    }

    private String extractMsg(int position, Object... args) {
        if (position < 0 || args == null || args.length < position + 1
                || args[position] == null) {
            DCHECK(false);
            return "";
        }
        return args[position].toString();
    }

    void sendSignalingMessage(final String type, final JSONObject message, final Ack ack) {
        DCHECK(signalingExecutor);
        DCHECK(signalingChannel);
        signalingExecutor.execute(new Runnable() {
            @Override
            public void run() {
                signalingChannel.sendMsg(type, message, ack);
            }
        });
    }

    private void processAck(final String id) {
        callbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (pubCallbacks.containsKey(id)) {
                    ActionCallback<Publication> callback = pubCallbacks.get(id);
                    ConferencePeerConnectionChannel pcChannel = getPeerConnection(id);
                    Publication publication = new Publication(id, pcChannel.getMediaStream(),
                                                              ConferenceClient.this);
                    getPeerConnection(id).muteEventObserver = publication;
                    callback.onSuccess(publication);
                    pubCallbacks.remove(id);
                    return;
                }
                if (subCallbacks.containsKey(id)) {
                    ActionCallback<Subscription> callback = subCallbacks.get(id);
                    Subscription subscription = new Subscription(id, ConferenceClient.this);
                    getPeerConnection(id).muteEventObserver = subscription;
                    callback.onSuccess(subscription);
                    subCallbacks.remove(id);
                }
            }
        });
    }

    private void processError(final String id, final String errorMsg) {
        callbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (pubCallbacks.containsKey(id)) {
                    ActionCallback<Publication> callback = pubCallbacks.get(id);
                    triggerCallback(callback, new IcsError(errorMsg));
                    pubCallbacks.remove(id);
                } else {
                    ActionCallback<Subscription> callback = subCallbacks.get(id);
                    triggerCallback(callback, new IcsError(errorMsg));
                    subCallbacks.remove(id);
                }
            }
        });
    }

    ///@cond
    //SignalingChannelObserver
    @Override
    public void onRoomConnected(final JSONObject info) {
        DCHECK(callbackExecutor);
        changeRoomStatus(RoomStates.CONNECTED);

        callbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                ConferenceInfo conferenceInfo;
                try {
                    if (joinCallback != null) {
                        conferenceInfo = new ConferenceInfo(info);
                        ConferenceClient.this.conferenceInfo = conferenceInfo;
                        joinCallback.onSuccess(conferenceInfo);
                    }
                } catch (JSONException e) {
                    triggerCallback(joinCallback, new IcsError(e.getMessage()));
                }
                joinCallback = null;
            }
        });
    }

    @Override
    public void onRoomConnectFailed(final String errorMsg) {
        DCHECK(callbackExecutor);
        changeRoomStatus(RoomStates.DISCONNECTED);
        signalingChannel = null;

        triggerCallback(joinCallback, new IcsError(errorMsg));
    }

    @Override
    public void onReconnecting() {

    }

    @Override
    public void onRoomDisconnected() {
        DCHECK(callbackExecutor);
        changeRoomStatus(RoomStates.DISCONNECTED);
        closeInternal();
        callbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                for (ConferenceClientObserver observer : observers) {
                    observer.onServerDisconnected();
                }
            }
        });
    }

    @Override
    public void onProgressMessage(JSONObject msg) {
        DCHECK(msg);
        try {
            ConferencePeerConnectionChannel pcChannel = getPeerConnection(msg.getString("id"));
            switch (msg.getString("status")) {
                case "soac":
                    pcChannel.processSignalingMessage(msg.getJSONObject("data"));
                    break;
                case "ready":
                    processAck(msg.getString("id"));
                    break;
                case "error":
                    processError(msg.getString("id"), msg.getString("data"));
                    break;
                default:
                    DCHECK(false);
            }
        } catch (JSONException e) {
            DCHECK(false);
        }
    }

    @Override
    public void onTextMessage(final String participantId, final String message) {
        callbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                for (ConferenceClientObserver observer : observers) {
                    observer.onMessageReceived(participantId, message);
                }
            }
        });
    }

    @Override
    public void onStreamAdded(final RemoteStream remoteStream) {
        callbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                conferenceInfo.remoteStreams.add(remoteStream);
                for (ConferenceClientObserver observer : observers) {
                    observer.onStreamAdded(remoteStream);
                }
            }
        });
    }

    @Override
    public void onStreamRemoved(final String streamId) {
        callbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                for (RemoteStream remoteStream : conferenceInfo.remoteStreams) {
                    if (remoteStream.id().equals(streamId)) {
                        remoteStream.onEnded();
                        conferenceInfo.remoteStreams.remove(remoteStream);
                        break;
                    }
                }
            }
        });
    }

    @Override
    public void onStreamUpdated(final String id, final JSONObject updateInfo) {
        callbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String field = updateInfo.getString("field");
                    if (field.equals("video.layout")) {
                        for (RemoteStream remoteStream : conferenceInfo.remoteStreams) {
                            if (remoteStream.id().equals(id)) {
                                ((RemoteMixedStream) remoteStream).updateRegions(
                                        updateInfo.getJSONArray("value"));
                            }
                        }
                    } else if (field.equals("audio.status") || field.equals("video.status")) {
                        for (ConferencePeerConnectionChannel pcChannel : pcChannels.values()) {
                            // For subscription id will be the RemoteStream id, for publication
                            // the id will be publication id which is pc.key.
                            if (pcChannel.stream.id().equals(id)
                                    || pcChannel.key.equals(id)) {
                                if (pcChannel.muteEventObserver != null) {
                                    TrackKind trackKind = field.equals("audio.status")
                                                          ? TrackKind.AUDIO : TrackKind.VIDEO;
                                    boolean active = updateInfo.getString("value").equals("active");
                                    pcChannel.muteEventObserver.onStatusUpdated(trackKind, active);
                                }
                            }
                        }
                    } else if (field.equals("activeInput")) {
                        for (RemoteStream remoteStream : conferenceInfo.remoteStreams) {
                            if (remoteStream.id().equals(id)) {
                                ((RemoteMixedStream) remoteStream).updateActiveInput(
                                        updateInfo.getString("value"));
                            }
                        }
                    }
                } catch (JSONException e) {
                    DCHECK(e);
                }
            }
        });
    }

    @Override
    public void onParticipantJoined(final JSONObject participantInfo) {
        callbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Participant participant = new Participant(participantInfo);
                    conferenceInfo.participants.add(participant);
                    for (ConferenceClientObserver observer : observers) {
                        observer.onParticipantJoined(participant);
                    }
                } catch (JSONException e) {
                    DCHECK(false);
                }
            }
        });
    }

    @Override
    public void onParticipantLeft(final String participantId) {
        callbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                for (Participant participant : conferenceInfo.participants) {
                    if (participant.id.equals(participantId)) {
                        participant.onLeft();
                        conferenceInfo.participants.remove(participant);
                        break;
                    }
                }
            }
        });
    }

    //PeerConnectionChannelObserver
    @Override
    public void onIceCandidate(final String id, final IceCandidate candidate) {
        signalingExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject candidateObj = new JSONObject();
                    candidateObj.put("sdpMLineIndex", candidate.sdpMLineIndex);
                    candidateObj.put("sdpMid", candidate.sdpMid);
                    candidateObj.put("candidate",
                                     candidate.sdp.indexOf("a=") == 0 ? candidate.sdp
                                                                      : "a=" + candidate.sdp);

                    JSONObject candidateMsg = new JSONObject();
                    candidateMsg.put("type", "candidate");
                    candidateMsg.put("candidate", candidateObj);

                    JSONObject msg = new JSONObject();
                    msg.put("id", id);
                    msg.put("signaling", candidateMsg);

                    sendSignalingMessage("soac", msg, null);
                } catch (JSONException e) {
                    DCHECK(e);
                }
            }
        });
    }

    @Override
    public void onLocalDescription(final String id, final SessionDescription localSdp) {
        signalingExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    SessionDescription sdp =
                            new SessionDescription(localSdp.type,
                                                   localSdp.description.replaceAll(
                                                           "a=ice-options:google-ice\r\n", ""));
                    JSONObject sdpObj = new JSONObject();
                    sdpObj.put("type", sdp.type.toString().toLowerCase(Locale.US));
                    sdpObj.put("sdp", sdp.description);

                    JSONObject msg = new JSONObject();
                    msg.put("id", id);
                    msg.put("signaling", sdpObj);

                    sendSignalingMessage("soac", msg, null);
                } catch (JSONException e) {
                    DCHECK(e);
                }

            }
        });
    }

    @Override
    public void onError(final String id, final String errorMsg) {
        if (pcChannels.containsKey(id)) {
            pcChannels.get(id).dispose();
            pcChannels.remove(id);
        }
        callbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (pubCallbacks.containsKey(id)) {
                    triggerCallback(pubCallbacks.get(id), new IcsError(0, errorMsg));
                    pubCallbacks.remove(id);
                }
                if (subCallbacks.containsKey(id)) {
                    triggerCallback(subCallbacks.get(id), new IcsError(0, errorMsg));
                    subCallbacks.remove(id);
                }
            }
        });
    }

    @Override
    public void onAddStream(final String key,
                            final com.intel.webrtc.base.RemoteStream remoteStream) {

    }

    @Override
    public void onDataChannelMessage(String key, String message) {

    }

    @Override
    public void onRenegotiationRequest(String key) {

    }

    ///@endcond
}
