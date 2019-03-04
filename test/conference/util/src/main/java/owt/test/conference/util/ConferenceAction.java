/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.test.conference.util;

import static owt.base.MediaCodecs.AudioCodec;
import static owt.base.MediaCodecs.VideoCodec;
import static owt.base.MediaConstraints.TrackKind.AUDIO_AND_VIDEO;
import static owt.test.util.Config.CONFERENCE_ROOM_ID;
import static owt.test.util.Config.CONFERENCE_SERVER_HTTP;
import static owt.test.util.Config.SLEEP;
import static owt.test.util.Config.TIMEOUT;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import owt.base.AudioCodecParameters;
import owt.base.AudioEncodingParameters;
import owt.base.LocalStream;
import owt.base.MediaConstraints.TrackKind;
import owt.base.VideoCodecParameters;
import owt.base.VideoEncodingParameters;
import owt.conference.ConferenceClient;
import owt.conference.ConferenceClientConfiguration;
import owt.conference.ConferenceInfo;
import owt.conference.Participant;
import owt.conference.Publication;
import owt.conference.PublishOptions;
import owt.conference.RemoteMixedStream;
import owt.conference.RemoteStream;
import owt.conference.SubscribeOptions;
import owt.conference.SubscribeOptions.AudioSubscriptionConstraints;
import owt.conference.SubscribeOptions.VideoSubscriptionConstraints;
import owt.conference.Subscription;
import owt.test.util.FakeRenderer;
import owt.test.util.TestCallback;
import owt.test.util.TestObserver;

import org.webrtc.RTCStatsReport;

import java.util.ArrayList;
import java.util.HashMap;

public class ConferenceAction {

    /**
     * Get token for the MCU server and room id configured by the config.xml.
     * Do not use this if you need to connect to a different MCU or room.
     */
    public static String getToken(String role, String name) {
        return HttpUtils.getToken(CONFERENCE_SERVER_HTTP, role, name, CONFERENCE_ROOM_ID);
    }

    /**
     * Create a ConferenceClient without verifying SSL certificate.
     * Do not use this method to create a client if you need to set up SSL context.
     */
    public static ConferenceClient createClient(ConferenceClientObserver newObserver) {
        HttpUtils.setUpINSECURESSLContext();
        ConferenceClientConfiguration configuration = ConferenceClientConfiguration
                .builder().setHostnameVerifier(HttpUtils.hostnameVerifier)
                .setSSLContext(HttpUtils.sslContext).build();
        ConferenceClient clientUser = new ConferenceClient(configuration);
        if (newObserver != null) {
            clientUser.addObserver(newObserver);
        }
        return clientUser;
    }

    /**
     * @param client ConferenceClient.
     * @param token token.
     * @param selfObserver |client|'s observer for adding previously joined users information.
     * @param otherObserver observer of another ConferenceClient rather than |client|.
     * @param expectation expectation.
     */
    public static ConferenceInfo join(ConferenceClient client, String token,
            ConferenceClientObserver selfObserver, ConferenceClientObserver otherObserver,
            boolean expectation) {
        if (otherObserver != null) {
            otherObserver.clearStatus(1);
        }
        TestCallback<ConferenceInfo> callback = new TestCallback<>();
        client.join(token, callback);
        assertTrue(callback.getResult(expectation, TIMEOUT));
        // add participants that previously joined before me.
        if (selfObserver != null) {
            for (Participant participant : callback.successCallbackResult.getParticipants()) {
                TestObserver participantObserver = new TestObserver(selfObserver.name,
                        participant.id);
                participant.addObserver(participantObserver);
                selfObserver.participants.add(participant);
                selfObserver.participantObservers.put(participant.id, participantObserver);
            }
        }
        if (expectation && otherObserver != null) {
            assertTrue(otherObserver.getResultForJoin(TIMEOUT));
        }
        return callback.successCallbackResult;
    }

    /**
     * @param client ConferenceClient.
     * @param stream LocalStream.
     * @param options PublishOptions.
     * @param anyObserver observer to watch the events.
     * @param expectation expectation.
     */
    public static Publication publish(ConferenceClient client, LocalStream stream,
            PublishOptions options, ConferenceClientObserver anyObserver, boolean expectation) {
        if (anyObserver != null) {
            anyObserver.clearStatus(1);
        }
        TestCallback<Publication> callback = new TestCallback<>();
        client.publish(stream, options, callback);
        assertTrue(callback.getResult(expectation, TIMEOUT));
        if (expectation && anyObserver != null) {
            assertTrue(anyObserver.getResultForPublish(TIMEOUT));
        }
        return callback.successCallbackResult;
    }

