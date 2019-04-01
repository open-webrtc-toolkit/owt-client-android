/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.p2p;

import static org.webrtc.DataChannel.State.OPEN;
import static org.webrtc.PeerConnection.IceConnectionState.COMPLETED;
import static org.webrtc.PeerConnection.IceConnectionState.CONNECTED;
import static org.webrtc.PeerConnection.SignalingState.STABLE;

import static owt.base.CheckCondition.DCHECK;
import static owt.base.CheckCondition.RCHECK;
import static owt.base.Const.LOG_TAG;
import static owt.p2p.OwtP2PError.P2P_CLIENT_INVALID_STATE;
import static owt.p2p.OwtP2PError.P2P_WEBRTC_SDP;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import owt.base.ActionCallback;
import owt.base.AudioEncodingParameters;
import owt.base.LocalStream;
import owt.base.OwtError;
import owt.base.PeerConnectionChannel;
import owt.base.VideoEncodingParameters;

final class P2PPeerConnectionChannel extends PeerConnectionChannel {

    // <MediaStreamId, CallbackInfo>
    ConcurrentHashMap<String, CallbackInfo> publishCallbacks;
    private Long messageId = 0L;
    // <MessageId, Callback>
    private ConcurrentHashMap<Long, ActionCallback<Void>> sendMsgCallbacks;
    // <LocalStream>
    ArrayList<LocalStream> publishedStreams;
    private String currentMediaStreamId;
    // <MediaStreamId, RemoteStream>
    private ConcurrentHashMap<String, RemoteStream> remoteStreams;
    // <MediaStreamId>
    private ArrayList<String> pendingAckRemoteStreams;
    private ArrayList<Publication> publications;

    private final Object negLock = new Object();
    private boolean renegotiationNeeded = false;
    private boolean negotiating = false;

    private boolean streamRemovable = true;
    private boolean unifiedPlan = false;
    private boolean continualIceGathering = true;
    private boolean everPublished = false;

    P2PPeerConnectionChannel(String peerId, P2PClientConfiguration configuration,
            PeerConnectionChannelObserver observer) {
        super(peerId, configuration.rtcConfiguration, true, true, observer);
        publishCallbacks = new ConcurrentHashMap<>();
        sendMsgCallbacks = new ConcurrentHashMap<>();
        publishedStreams = new ArrayList<>();
        remoteStreams = new ConcurrentHashMap<>();
        pendingAckRemoteStreams = new ArrayList<>();
        publications = new ArrayList<>();

        for (VideoEncodingParameters parameters : configuration.videoEncodings) {
            if (videoCodecs == null) {
                videoCodecs = new ArrayList<>();
            }
            videoCodecs.add(parameters.codec.name);
        }
        videoMaxBitrate = VideoEncodingParameters.maxBitrate;

        for (AudioEncodingParameters parameters : configuration.audioEncodings) {
            if (audioCodecs == null) {
                audioCodecs = new ArrayList<>();
            }
            audioCodecs.add(parameters.codec.name);
        }
        audioMaxBitrate = AudioEncodingParameters.maxBitrate;
    }

    void publish(LocalStream localStream, ActionCallback<Publication> callback) {
        if (!streamRemovable && everPublished) {
            if (callback != null) {
                callback.onFailure(new OwtError(P2P_CLIENT_INVALID_STATE.value,
                        "Cannot publish multiple streams due to the ability of peer client."));
            }
            return;
        }
        MediaStream currentMediaStream = GetMediaStream(localStream);
        RCHECK(currentMediaStream);
        currentMediaStreamId = localStream.id();
        if (publishedStreams.contains(localStream)) {
            if (callback != null) {
                callback.onFailure(
                        new OwtError(P2P_CLIENT_INVALID_STATE.value, "Duplicated stream."));
            }
            return;
        }

        CallbackInfo callbackInfo = new CallbackInfo(currentMediaStream, callback);
        if (!currentMediaStream.audioTracks.isEmpty()) {
            publishCallbacks.put(currentMediaStream.audioTracks.get(0).id(), callbackInfo);
        }
        if (!currentMediaStream.videoTracks.isEmpty()) {
            publishCallbacks.put(currentMediaStream.videoTracks.get(0).id(), callbackInfo);
        }

        publishedStreams.add(localStream);
        addStream(currentMediaStream);
        everPublished = true;
        // create the data channel here due to BUG1418.
        if (localDataChannel == null) {
            createDataChannel();
        }
    }

