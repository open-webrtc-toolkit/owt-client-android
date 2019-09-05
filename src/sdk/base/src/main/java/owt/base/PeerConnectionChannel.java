/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.base;

import static owt.base.CheckCondition.DCHECK;
import static owt.base.CheckCondition.RCHECK;
import static owt.base.Const.LOG_TAG;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaConstraints.KeyValuePair;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.RTCStatsReport;
import org.webrtc.RtpParameters;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpSender;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.VideoTrack;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import owt.base.MediaCodecs.AudioCodec;
import owt.base.MediaCodecs.VideoCodec;

///@cond
public abstract class PeerConnectionChannel
        implements PeerConnection.Observer, SdpObserver, DataChannel.Observer {

    //For P2P, key is peer id, for conference, key is Publication/Subscription id.
    public final String key;
    protected final PeerConnectionChannelObserver observer;
    protected final ExecutorService callbackExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService pcExecutor = Executors.newSingleThreadExecutor();
    private final List<IceCandidate> queuedRemoteCandidates;
    private final Object remoteIceLock = new Object();
    private final Object disposeLock = new Object();
    protected PeerConnection peerConnection;
    protected PeerConnection.SignalingState signalingState;
    protected PeerConnection.IceConnectionState iceConnectionState;
    protected DataChannel localDataChannel;
    protected List<VideoCodec> videoCodecs;
    protected List<AudioCodec> audioCodecs;
    protected Integer videoMaxBitrate = null, audioMaxBitrate = null;
    protected ArrayList<String> queuedMessage;
    private MediaConstraints sdpConstraints;
    private SessionDescription localSdp;
    private boolean disposed = false;
    protected boolean onError = false;
    // <MediaStream id, RtpSender>
    private ConcurrentHashMap<String, RtpSender> videoRtpSenders, audioRtpSenders;

    protected PeerConnectionChannel(String key, PeerConnection.RTCConfiguration configuration,
            boolean receiveVideo, boolean receiveAudio, PeerConnectionChannelObserver observer) {
        this.key = key;
        this.observer = observer;

        videoRtpSenders = new ConcurrentHashMap<>();
        audioRtpSenders = new ConcurrentHashMap<>();
        queuedRemoteCandidates = new LinkedList<>();
        queuedMessage = new ArrayList<>();
        sdpConstraints = new MediaConstraints();
        sdpConstraints.mandatory.add(
                new KeyValuePair("OfferToReceiveAudio", String.valueOf(receiveAudio)));
        sdpConstraints.mandatory.add(
                new KeyValuePair("OfferToReceiveVideo", String.valueOf(receiveVideo)));
        peerConnection = PCFactoryProxy.instance().createPeerConnection(configuration, this);
        RCHECK(peerConnection);
        signalingState = peerConnection.signalingState();
    }

    //Utility method for getting the mediastream as it is package privileged.
    protected static MediaStream GetMediaStream(Stream localStream) {
        return localStream.mediaStream;
    }

    public boolean disposed() {
        synchronized (disposeLock) {
            return disposed;
        }
    }

    protected void createOffer() {
        DCHECK(pcExecutor);
        pcExecutor.execute(() -> {
            if (disposed()) {
                return;
            }
            Log.d(LOG_TAG, "create offer");
            peerConnection.createOffer(PeerConnectionChannel.this, sdpConstraints);
        });
    }

    protected void createAnswer() {
        DCHECK(pcExecutor);
        pcExecutor.execute(() -> {
            if (disposed()) {
                return;
            }
            Log.d(LOG_TAG, "creating answer");
            peerConnection.createAnswer(PeerConnectionChannel.this, sdpConstraints);
        });
    }

    public void processSignalingMessage(JSONObject data) throws JSONException {
        String signalingType = data.getString("type");
        if (signalingType.equals("candidates")) {
            IceCandidate candidate = new IceCandidate(data.getString("sdpMid"),
                    data.getInt("sdpMLineIndex"),
                    data.getString("candidate"));
            addOrQueueCandidate(candidate);
        } else if (signalingType.equals("offer") || signalingType.equals("answer")) {
            // When gets an SDP during onError, it means that i'm waiting for peer to re-configure
            // itself to keep compatibility with me.
            if (onError) {
                // Ignore the SDP only once.
                onError = false;
                return;
            }
            String sdpString = data.getString("sdp");
            SessionDescription remoteSdp = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(signalingType),
                    sdpString);
            setRemoteDescription(remoteSdp);
        }
    }

    private void addOrQueueCandidate(final IceCandidate iceCandidate) {
        if (disposed()) {
            return;
        }
        DCHECK(pcExecutor);
        DCHECK(iceCandidate);
        pcExecutor.execute(() -> {
            if (disposed()) {
                return;
            }
            if (peerConnection.signalingState() == PeerConnection.SignalingState.STABLE) {
                Log.d(LOG_TAG, "add ice candidate");
                peerConnection.addIceCandidate(iceCandidate);
            } else {
                synchronized (remoteIceLock) {
                    Log.d(LOG_TAG, "queue ice candidate");
                    queuedRemoteCandidates.add(iceCandidate);
                }
            }
        });
    }

    protected void drainRemoteCandidates() {
        DCHECK(pcExecutor);
        DCHECK(queuedRemoteCandidates);
        synchronized (remoteIceLock) {
            for (final IceCandidate candidate : queuedRemoteCandidates) {
                pcExecutor.execute(() -> {
                    if (disposed()) {
                        return;
                    }
                    Log.d(LOG_TAG, "add ice candidate");
                    peerConnection.addIceCandidate(candidate);
                    queuedRemoteCandidates.remove(candidate);
                });
            }
        }
    }

    private void setRemoteDescription(final SessionDescription remoteDescription) {
        pcExecutor.execute(() -> {
            if (disposed()) {
                return;
            }
            SessionDescription remoteSdp = remoteDescription;
            if (audioCodecs != null) {
                remoteSdp = preferCodecs(remoteSdp, false);
            }

            if (videoCodecs != null) {
                remoteSdp = preferCodecs(remoteSdp, true);
            }
            peerConnection.setRemoteDescription(PeerConnectionChannel.this, remoteSdp);
        });
    }

    protected void addStream(final MediaStream mediaStream) {
        DCHECK(mediaStream);
        DCHECK(pcExecutor);
        pcExecutor.execute(() -> {
            if (disposed()) {
                return;
            }
            ArrayList<String> streamIds = new ArrayList<>();
            streamIds.add(mediaStream.getId());
            for (AudioTrack audioTrack : mediaStream.audioTracks) {
                RtpSender audioSender = peerConnection.addTrack(audioTrack, streamIds);
                audioRtpSenders.put(mediaStream.getId(), audioSender);
            }
            for (VideoTrack videoTrack : mediaStream.videoTracks) {
                RtpSender videoSender = peerConnection.addTrack(videoTrack, streamIds);
                videoRtpSenders.put(mediaStream.getId(), videoSender);
            }
        });
    }

    protected void removeStream(String mediaStreamId) {
        DCHECK(pcExecutor);
        pcExecutor.execute(() -> {
            if (disposed()) {
                return;
            }
            Log.d(LOG_TAG, "remove stream");
            if (audioRtpSenders.get(mediaStreamId) != null) {
                peerConnection.removeTrack(audioRtpSenders.get(mediaStreamId));
            }
            if (videoRtpSenders.get(mediaStreamId) != null) {
                peerConnection.removeTrack(videoRtpSenders.get(mediaStreamId));
            }
        });
    }

    protected void createDataChannel() {
        DCHECK(pcExecutor);
        DCHECK(localDataChannel == null);
        pcExecutor.execute(() -> {
            if (disposed()) {
                return;
            }
            DataChannel.Init init = new DataChannel.Init();
            localDataChannel = peerConnection.createDataChannel("message", init);
            localDataChannel.registerObserver(PeerConnectionChannel.this);
        });
    }

    public void getConnectionStats(final ActionCallback<RTCStatsReport> callback) {
        DCHECK(pcExecutor);
        pcExecutor.execute(() -> {
            if (disposed()) {
                callback.onFailure(new OwtError("Invalid stats."));
                return;
            }
            peerConnection.getStats(callback::onSuccess);
        });
    }

    private SessionDescription preferCodecs(SessionDescription sdp, boolean video) {
        LinkedHashSet<String> preferredCodecs = new LinkedHashSet<>();
        if (video) {
            for (VideoCodec codec : this.videoCodecs) {
                preferredCodecs.add(codec.name);
            }
            preferredCodecs.add("red");
            preferredCodecs.add("ulpfec");
        } else {
            for (AudioCodec codec : audioCodecs) {
                preferredCodecs.add(codec.name);
            }
            preferredCodecs.add("CN");
            preferredCodecs.add("telephone-event");
        }

        return preferCodec(sdp, preferredCodecs, video);
    }

    private SessionDescription preferCodec(SessionDescription originalSdp,
            LinkedHashSet<String> preferredCodecs, boolean video) {
        String[] lines = originalSdp.description.split("(\r\n|\n)");
        ArrayList<String> newLines = new ArrayList<>();

        int audioMLineIndex = -1;
        int videoMLineIndex = -1;
        //<codecName, payloadType>
        HashMap<String, ArrayList<String>> preferredPayloadTypes = new HashMap<>();
        //skipped all video payload types when dealing with audio codecs, and vice versa.
        HashSet<String> misMatchedPayloadTypes = new HashSet<>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.startsWith("a=rtpmap:")) {
                String payloadType = line.split(" ")[0].split(":")[1];
                String codecName = line.split(" ")[1].split("/")[0];
                boolean typeMismatched = video ? VideoCodec.get(codecName) == VideoCodec.INVALID
                        : AudioCodec.get(codecName) == AudioCodec.INVALID;
                boolean codecPreferred = preferredCodecs.contains(codecName);
                boolean rtxPreferred = codecName.equals("rtx")
                        && containsValue(preferredPayloadTypes, lines[i + 1].split("apt=")[1]);
                if (codecPreferred || rtxPreferred) {
                    putEntry(preferredPayloadTypes, codecName, payloadType);
                } else if (typeMismatched && !codecName.equals("rtx")) {
                    misMatchedPayloadTypes.add(payloadType);
                } else {
                    continue;
                }
            } else if (line.startsWith("a=rtcp-fb:") || line.startsWith("a=fmtp:")) {
                String payloadType = line.split(" ")[0].split(":")[1];
                if (!misMatchedPayloadTypes.contains(payloadType)
                        && !containsValue(preferredPayloadTypes, payloadType)) {
                    continue;
                }
            } else if (line.startsWith("m=audio")) {
                audioMLineIndex = newLines.size();
            } else if (line.startsWith("m=video")) {
                videoMLineIndex = newLines.size();
            }
            newLines.add(line);
        }

        if (!video && audioMLineIndex != -1) {
            newLines.set(audioMLineIndex, changeMLine(newLines.get(audioMLineIndex),
                    preferredCodecs,
                    preferredPayloadTypes));
        }
        if (video && videoMLineIndex != -1) {
            newLines.set(videoMLineIndex, changeMLine(newLines.get(videoMLineIndex),
                    preferredCodecs,
                    preferredPayloadTypes));
        }
        String newSdp = joinString(newLines, "\r\n", true);

        return new SessionDescription(originalSdp.type, newSdp);
    }

    private boolean containsValue(HashMap<String, ArrayList<String>> payloadTypes, String value) {
        for (ArrayList<String> v : payloadTypes.values()) {
            for (String s : v) {
                if (s.equals(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void putEntry(HashMap<String, ArrayList<String>> payloadTypes, String key,
            String value) {
        if (payloadTypes.containsKey(key)) {
            payloadTypes.get(key).add(value);
        } else {
            ArrayList<String> valueList = new ArrayList<>();
            valueList.add(value);
            payloadTypes.put(key, valueList);
        }
    }

    private String changeMLine(String mLine, LinkedHashSet<String> preferredCodecs,
            HashMap<String, ArrayList<String>> preferredPayloadTypes) {
        List<String> oldMLineParts = Arrays.asList(mLine.split(" "));
        List<String> mLineHeader = oldMLineParts.subList(0, 3);

        ArrayList<String> newMLineParts = new ArrayList<>(mLineHeader);
        for (String preferredCodec : preferredCodecs) {
            if (preferredPayloadTypes.containsKey(preferredCodec)) {
                newMLineParts.addAll(preferredPayloadTypes.get(preferredCodec));
            }
        }
        if (preferredPayloadTypes.containsKey("rtx")) {
            newMLineParts.addAll(preferredPayloadTypes.get("rtx"));
        }
        return joinString(newMLineParts, " ", false);
    }

    private String joinString(
            ArrayList<String> strings, String delimiter, boolean delimiterAtEnd) {
        Iterator<String> iter = strings.iterator();
        if (!iter.hasNext()) {
            return "";
        }
        StringBuilder buffer = new StringBuilder(iter.next());
        while (iter.hasNext()) {
            buffer.append(delimiter).append(iter.next());
        }
        if (delimiterAtEnd) {
            buffer.append(delimiter);
        }
        return buffer.toString();
    }

    private void setMaxBitrate(RtpSender sender, Integer bitrate) {
        if (sender == null) {
            return;
        }
        RtpParameters rtpParameters = sender.getParameters();
        if (rtpParameters == null) {
            Log.e(LOG_TAG, "Null rtp paramters");
            return;
        }
        for (RtpParameters.Encoding encoding : rtpParameters.encodings) {
            encoding.maxBitrateBps = bitrate == null ? null : bitrate * 1000;
        }
        if (!sender.setParameters(rtpParameters)) {
            Log.e(LOG_TAG, "Failed to configure max video bitrate");
        }
    }

    protected void setMaxBitrate(String mediaStreamId) {
        DCHECK(peerConnection);

        if (videoRtpSenders.get(mediaStreamId) != null) {
            setMaxBitrate(videoRtpSenders.get(mediaStreamId), videoMaxBitrate);
        }
        if (audioRtpSenders.get(mediaStreamId) != null) {
            setMaxBitrate(audioRtpSenders.get(mediaStreamId), audioMaxBitrate);
        }
    }

    protected void dispose() {
        pcExecutor.execute(() -> {
            synchronized (disposeLock) {
                disposed = true;
                if (peerConnection != null) {
                    peerConnection.dispose();
                }
                peerConnection = null;
            }
        });
    }

    //SdpObserver
    @Override
    public void onCreateSuccess(final SessionDescription sessionDescription) {
        localSdp = sessionDescription;

        if (audioCodecs != null) {
            localSdp = preferCodecs(localSdp, false);
        }

        if (videoCodecs != null) {
            localSdp = preferCodecs(localSdp, true);
        }

        callbackExecutor.execute(() -> {
            if (disposed) {
                return;
            }
            observer.onLocalDescription(key, localSdp);
        });

        pcExecutor.execute(() -> {
            if (disposed) {
                return;
            }
            peerConnection.setLocalDescription(PeerConnectionChannel.this, localSdp);
        });
    }

    @Override
    abstract public void onSetSuccess();

    @Override
    abstract public void onCreateFailure(final String error);

    @Override
    abstract public void onSetFailure(final String error);

    //PeerConnection.Observer interface
    @Override
    abstract public void onSignalingChange(PeerConnection.SignalingState signalingState);

    @Override
    abstract public void onIceConnectionChange(
            PeerConnection.IceConnectionState iceConnectionState);

    @Override
    public void onIceConnectionReceivingChange(boolean b) {
    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
    }

    @Override
    abstract public void onIceCandidate(IceCandidate iceCandidate);

    @Override
    abstract public void onIceCandidatesRemoved(IceCandidate[] iceCandidates);

    @Override
    abstract public void onAddStream(MediaStream mediaStream);

    @Override
    abstract public void onRemoveStream(final MediaStream mediaStream);

    @Override
    public void onDataChannel(final DataChannel dataChannel) {
        if (disposed) {
            return;
        }
        callbackExecutor.execute(() -> {
            localDataChannel = dataChannel;
            localDataChannel.registerObserver(PeerConnectionChannel.this);
        });
    }

    @Override
    abstract public void onRenegotiationNeeded();

    @Override
    public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
    }

    //DataChannel.Observer interface
    @Override
    public void onBufferedAmountChange(long l) {
    }

    @Override
    public void onStateChange() {
        if (disposed) {
            return;
        }
        callbackExecutor.execute(() -> {
            if (localDataChannel.state() == DataChannel.State.OPEN) {
                for (int i = 0; i < queuedMessage.size(); i++) {
                    ByteBuffer byteBuffer = ByteBuffer.wrap(
                            queuedMessage.get(i).getBytes(Charset.forName("UTF-8")));
                    DataChannel.Buffer buffer = new DataChannel.Buffer(byteBuffer, false);
                    localDataChannel.send(buffer);
                }
                queuedMessage.clear();
            }
        });
    }

    @Override
    public void onMessage(final DataChannel.Buffer buffer) {
        if (disposed) {
            return;
        }
        callbackExecutor.execute(() -> {
            ByteBuffer data = buffer.data;
            final byte[] bytes = new byte[data.capacity()];
            data.get(bytes);
            String message = new String(bytes);
            observer.onDataChannelMessage(key, message);
        });
    }

    public interface PeerConnectionChannelObserver {
        void onIceCandidate(String key, IceCandidate candidate);

        void onIceCandidatesRemoved(String key, IceCandidate[] candidates);

        void onLocalDescription(String key, SessionDescription localSdp);

        /**
         * @param recoverable true means SDK gotta reconfigure the peerconnection instead of
         * triggering failure actions.
         */
        void onError(String key, String errorMsg, boolean recoverable);

        void onEnded(String key);

        void onAddStream(String key, RemoteStream remoteStream);

        void onDataChannelMessage(String key, String message);

        void onRenegotiationRequest(String key);
    }

}
///@endcond
