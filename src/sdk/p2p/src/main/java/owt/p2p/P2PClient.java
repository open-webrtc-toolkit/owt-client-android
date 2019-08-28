/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.p2p;

import static org.webrtc.PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
import static org.webrtc.PeerConnection.ContinualGatheringPolicy.GATHER_ONCE;
import static org.webrtc.PeerConnection.SignalingState.HAVE_LOCAL_OFFER;

import static owt.base.CheckCondition.DCHECK;
import static owt.base.CheckCondition.RCHECK;
import static owt.base.Const.LOG_TAG;
import static owt.base.Stream.StreamSourceInfo.AudioSourceInfo;
import static owt.base.Stream.StreamSourceInfo.VideoSourceInfo;
import static owt.p2p.P2PClient.ServerConnectionStatus.CONNECTED;
import static owt.p2p.P2PClient.ServerConnectionStatus.CONNECTING;
import static owt.p2p.P2PClient.ServerConnectionStatus.DISCONNECTED;
import static owt.p2p.P2PClient.SignalingMessageType.CHAT_CLOSED;
import static owt.p2p.P2PClient.SignalingMessageType.CHAT_DATA_ACK;
import static owt.p2p.P2PClient.SignalingMessageType.CHAT_UA;
import static owt.p2p.P2PClient.SignalingMessageType.SIGNALING_MESSAGE;
import static owt.p2p.P2PClient.SignalingMessageType.STREAM_INFO;
import static owt.p2p.P2PClient.SignalingMessageType.TRACK_ADD_ACK;
import static owt.p2p.P2PClient.SignalingMessageType.TRACK_INFO;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.RTCStatsReport;
import org.webrtc.SessionDescription;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import owt.base.ActionCallback;
import owt.base.Const;
import owt.base.LocalStream;
import owt.base.OwtError;
import owt.base.PeerConnectionChannel;
import owt.base.Stream;
import owt.p2p.SignalingChannelInterface.SignalingChannelObserver;

/**
 * P2PClient handles PeerConnection interactions between clients.
 */
