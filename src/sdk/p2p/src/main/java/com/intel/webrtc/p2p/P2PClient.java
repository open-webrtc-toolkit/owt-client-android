/*
 * Intel License Header Holder
 */
package com.intel.webrtc.p2p;

import android.util.Log;

import com.intel.webrtc.base.ActionCallback;
import com.intel.webrtc.base.IcsConst;
import com.intel.webrtc.base.IcsError;
import com.intel.webrtc.base.LocalStream;
import com.intel.webrtc.base.PeerConnectionChannel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.RTCStatsCollectorCallback;
import org.webrtc.RTCStatsReport;
import org.webrtc.SessionDescription;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.intel.webrtc.p2p.P2PPeerConnectionChannel.CallbackInfo;

import static com.intel.webrtc.base.CheckCondition.DCHECK;
import static com.intel.webrtc.base.CheckCondition.RCHECK;
import static com.intel.webrtc.base.Stream.StreamSourceInfo;
import static com.intel.webrtc.base.Stream.StreamSourceInfo.AudioSourceInfo;
import static com.intel.webrtc.base.Stream.StreamSourceInfo.VideoSourceInfo;
import static com.intel.webrtc.p2p.IcsP2PError.P2P_CLIENT_ILLEGAL_ARGUMENT;
import static com.intel.webrtc.p2p.IcsP2PError.P2P_CLIENT_INVALID_STATE;
import static com.intel.webrtc.p2p.IcsP2PError.P2P_WEBRTC_SDP;
import static com.intel.webrtc.p2p.P2PClient.SignalingMessageType.CHAT_CLOSED;
import static com.intel.webrtc.p2p.P2PClient.SignalingMessageType.CHAT_DATA_ACK;
import static com.intel.webrtc.p2p.P2PClient.SignalingMessageType.CHAT_UA;
import static com.intel.webrtc.p2p.P2PClient.SignalingMessageType.NEGOTIATION_REQUEST;
import static com.intel.webrtc.p2p.P2PClient.SignalingMessageType.SIGNALING_MESSAGE;
import static com.intel.webrtc.p2p.P2PClient.SignalingMessageType.STREAM_INFO;
import static com.intel.webrtc.p2p.P2PClient.SignalingMessageType.TRACK_ADD_ACK;
import static com.intel.webrtc.p2p.P2PClient.SignalingMessageType.TRACK_INFO;
import static org.webrtc.PeerConnection.SignalingState.HAVE_LOCAL_OFFER;

/**
 * P2PClient handles PeerConnection interactions between clients.
 */
