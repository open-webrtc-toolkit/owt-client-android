/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.conference;

import static owt.base.CheckCondition.DCHECK;
import static owt.base.CheckCondition.RCHECK;
import static owt.base.Const.LOG_TAG;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
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
import owt.base.ActionCallback;
import owt.base.LocalStream;
import owt.base.MediaConstraints.TrackKind;
import owt.base.OwtError;
import owt.base.PeerConnectionChannel;

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
         * @param message message received.
         * @param from id of the message sender.
         * @param to receiver of this message, values: 'all', 'me'.
         */
        void onMessageReceived(String message, String from, String to);

        /**
         * Called upon server disconnected.
         */
        void onServerDisconnected();
    }

    // All callbacks need to be triggered on |callbackExecutor|.
    private final ExecutorService callbackExecutor = Executors.newSingleThreadExecutor();
    // key: publication/subscription id.
    private final ConcurrentHashMap<String, ConferencePeerConnectionChannel> pcChannels;
    // key: subscription id.
    private final ConcurrentHashMap<String, ActionCallback<Subscription>> subCallbacks;
    // key: publication id.
    private final ConcurrentHashMap<String, ActionCallback<Publication>> pubCallbacks;
    private ActionCallback<ConferenceInfo> joinCallback;
    private final ConferenceClientConfiguration configuration;
    private final List<ConferenceClientObserver> observers;
    // signalingChannel will be created upon join() and will be destructed upon leave().
    private SignalingChannel signalingChannel;
    private ConferenceInfo conferenceInfo;
    private final Object infoLock = new Object();
    private RoomStates roomStates;
    private final Object statesLock = new Object();

    /**
     * Constructor for ConferenceClient.
     *
     * @param configuration ConferenceClientConfiguration for ConferenceClient
     */
    public ConferenceClient(ConferenceClientConfiguration configuration) {
        DCHECK(configuration);
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
        if (observers.contains(observer)) {
            Log.w(LOG_TAG, "Skipped adding a duplicated observer.");
            return;
        }
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
        // ConferenceInfo is immutable for API users, so it is safe here just to return
        // |conferenceInfo|.
        synchronized (infoLock) {
            return conferenceInfo;
        }
    }

    /**
     * Join a conference specified by |token|.
     *
     * @param token token issued by conference server (nuve).
     * @param callback ActionCallback.onSuccess will be invoked with the ConferenceInfo when
     * succeeds to join the conference room. Otherwise when fails to do so, ActionCallback
     * .onFailure will be invoked with the corresponding OwtError.
     */
    public synchronized void join(String token, ActionCallback<ConferenceInfo> callback) {
        if (!checkRoomStatus(RoomStates.DISCONNECTED)) {
            triggerCallback(callback, new OwtError("Wrong room status."));
            return;
        }
        DCHECK(signalingChannel == null);
        DCHECK(joinCallback == null);
        this.joinCallback = callback;
        signalingChannel = new SignalingChannel(token, this);
        Log.d(LOG_TAG, "Connecting to the conference room.");
        changeRoomStatus(RoomStates.CONNECTING);
        signalingChannel.connect(configuration);
    }

    /**
     * Leave the conference.
     */
    public synchronized void leave() {
        if (checkRoomStatus(RoomStates.DISCONNECTED)) {
            Log.w(LOG_TAG, "Wrong room status when leave.");
            return;
        }
        sendSignalingMessage("logout", null, args -> {
            // Only care about the result in debug mode.
            DCHECK(extractMsg(0, args).equals("ok"));
            signalingChannel.disconnect();
        });
    }

    /**
     * Publish a LocalStream to the conference.
     *
     * @param localStream LocalStream to be published.
     * @param callback ActionCallback.onSuccess will be invoked with the Publication when
     * succeeds to publish the LocalStream. Otherwise when fails to do so, ActionCallback
     * .onFailure will be invoked with the corresponding OwtError.
     */
    public void publish(LocalStream localStream, ActionCallback<Publication> callback) {
        publish(localStream, null, callback);
    }

    /**
     * Publish a LocalStream to the conference.
     *
     * @param localStream LocalStream to be published.
     * @param options PublishOptions for publishing this LocalStream.
     * @param callback ActionCallback.onSuccess will be invoked with the Publication when
     * succeeds to publish the LocalStream. Otherwise when fails to do so, ActionCallback
     * .onFailure will be invoked with the corresponding OwtError.
     */
    public synchronized void publish(final LocalStream localStream, final PublishOptions options,
            final ActionCallback<Publication> callback) {
        RCHECK(localStream);
        if (!checkRoomStatus(RoomStates.CONNECTED)) {
            triggerCallback(callback, new OwtError("Wrong room status."));
            return;
        }
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

            sendSignalingMessage("publish", publishMsg, args -> {
                if (extractMsg(0, args).equals("ok")) {
                    try {
                        JSONObject result = (JSONObject) args[1];
                        // Do not receive video and audio for publication cpcc.
                        ConferencePeerConnectionChannel pcChannel =
                                getPeerConnection(result.getString("id"), false, false);
                        if (callback != null) {
                            pubCallbacks.put(result.getString("id"), callback);
                        }
                        pcChannel.publish(localStream, options);
                    } catch (JSONException e) {
                        DCHECK(e);
                    }
                } else {
                    triggerCallback(callback, new OwtError(extractMsg(1, args)));
                }
            });

        } catch (JSONException e) {
            DCHECK(e);
        }
    }

    // Not a public API.
    synchronized void unpublish(final String publicationId, final Publication publication) {
        DCHECK(publicationId);
        DCHECK(publication);
        if (!checkRoomStatus(RoomStates.CONNECTED)) {
            Log.w(LOG_TAG, "Wrong room status when unpublish.");
            return;
        }
        try {
            JSONObject unpubMsg = new JSONObject();
            unpubMsg.put("id", publicationId);

            sendSignalingMessage("unpublish", unpubMsg, args -> {
                // Clean resources associated with this publication regardless of the result from
                // MCU. But we monitor the result in debug mode.
                DCHECK(extractMsg(0, args).equals("ok"));
                if (pcChannels.containsKey(publicationId)) {
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
     * @param callback ActionCallback.onSuccess will be invoked with the Subscription when
     * succeeds to subscribe the RemoteStream. Otherwise when fails to do so, ActionCallback
     * .onFailure will be invoked with the corresponding OwtError.
     */
    public void subscribe(RemoteStream remoteStream, ActionCallback<Subscription> callback) {
        subscribe(remoteStream, null, callback);
    }

    /**
     * Subscribe a RemoteStream from the conference.
     *
     * @param remoteStream RemoteStream to be subscribed.
     * @param options SubscribeOptions for subscribing the RemoteStream.
     * @param callback ActionCallback.onSuccess will be invoked with the Subscription when
     * succeeds to subscribe the RemoteStream. Otherwise when fails to do so, ActionCallback
     * .onFailure will be invoked with the corresponding OwtError.
     */
    public synchronized void subscribe(final RemoteStream remoteStream,
            final SubscribeOptions options, final ActionCallback<Subscription> callback) {
        RCHECK(remoteStream);
        if (!checkRoomStatus(RoomStates.CONNECTED)) {
            triggerCallback(callback, new OwtError("Wrong room status."));
            return;
        }

        final boolean subVideo = options == null || options.videoOption != null;
        final boolean subAudio = options == null || options.audioOption != null;

        try {
            JSONObject media = new JSONObject();
            if (subVideo) {
                JSONObject video = new JSONObject();
                video.put("from", remoteStream.id());
                if (options != null) {
                    video.put("parameters", options.videoOption.generateOptionsMsg());
                    if (options.videoOption.rid != null) {
                        video.put("simulcastRid", options.videoOption.rid);
                    }
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

            sendSignalingMessage("subscribe", subscribeMsg, args -> {
                if (extractMsg(0, args).equals("ok")) {
                    for (ConferencePeerConnectionChannel pcChannel : pcChannels.values()) {
                        if (pcChannel.stream.id().equals(remoteStream.id())) {
                            triggerCallback(callback,
                                    new OwtError("Remote stream has been subscribed."));
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
                        DCHECK(e);
                    }
                } else {
                    triggerCallback(callback, new OwtError(extractMsg(1, args)));
                }
            });

        } catch (JSONException e) {
            DCHECK(e);
        }
    }

    // Not a public API.
    synchronized void unsubscribe(final String subscriptionId, final Subscription subscription) {
        DCHECK(subscriptionId);
        DCHECK(subscription);
        if (!checkRoomStatus(RoomStates.CONNECTED)) {
            Log.w(LOG_TAG, "Wrong room status when unsubscribe.");
            return;
        }

        try {
            JSONObject unpubMsg = new JSONObject();
            unpubMsg.put("id", subscriptionId);

            sendSignalingMessage("unsubscribe", unpubMsg, args -> {
                if (pcChannels.containsKey(subscriptionId)) {
                    ConferencePeerConnectionChannel pcChannel = getPeerConnection(subscriptionId);
                    pcChannel.dispose();
                    pcChannels.remove(subscriptionId);
                    subscription.onEnded();
                }
            });

        } catch (JSONException e) {
            DCHECK(false);
        }
    }

    /**
     * Send a text message to all participants in the conference.
     *
     * @param message message to be sent.
     * @param callback ActionCallback.onSuccess will be invoked succeeds to send the message.
     * Otherwise when fails to do so, ActionCallback.onFailure will be invoked with the
     * corresponding OwtError.
     */
    public void send(String message, ActionCallback<Void> callback) {
        send(null, message, callback);
    }

    /**
     * Send a text message to a specific participant in the conference.
     *
     * @param participantId id of Participant the message to be sent to.
     * @param message message to be sent.
     * @param callback ActionCallback.onSuccess will be invoked succeeds to send the message.
     * Otherwise when fails to do so, ActionCallback.onFailure will be invoked with the
     * corresponding OwtError.
     */
    public synchronized void send(String participantId, String message,
            final ActionCallback<Void> callback) {
        RCHECK(message);
        if (!checkRoomStatus(RoomStates.CONNECTED)) {
            triggerCallback(callback, new OwtError(0, "Wrong status"));
            return;
        }

        try {
            JSONObject sendMsg = new JSONObject();
            sendMsg.put("to", participantId == null ? "all" : participantId);
            sendMsg.put("message", message);

            sendSignalingMessage("text", sendMsg, args -> {
                if (extractMsg(0, args).equals("ok")) {
                    callbackExecutor.execute(() -> {
                        if (callback != null) {
                            callback.onSuccess(null);
                        }
                    });
                } else {
                    triggerCallback(callback, new OwtError(extractMsg(1, args)));
                }
            });

        } catch (JSONException e) {
            DCHECK(false);
        }
    }

    // Not a public API.
    synchronized void getStats(String id, final ActionCallback<RTCStatsReport> callback) {
        if (!pcChannels.containsKey(id)) {
            triggerCallback(callback, new OwtError(0, "Wrong state"));
            return;
        }
        if (callback != null) {
            ConferencePeerConnectionChannel pcChannel = getPeerConnection(id);
            pcChannel.getConnectionStats(callback);
        }
    }

    private void closeInternal() {
        for (String key : pcChannels.keySet()) {
            pcChannels.get(key).dispose();
        }
        pcChannels.clear();
        subCallbacks.clear();
        pubCallbacks.clear();
        signalingChannel = null;
        joinCallback = null;
        synchronized (infoLock) {
            conferenceInfo = null;
        }
    }

    private boolean checkRoomStatus(RoomStates roomStates) {
        synchronized (statesLock) {
            return this.roomStates == roomStates;
        }
    }

    private void changeRoomStatus(RoomStates roomStates) {
        synchronized (statesLock) {
            this.roomStates = roomStates;
        }
    }

    private ConferencePeerConnectionChannel getPeerConnection(String id) {
        DCHECK(pcChannels.containsKey(id));
        return getPeerConnection(id, true/*DoesNotMatter*/, true/*DoesNotMatter*/);
    }

    private ConferencePeerConnectionChannel getPeerConnection(String id, boolean receiveVideo,
            boolean receiveAudio) {
        if (pcChannels.containsKey(id)) {
            return pcChannels.get(id);
        }
        ConferencePeerConnectionChannel pcChannel =
                new ConferencePeerConnectionChannel(id, configuration.rtcConfiguration,
                        receiveVideo, receiveAudio, this);
        pcChannels.put(id, pcChannel);
        return pcChannel;
    }

    <T> void triggerCallback(final ActionCallback<T> callback, final OwtError error) {
        DCHECK(callbackExecutor);
        if (callback == null) {
            return;
        }
        callbackExecutor.execute(() -> callback.onFailure(error));
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
        DCHECK(signalingChannel);
        if (signalingChannel != null) {
            signalingChannel.sendMsg(type, message, ack);
        }
    }

    private void processAck(final String id) {
        DCHECK(callbackExecutor);
        callbackExecutor.execute(() -> {
            if (pubCallbacks.containsKey(id)) {
                ActionCallback<Publication> callback = pubCallbacks.get(id);
                Publication publication = new Publication(id, ConferenceClient.this);
                getPeerConnection(id).publication = publication;
                callback.onSuccess(publication);
                pubCallbacks.remove(id);
                return;
            }
            if (subCallbacks.containsKey(id)) {
                ActionCallback<Subscription> callback = subCallbacks.get(id);
                Subscription subscription = new Subscription(id, ConferenceClient.this);
                getPeerConnection(id).subscription = subscription;
                callback.onSuccess(subscription);
                subCallbacks.remove(id);
            }
        });
    }

    private void processError(final String id, final String errorMsg) {
        DCHECK(callbackExecutor);
        callbackExecutor.execute(() -> {
            if (pubCallbacks.containsKey(id)) {
                ActionCallback<Publication> callback = pubCallbacks.get(id);
                triggerCallback(callback, new OwtError(errorMsg));
                pubCallbacks.remove(id);
            } else if (subCallbacks.containsKey(id)) {
                ActionCallback<Subscription> callback = subCallbacks.get(id);
                triggerCallback(callback, new OwtError(errorMsg));
                subCallbacks.remove(id);
            }
        });
    }

    ///@cond
    // SignalingChannelObserver
    @Override
    public void onRoomConnected(final JSONObject info) {
        Log.d(LOG_TAG, "Room connected.");
        DCHECK(callbackExecutor);
        changeRoomStatus(RoomStates.CONNECTED);
        callbackExecutor.execute(() -> {
            ConferenceInfo conferenceInfo;
            try {
                if (joinCallback != null) {
                    conferenceInfo = new ConferenceInfo(info);
                    synchronized (infoLock) {
                        ConferenceClient.this.conferenceInfo = conferenceInfo;
                    }
                    joinCallback.onSuccess(conferenceInfo);
                }
            } catch (JSONException e) {
                triggerCallback(joinCallback, new OwtError(e.getMessage()));
            }
            joinCallback = null;
        });
    }

    @Override
    public void onRoomConnectFailed(final String errorMsg) {
        Log.d(LOG_TAG, "Failed to connect to the conference room: " + errorMsg);
        DCHECK(callbackExecutor);
        changeRoomStatus(RoomStates.DISCONNECTED);
        signalingChannel = null;

        triggerCallback(joinCallback, new OwtError(errorMsg));
        joinCallback = null;
    }

    @Override
    public void onReconnecting() {
        // TODO: consider adding a new event for client.
    }

    @Override
    public void onRoomDisconnected() {
        Log.d(LOG_TAG, "Room disconnected.");
        DCHECK(callbackExecutor);
        changeRoomStatus(RoomStates.DISCONNECTED);
        callbackExecutor.execute(() -> {
            closeInternal();
            for (ConferenceClientObserver observer : observers) {
                observer.onServerDisconnected();
            }
        });
    }

    @Override
    public void onProgressMessage(JSONObject msg) {
        DCHECK(msg);
        try {
            // Do not check pcChannels.contain(id) here. Let is throw exception in debug mode.
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
            DCHECK(e);
        }
    }

    @Override
    public void onTextMessage(final String message, final String from, String to) {
        DCHECK(callbackExecutor);
        callbackExecutor.execute(() -> {
            for (ConferenceClientObserver observer : observers) {
                observer.onMessageReceived(message, from, to);
            }
        });
    }

    @Override
    public void onStreamAdded(final RemoteStream remoteStream) {
        DCHECK(callbackExecutor);
        callbackExecutor.execute(() -> {
            synchronized (infoLock) {
                if (conferenceInfo != null) {
                    conferenceInfo.remoteStreams.add(remoteStream);
                }
            }
            for (ConferenceClientObserver observer : observers) {
                observer.onStreamAdded(remoteStream);
            }
        });
    }

    @Override
    public void onStreamRemoved(final String streamId) {
        DCHECK(callbackExecutor);
        callbackExecutor.execute(() -> {
            synchronized (infoLock) {
                if (conferenceInfo != null) {
                    for (RemoteStream remoteStream : conferenceInfo.remoteStreams) {
                        if (remoteStream.id().equals(streamId)) {
                            conferenceInfo.remoteStreams.remove(remoteStream);
                            remoteStream.onEnded();
                            break;
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onStreamUpdated(final String id, final JSONObject updateInfo) {
        DCHECK(callbackExecutor);
        callbackExecutor.execute(() -> {
            try {
                String field = updateInfo.getString("field");
                switch (field) {
                    case "video.layout":
                        synchronized (infoLock) {
                            for (RemoteStream remoteStream : conferenceInfo.remoteStreams) {
                                if (remoteStream.id().equals(id)) {
                                    ((RemoteMixedStream) remoteStream).updateRegions(
                                            updateInfo.getJSONArray("value"));
                                }
                            }
                        }
                        break;
                    case "audio.status":
                    case "video.status":
                        for (ConferencePeerConnectionChannel pcChannel : pcChannels.values()) {
                            // For subscription id will be the RemoteStream id, for publication
                            // the id will be publication id which is pc.key.
                            if (pcChannel.stream.id().equals(id) || pcChannel.key.equals(id)) {
                                TrackKind trackKind = field.equals("audio.status")
                                        ? TrackKind.AUDIO : TrackKind.VIDEO;
                                boolean active = updateInfo.getString("value").equals("active");
                                if (pcChannel.publication != null) {
                                    pcChannel.publication.onStatusUpdated(trackKind, active);
                                } else {
                                    pcChannel.subscription.onStatusUpdated(trackKind, active);
                                }
                            }
                        }
                        break;
                    case "activeInput":
                        synchronized (infoLock) {
                            for (RemoteStream remoteStream : conferenceInfo.remoteStreams) {
                                if (remoteStream.id().equals(id)) {
                                    ((RemoteMixedStream) remoteStream).updateActiveInput(
                                            updateInfo.getString("value"));
                                }
                            }
                        }
                        break;
                    case ".":
                        for (RemoteStream remoteStream : conferenceInfo.remoteStreams) {
                            if (remoteStream.id().equals(id)) {
                                JSONObject streamInfo = updateInfo.getJSONObject("value");
                                remoteStream.updateStreamInfo(streamInfo, true);
                            }
                        }
                        break;
                }
            } catch (JSONException e) {
                DCHECK(e);
            }
        });
    }

    @Override
    public void onParticipantJoined(final JSONObject participantInfo) {
        DCHECK(callbackExecutor);
        callbackExecutor.execute(() -> {
            try {
                Participant participant = new Participant(participantInfo);
                synchronized (infoLock) {
                    if (conferenceInfo != null) {
                        conferenceInfo.participants.add(participant);
                    }
                }
                for (ConferenceClientObserver observer : observers) {
                    observer.onParticipantJoined(participant);
                }
            } catch (JSONException e) {
                DCHECK(false);
            }
        });
    }

    @Override
    public void onParticipantLeft(final String participantId) {
        DCHECK(callbackExecutor);
        callbackExecutor.execute(() -> {
            synchronized (infoLock) {
                if (conferenceInfo != null) {
                    for (Participant participant : conferenceInfo.participants) {
                        if (participant.id.equals(participantId)) {
                            conferenceInfo.participants.remove(participant);
                            participant.onLeft();
                            break;
                        }
                    }
                }
            }
        });
    }

    // PeerConnectionChannelObserver
    @Override
    public void onIceCandidate(final String id, final IceCandidate candidate) {
        try {
            JSONObject candidateObj = new JSONObject();
            candidateObj.put("sdpMLineIndex", candidate.sdpMLineIndex);
            candidateObj.put("sdpMid", candidate.sdpMid);
            candidateObj.put("candidate",
                    candidate.sdp.indexOf("a=") == 0 ? candidate.sdp : "a=" + candidate.sdp);

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

    @Override
    public void onIceCandidatesRemoved(final String id, final IceCandidate[] candidates) {
        try {
            JSONArray removedCandidates = new JSONArray();
            for (IceCandidate candidate : candidates) {
                JSONObject candidateObj = new JSONObject();
                candidateObj.put("sdpMLineIndex", candidate.sdpMLineIndex);
                candidateObj.put("sdpMid", candidate.sdpMid);
                candidateObj.put("candidate",
                        candidate.sdp.indexOf("a=") == 0 ? candidate.sdp : "a=" + candidate.sdp);
                removedCandidates.put(candidateObj);
            }

            JSONObject rmCanMsg = new JSONObject();
            rmCanMsg.put("type", "removed-candidates");
            rmCanMsg.put("candidates", removedCandidates);

            JSONObject msg = new JSONObject();
            msg.put("id", id);
            msg.put("signaling", rmCanMsg);
            sendSignalingMessage("soac", msg, null);
        } catch (JSONException e) {
            DCHECK(e);
        }
    }

    @Override
    public void onLocalDescription(final String id, final SessionDescription localSdp) {
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

    @Override
    public void onEnded(final String id) {
        if (pcChannels.containsKey(id)) {
            pcChannels.get(id).dispose();
            pcChannels.remove(id);
        }
    }

    @Override
    public void onError(final String id, final String errorMsg, boolean ignored) {
        OwtError error = new OwtError(3000, errorMsg);
        callbackExecutor.execute(() -> {
            if (pubCallbacks.containsKey(id)) {
                triggerCallback(pubCallbacks.get(id), error);
                pubCallbacks.remove(id);
            }
            if (subCallbacks.containsKey(id)) {
                triggerCallback(subCallbacks.get(id), error);
                subCallbacks.remove(id);
            }
        });
        for (ConferencePeerConnectionChannel pcChannel : pcChannels.values()) {
            if (pcChannel.key.equals(id)) {
                if (pcChannel.publication != null) {
                    pcChannel.publication.onError(error);
                } else {
                    pcChannel.subscription.onError(error);
                }
            }
        }
        onEnded(id);
    }

    @Override
    public void onAddStream(final String key,
            final owt.base.RemoteStream remoteStream) {

    }

    @Override
    public void onDataChannelMessage(String key, String message) {

    }

    @Override
    public void onRenegotiationRequest(String key) {

    }

    private enum RoomStates {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }
    ///@endcond
}