    /**
     * @param publication Publication.
     * @param anyObserver observer of which StreamObserver will be watched.
     * @param expectation expectation.
     */
    public static void stop(Publication publication,
            ConferenceClientObserver anyObserver, boolean expectation) {
        PubSubObserver pubSubObserver = new PubSubObserver(1);
        publication.addObserver(pubSubObserver);
        publication.stop();
        if (expectation) {
            assertTrue(pubSubObserver.getResultForEnded(TIMEOUT));
            if (anyObserver != null) {
                assertTrue(anyObserver.streamObservers.get(publication.id()).getResult(TIMEOUT));
            }
        }
    }

    /**
     * @param client ConferenceClient.
     * @param stream RemoteStream.
     * @param options SubscribeOptions.
     * @param checkFrame If to check frame rendered.
     * @param expectation expectation.
     */
    public static Subscription subscribe(ConferenceClient client, RemoteStream stream,
            SubscribeOptions options, boolean checkFrame, boolean expectation) {
        TestCallback<Subscription> callback = new TestCallback<>();
        client.subscribe(stream, options, callback);
        assertTrue(callback.getResult(expectation, TIMEOUT));
        if (checkFrame) {
            FakeRenderer renderer = new FakeRenderer();
            stream.attach(renderer);
            assertTrue(stream.hasVideo() == (renderer.getFramesRendered(SLEEP) != 0));
        }
        return callback.successCallbackResult;
    }

    public static void stop(Subscription subscription, RemoteStream remoteStream,
            boolean expectation) {
        FakeRenderer renderer = null;
        if (remoteStream != null) {
            renderer = new FakeRenderer();
            remoteStream.attach(renderer);
        }
        PubSubObserver subObserver = new PubSubObserver(1);
        subscription.addObserver(subObserver);
        subscription.stop();
        if (expectation) {
            assertTrue(subObserver.getResultForEnded(TIMEOUT));
            if (remoteStream != null) {
                assertEquals(renderer.getFramesRendered(SLEEP),
                        renderer.getFramesRendered(SLEEP));
            }
        }
    }

    /**
     * @param otherObserver observer of another ConferenceClient rather than |client|.
     * @param expectation expectation.
     */
    public static void send(ConferenceClient client, String receiver, String message,
            ConferenceClientObserver otherObserver, boolean expectation) {
        if (otherObserver != null) {
            otherObserver.clearStatus(1);
        }
        TestCallback<Void> callback = new TestCallback<>();
        client.send(receiver, message, callback);
        assertTrue(callback.getResult(expectation, TIMEOUT));
        if (expectation && otherObserver != null) {
            assertTrue(otherObserver.getResultForSend(TIMEOUT));
            assertEquals(message, otherObserver.lastRecvMsg);
            assertEquals(client.info().self().id, otherObserver.lastSenderId);
        }
    }

    /**
     * @param client ConferenceClient.
     * @param selfObserver |client|'s observer..
     * @param otherObserver observer of another ConferenceClient rather than |client|.
     */
    public static void leave(ConferenceClient client, ConferenceClientObserver selfObserver,
            ConferenceClientObserver otherObserver) {
        if (selfObserver != null) {
            selfObserver.clearStatus(1);
        }
        String id = client.info().self().id;
        client.leave();
        if (selfObserver != null) {
            assertTrue(selfObserver.getResultForLeave(TIMEOUT));
        }
        if (otherObserver != null) {
            assertTrue(otherObserver.participantObservers.get(id).getResult(TIMEOUT));
        }
    }

    /**
     * @param publication Publication to be muted.
     * @param trackKind track kind.
     * @param pubObserver observer to watch Publication.onMute event.
     * @param subObserver observer to watch Subscription.onMute event.
     * @param expectation expectation.
     */
    public static void mute(Publication publication, TrackKind trackKind,
            PubSubObserver pubObserver, PubSubObserver subObserver, boolean expectation) {
        if (pubObserver != null) {
            pubObserver.clearStatus(trackKind == AUDIO_AND_VIDEO ? 2 : 1);
        }
        if (subObserver != null) {
            subObserver.clearStatus(trackKind == AUDIO_AND_VIDEO ? 2 : 1);
        }
        TestCallback<Void> callback = new TestCallback<>();
        publication.mute(trackKind, callback);
        assertTrue(callback.getResult(expectation, TIMEOUT));
        if (expectation && pubObserver != null) {
            assertTrue(pubObserver.getResultForMute(trackKind, TIMEOUT));
            if (subObserver != null) {
                assertTrue(subObserver.getResultForMute(trackKind, TIMEOUT));
            }
        }
    }