    void unpublish(String mediaStreamId) {
        for (LocalStream localStream : publishedStreams) {
            if (localStream.id().equals(mediaStreamId)) {
                publishedStreams.remove(localStream);
                removeStream(mediaStreamId);
                break;
            }
        }
    }

    protected synchronized void dispose() {
        super.dispose();
        for (RemoteStream remoteStream : remoteStreams.values()) {
            remoteStream.onEnded();
        }
        for (Publication publication : publications) {
            publication.onEnded();
        }
        publishedStreams.clear();
        remoteStreams.clear();
        publications.clear();
    }

    void processTrackAck(JSONArray tracksData) throws JSONException {
        for (int i = 0; i < tracksData.length(); i++) {
            String trackId = tracksData.getString(i);
            CallbackInfo callbackInfo = publishCallbacks.get(trackId);
            if (callbackInfo != null
                    && --callbackInfo.trackNum == 0 && callbackInfo.callback != null) {
                Publication publication = new Publication(callbackInfo.mediaStreamId, this);
                publications.add(publication);
                callbackInfo.callback.onSuccess(publication);
            }
            publishCallbacks.remove(trackId);
        }
    }

    void processDataAck(Long msgId) {
        if (sendMsgCallbacks.containsKey(msgId)) {
            sendMsgCallbacks.get(msgId).onSuccess(null);
            sendMsgCallbacks.remove(msgId);
        }
    }

    void processError(OwtError error) {
        for (CallbackInfo callbackInfo : publishCallbacks.values()) {
            if (--callbackInfo.trackNum == 0 && callbackInfo.callback != null) {
                callbackInfo.callback.onFailure(error);

            }
        }
        publishCallbacks.clear();

        for (ActionCallback<Void> callback : sendMsgCallbacks.values()) {
            callback.onFailure(error);
        }
        sendMsgCallbacks.clear();
    }

    // TODO: currently (v4.1) Android is compatible with all other platforms.
    private boolean checkCompatibility(JSONObject userInfo) {
        try {
            boolean hasCap = userInfo.has("capabilities");
            JSONObject cap = hasCap ? userInfo.getJSONObject("capabilities") : null;
            streamRemovable = cap == null ?
                    !userInfo.getJSONObject("runtime").getString("name").equals("Firefox")
                    : cap.getBoolean("streamRemovable");
            unifiedPlan = cap != null && cap.getBoolean("unifiedPlan");
            continualIceGathering = cap != null && cap.getBoolean("continualIceGathering");
        } catch (JSONException e) {
            DCHECK(e);
        }
        return true;
    }

    void processUserInfo(JSONObject userInfo) {
        // check capabilities.
        if (!checkCompatibility(userInfo)) {
            onError = true;
            observer.onError(key, "Incompatible", true);
        }
    }

    void processNegotiationRequest() {
        synchronized (negLock) {
            if (!negotiating && getSignalingState() == STABLE) {
                negotiating = true;
                renegotiationNeeded = false;
                createOffer();
            } else {
                renegotiationNeeded = true;
            }
        }
    }

    PeerConnection.SignalingState getSignalingState() {
        return signalingState;
    }

    private void checkWaitingList() {
        if (renegotiationNeeded) {
            renegotiationNeeded = false;
            processNegotiationRequest();
        }
        for (String id : pendingAckRemoteStreams) {
            observer.onAddStream(key, remoteStreams.get(id));
        }
        pendingAckRemoteStreams.clear();
    }

    void sendData(final String message, ActionCallback<Void> callback) {
        Long msgId = ++messageId;
        final JSONObject messageObj = new JSONObject();
        try {
            messageObj.put("id", msgId);
            messageObj.put("data", message);
        } catch (JSONException e) {
            DCHECK(e);
        }
        if (callback != null) {
            sendMsgCallbacks.put(msgId, callback);
        }

        if (localDataChannel == null || localDataChannel.state() != OPEN) {
            queuedMessage.add(messageObj.toString());
            if (localDataChannel == null) {
                createDataChannel();
            }
            return;
        }

        ByteBuffer byteBuffer =
                ByteBuffer.wrap(messageObj.toString().getBytes(Charset.forName("UTF-8")));
        DataChannel.Buffer buffer = new DataChannel.Buffer(byteBuffer, false);
        localDataChannel.send(buffer);
    }

