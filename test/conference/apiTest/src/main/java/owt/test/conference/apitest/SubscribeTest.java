/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.test.conference.apitest;

import static owt.base.MediaCodecs.AudioCodec.G722;
import static owt.base.MediaCodecs.AudioCodec.ILBC;
import static owt.base.MediaCodecs.AudioCodec.ISAC;
import static owt.base.MediaCodecs.AudioCodec.OPUS;
import static owt.base.MediaCodecs.AudioCodec.PCMA;
import static owt.base.MediaCodecs.AudioCodec.PCMU;
import static owt.base.MediaCodecs.VideoCodec.H264;
import static owt.base.MediaCodecs.VideoCodec.VP8;
import static owt.base.MediaCodecs.VideoCodec.VP9;
import static owt.test.conference.util.ConferenceAction.applyOption;
import static owt.test.conference.util.ConferenceAction.createClient;
import static owt.test.conference.util.ConferenceAction.createPublishOptions;
import static owt.test.conference.util.ConferenceAction.createSubscribeOptions;
import static owt.test.conference.util.ConferenceAction.getRemoteForwardStream;
import static owt.test.conference.util.ConferenceAction.getRemoteMixStream;
import static owt.test.conference.util.ConferenceAction.getStats;
import static owt.test.conference.util.ConferenceAction.getToken;
import static owt.test.conference.util.ConferenceAction.join;
import static owt.test.conference.util.ConferenceAction.publish;
import static owt.test.conference.util.ConferenceAction.stop;
import static owt.test.conference.util.ConferenceAction.subscribe;
import static owt.test.util.CommonAction.checkRTCStats;
import static owt.test.util.CommonAction.createDefaultCapturer;
import static owt.test.util.CommonAction.createLocalStream;
import static owt.test.util.Config.AUDIO_ONLY_PRESENTER_ROLE;
import static owt.test.util.Config.MIXED_STREAM_SIZE;
import static owt.test.util.Config.PRESENTER_ROLE;
import static owt.test.util.Config.SLEEP;
import static owt.test.util.Config.TIMEOUT;
import static owt.test.util.Config.USER1_NAME;
import static owt.test.util.Config.USER2_NAME;
import static owt.test.util.Config.VIDEO_ONLY_VIEWER_ROLE;
import static owt.test.util.Config.VIEWER_ROLE;

import owt.base.MediaCodecs;
import owt.base.MediaCodecs.AudioCodec;
import owt.base.MediaCodecs.VideoCodec;
import owt.conference.Publication;
import owt.conference.PublishOptions;
import owt.conference.RemoteStream;
import owt.conference.SubscribeOptions;
import owt.conference.Subscription;
import owt.test.conference.util.ConferenceClientObserver;
import owt.test.util.Config;
import owt.test.util.FakeRenderer;
import owt.test.util.TestCallback;

import org.webrtc.RTCStatsReport;

import java.util.HashMap;
import java.util.List;

public class SubscribeTest extends TestBase {

    public void testSubscribe_beforeJoin_shouldFail() {
        client1 = createClient(null);
        client2 = createClient(null);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        RemoteStream mixSteam = getRemoteMixStream(client1);
        subscribe(client2, mixSteam, null, false, false);
        client2 = null;
    }

    public void testSubscribe_withoutOption_shouldSucceed() {
        observer1 = new ConferenceClientObserver(USER1_NAME, 1);
        client1 = createClient(observer1);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(true, capturer1);
        publish(client1, localStream1, null, observer1, true);
        RemoteStream mixSteam = getRemoteMixStream(client1);
        Subscription subscription = subscribe(client1, mixSteam, null, true, true);
        RTCStatsReport statsReport = getStats(subscription, true);
        HashMap<String, String> expectation = new HashMap<>();
        expectation.put("videoCodec", "VP8");
        checkRTCStats(statsReport, expectation, false, true, true);
    }

