/*
 * Intel License Header Holder
 */
package com.intel.webrtc.p2p;

import static com.intel.webrtc.base.CheckCondition.DCHECK;
import static com.intel.webrtc.base.CheckCondition.RCHECK;
import static com.intel.webrtc.p2p.IcsP2PError.P2P_CLIENT_INVALID_STATE;
import static com.intel.webrtc.p2p.IcsP2PError.P2P_WEBRTC_SDP;

import static org.webrtc.DataChannel.State.OPEN;
import static org.webrtc.PeerConnection.IceConnectionState.COMPLETED;
import static org.webrtc.PeerConnection.IceConnectionState.CONNECTED;
import static org.webrtc.PeerConnection.SignalingState.STABLE;

import android.util.Log;

import com.intel.webrtc.base.ActionCallback;
import com.intel.webrtc.base.AudioEncodingParameters;
import com.intel.webrtc.base.IcsError;
import com.intel.webrtc.base.LocalStream;
import com.intel.webrtc.base.PeerConnectionChannel;
import com.intel.webrtc.base.VideoEncodingParameters;

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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

final class P2PPeerConnectionChannel extends PeerConnectionChannel {

    private final Object negLock = new Object();
    //key: trackId
    ConcurrentHashMap<String, CallbackInfo> publishCallbacks;
    //key: localstream id
    ConcurrentHashMap<String, LocalStream> localStreams;
    private boolean renegotiationNeeded = false;
    private boolean negotiating = false;
    private ConcurrentHashMap<MediaStream, RemoteStream> remoteStreams;
    private List<RemoteStream> pendingAckRemoteStreams;
    private ConcurrentHashMap<Long, ActionCallback<Void>> sendMsgCallbacks;
    private List<Publication> publications;
    private MediaStream currentMediaStream;
    private Long messageId = 0L;
    private boolean streamRemovable = true;
    private boolean unifiedPlan = false;
    private boolean continualIceGathering = true;
    private boolean everPublished = false;
    // default isCaller true
    private boolean isCaller = true;