    //All PeerConnection.Observer publishCallbacks should be pooled onto callbackExecutor.
    @Override
    public void onSignalingChange(final PeerConnection.SignalingState signalingState) {
        callbackExecutor.execute(() -> {
            Log.d(LOG_TAG, "onSignalingChange " + signalingState);
            P2PPeerConnectionChannel.this.signalingState = signalingState;
            if (signalingState == STABLE) {
                synchronized (negLock) {
                    negotiating = false;
                }
                checkWaitingList();
            }
        });
    }

    @Override
    public void onIceConnectionChange(
            final PeerConnection.IceConnectionState iceConnectionState) {
        callbackExecutor.execute(() -> {
            Log.d(LOG_TAG, "onIceConnectionChange " + iceConnectionState);
            P2PPeerConnectionChannel.this.iceConnectionState = iceConnectionState;
            if (iceConnectionState == CONNECTED || iceConnectionState == COMPLETED) {
                checkWaitingList();
            }
            if (iceConnectionState == PeerConnection.IceConnectionState.FAILED) {
                for (RemoteStream remoteStream : remoteStreams.values()) {
                    remoteStream.onEnded();
                }
                for (Publication publication : publications) {
                    publication.onEnded();
                }
                remoteStreams.clear();
                publications.clear();
            }
        });
    }

    @Override
    public void onIceCandidate(final IceCandidate iceCandidate) {
        callbackExecutor.execute(() -> {
            Log.d(LOG_TAG, "onIceCandidate");
            observer.onIceCandidate(key, iceCandidate);
        });
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
    }

    @Override
    public void onAddStream(final MediaStream mediaStream) {
        callbackExecutor.execute(() -> {
            RemoteStream remoteStream = new RemoteStream(key, mediaStream);
            remoteStreams.put(mediaStream.getId(), remoteStream);
            if (iceConnectionState == CONNECTED || iceConnectionState == COMPLETED) {
                observer.onAddStream(key, remoteStream);
            } else {
                pendingAckRemoteStreams.add(mediaStream.getId());
            }
        });
    }

    @Override
    public void onRemoveStream(final MediaStream mediaStream) {
        String id = mediaStream.getId();
        callbackExecutor.execute(() -> {
            Log.d(LOG_TAG, "onRemoveStream");
            if (remoteStreams.containsKey(id)) {
                remoteStreams.remove(id).onEnded();
            }
        });
    }

    @Override
    public void onRenegotiationNeeded() {
        callbackExecutor.execute(() -> {
            if (disposed()) {
                return;
            }
            Log.d(LOG_TAG, "onRenegotiationNeeded");
            processNegotiationRequest();
        });
    }

    @Override
    public void onSetSuccess() {
        callbackExecutor.execute(() -> {
            if (disposed()) {
                return;
            }
            Log.d(LOG_TAG, "onSetSuccess ");
            if (signalingState == PeerConnection.SignalingState.HAVE_REMOTE_OFFER
                    || peerConnection.getLocalDescription() == null) {
                createAnswer();
            } else {
                drainRemoteCandidates();

                if (currentMediaStreamId != null) {
                    setMaxBitrate(currentMediaStreamId);
                    currentMediaStreamId = null;
                }
            }
        });
    }

    @Override
    public void onCreateFailure(final String error) {
        callbackExecutor.execute(() -> {
            for (CallbackInfo callbackInfo : publishCallbacks.values()) {
                if (callbackInfo.callback != null) {
                    callbackInfo.callback.onFailure(new OwtError(P2P_WEBRTC_SDP.value, error));
                }
            }
            publishCallbacks.clear();
            observer.onError(key, error, false);
        });
    }

    @Override
    public void onSetFailure(final String error) {
        callbackExecutor.execute(() -> {
            for (CallbackInfo callbackInfo : publishCallbacks.values()) {
                if (callbackInfo.callback != null) {
                    callbackInfo.callback.onFailure(new OwtError(P2P_WEBRTC_SDP.value, error));
                }
            }
            publishCallbacks.clear();
            observer.onError(key, error, false);
        });
    }

    static class CallbackInfo {
        final String mediaStreamId;
        final ActionCallback<Publication> callback;
        int trackNum;

        CallbackInfo(MediaStream mediaStream, ActionCallback<Publication> callback) {
            this.mediaStreamId = mediaStream.getId();
            this.callback = callback;
            trackNum = mediaStream.audioTracks.size() + mediaStream.videoTracks.size();
        }
    }
}