public final class P2PClient implements PeerConnectionChannel.PeerConnectionChannelObserver,
                                        SignalingChannelInterface.SignalingChannelObserver {

    /**
     * Interface for observing client events.
     */
    public interface P2PClientObserver {
        /**
         * Called upon server disconnected.
         */
        void onServerDisconnected();

        /**
         * Called upon a RemoteStream gets added to the conference.
         *
         * @param remoteStream RemoteStream added.
         */
        void onStreamAdded(RemoteStream remoteStream);

        /**
         * Called upon receiving a message.
         *
         * @param peerId  id of the message sender.
         * @param message message received.
         */
        void onDataReceived(String peerId, String message);
    }

    private enum ServerConnectionStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    enum SignalingMessageType {
        NEGOTIATION_REQUEST("chat-negotiation-needed"),
        SIGNALING_MESSAGE("chat-signal"),
        TRACK_ADD_ACK("chat-tracks-added"),
        TRACK_INFO("chat-track-sources"),
        STREAM_INFO("chat-stream-info"),
        CHAT_UA("chat-ua"),
        CHAT_DATA_ACK("chat-data-received"),
        CHAT_CLOSED("chat-closed"),
        INVALID_TYPE("");

        String type;

        SignalingMessageType(String type) {
            this.type = type;
        }

        static SignalingMessageType get(String type) {
            switch (type) {
                case "chat-negotiation-needed":
                    return NEGOTIATION_REQUEST;
                case "chat-signal":
                    return SIGNALING_MESSAGE;
                case "chat-tracks-added":
                    return TRACK_ADD_ACK;
                case "chat-track-sources":
                    return TRACK_INFO;
                case "chat-stream-info":
                    return STREAM_INFO;
                case "chat-ua":
                    return CHAT_UA;
                case "chat-data-received":
                    return CHAT_DATA_ACK;
                case "chat-closed":
                     //TODO: remove 'chat-denied' on all platforms
                case "chat-denied":
                    return CHAT_CLOSED;
                default:
                    return INVALID_TYPE;
            }
        }
    }

    private final String TAG = "ICS";
    private String id;
    private final P2PClientConfiguration configuration;
    private SignalingChannelInterface signalingChannel;
    private final List<P2PClientObserver> observers;
    private final HashSet<String> allowedRemotePeers;
    private final ConcurrentHashMap<String, P2PPeerConnectionChannel> pcChannels;
    private final ConcurrentHashMap<String, String> streamInfos;
    private ServerConnectionStatus serverConnectionStatus;
    private final Object statusLock = new Object();
    private ExecutorService callbackExecutor;
    private ExecutorService signalingExecutor;

    /**
     * Constructor for P2PClient.
     *
     * @param configuration    P2PClientConfiguration for P2PClient.
     * @param signalingChannel SignalingChannelInterface that P2PClient replied on for sending
     *                         and receiving data.
     */
    public P2PClient(P2PClientConfiguration configuration,
                     SignalingChannelInterface signalingChannel) {
        RCHECK(signalingChannel);
        this.configuration = configuration;
        this.signalingChannel = signalingChannel;
        signalingChannel.addObserver(this);
        observers = Collections.synchronizedList(new ArrayList<P2PClientObserver>());
        allowedRemotePeers = new HashSet<>();
        pcChannels = new ConcurrentHashMap<>();
        streamInfos = new ConcurrentHashMap<>();
        serverConnectionStatus = ServerConnectionStatus.DISCONNECTED;
        callbackExecutor = Executors.newSingleThreadExecutor();
        signalingExecutor = Executors.newSingleThreadExecutor();
    }

    /**
     * Add a P2PClientObserver.
     *
     * @param observer P2PClientObserver to be added.
     */
    public void addObserver(P2PClientObserver observer) {
        RCHECK(observer);
        observers.add(observer);
    }

    /**
     * Remove a P2PClientObserver.
     *
     * @param observer P2PClientObserver to be removed.
     */
    public void removeObserver(P2PClientObserver observer) {
        RCHECK(observer);
        observers.remove(observer);
    }

    /**
     * Get the id of the P2PClient.
     *
     * @return id of P2PClient.
     */
    public String id() {
        if (id == null) {
            Log.d(TAG, "P2PClient hasn't connected to server, no id yet");
        }
        return id;
    }

    /**
     * Add an id of remote P2PClient that allowed to interact with.
     * Only a remote P2PClient whose id has been added by this method will be able to interact
     * with this P2PClient.
     *
     * @param peerId id of P2PClient to be allowed.
     */
    public void addAllowedRemotePeer(String peerId) {
        allowedRemotePeers.add(peerId);
    }

    /**
     * Remove an id of remote P2PClient from allowing to interact with.
     *
     * @param peerId id of P2PClient to be removed.
     */
    public void removeAllowedRemotePeer(String peerId) {
        allowedRemotePeers.remove(peerId);
    }

    /**
     * Connect to signaling server. Since signaling channel can be customized, this method does not
     * define how a token should look like. Token will be passed into SignalingChannelInterface
     * implemented by the app without any changes.
     *
     * @param token    token information for connecting to the signaling server.
     * @param callback ActionCallback.onSuccess will be invoked with the id when succeeds to connect
     *                 the signaling server. Otherwise when fails to do so, ActionCallback
     *                 .onFailure will be invoked with the corresponding IcsError.
     */
    public void connect(final String token, final ActionCallback<String> callback) {
        if (!checkConnectionStatus(ServerConnectionStatus.DISCONNECTED)) {
            triggerCallback(callback, new IcsError(P2P_CLIENT_INVALID_STATE.value,
                                                   "Wrong server connection status."));
            return;
        }
        DCHECK(signalingChannel);
        changeConnectionStatus(ServerConnectionStatus.CONNECTING);
        signalingChannel.connect(token, new ActionCallback<String>() {
            @Override
            public void onSuccess(String result) {
                try {
                    JSONObject resultObj = new JSONObject(result);
                    id = resultObj.getString("uid");
                } catch (JSONException e) {
                    RCHECK(e);
                }
                changeConnectionStatus(ServerConnectionStatus.CONNECTED);
                triggerCallback(callback, result);
            }

            @Override
            public void onFailure(IcsError error) {
                changeConnectionStatus(ServerConnectionStatus.DISCONNECTED);
                triggerCallback(callback, error);
            }
        });
    }

    /**
     * Disconnect from the signaling server. This will stop all current active sessions with
     * other P2PClients.
     */
    public void disconnect() {
        if (checkConnectionStatus(ServerConnectionStatus.DISCONNECTED)) {
            return;
        }
        DCHECK(signalingChannel);
        signalingChannel.disconnect();
    }

    /**
     * Publish a LocalStream to remote P2PClient.
     *
     * @param peerId      id of remote P2PClient.
     * @param localStream LocalStream to be published.
     * @param callback    ActionCallback.onSuccess will be invoked with the Publication when
     *                    succeeds to publish the LocalStream. Otherwise when fails to do so,
     *                    ActionCallback.onFailure will be invoked with the corresponding IcsError.
     */
    public void publish(final String peerId, final LocalStream localStream,
                        final ActionCallback<Publication> callback) {
        if (!checkConnectionStatus(ServerConnectionStatus.CONNECTED)) {
            triggerCallback(callback, new IcsError(P2P_CLIENT_INVALID_STATE.value,
                                                   "Wrong server connection status."));
            return;
        }
        if (!checkPermission(peerId, callback)) {
            return;
        }

        RCHECK(localStream);

        if (!pcChannels.containsKey(peerId)) {
            sendStop(peerId);
            sendUserInfo(peerId);
        }

        final P2PPeerConnectionChannel pcChannel = getPeerConnection(peerId);
        sendStreamInfo(peerId, localStream, new ActionCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                pcChannel.publish(localStream, callback);
            }

            @Override
            public void onFailure(IcsError error) {
                pcChannels.get(peerId).dispose();
                pcChannels.remove(peerId);
                triggerCallback(callback, error);
            }
        });
    }

    /**
     * Clean all resources associated with given remote endpoint. It may include
     * RTCPeerConnection, RTCRtpTransceiver and RTCDataChannel. It still possible to publish a
     * stream, or send a message to given remote P2PClient after stop.
     *
     * @param peerId id of remote P2PClient.
     */
    public void stop(String peerId) {
        if (!checkConnectionStatus(ServerConnectionStatus.CONNECTED)) {
            return;
        }
        RCHECK(peerId);
        if (!pcChannels.containsKey(peerId)) {
            return;
        }
        sendSignalingMessage(peerId, CHAT_CLOSED, null, null);
        pcChannels.get(peerId).dispose();
        pcChannels.remove(peerId);
    }

    /**
     * Get the PeerConnection stats.
     *
     * @param peerId   id of remote P2PClient.
     * @param callback ActionCallback.onSuccess will be invoked with RTCStatsReport when succeeds
     *                 to get the stats. Otherwise when fails to do so, ActionCallback.onFailure
     *                 will be invoked with the corresponding IcsError.
     */
    public void getStats(String peerId, final ActionCallback<RTCStatsReport> callback) {
        RCHECK(peerId);
        if (!pcChannels.containsKey(peerId)) {
            triggerCallback(callback, new IcsError(P2P_CLIENT_INVALID_STATE.value,
                                                   "No peerconnection established yet."));
            return;
        }
        P2PPeerConnectionChannel pcChannel = pcChannels.get(peerId);
        pcChannel.getConnectionStats(new RTCStatsCollectorCallback() {
            @Override
            public void onStatsDelivered(RTCStatsReport rtcStatsReport) {
                triggerCallback(callback, rtcStatsReport);
            }
        });
    }

    /**
     * Send a text message to a remote P2PClient.
     *
     * @param peerId   id of remote P2PClient.
     * @param message  message to be sent.
     * @param callback ActionCallback.onSuccess will be invoked succeeds to send the message.
     *                 Otherwise when fails to do so, ActionCallback.onFailure will be invoked
     *                 with the corresponding IcsError.
     */
    public void send(String peerId, String message, ActionCallback<Void> callback) {
        if (!checkConnectionStatus(ServerConnectionStatus.CONNECTED)) {
            triggerCallback(callback, new IcsError(P2P_CLIENT_INVALID_STATE.value,
                                                   "Wrong server connection status."));
            return;
        }
        if (!checkPermission(peerId, callback)) {
            return;
        }
        RCHECK(message);
        if (message.length() > 0xFFFF) {
            triggerCallback(callback,
                            new IcsError(P2P_CLIENT_ILLEGAL_ARGUMENT.value, "Message too long."));
            return;
        }
        if (!pcChannels.containsKey(peerId)) {
            sendStop(peerId);
            sendUserInfo(peerId);
        }
        P2PPeerConnectionChannel pcChannel = getPeerConnection(peerId);
        pcChannel.sendData(message, callback);
    }

    private void permissionDenied(String peerId) {
        if (pcChannels.containsKey(peerId)) {
            pcChannels.get(peerId).dispose();
            pcChannels.remove(peerId);
        }

        JSONObject errorMsg = new JSONObject();
        try {
            errorMsg.put("message", "Denied");
            errorMsg.put("code", 0);
        } catch (JSONException e) {
            DCHECK(e);
        }
        sendSignalingMessage(peerId, CHAT_CLOSED, errorMsg, null);
    }

    private void closeInternal() {
        for (String key : pcChannels.keySet()) {
            pcChannels.get(key).dispose();
        }
        pcChannels.clear();
        streamInfos.clear();
    }

    private void changeConnectionStatus(ServerConnectionStatus newStatus) {
        synchronized (statusLock) {
            serverConnectionStatus = newStatus;
        }
    }

    private boolean checkConnectionStatus(ServerConnectionStatus statusToCheck) {
        synchronized (statusLock) {
            return serverConnectionStatus == statusToCheck;
        }
    }

    private <T> void triggerCallback(final ActionCallback<T> callback, final T result) {
        DCHECK(callbackExecutor);
        if (callback == null) {
            return;
        }
        callbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                callback.onSuccess(result);
            }
        });
    }

    private <T> void triggerCallback(final ActionCallback<T> callback, final IcsError e) {
        DCHECK(callbackExecutor);
        if (callback == null) {
            return;
        }
        callbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                callback.onFailure(e);
            }
        });
    }

    private P2PPeerConnectionChannel getPeerConnection(String peerId) {
        DCHECK(pcChannels);
        if (pcChannels.containsKey(peerId)) {
            return pcChannels.get(peerId);
        }
        P2PPeerConnectionChannel pcChannel = new P2PPeerConnectionChannel(peerId, configuration,
                                                                          this);
        pcChannels.put(peerId, pcChannel);
        return pcChannel;
    }

    private <T> boolean checkPermission(String peerId, ActionCallback<T> callback) {
        if (!allowedRemotePeers.contains(peerId)) {
            triggerCallback(callback,
                            new IcsError(P2P_CLIENT_ILLEGAL_ARGUMENT.value, "Not allowed."));
            return false;
        }
        return true;
    }

    private void sendUserInfo(String peerId) {
        try {
            sendSignalingMessage(peerId, CHAT_UA, new JSONObject(IcsConst.userAgent), null);
        } catch (JSONException e) {
            DCHECK(e);
        }
    }

    private void sendStop(String peerId) {
        sendSignalingMessage(peerId, CHAT_CLOSED, null, null);
    }

    private void sendStreamInfo(final String peerId, final LocalStream localStream,
                                ActionCallback<Void> callback) {
        try {
            JSONArray tracks = new JSONArray();
            JSONArray trackIds = new JSONArray();
            JSONObject sourceInfo = new JSONObject();
            if (localStream.hasAudio()) {
                JSONObject audioTrack = new JSONObject();
                audioTrack.put("id", localStream.audioTrackId());
                audioTrack.put("source", localStream.getStreamSourceInfo().audioSourceInfo.type);
                tracks.put(audioTrack);
                trackIds.put(localStream.audioTrackId());
                sourceInfo.put("audio", localStream.getStreamSourceInfo().audioSourceInfo.type);
            }

            if (localStream.hasVideo()) {
                JSONObject videoTrack = new JSONObject();
                videoTrack.put("id", localStream.videoTrackId());
                videoTrack.put("source", localStream.getStreamSourceInfo().videoSourceInfo.type);
                tracks.put(videoTrack);
                trackIds.put(localStream.videoTrackId());
                sourceInfo.put("video", localStream.getStreamSourceInfo().videoSourceInfo.type);
            }

            sendSignalingMessage(peerId, TRACK_INFO, tracks, callback);

            JSONObject streamInfo = new JSONObject();
            streamInfo.put("id", localStream.id());
            streamInfo.put("tracks", trackIds);
            streamInfo.put("source", sourceInfo);
            streamInfo.put("attributes", localStream.getAttributes());

            sendSignalingMessage(peerId, STREAM_INFO, streamInfo, null);
        } catch (JSONException e) {
            DCHECK(e);
        }

    }

    // message here only accepts JSONObject and JSONArray objects.
    private void sendSignalingMessage(final String peerId, final SignalingMessageType type,
                                      final Object message, final ActionCallback<Void> callback) {
        DCHECK(signalingExecutor);
        DCHECK(signalingChannel);
        signalingExecutor.execute(new Runnable() {
            @Override
            public void run() {

                try {
                    JSONObject messageObject = new JSONObject();
                    messageObject.put("type", type.type);
                    messageObject.put("data", message);

                    signalingChannel.sendMessage(peerId, messageObject.toString(),
                                                 new ActionCallback<Void>() {
                                                     @Override
                                                     public void onSuccess(Void result) {
                                                         if (callback != null) {
                                                             callback.onSuccess(null);
                                                         }
                                                     }

                                                     @Override
                                                     public void onFailure(IcsError error) {
                                                         if (callback != null) {
                                                             callback.onFailure(error);
                                                         }
                                                     }
                                                 });
                } catch (JSONException e) {
                    triggerCallback(callback, new IcsError(P2P_CLIENT_ILLEGAL_ARGUMENT.value,
                                                           e.getMessage()));
                }

            }
        });
    }

    private void sendTrackAck(String peerId, RemoteStream remoteStream) {
        JSONArray tracks = new JSONArray();
        if (remoteStream.hasAudio()) {
            tracks.put(remoteStream.audioTrackId());
        }
        if (remoteStream.hasVideo()) {
            tracks.put(remoteStream.videoTrackId());
        }

        sendSignalingMessage(peerId, TRACK_ADD_ACK, tracks, null);
    }

    private void processSignalingMsg(String peerId, JSONObject message) {
        try {
            if (pcChannels.containsKey(peerId)
                    && message.getString("type").equals("offer")
                    && pcChannels.get(peerId).getSignalingState() == HAVE_LOCAL_OFFER) {
                if (id.compareTo(peerId) > 0) {
                    // Let the remote side be the publisher
                    P2PPeerConnectionChannel oldChannel = getPeerConnection(peerId);

                    LocalStream localStream = null;
                    ActionCallback<Publication> callback = null;
                    // As this situation will happen only at the initial phase of a pc,
                    // so iterate the lists below will only get one instance of each kind.
                    for (LocalStream ls : oldChannel.localStreams.values()) {
                        localStream = ls;
                    }
                    for (CallbackInfo cbi : oldChannel.publishCallbacks.values()) {
                        callback = cbi.callback;
                    }

                    oldChannel.dispose();
                    pcChannels.remove(peerId);
                    DCHECK(!pcChannels.containsKey(peerId));
                    P2PPeerConnectionChannel newChannel = getPeerConnection(peerId);
                    newChannel.processSignalingMessage(message);
                    newChannel.publish(localStream, callback);
                }
            } else {
                getPeerConnection(peerId).processSignalingMessage(message);
            }
        } catch (JSONException e) {
            DCHECK(e);
        }
    }

    ///@cond
    //PeerConnectionChannelObserver
    @Override
    public void onIceCandidate(String peerId, IceCandidate candidate) {
        try {
            JSONObject candidateObject = new JSONObject();
            candidateObject.put("type", "candidates");
            candidateObject.put("candidate", candidate.sdp);
            candidateObject.put("sdpMLineIndex", candidate.sdpMLineIndex);
            candidateObject.put("sdpMid", candidate.sdpMid);

            sendSignalingMessage(peerId, SIGNALING_MESSAGE, candidateObject, null);
        } catch (JSONException e) {
            DCHECK(e);
        }
    }

    @Override
    public void onLocalDescription(final String peerId, SessionDescription localSdp) {
        try {
            JSONObject sdpObject = new JSONObject();
            sdpObject.put("type", localSdp.type.canonicalForm());
            sdpObject.put("sdp", localSdp.description);

            sendSignalingMessage(peerId, SIGNALING_MESSAGE, sdpObject, new ActionCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    //succeed to send sdp, no action needed here.
                }

                @Override
                public void onFailure(IcsError error) {
                    //failed to send sdp, trigger callbacks.
                    getPeerConnection(peerId).processError(error);
                    getPeerConnection(peerId).dispose();
                    pcChannels.remove(peerId);
                }
            });
        } catch (JSONException e) {
            DCHECK(e);
        }
    }

    @Override
    public void onError(String peerId, String error) {
        if (pcChannels.containsKey(peerId)) {
            pcChannels.get(peerId).dispose();
            pcChannels.remove(peerId);
        }

        JSONObject errorMsg = new JSONObject();
        try {
            errorMsg.put("code", P2P_WEBRTC_SDP.value);
            errorMsg.put("message", error);
        } catch (JSONException e) {
            DCHECK(e);
        }
        sendSignalingMessage(peerId, CHAT_CLOSED, errorMsg, null);
    }

    @Override
    public void onAddStream(final String peerId,
                            final com.intel.webrtc.base.RemoteStream remoteStream) {
        DCHECK(callbackExecutor);
        DCHECK(pcChannels.containsKey(peerId));

        String videoSource = null;
        String audioSource = null;
        if (remoteStream.hasVideo()) {
            String id = remoteStream.videoTrackId();
            videoSource = streamInfos.get(id);
            streamInfos.remove(id);
        }
        if (remoteStream.hasAudio()) {
            String id = remoteStream.audioTrackId();
            audioSource = streamInfos.get(id);
            streamInfos.remove(id);
        }
        StreamSourceInfo streamSourceInfo = new StreamSourceInfo(
                videoSource == null ? null : VideoSourceInfo.get(videoSource),
                audioSource == null ? null : AudioSourceInfo.get(audioSource));

        ((RemoteStream) remoteStream).setInfo(streamSourceInfo);

        callbackExecutor.execute(new Runnable() {
            @Override
            public void run() {

                synchronized (observers) {
                    for (P2PClientObserver observer : observers) {
                        observer.onStreamAdded((RemoteStream) remoteStream);
                    }
                }
            }
        });

        sendTrackAck(peerId, (RemoteStream) remoteStream);
    }

    @Override
    public void onDataChannelMessage(final String peerId, final String message) {
        DCHECK(callbackExecutor);
        DCHECK(pcChannels.containsKey(peerId));

        try {
            JSONObject msg = new JSONObject(message);
            Long msgId = msg.getLong("id");
            final String msgData = msg.getString("data");
            callbackExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    for (P2PClientObserver observer : observers) {
                        observer.onDataReceived(peerId, msgData);
                    }
                }
            });

            sendSignalingMessage(peerId, CHAT_DATA_ACK, msgId, null);
        } catch (JSONException e) {
            DCHECK(e);
        }
    }

    @Override
    public void onRenegotiationRequest(String peerId) {
        sendSignalingMessage(peerId, NEGOTIATION_REQUEST, null, null);
    }

    //SignalingChannelObserver
    @Override
    public void onMessage(String peerId, String message) {
        try {
            JSONObject messageObject = new JSONObject(message);
            SignalingMessageType messageType =
                    SignalingMessageType.get(messageObject.getString("type"));

            if (!checkPermission(peerId, null) && messageType != CHAT_CLOSED) {
                permissionDenied(peerId);
                return;
            }

            switch (messageType) {
                case SIGNALING_MESSAGE:
                    processSignalingMsg(peerId, messageObject.getJSONObject("data"));
                    break;
                case TRACK_INFO:
                    JSONArray data = messageObject.getJSONArray("data");
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject trackInfo = data.getJSONObject(i);
                        streamInfos.put(trackInfo.getString("id"), trackInfo.getString("source"));
                    }
                    break;
                case TRACK_ADD_ACK:
                    if (pcChannels.containsKey(peerId)) {
                        getPeerConnection(peerId)
                                .processTrackAck(messageObject.getJSONArray("data"));
                    }
                    break;
                case NEGOTIATION_REQUEST:
                    if (pcChannels.containsKey(peerId)) {
                        getPeerConnection(peerId).processNegotiationRequest();
                    }
                    break;
                case CHAT_UA:
                    if (!pcChannels.containsKey(peerId)) {
                        sendUserInfo(peerId);
                    }
                    getPeerConnection(peerId).processUserInfo(messageObject.getJSONObject("data"));
                    break;
                case CHAT_CLOSED:
                    if (pcChannels.containsKey(peerId)) {
                        JSONObject dataObj;
                        int code = 0;
                        String error = null;
                        if (messageObject.has("data")) {
                            dataObj = new JSONObject(messageObject.getString("data"));
                            code = dataObj.has("code") ? dataObj.getInt("code") : 0;
                            error = dataObj.has("message") ? dataObj.getString("message") : "";
                        }
                        getPeerConnection(peerId).processError(new IcsError(code, error));
                        getPeerConnection(peerId).dispose();
                        pcChannels.remove(peerId);
                    }
                    break;
                case CHAT_DATA_ACK:
                    if (pcChannels.containsKey(peerId)) {
                        getPeerConnection(peerId).processDataAck(messageObject.getLong("data"));
                    }
                    break;
            }

        } catch (JSONException e) {
            DCHECK(e);
        }
    }

    @Override
    public void onServerDisconnected() {
        DCHECK(callbackExecutor);
        changeConnectionStatus(ServerConnectionStatus.DISCONNECTED);
        closeInternal();
        callbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                synchronized (observers) {
                    for (P2PClientObserver observer : observers) {
                        observer.onServerDisconnected();
                    }
                }
            }
        });
    }

    ///@endcond
}