    public void testSubscribe_withVideoCodec_shouldSucceed() {
        VideoCodec[] videoCodecs = new VideoCodec[]{VP8, VP9, H264};
        String[] checkCodecs = new String[]{"vp8", "vp9", "h264"};
        client1 = createClient(null);
        observer2 = new ConferenceClientObserver(USER2_NAME, 1);
        client2 = createClient(observer2);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        join(client2, getToken(PRESENTER_ROLE, USER2_NAME), null, null, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(true, capturer1);
        PublishOptions publishOptions = createPublishOptions(new MediaCodecs.AudioCodec[]{},
                new VideoCodec[]{H264});
        publish(client1, localStream1, publishOptions, observer2, true);
        int streamsN = client2.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
        RemoteStream forwardStream = getRemoteForwardStream(client2, streamsN - 1);
        for (int i = 0; i < videoCodecs.length; i++) {
            SubscribeOptions subOption = createSubscribeOptions(new AudioCodec[]{},
                    new VideoCodec[]{videoCodecs[i]}, null);
            Subscription subscription = subscribe(client2, forwardStream, subOption, true, true);
            RTCStatsReport statsReport = getStats(subscription, true);
            HashMap<String, String> expectation = new HashMap<>();
            expectation.put("videoCodec", checkCodecs[i]);
            checkRTCStats(statsReport, expectation, false, true, true);
            stop(subscription, forwardStream, true);
        }
    }

    public void testSubscribe_withAudioCodec_shouldSucceed() {
        AudioCodec[] audioCodecs = new AudioCodec[]{OPUS, PCMU, PCMA, G722, ISAC, ILBC};
        String[] checkCodecs = new String[]{"opus", "pcmu", "pcma", "g722", "isac", "ilbc"};
        client1 = createClient(null);
        observer2 = new ConferenceClientObserver(USER2_NAME, 1);
        client2 = createClient(observer2);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        join(client2, getToken(PRESENTER_ROLE, USER2_NAME), null, null, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(true, capturer1);
        PublishOptions publishOptions = createPublishOptions(new MediaCodecs.AudioCodec[]{OPUS},
                new VideoCodec[]{});
        publish(client1, localStream1, publishOptions, observer2, true);
        int streamsN = client2.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
        RemoteStream forwardStream = getRemoteForwardStream(client2, streamsN - 1);
        for (int i = 0; i < audioCodecs.length; i++) {
            SubscribeOptions subOption = createSubscribeOptions(new AudioCodec[]{audioCodecs[i]},
                    new VideoCodec[]{}, null);
            Subscription subscription = subscribe(client2, forwardStream, subOption, true, true);
            RTCStatsReport statsReport = getStats(subscription, true);
            HashMap<String, String> expectation = new HashMap<>();
            expectation.put("audioCodec", checkCodecs[i]);
            checkRTCStats(statsReport, expectation, false, true, true);
            stop(subscription, forwardStream, true);
        }
    }

    public void testSubscribe_withBitrateMultiplier_shouldSucceed() {
        observer1 = new ConferenceClientObserver(USER1_NAME, 1);
        client1 = createClient(observer1);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(true, capturer1);
        publish(client1, localStream1, null, observer1, true);
        RemoteStream mixSteam = getRemoteMixStream(client1);
        List<Double> bitrateMultipliers =
                mixSteam.extraSubscriptionCapability.videoSubscriptionCapabilities.bitrateMultipliers;
        for (Double bitrateMultiplier : bitrateMultipliers) {
            HashMap<String, String> videoParams = new HashMap<>();
            videoParams.put("bitrateMultiplier", String.valueOf(bitrateMultiplier));
            SubscribeOptions subOption = createSubscribeOptions(new AudioCodec[]{},
                    new VideoCodec[]{}, videoParams);
            Subscription subscription = subscribe(client1, mixSteam, subOption, true, true);
            stop(subscription, mixSteam, true);
        }
    }

    public void testSubscribe_twiceOnSameStream_shouldFailAt2nd() {
        observer1 = new ConferenceClientObserver(USER1_NAME, 1);
        client1 = createClient(null);
        observer2 = new ConferenceClientObserver(USER2_NAME, 1);
        client2 = createClient(null);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        join(client2, getToken(PRESENTER_ROLE, USER2_NAME), null, null, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(true, capturer1);
        client1.addObserver(observer1);
        client2.addObserver(observer2);
        publish(client1, localStream1, null, observer1, true);
        RemoteStream remoteMixStream = getRemoteMixStream(client2);
        subscribe(client2, remoteMixStream, null, true, true);
        subscribe(client2, remoteMixStream, null, false, false);
    }

    public void testSubscribe_NullStream_shouldThrowException() {
        client1 = createClient(null);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        try {
            subscribe(client1, null, null, false, false);
            fail("RuntimeException expected.");
        } catch (RuntimeException ignored) {
        }
    }

    public void testSubscribe_videoOnly_shouldSucceed() {
        observer1 = new ConferenceClientObserver(USER1_NAME, 1);
        client1 = createClient(observer1);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(false, capturer1);
        publish(client1, localStream1, null, observer1, true);
        int streamsN = client1.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
        RemoteStream forwardStream = getRemoteForwardStream(client1, streamsN - 1);
        SubscribeOptions subOption = createSubscribeOptions(null, new MediaCodecs.VideoCodec[]{},
                null);
        Subscription subscription = subscribe(client1, forwardStream, subOption, true, true);
        RTCStatsReport statsReport = getStats(subscription, true);
        checkRTCStats(statsReport, null, false, false, true);
    }

    public void testSubscribe_audioOnly_shouldSucceed() {
        observer1 = new ConferenceClientObserver(USER1_NAME, 1);
        client1 = createClient(observer1);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        localStream1 = createLocalStream(true, null);
        publish(client1, localStream1, null, observer1, true);
        int streamsN = client1.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
        RemoteStream forwardStream = getRemoteForwardStream(client1, streamsN - 1);
        SubscribeOptions subOption = createSubscribeOptions(new MediaCodecs.AudioCodec[]{}, null,
                null);
        Subscription subscription = subscribe(client1, forwardStream, subOption, false, true);
        RTCStatsReport statsReport = getStats(subscription, true);
        checkRTCStats(statsReport, null, false, true, false);
    }

    public void testSubscribe_videoOnlyStreamWithAudioOnly_shouldSucceed() {
        observer1 = new ConferenceClientObserver(USER1_NAME, 1);
        client1 = createClient(observer1);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(false, capturer1);
        publish(client1, localStream1, null, observer1, true);
        int streamsN = client1.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
        RemoteStream forwardStream = getRemoteForwardStream(client1, streamsN - 1);
        SubscribeOptions subOption = createSubscribeOptions(new MediaCodecs.AudioCodec[]{}, null,
                null);
        subscribe(client1, forwardStream, subOption, false, false);
    }

    public void testSubscribe_audioOnlyStreamWithVideoOnly_shouldSucceed() {
        observer1 = new ConferenceClientObserver(USER1_NAME, 1);
        client1 = createClient(observer1);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        localStream1 = createLocalStream(true, null);
        publish(client1, localStream1, null, observer1, true);
        int streamsN = client1.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
        RemoteStream forwardStream = getRemoteForwardStream(client1, streamsN - 1);
        SubscribeOptions subOption = createSubscribeOptions(null, new MediaCodecs.VideoCodec[]{},
                null);
        subscribe(client1, forwardStream, subOption, false, false);
    }

    public void testSubscribe_withResolution_shouldSucceed() {
        observer1 = new ConferenceClientObserver(USER1_NAME, 1);
        client1 = createClient(observer1);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(true, capturer1);
        publish(client1, localStream1, null, observer1, true);
        RemoteStream mixSteam = getRemoteMixStream(client1);
        List<HashMap<String, Integer>> resolutions =
                mixSteam.extraSubscriptionCapability.videoSubscriptionCapabilities.resolutions;
        for (HashMap<String, Integer> resolution : resolutions) {
            HashMap<String, String> videoParams = new HashMap<>();
            videoParams.put("width", String.valueOf(resolution.get("width")));
            videoParams.put("height", String.valueOf(resolution.get("height")));
            SubscribeOptions subOption = createSubscribeOptions(new AudioCodec[]{},
                    new VideoCodec[]{}, videoParams);
            Subscription subscription = subscribe(client1, mixSteam, subOption, true, true);
            stop(subscription, mixSteam, true);
        }
    }

    public void testSubscribe_withViewerRole_shouldSucceed() {
        client1 = createClient(null);
        observer2 = new ConferenceClientObserver(USER2_NAME, 1);
        client2 = createClient(observer2);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        join(client2, getToken(VIEWER_ROLE, USER2_NAME), null, null, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(true, capturer1);
        publish(client1, localStream1, null, observer2, true);
        int streamsN = client2.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
        RemoteStream forwardStream = getRemoteForwardStream(client2, streamsN - 1);
        SubscribeOptions subOption = createSubscribeOptions(new AudioCodec[]{}, new VideoCodec[]{},
                null);
        subscribe(client2, forwardStream, subOption, true, true);
    }

    public void testSubscribe_withVideoOnlyViewer_shouldFail() {
        client1 = createClient(null);
        observer2 = new ConferenceClientObserver(USER2_NAME, 1);
        client2 = createClient(observer2);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        join(client2, getToken(VIDEO_ONLY_VIEWER_ROLE, USER2_NAME), null, null, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(true, capturer1);
        publish(client1, localStream1, null, observer2, true);
        int streamsN = client2.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
        RemoteStream forwardStream = getRemoteForwardStream(client2, streamsN - 1);
        SubscribeOptions subOption = createSubscribeOptions(new AudioCodec[]{}, new VideoCodec[]{},
                null);
        subscribe(client2, forwardStream, subOption, false, false);
    }

    public void testSubscribe_withAudioOnlyPresenter_shouldFail() {
        client1 = createClient(null);
        observer2 = new ConferenceClientObserver(USER2_NAME, 1);
        client2 = createClient(observer2);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        join(client2, getToken(AUDIO_ONLY_PRESENTER_ROLE, USER2_NAME), null, null, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(true, capturer1);
        publish(client1, localStream1, null, observer2, true);
        int streamsN = client2.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
        RemoteStream forwardStream = getRemoteForwardStream(client2, streamsN - 1);
        SubscribeOptions subOption = createSubscribeOptions(new AudioCodec[]{}, new VideoCodec[]{},
                null);
        subscribe(client2, forwardStream, subOption, false, false);
    }

    public void testSubscribe_audioOnlyByAudioOnlyPresenter_shouldSucceed() {
        client1 = createClient(null);
        observer2 = new ConferenceClientObserver(USER2_NAME, 1);
        client2 = createClient(observer2);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        join(client2, getToken(AUDIO_ONLY_PRESENTER_ROLE, USER2_NAME), null, null, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(true, capturer1);
        publish(client1, localStream1, null, observer2, true);
        int streamsN = client2.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
        RemoteStream forwardStream = getRemoteForwardStream(client2, streamsN - 1);
        SubscribeOptions subOption = createSubscribeOptions(new AudioCodec[]{}, null, null);
        subscribe(client2, forwardStream, subOption, false, true);
    }

    public void testSubscribe_videoOnlyWithAudioOnlyPresenter_shouldFail() {
        client1 = createClient(null);
        observer2 = new ConferenceClientObserver(USER2_NAME, 1);
        client2 = createClient(observer2);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        join(client2, getToken(AUDIO_ONLY_PRESENTER_ROLE, USER2_NAME), null, null, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(true, capturer1);
        publish(client1, localStream1, null, observer2, true);
        int streamsN = client2.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
        RemoteStream forwardStream = getRemoteForwardStream(client2, streamsN - 1);
        SubscribeOptions subOption = createSubscribeOptions(null, new VideoCodec[]{}, null);
        subscribe(client2, forwardStream, subOption, false, false);
    }

    public void testSubscribe_videoOnlyWithVideoOnlyViewer_shouldSucceed() {
        client1 = createClient(null);
        observer2 = new ConferenceClientObserver(USER2_NAME, 1);
        client2 = createClient(observer2);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        join(client2, getToken(VIDEO_ONLY_VIEWER_ROLE, USER2_NAME), null, null, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(true, capturer1);
        publish(client1, localStream1, null, observer2, true);
        int streamsN = client2.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
        RemoteStream forwardStream = getRemoteForwardStream(client2, streamsN - 1);
        SubscribeOptions subOption = createSubscribeOptions(null, new VideoCodec[]{}, null);
        subscribe(client2, forwardStream, subOption, true, true);
    }

    public void testSubscribe_audioWithVideoOnlyViewer_shouldFail() {
        client1 = createClient(null);
        observer2 = new ConferenceClientObserver(USER2_NAME, 1);
        client2 = createClient(observer2);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        join(client2, getToken(VIDEO_ONLY_VIEWER_ROLE, USER2_NAME), null, null, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(true, capturer1);
        publish(client1, localStream1, null, observer2, true);
        int streamsN = client2.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
        RemoteStream forwardStream = getRemoteForwardStream(client2, streamsN - 1);
        SubscribeOptions subOption = createSubscribeOptions(new AudioCodec[]{}, new VideoCodec[]{},
                null);
        subscribe(client2, forwardStream, subOption, false, false);
    }

    public void testSubscribe_withKeyFrameInterval_shouldSucceed() {
        observer1 = new ConferenceClientObserver(USER1_NAME, 1);
        client1 = createClient(observer1);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(true, capturer1);
        publish(client1, localStream1, null, observer1, true);
        int streamsN = client1.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
        RemoteStream forwardStream = getRemoteForwardStream(client1, streamsN - 1);
        List<Integer> keyFrameIntervals =
                forwardStream.extraSubscriptionCapability.videoSubscriptionCapabilities
                        .keyFrameIntervals;
        for (int keyFrameInterval : keyFrameIntervals) {
            HashMap<String, String> videoParams = new HashMap<>();
            videoParams.put("keyFrameInterval", String.valueOf(keyFrameInterval));
            SubscribeOptions subOption = createSubscribeOptions(new AudioCodec[]{},
                    new VideoCodec[]{}, videoParams);
            Subscription subscription = subscribe(client1, forwardStream, subOption, true, true);
            stop(subscription, forwardStream, true);
        }
    }

    public void testSubscribe_withFrameRate_shouldSucceed() {
        observer1 = new ConferenceClientObserver(USER1_NAME, 1);
        client1 = createClient(observer1);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(true, capturer1);
        publish(client1, localStream1, null, observer1, true);
        int streamsN = client1.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
        RemoteStream forwardStream = getRemoteForwardStream(client1, streamsN - 1);
        List<Integer> frameRates =
                forwardStream.extraSubscriptionCapability.videoSubscriptionCapabilities.frameRates;
        for (int frameRate : frameRates) {
            HashMap<String, String> videoParams = new HashMap<>();
            videoParams.put("frameRate", String.valueOf(frameRate));
            SubscribeOptions subOption = createSubscribeOptions(new AudioCodec[]{},
                    new VideoCodec[]{}, videoParams);
            Subscription subscription = subscribe(client1, forwardStream, subOption, true, true);
            stop(subscription, forwardStream, true);
        }
    }

    public void testSubscribe_differentStream_shouldSucceed() {
        observer1 = new ConferenceClientObserver(USER1_NAME, 1);
        client1 = createClient(observer1);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(true, capturer1);
        publish(client1, localStream1, null, observer1, true);
        RemoteStream mixSteam = getRemoteMixStream(client1);
        int streamsN = client1.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
        RemoteStream forwardStream = getRemoteForwardStream(client1, streamsN - 1);
        subscribe(client1, mixSteam, null, true, true);
        subscribe(client1, forwardStream, null, true, true);
    }

    public void testSubscribe_afterSubscriptionStop_shouldSucceed() {
        client1 = createClient(null);
        observer2 = new ConferenceClientObserver(USER2_NAME, 1);
        client2 = createClient(observer2);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        join(client2, getToken(PRESENTER_ROLE, USER2_NAME), null, null, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(true, capturer1);
        publish(client1, localStream1, null, observer2, true);
        int streamsN = client2.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
        RemoteStream forwardStream = getRemoteForwardStream(client2, streamsN - 1);
        Subscription subscription = subscribe(client2, forwardStream, null, true, true);
        stop(subscription, forwardStream, true);
        subscribe(client2, forwardStream, null, true, true);
    }

    public void testSubscribe_twiceWithoutWaitCallBack_shouldSucceed() {
        client1 = createClient(null);
        observer2 = new ConferenceClientObserver(USER2_NAME, 1);
        client2 = createClient(observer2);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        join(client2, getToken(PRESENTER_ROLE, USER2_NAME), null, null, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(true, capturer1);
        publish(client1, localStream1, null, observer2, true);
        int streamsN = client2.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
        RemoteStream forwardStream = getRemoteForwardStream(client2, streamsN - 1);
        TestCallback<Subscription> callback1 = new TestCallback<>();
        TestCallback<Subscription> callback2 = new TestCallback<>();
        client1.subscribe(forwardStream, callback1);
        client2.subscribe(forwardStream, callback2);
        assertTrue(callback1.getResult(true, TIMEOUT));
        assertTrue(callback2.getResult(true, TIMEOUT));
    }

    public void testSubscribe_onStreamEndedRemoteStream_shouldFail() {
        client1 = createClient(null);
        observer2 = new ConferenceClientObserver(USER2_NAME, 1);
        client2 = createClient(observer2);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        join(client2, getToken(PRESENTER_ROLE, USER2_NAME), null, null, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(true, capturer1);
        Publication publication = publish(client1, localStream1, null, observer2, true);
        assertTrue(observer2.getResultForPublish(Config.TIMEOUT));
        int streamsN = client2.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
        RemoteStream forwardStream = getRemoteForwardStream(client2, streamsN - 1);
        Subscription subscription = subscribe(client2, forwardStream, null, true, true);
        stop(publication, observer2, true);
        subscription.stop();
        subscribe(client2, forwardStream, null, false, false);
    }

    public void testSubscribe_applyOption_shouldSucceed() {
        observer1 = new ConferenceClientObserver(USER1_NAME, 1);
        client1 = createClient(observer1);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(true, capturer1);
        publish(client1, localStream1, null, observer1, true);
        RemoteStream mixSteam = getRemoteMixStream(client1);
        Subscription subscription = subscribe(client1, mixSteam, null, true, true);
        HashMap<String, String> videoParams = new HashMap<>();
        HashMap<String, Integer> resolution =
                mixSteam.extraSubscriptionCapability.videoSubscriptionCapabilities.resolutions.get(0);
        videoParams.put("width", String.valueOf(resolution.get("width")));
        videoParams.put("height", String.valueOf(resolution.get("height")));
        applyOption(subscription, videoParams, true);
        FakeRenderer renderer = new FakeRenderer();
        mixSteam.attach(renderer);
        assertTrue(localStream1.hasVideo() == (renderer.getFramesRendered(SLEEP) != 0));
        assertTrue(renderer.frameHeight() == resolution.get("height"));
        assertTrue(renderer.frameWidth() == resolution.get("width"));
    }

    public void testSubscribe_applyOptionWrongParameter_shouldFail() {
        observer1 = new ConferenceClientObserver(USER1_NAME, 1);
        client1 = createClient(observer1);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(true, capturer1);
        publish(client1, localStream1, null, observer1, true);
        RemoteStream mixSteam = getRemoteMixStream(client1);
        Subscription subscription = subscribe(client1, mixSteam, null, true, true);
        HashMap<String, String> videoParams = new HashMap<>();
        videoParams.put("width", "-1");
        videoParams.put("height", "-1");
        applyOption(subscription, videoParams, false);
        videoParams = new HashMap<>();
        videoParams.put("bitrateMultiplier", "-1");
        applyOption(subscription, videoParams, false);
        videoParams = new HashMap<>();
        videoParams.put("frameRate", "-1");
        applyOption(subscription, videoParams, false);
        videoParams = new HashMap<>();
        videoParams.put("keyFrameInterval", "-1");
        applyOption(subscription, videoParams, false);
    }
}