    /**
     * @param subscription Subscribe to be muted.
     * @param trackKind track kind.
     * @param subObserver observer to watch Subscription.onMute event.
     * @param expectation expectation.
     */
    public static void mute(Subscription subscription, TrackKind trackKind,
            PubSubObserver subObserver, boolean expectation) {
        if (subObserver != null) {
            subObserver.clearStatus(1);
        }
        TestCallback<Void> callback = new TestCallback<>();
        subscription.mute(trackKind, callback);
        assertTrue(callback.getResult(expectation, TIMEOUT));
        if (expectation && subObserver != null) {
            assertTrue(subObserver.getResultForMute(trackKind, TIMEOUT));
        }
    }

    /**
     * @param publication Publication to be muted.
     * @param trackKind track kind.
     * @param pubObserver observer to watch Publication.onUnmute event.
     * @param subObserver observer to watch Subscription.onUnmute event.
     * @param expectation expectation.
     */
    public static void unmute(Publication publication,
            TrackKind trackKind, PubSubObserver pubObserver, PubSubObserver subObserver,
            boolean expectation) {
        if (pubObserver != null) {
            pubObserver.clearStatus(trackKind == AUDIO_AND_VIDEO ? 2 : 1);
        }
        if (subObserver != null) {
            subObserver.clearStatus(trackKind == AUDIO_AND_VIDEO ? 2 : 1);
        }
        TestCallback<Void> callback = new TestCallback<>();
        publication.unmute(trackKind, callback);
        assertTrue(callback.getResult(expectation, TIMEOUT));
        if (expectation && pubObserver != null) {
            assertTrue(pubObserver.getResultForUnmute(trackKind, TIMEOUT));
            if (subObserver != null) {
                assertTrue(subObserver.getResultForUnmute(trackKind, TIMEOUT));
            }
        }
    }

    /**
     * @param subscription Subscribe to be muted.
     * @param trackKind track kind.
     * @param subObserver observer to watch Subscription.onUnmute event.
     * @param expectation expectation.
     */
    public static void unmute(Subscription subscription, TrackKind trackKind,
            PubSubObserver subObserver, boolean expectation) {
        if (subObserver != null) {
            subObserver.clearStatus(1);
        }
        TestCallback<Void> callback = new TestCallback<>();
        subscription.unmute(trackKind, callback);
        assertTrue(callback.getResult(expectation, TIMEOUT));
        if (expectation && subObserver != null) {
            assertTrue(subObserver.getResultForUnmute(trackKind, TIMEOUT));
        }
    }

    /**
     * @param publication publication.
     * @param expectation expectation.
     */
    public static RTCStatsReport getStats(Publication publication, boolean expectation) {
        TestCallback<RTCStatsReport> callback = new TestCallback<>();
        publication.getStats(callback);
        assertTrue(callback.getResult(expectation, TIMEOUT));
        return callback.successCallbackResult;
    }

    /**
     * @param subscription subscription.
     * @param expectation expectation.
     */
    public static RTCStatsReport getStats(Subscription subscription, boolean expectation) {
        TestCallback<RTCStatsReport> callback = new TestCallback<>();
        subscription.getStats(callback);
        assertTrue(callback.getResult(expectation, TIMEOUT));
        return callback.successCallbackResult;
    }

    public static RemoteMixedStream getRemoteMixStream(ConferenceClient client) {
        for (RemoteStream remoteStream : client.info().getRemoteStreams()) {
            if (remoteStream instanceof RemoteMixedStream) {
                return (RemoteMixedStream) remoteStream;
            }
        }
        fail("No mixed stream found.");
        return null;
    }

    public static RemoteStream getRemoteForwardStream(ConferenceClient client, int index) {
        int i = 0;
        for (RemoteStream remoteStream : client.info().getRemoteStreams()) {
            if (!(remoteStream instanceof RemoteMixedStream)) {
                if (i == index) {
                    return remoteStream;
                } else {
                    i++;
                }
            }
        }
        fail("No expected forward stream found.");
        return null;
    }

    public static PublishOptions createPublishOptions(AudioCodec[] audioCodecs,
            VideoCodec[] videoCodecs) {
        return createPublishOptions(audioCodecs, 0, videoCodecs, 0);
    }