public final class P2PClient implements PeerConnectionChannel.PeerConnectionChannelObserver,
        SignalingChannelObserver {

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
         * @param peerId id of the message sender.
         * @param message message received.
         */
        void onDataReceived(String peerId, String message);
    }

    private final P2PClientConfiguration configuration;
    private final List<P2PClientObserver> observers;
    private final HashSet<String> allowedRemotePeers;
    // key: peer id.
    private final ConcurrentHashMap<String, P2PPeerConnectionChannel> pcChannels;
    private final Object pcChannelsLock = new Object();
    private String id;
    private SignalingChannelInterface signalingChannel;
    private ServerConnectionStatus serverConnectionStatus;
    private final Object statusLock = new Object();
    // All callbacks need to be triggered on |callbackExecutor|.
    private ExecutorService callbackExecutor;
    // All signaling works should be ran on signalingExecutor.
    private ExecutorService signalingExecutor;
    // key: stream id.
    private final ConcurrentHashMap<String, JSONObject> streamInfos;

    /**
     * Constructor for P2PClient.
     *
     * @param configuration P2PClientConfiguration for P2PClient.
     * @param signalingChannel SignalingChannelInterface that P2PClient replied on for sending
     * and receiving data.
     */
    public P2PClient(P2PClientConfiguration configuration,
            SignalingChannelInterface signalingChannel) {
        RCHECK(configuration);
        RCHECK(signalingChannel);
        this.configuration = configuration;
        this.signalingChannel = signalingChannel;
        signalingChannel.addObserver(this);
        observers = Collections.synchronizedList(new ArrayList<>());
        allowedRemotePeers = new HashSet<>();
        pcChannels = new ConcurrentHashMap<>();
        serverConnectionStatus = DISCONNECTED;
        callbackExecutor = Executors.newSingleThreadExecutor();
        signalingExecutor = Executors.newSingleThreadExecutor();
        streamInfos = new ConcurrentHashMap<>();
    }

    /**
     * Add a P2PClientObserver.
     *
     * @param observer P2PClientObserver to be added.
     */
    public void addObserver(P2PClientObserver observer) {
        RCHECK(observer);
        if (observers.contains(observer)) {
            Log.d(LOG_TAG, "Skipped adding a duplicated observer.");
            return;
        }
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
            Log.d(LOG_TAG, "P2PClient hasn't connected to server, no id yet");
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
        if (!allowedRemotePeers.add(peerId)) {
            Log.w(LOG_TAG, "Duplicated peer id.");
        }
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
     * @param token token information for connecting to the signaling server.
     * @param callback ActionCallback.onSuccess will be invoked with the id when succeeds to connect
     * the signaling server. Otherwise when fails to do so, ActionCallback.onFailure will be
     * invoked with the corresponding OwtError.
     */
    public synchronized void connect(final String token, final ActionCallback<String> callback) {
        // Format and content of |token| can be customized, so we do not assume any expectations.
        DCHECK(signalingChannel);
        DCHECK(signalingExecutor);
        if (!checkConnectionStatus(DISCONNECTED)) {
            triggerCallback(callback, new OwtError(OwtP2PError.P2P_CLIENT_INVALID_STATE.value,
                    "Wrong server connection status."));
            return;
        }
        changeConnectionStatus(CONNECTING);
        signalingExecutor.execute(
                () -> signalingChannel.connect(token, new ActionCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        try {
                            JSONObject resultObj = new JSONObject(result);
                            id = resultObj.getString("uid");
                        } catch (JSONException e) {
                            RCHECK(e);
                        }
                        changeConnectionStatus(CONNECTED);
                        triggerCallback(callback, result);
                    }

                    @Override
                    public void onFailure(OwtError error) {
                        changeConnectionStatus(DISCONNECTED);
                        triggerCallback(callback, error);
                    }
                }));
    }

    /**
     * Disconnect from the signaling server. This will stop all current active sessions with
     * other P2PClients.
     */
    public void disconnect() {
        if (checkConnectionStatus(DISCONNECTED)) {
            return;
        }
        DCHECK(signalingChannel);
        signalingChannel.disconnect();
    }

    /**
     * Publish a LocalStream to remote P2PClient.
     *
     * @param peerId id of remote P2PClient.
     * @param localStream LocalStream to be published.
     * @param callback ActionCallback.onSuccess will be invoked with the Publication when
     * succeeds to publish the LocalStream. Otherwise when fails to do so, ActionCallback
     * .onFailure will be invoked with the corresponding OwtError.
     */
    public synchronized void publish(final String peerId, final LocalStream localStream,
            final ActionCallback<Publication> callback) {
        RCHECK(localStream);
        if (!checkConnectionStatus(CONNECTED)) {
            triggerCallback(callback, new OwtError(OwtP2PError.P2P_CLIENT_INVALID_STATE.value,
                    "Wrong server connection status."));
            return;
        }
        if (!checkPermission(peerId, callback)) {
            return;
        }
        if (!containsPCChannel(peerId)) {
            sendStop(peerId);
            sendUserInfo(peerId);
        }
        sendStreamInfo(peerId, localStream, new ActionCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                P2PPeerConnectionChannel pcChannel = getPeerConnection(peerId);
                pcChannel.publish(localStream, callback);
            }

            @Override
            public void onFailure(OwtError error) {
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
    public synchronized void stop(String peerId) {
        RCHECK(peerId);
        if (!checkConnectionStatus(CONNECTED)) {
            return;
        }
        synchronized (pcChannelsLock) {
            if (!pcChannels.containsKey(peerId)) {
                return;
            }
            pcChannels.get(peerId).dispose();
            pcChannels.remove(peerId);
        }
        sendSignalingMessage(peerId, CHAT_CLOSED, null, null);
    }

    /**
     * Get the PeerConnection stats.
     *
     * @param peerId id of remote P2PClient.
     * @param callback ActionCallback.onSuccess will be invoked with RTCStatsReport when succeeds
     * to get the stats. Otherwise when fails to do so, ActionCallback.onFailure will be invoked
     * with the corresponding OwtError.
     */
    public synchronized void getStats(String peerId,
            final ActionCallback<RTCStatsReport> callback) {
        RCHECK(peerId);
        if (!containsPCChannel(peerId)) {
            triggerCallback(callback, new OwtError(OwtP2PError.P2P_CLIENT_INVALID_STATE.value,
                    "No peerconnection established yet."));
            return;
        }
        if (callback != null) {
            P2PPeerConnectionChannel pcChannel = getPeerConnection(peerId);
            pcChannel.getConnectionStats(callback);
        }
    }

    /**
     * Send a text message to a remote P2PClient.
     *
     * @param peerId id of remote P2PClient.
     * @param message message to be sent.
     * @param callback ActionCallback.onSuccess will be invoked succeeds to send the message.
     * Otherwise when fails to do so, ActionCallback.onFailure will be invoked with the
     * corresponding OwtError.
     */
    public synchronized void send(String peerId, String message, ActionCallback<Void> callback) {
        if (!checkConnectionStatus(CONNECTED)) {
            triggerCallback(callback, new OwtError(OwtP2PError.P2P_CLIENT_INVALID_STATE.value,
                    "Wrong server connection status."));
            return;
        }
        if (!checkPermission(peerId, callback)) {
            return;
        }
        RCHECK(message);
        if (message.length() > 0xFFFF) {
            triggerCallback(callback,
                    new OwtError(OwtP2PError.P2P_CLIENT_ILLEGAL_ARGUMENT.value,
                            "Message too long."));
            return;
        }
        if (!containsPCChannel(peerId)) {
            sendStop(peerId);
            sendUserInfo(peerId);
        }
        P2PPeerConnectionChannel pcChannel = getPeerConnection(peerId);
        pcChannel.sendData(message, callback);
    }

    private void permissionDenied(String peerId) {
        synchronized (pcChannelsLock) {
            if (pcChannels.containsKey(peerId)) {
                pcChannels.get(peerId).dispose();
                pcChannels.remove(peerId);
            }
        }

        JSONObject errorMsg = new JSONObject();
        try {
            errorMsg.put("message", "Denied");
            errorMsg.put("code", OwtP2PError.P2P_CLIENT_DENIED.value);
        } catch (JSONException e) {
            DCHECK(e);
        }
        sendSignalingMessage(peerId, CHAT_CLOSED, errorMsg, null);
    }

    private void closeInternal() {
        synchronized (pcChannelsLock) {
            for (String key : pcChannels.keySet()) {
                pcChannels.get(key).dispose();
            }
            pcChannels.clear();
        }
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
        callbackExecutor.execute(() -> callback.onSuccess(result));
    }

    private <T> void triggerCallback(final ActionCallback<T> callback, final OwtError e) {
        DCHECK(callbackExecutor);
        if (callback == null) {
            return;
        }
        callbackExecutor.execute(() -> callback.onFailure(e));
    }

    private boolean containsPCChannel(String key) {
        synchronized (pcChannelsLock) {
            return pcChannels != null && pcChannels.containsKey(key);
        }
    }

    private P2PPeerConnectionChannel getPeerConnection(String peerId) {
        return getPeerConnection(peerId, null);
    }

    private P2PPeerConnectionChannel getPeerConnection(String peerId,
            P2PClientConfiguration config) {
        synchronized (pcChannelsLock) {
            DCHECK(pcChannels);
            if (pcChannels.containsKey(peerId)) {
                return pcChannels.get(peerId);
            }
            P2PPeerConnectionChannel pcChannel = new P2PPeerConnectionChannel(peerId,
                    config == null ? this.configuration : config, this);
            pcChannels.put(peerId, pcChannel);
            return pcChannel;
        }
    }

    private <T> boolean checkPermission(String peerId, ActionCallback<T> callback) {
        if (!allowedRemotePeers.contains(peerId) || peerId.equals(id)) {
            triggerCallback(callback,
                    new OwtError(OwtP2PError.P2P_CLIENT_NOT_ALLOWED.value, "Not allowed."));
            return false;
        }
        return true;
    }

    private void sendUserInfo(String peerId) {
        try {
            sendSignalingMessage(peerId, CHAT_UA, new JSONObject(Const.userAgent), null);
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
        signalingExecutor.execute(() -> {

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
                            public void onFailure(OwtError error) {
                                if (callback != null) {
                                    callback.onFailure(error);
                                }
                            }
                        });
            } catch (JSONException e) {
                triggerCallback(callback,
                        new OwtError(OwtP2PError.P2P_CLIENT_ILLEGAL_ARGUMENT.value,
                                e.getMessage()));
            }

        });
    }

    private void sendTrackAck(String peerId, JSONArray tracks) {
        sendSignalingMessage(peerId, TRACK_ADD_ACK, tracks, null);
    }

    private void processSignalingMsg(String peerId, JSONObject message) throws JSONException {
        synchronized (pcChannelsLock) {
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
                    for (LocalStream ls : oldChannel.publishedStreams) {
                        localStream = ls;
                    }
                    for (P2PPeerConnectionChannel.CallbackInfo cbi : oldChannel.publishCallbacks
                            .values()) {
                        callback = cbi.callback;
                    }

                    oldChannel.dispose();
                    pcChannels.remove(peerId);
                    DCHECK(!pcChannels.containsKey(peerId));
                    P2PPeerConnectionChannel newChannel = getPeerConnection(peerId);
                    newChannel.processSignalingMessage(message);
                    if (localStream != null) {
                        newChannel.publish(localStream, callback);
                    }
                }
            } else {
                getPeerConnection(peerId).processSignalingMessage(message);
            }
        }
    }

    private void processStreamInfo(JSONObject streamInfo) throws JSONException {
        streamInfos.put(streamInfo.getString("id"), streamInfo);
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
    public void onIceCandidatesRemoved(String key, IceCandidate[] candidates) {

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
                public void onFailure(OwtError error) {
                    synchronized (pcChannelsLock) {
                        //failed to send sdp, trigger callbacks.
                        getPeerConnection(peerId).processError(error);
                        getPeerConnection(peerId).dispose();
                        pcChannels.remove(peerId);
                    }
                }
            });
        } catch (JSONException e) {
            DCHECK(e);
        }
    }

    @Override
    public void onError(String peerId, String error, boolean recoverable) {
        synchronized (pcChannelsLock) {
            if (!recoverable && pcChannels.containsKey(peerId)) {
                pcChannels.get(peerId).dispose();
                pcChannels.remove(peerId);
            }
        }

        JSONObject errorMsg = new JSONObject();
        try {
            errorMsg.put("code",
                    recoverable ? OwtP2PError.P2P_WEBRTC_ICE_POLICY_UNSUPPORTED.value
                            : OwtP2PError.P2P_WEBRTC_SDP.value);
            errorMsg.put("message", error);
        } catch (JSONException e) {
            DCHECK(e);
        }
        sendSignalingMessage(peerId, CHAT_CLOSED, errorMsg, null);
    }

    @Override
    public void onEnded(final String peerId){
        synchronized (pcChannelsLock) {
            if (pcChannels.containsKey(peerId)) {
                pcChannels.get(peerId).dispose();
                pcChannels.remove(peerId);
            }
        }
        sendSignalingMessage(peerId, CHAT_CLOSED, null, null);
    }

    @Override
    public void onAddStream(final String peerId, final owt.base.RemoteStream remoteStream) {
        DCHECK(callbackExecutor);
        DCHECK(pcChannels.containsKey(peerId));

        callbackExecutor.execute(() -> {
            try {
                if (streamInfos.containsKey(remoteStream.id())) {
                    JSONObject streamInfo = streamInfos.remove(remoteStream.id());
                    JSONObject sourceInfo = streamInfo.getJSONObject("source");
                    VideoSourceInfo vInfo = sourceInfo.has("video")
                            ? VideoSourceInfo.get(sourceInfo.getString("video")) : null;
                    AudioSourceInfo aInfo = sourceInfo.has("audio")
                            ? AudioSourceInfo.get(sourceInfo.getString("audio")) : null;
                    Stream.StreamSourceInfo info = new Stream.StreamSourceInfo(vInfo, aInfo);
                    ((owt.p2p.RemoteStream) remoteStream).setInfo(info);

                    if (streamInfo.has("attributes")) {
                        JSONObject attr = streamInfo.getJSONObject("attributes");
                        HashMap<String, String> attributes = new HashMap<>();
                        for (Iterator<String> it = attr.keys(); it.hasNext(); ) {
                            String key = it.next();
                            attributes.put(key, attr.getString(key));
                        }
                        remoteStream.setAttributes(attributes);
                    }

                    sendTrackAck(peerId, streamInfo.getJSONArray("tracks"));
                }
                synchronized (observers) {
                    for (P2PClientObserver observer : observers) {
                        observer.onStreamAdded((RemoteStream) remoteStream);
                    }
                }
            } catch (JSONException e) {
                DCHECK(e);
            }
        });
    }

    @Override
    public void onDataChannelMessage(final String peerId, final String message) {
        DCHECK(callbackExecutor);
        DCHECK(pcChannels.containsKey(peerId));

        try {
            JSONObject msg = new JSONObject(message);
            Long msgId = msg.getLong("id");
            final String msgData = msg.getString("data");
            callbackExecutor.execute(() -> {
                for (P2PClientObserver observer : observers) {
                    observer.onDataReceived(peerId, msgData);
                }
            });

            sendSignalingMessage(peerId, CHAT_DATA_ACK, msgId, null);
        } catch (JSONException e) {
            // TODO: Webrtc sometimes trigger this event with weird messages,
            // need to investigate this issue. Temporarily comment out this DCHECK.
            //DCHECK(e);
        }
    }

    @Override
    public void onRenegotiationRequest(String peerId) {
    }

    //SignalingChannelObserver
    @Override
    public void onMessage(String peerId, String message) {
        try {
            JSONObject msgObj = new JSONObject(message);
            SignalingMessageType messageType =
                    SignalingMessageType.get(msgObj.getString("type"));

            if (!checkPermission(peerId, null) && messageType != CHAT_CLOSED) {
                permissionDenied(peerId);
                return;
            }
            switch (messageType) {
                case SIGNALING_MESSAGE:
                    processSignalingMsg(peerId, msgObj.getJSONObject("data"));
                    break;
                case STREAM_INFO:
                    JSONObject streamInfo = msgObj.getJSONObject("data");
                    processStreamInfo(streamInfo);
                    break;
                case TRACK_ADD_ACK:
                    synchronized (pcChannelsLock) {
                        if (pcChannels.containsKey(peerId)) {
                            getPeerConnection(peerId).processTrackAck(msgObj.getJSONArray("data"));
                        }
                    }
                    break;
                case CHAT_UA:
                    P2PPeerConnectionChannel pcChannel;
                    synchronized (pcChannelsLock) {
                        if (!pcChannels.containsKey(peerId)) {
                            sendUserInfo(peerId);
                            boolean hasCap = msgObj.getJSONObject("data").has("capabilities");
                            JSONObject cap = hasCap ?
                                    msgObj.getJSONObject("data").getJSONObject("capabilities")
                                    : null;
                            boolean cont = cap != null && cap.getBoolean("continualIceGathering");
                            configuration.rtcConfiguration.continualGatheringPolicy =
                                    cont ? GATHER_CONTINUALLY : GATHER_ONCE;
                            pcChannel = getPeerConnection(peerId, configuration);
                        } else {
                            pcChannel = getPeerConnection(peerId);
                        }
                    }
                    pcChannel.processUserInfo(msgObj.getJSONObject("data"));
                    break;
                case CHAT_CLOSED:
                    if (containsPCChannel(peerId)) {
                        P2PPeerConnectionChannel oldChannel = getPeerConnection(peerId);
                        JSONObject dataObj;
                        int code = 0;
                        String error = null;
                        if (msgObj.has("data")) {
                            dataObj = new JSONObject(msgObj.getString("data"));
                            code = dataObj.has("code") ? dataObj.getInt("code") : 0;
                            error = dataObj.has("message") ? dataObj.getString("message") : "";
                        }
                        if (code == 0) {
                            if (oldChannel.getSignalingState() == null
                                    || oldChannel.getSignalingState() == HAVE_LOCAL_OFFER) {
                                // Having reached here, we need to deal with the case in which the
                                // peer client and me publish at the same time.
                                return;
                            }
                        }
                        pcChannels.remove(peerId);
                        if (code == OwtP2PError.P2P_WEBRTC_ICE_POLICY_UNSUPPORTED.value) {
                            // re-create peerconnection and re-publish.
                            LocalStream localStream = null;
                            ActionCallback<Publication> callback = null;
                            // As this situation will happen only at the initial phase of a pc,
                            // so iterate the lists below will only get one instance of each kind.
                            for (LocalStream ls : oldChannel.publishedStreams) {
                                localStream = ls;
                            }
                            for (P2PPeerConnectionChannel.CallbackInfo cbi : oldChannel
                                    .publishCallbacks.values()) {
                                callback = cbi.callback;

                            }
                            // disable continual gathering.
                            P2PClientConfiguration config = this.configuration;
                            config.rtcConfiguration.continualGatheringPolicy = GATHER_ONCE;
                            P2PPeerConnectionChannel newChannel = getPeerConnection(peerId, config);
                            newChannel.publish(localStream, callback);
                        } else {
                            // trigger callbacks.
                            oldChannel.processError(new OwtError(code, error));
                        }
                        oldChannel.dispose();
                    }
                    break;
                case CHAT_DATA_ACK:
                    synchronized (pcChannelsLock) {
                        if (pcChannels.containsKey(peerId)) {
                            getPeerConnection(peerId).processDataAck(msgObj.getLong("data"));
                        }
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
        changeConnectionStatus(DISCONNECTED);
        closeInternal();
        callbackExecutor.execute(() -> {
            synchronized (observers) {
                for (P2PClientObserver observer : observers) {
                    observer.onServerDisconnected();
                }
            }
        });
    }

    enum ServerConnectionStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    enum SignalingMessageType {
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
    ///@endcond
}