    P2PPeerConnectionChannel(String peerId, P2PClientConfiguration configuration,
            PeerConnectionChannelObserver observer) {
        super(peerId, configuration.rtcConfiguration, true, true, observer);
        publishCallbacks = new ConcurrentHashMap<>();
        localStreams = new ConcurrentHashMap<>();
        remoteStreams = new ConcurrentHashMap<>();
        sendMsgCallbacks = new ConcurrentHashMap<>();
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
                callback.onFailure(new IcsError(P2P_CLIENT_INVALID_STATE.value,
                        "Cannot publish multiple streams due to the ability of peer client."));
            }
            return;
        }
        currentMediaStream = GetMediaStream(localStream);
        RCHECK(currentMediaStream);
        if (localStreams.containsKey(currentMediaStream.getId())) {
            if (callback != null) {
                callback.onFailure(
                        new IcsError(P2P_CLIENT_INVALID_STATE.value, "Duplicated stream."));
            }
            return;
        }

        localStreams.put(currentMediaStream.getId(), localStream);

        CallbackInfo callbackInfo = new CallbackInfo(currentMediaStream, callback);

        if (!currentMediaStream.audioTracks.isEmpty()) {
            publishCallbacks.put(currentMediaStream.audioTracks.get(0).id(), callbackInfo);
        }
        if (!currentMediaStream.videoTracks.isEmpty()) {
            publishCallbacks.put(currentMediaStream.videoTracks.get(0).id(), callbackInfo);
        }
        addStream(currentMediaStream);
        everPublished = true;
        // create the data channel here due to BUG1418.
        if (localDataChannel == null) {
            createDataChannel();
        }
    }

    void unpublish(MediaStream mediaStream) {
        DCHECK(localStreams.containsKey(mediaStream.getId()));
        localStreams.remove(mediaStream.getId());
        removeStream(mediaStream);
    }

    protected synchronized void dispose() {
        for (LocalStream localStream : localStreams.values()) {
            removeStream(GetMediaStream(localStream));
        }
        super.dispose();
        for (RemoteStream remoteStream : remoteStreams.values()) {
            remoteStream.onEnded();
        }
        for (Publication publication : publications) {
            publication.onEnded();
        }
        localStreams.clear();
        remoteStreams.clear();
        publications.clear();
    }

    void processTrackAck(JSONArray tracksData) throws JSONException {
        for (int i = 0; i < tracksData.length(); i++) {
            String trackId = tracksData.getString(i);
            CallbackInfo callbackInfo = publishCallbacks.get(trackId);
            if (callbackInfo != null
                    && --callbackInfo.trackNum == 0 && callbackInfo.callback != null) {
                Publication publication = new Publication(callbackInfo.mediaStream, this);
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

    void processError(IcsError error) {
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
            if (!negotiating) {
                negotiating = true;
                renegotiationNeeded = false;
                if (isCaller) {
                    createOffer();
                } else {
                    observer.onRenegotiationRequest(key);
                }
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
        for (RemoteStream remoteStream : pendingAckRemoteStreams) {
            observer.onAddStream(key, remoteStream);
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
            Log.d(TAG, "onSignalingChange " + signalingState);
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
    public void onIceConnectionChange(final PeerConnection.IceConnectionState iceConnectionState) {
        callbackExecutor.execute(() -> {
            Log.d(TAG, "onIceConnectionChange " + iceConnectionState);
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
            Log.d(TAG, "onIceCandidate");
            observer.onIceCandidate(key, iceCandidate);
        });
    }

    @Override
    public void onAddStream(final MediaStream mediaStream) {
        callbackExecutor.execute(() -> {
            Log.d(TAG, "onAddStream");
            RemoteStream remoteStream = new RemoteStream(key, mediaStream);
            remoteStreams.put(mediaStream, remoteStream);
            if (iceConnectionState == CONNECTED || iceConnectionState == COMPLETED) {
                observer.onAddStream(key, remoteStream);
            } else {
                pendingAckRemoteStreams.add(remoteStream);
            }
        });
    }

    @Override
    public void onRemoveStream(final MediaStream mediaStream) {
        callbackExecutor.execute(() -> {
            Log.d(TAG, "onRemoveStream");
            if (remoteStreams.containsKey(mediaStream)) {
                RemoteStream remoteStream = remoteStreams.get(mediaStream);
                remoteStream.onEnded();
                remoteStreams.remove(mediaStream);
            }
        });
    }

    @Override
    public void onRenegotiationNeeded() {
        callbackExecutor.execute(() -> {
            if (disposed()) {
                return;
            }
            Log.d(TAG, "onRenegotiationNeeded");
            processNegotiationRequest();
        });
    }

    @Override
    public void onSetSuccess() {
        callbackExecutor.execute(() -> {
            if (disposed()) {
                return;
            }
            Log.d(TAG, "onSetSuccess ");
            if (signalingState == PeerConnection.SignalingState.HAVE_REMOTE_OFFER
                    || peerConnection.getLocalDescription() == null) {
                isCaller = false;
                createAnswer();
            } else {
                drainRemoteCandidates();

                if (currentMediaStream != null) {
                    setMaxBitrate(currentMediaStream);
                    currentMediaStream = null;
                }
            }
        });
    }

    @Override
    public void onCreateFailure(final String error) {
        callbackExecutor.execute(() -> {
            for (CallbackInfo callbackInfo : publishCallbacks.values()) {
                if (callbackInfo.callback != null) {
                    callbackInfo.callback.onFailure(new IcsError(P2P_WEBRTC_SDP.value, error));
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
                    callbackInfo.callback.onFailure(new IcsError(P2P_WEBRTC_SDP.value, error));
                }
            }
            publishCallbacks.clear();
            observer.onError(key, error, false);
        });
    }

    static class CallbackInfo {
        final MediaStream mediaStream;
        final ActionCallback<Publication> callback;
        int trackNum;

        CallbackInfo(MediaStream mediaStream, ActionCallback<Publication> callback) {
            this.mediaStream = mediaStream;
            this.callback = callback;
            trackNum = mediaStream.audioTracks.size() + mediaStream.videoTracks.size();
        }
    }
}