    public static PublishOptions createPublishOptions(AudioCodec[] audioCodecs,
            int audioMaxBitrate, VideoCodec[] videoCodecs, int videoMaxBitrate) {
        ArrayList<VideoEncodingParameters> videoEncodingParameters = new ArrayList<>();
        if (videoCodecs != null) {
            for (VideoCodec videoCodec : videoCodecs) {
                videoEncodingParameters.add(
                        new VideoEncodingParameters(new VideoCodecParameters(videoCodec)));
            }
            if (videoMaxBitrate != 0) {
                VideoEncodingParameters.maxBitrate = videoMaxBitrate;
            }
        }

        ArrayList<AudioEncodingParameters> audioEncodingParameters = new ArrayList<>();
        if (audioCodecs != null) {
            for (AudioCodec audioCodec : audioCodecs) {
                audioEncodingParameters.add(
                        new AudioEncodingParameters(new AudioCodecParameters(audioCodec)));
            }
            if (audioMaxBitrate != 0) {
                AudioEncodingParameters.maxBitrate = audioMaxBitrate;
            }
        }

        PublishOptions.Builder option = PublishOptions.builder();
        for (AudioEncodingParameters audioParameter : audioEncodingParameters) {
            option.addAudioParameter(audioParameter);
        }
        for (VideoEncodingParameters videoParameter : videoEncodingParameters) {
            option.addVideoParameter(videoParameter);
        }
        return option.build();
    }

    public static SubscribeOptions createSubscribeOptions(AudioCodec[] audioCodecs,
            VideoCodec[] videoCodecs, HashMap<String, String> videoParams) {
        AudioSubscriptionConstraints aOptions = null;
        if (audioCodecs != null) {
            AudioSubscriptionConstraints.Builder aBuilder = AudioSubscriptionConstraints.builder();
            for (AudioCodec audioCodec : audioCodecs) {
                aBuilder.addCodec(new AudioCodecParameters(audioCodec));
            }
            aOptions = aBuilder.build();
        }

        VideoSubscriptionConstraints vOptions = null;
        if (videoCodecs != null || videoParams != null) {
            VideoSubscriptionConstraints.Builder vBuilder = VideoSubscriptionConstraints.builder();
            if (videoCodecs != null) {
                for (VideoCodec videoCodec : videoCodecs) {
                    vBuilder.addCodec(new VideoCodecParameters(videoCodec));
                }
            }
            if (videoParams != null) {
                if (videoParams.containsKey("bitrateMultiplier")) {
                    vBuilder.setBitrateMultiplier(
                            Double.parseDouble(videoParams.get("bitrateMultiplier")));
                }
                if (videoParams.containsKey("frameRate")) {
                    vBuilder.setFrameRate(Integer.valueOf(videoParams.get("frameRate")));
                }
                if (videoParams.containsKey("width") && videoParams.containsKey("height")) {
                    int width = Integer.valueOf(videoParams.get("width"));
                    int height = Integer.valueOf(videoParams.get("height"));
                    vBuilder.setResolution(width, height);
                }
                if (videoParams.containsKey("keyFrameInterval")) {
                    vBuilder.setKeyFrameInterval(
                            Integer.valueOf(videoParams.get("keyFrameInterval")));
                }
            }
            vOptions = vBuilder.build();
        }

        SubscribeOptions.Builder subOptionBuilder = SubscribeOptions.builder(aOptions != null,
                vOptions != null);
        subOptionBuilder.setAudioOption(aOptions);
        subOptionBuilder.setVideoOption(vOptions);
        return subOptionBuilder.build();
    }

    public static void applyOption(Subscription subscription, HashMap<String, String> videoParams,
            boolean expectation) {
        Subscription.VideoUpdateOptions videoUpdateOptions = new Subscription.VideoUpdateOptions();
        if (videoParams != null) {
            if (videoParams.containsKey("bitrateMultiplier")) {
                videoUpdateOptions.bitrateMultiplier = Double.parseDouble(
                        videoParams.get("bitrateMultiplier"));
            }
            if (videoParams.containsKey("frameRate")) {
                videoUpdateOptions.fps = Integer.valueOf(videoParams.get("frameRate"));
            }
            if (videoParams.containsKey("width")) {
                videoUpdateOptions.resolutionWidth = Integer.valueOf(videoParams.get("width"));
            }
            if (videoParams.containsKey("height")) {
                videoUpdateOptions.resolutionHeight = Integer.valueOf(videoParams.get("height"));
            }
            if (videoParams.containsKey("keyFrameInterval")) {
                videoUpdateOptions.keyframeInterval = Integer.valueOf(
                        videoParams.get("keyFrameInterval"));
            }
        }
        TestCallback<Void> callback = new TestCallback<>();
        subscription.applyOptions(videoUpdateOptions, callback);
        assertTrue(callback.getResult(expectation, TIMEOUT));
    }
}
