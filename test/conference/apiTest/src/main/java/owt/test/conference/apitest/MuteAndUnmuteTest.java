/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.test.conference.apitest;

import static owt.test.conference.util.ConferenceAction.createClient;
import static owt.test.conference.util.ConferenceAction.createSubscribeOptions;
import static owt.test.conference.util.ConferenceAction.getRemoteForwardStream;
import static owt.test.conference.util.ConferenceAction.getToken;
import static owt.test.conference.util.ConferenceAction.join;
import static owt.test.conference.util.ConferenceAction.mute;
import static owt.test.conference.util.ConferenceAction.publish;
import static owt.test.conference.util.ConferenceAction.stop;
import static owt.test.conference.util.ConferenceAction.subscribe;
import static owt.test.conference.util.ConferenceAction.unmute;
import static owt.test.util.CommonAction.createDefaultCapturer;
import static owt.test.util.CommonAction.createLocalStream;
import static owt.test.util.Config.MIXED_STREAM_SIZE;
import static owt.test.util.Config.PRESENTER_ROLE;
import static owt.test.util.Config.USER1_NAME;

import owt.base.MediaCodecs;
import owt.base.MediaCodecs.VideoCodec;
import owt.base.MediaConstraints.TrackKind;
import owt.conference.Publication;
import owt.conference.RemoteStream;
import owt.conference.SubscribeOptions;
import owt.conference.Subscription;
import owt.test.conference.util.ConferenceAction;
import owt.test.conference.util.ConferenceClientObserver;
import owt.test.conference.util.PubSubObserver;

public class MuteAndUnmuteTest extends TestBase {

    public void testMuteAndUnmute_stoppedPublication_shouldFail() {
        client1 = createClient(null);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(true, capturer1);
        Publication publication = publish(client1, localStream1, null, null, true);
        ConferenceAction.stop(publication, null, true);
        mute(publication, TrackKind.AUDIO_AND_VIDEO, null, null, false);
        unmute(publication, TrackKind.AUDIO_AND_VIDEO, null, null, false);
    }

    public void testMuteAndUnmute_videoOnPublicationWithAudioAndVideo_shouldSucceed() {
        observer1 = new ConferenceClientObserver(USER1_NAME, 1);
        client1 = createClient(observer1);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(true, capturer1);
        Publication publication = publish(client1, localStream1, null, observer1, true);
        int streamsN = client1.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
        RemoteStream forwardStream = getRemoteForwardStream(client1, streamsN - 1);
        Subscription subscription = subscribe(client1, forwardStream, null, true, true);
        PubSubObserver pubObserver = new PubSubObserver(1);
        publication.addObserver(pubObserver);
        PubSubObserver subObserver = new PubSubObserver(1);
        subscription.addObserver(subObserver);
        mute(publication, TrackKind.VIDEO, pubObserver, subObserver, true);
        unmute(publication, TrackKind.VIDEO, pubObserver, subObserver, true);
        publication.removeObserver(pubObserver);
        subscription.removeObserver(subObserver);
    }

    public void testMuteAndUnmute_audioOnPublicationWithAudioAndVideo_shouldSucceed() {
        observer1 = new ConferenceClientObserver(USER1_NAME, 1);
        client1 = createClient(observer1);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(true, capturer1);
        Publication publication = publish(client1, localStream1, null, observer1, true);
        int streamsN = client1.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
        RemoteStream forwardStream = getRemoteForwardStream(client1, streamsN - 1);
        Subscription subscription = subscribe(client1, forwardStream, null, true, true);
        PubSubObserver pubObserver = new PubSubObserver(1);
        publication.addObserver(pubObserver);
        PubSubObserver subObserver = new PubSubObserver(1);
        subscription.addObserver(subObserver);
        mute(publication, TrackKind.AUDIO, pubObserver, subObserver, true);
        unmute(publication, TrackKind.AUDIO, pubObserver, subObserver, true);
        publication.removeObserver(pubObserver);
        subscription.removeObserver(subObserver);
    }

    public void testMuteAndUnmute_audioAndVideoOnPublicationWithAudioAndVideo_shouldSucceed() {
        observer1 = new ConferenceClientObserver(USER1_NAME, 1);
        client1 = createClient(observer1);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(true, capturer1);
        Publication publication = publish(client1, localStream1, null, observer1, true);
        int streamsN = client1.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
        RemoteStream forwardStream = getRemoteForwardStream(client1, streamsN - 1);
        Subscription subscription = subscribe(client1, forwardStream, null, true, true);
        PubSubObserver pubObserver = new PubSubObserver(1);
        publication.addObserver(pubObserver);
        PubSubObserver subObserver = new PubSubObserver(1);
        subscription.addObserver(subObserver);
        mute(publication, TrackKind.AUDIO_AND_VIDEO, pubObserver, subObserver, true);
        unmute(publication, TrackKind.AUDIO_AND_VIDEO, pubObserver, subObserver, true);
        publication.removeObserver(pubObserver);
        subscription.removeObserver(subObserver);
    }

    public void testMuteAndUnmute_videoOnPublicationWithVideoOnly_shouldSucceed() {
        observer1 = new ConferenceClientObserver(USER1_NAME, 1);
        client1 = createClient(observer1);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(false, capturer1);
        Publication publication = publish(client1, localStream1, null, observer1, true);
        int streamsN = client1.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
        RemoteStream forwardStream = getRemoteForwardStream(client1, streamsN - 1);
        SubscribeOptions subOption = createSubscribeOptions(null, new MediaCodecs.VideoCodec[]{},
                null);
        Subscription subscription = subscribe(client1, forwardStream, subOption, true, true);
        PubSubObserver pubObserver = new PubSubObserver(1);
        publication.addObserver(pubObserver);
        PubSubObserver subObserver = new PubSubObserver(1);
        subscription.addObserver(subObserver);
        mute(publication, TrackKind.VIDEO, pubObserver, subObserver, true);
        unmute(publication, TrackKind.VIDEO, pubObserver, subObserver, true);
        publication.removeObserver(pubObserver);
        subscription.removeObserver(subObserver);
    }

    public void testMuteAndUnmute_audioOnPublicationWithVideoOnly_shouldFail() {
        observer1 = new ConferenceClientObserver(USER1_NAME, 1);
        client1 = createClient(observer1);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(false, capturer1);
        Publication publication = publish(client1, localStream1, null, observer1, true);
        mute(publication, TrackKind.AUDIO, null, null, false);
        unmute(publication, TrackKind.AUDIO, null, null, false);
    }

    public void testMuteAndUnmute_audioAndVideoOnPublicationWithVideoOnly_shouldFail() {
        observer1 = new ConferenceClientObserver(USER1_NAME, 1);
        client1 = createClient(observer1);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(false, capturer1);
        Publication publication = publish(client1, localStream1, null, observer1, true);
        mute(publication, TrackKind.AUDIO_AND_VIDEO, null, null, false);
        unmute(publication, TrackKind.AUDIO_AND_VIDEO, null, null, false);
    }

    public void testMuteAndUnmute_audioOnPublicationWithAudioOnly_shouldSucceed() {
        observer1 = new ConferenceClientObserver(USER1_NAME, 1);
        client1 = createClient(observer1);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        localStream1 = createLocalStream(true, null);
        Publication publication = publish(client1, localStream1, null, observer1, true);
        int streamsN = client1.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
        RemoteStream forwardStream = getRemoteForwardStream(client1, streamsN - 1);
        SubscribeOptions subOption = createSubscribeOptions(new MediaCodecs.AudioCodec[]{}, null,
                null);
        Subscription subscription = subscribe(client1, forwardStream, subOption, false, true);
        PubSubObserver pubObserver = new PubSubObserver(1);
        publication.addObserver(pubObserver);
        PubSubObserver subObserver = new PubSubObserver(1);
        subscription.addObserver(subObserver);
        mute(publication, TrackKind.AUDIO, pubObserver, subObserver, true);
        unmute(publication, TrackKind.AUDIO, pubObserver, subObserver, true);
        publication.removeObserver(pubObserver);
        subscription.removeObserver(subObserver);
    }

    public void testMuteAndUnmute_videoOnPublicationWithAudioOnly_shouldFail() {
        client1 = createClient(null);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        localStream1 = createLocalStream(true, null);
        Publication publication = publish(client1, localStream1, null, null, true);
        mute(publication, TrackKind.VIDEO, null, null, false);
        unmute(publication, TrackKind.VIDEO, null, null, false);
    }

    public void testMuteAndUnmute_audioAndVideoOnPublicationWithAudioOnly_shouldFail() {
        client1 = createClient(null);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        localStream1 = createLocalStream(true, null);
        Publication publication = publish(client1, localStream1, null, null, true);
        mute(publication, TrackKind.AUDIO_AND_VIDEO, null, null, false);
        unmute(publication, TrackKind.AUDIO_AND_VIDEO, null, null, false);
    }

    public void testMuteAndUnmute_stoppedSubscription_shouldFail() {
        observer1 = new ConferenceClientObserver(USER1_NAME, 1);
        client1 = createClient(observer1);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(true, capturer1);
        publish(client1, localStream1, null, observer1, true);
        int streamsN = client1.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
        RemoteStream forwardStream = getRemoteForwardStream(client1, streamsN - 1);
        Subscription subscription = subscribe(client1, forwardStream, null, true, true);
        stop(subscription, forwardStream, true);
        mute(subscription, TrackKind.AUDIO_AND_VIDEO, null, false);
        unmute(subscription, TrackKind.AUDIO_AND_VIDEO, null, false);
    }

    public void testMuteAndUnmute_videoOnSubscriptionWithAudioAndVideo_shouldSucceed() {
        observer1 = new ConferenceClientObserver(USER1_NAME, 1);
        client1 = createClient(observer1);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(true, capturer1);
        publish(client1, localStream1, null, observer1, true);
        int streamsN = client1.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
        RemoteStream forwardStream = getRemoteForwardStream(client1, streamsN - 1);
        Subscription subscription = subscribe(client1, forwardStream, null, true, true);
        PubSubObserver subObserver = new PubSubObserver(1);
        subscription.addObserver(subObserver);
        mute(subscription, TrackKind.VIDEO, subObserver, true);
        unmute(subscription, TrackKind.VIDEO, subObserver, true);
        subscription.removeObserver(subObserver);
    }

    public void testMuteAndUnmute_audioOnSubscriptionWithAudioAndVideo_shouldSucceed() {
        observer1 = new ConferenceClientObserver(USER1_NAME, 1);
        client1 = createClient(observer1);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(true, capturer1);
        publish(client1, localStream1, null, observer1, true);
        int streamsN = client1.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
        RemoteStream forwardStream = getRemoteForwardStream(client1, streamsN - 1);
        Subscription subscription = subscribe(client1, forwardStream, null, true, true);
        PubSubObserver subObserver = new PubSubObserver(1);
        subscription.addObserver(subObserver);
        mute(subscription, TrackKind.AUDIO, subObserver, true);
        unmute(subscription, TrackKind.AUDIO, subObserver, true);
        subscription.removeObserver(subObserver);
    }

    public void testMuteAndUnmute_audioAndVideoOnSubscriptionWithAudioAndVideo_shouldSucceed() {
        observer1 = new ConferenceClientObserver(USER1_NAME, 1);
        client1 = createClient(observer1);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(true, capturer1);
        publish(client1, localStream1, null, observer1, true);
        int streamsN = client1.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
        RemoteStream forwardStream = getRemoteForwardStream(client1, streamsN - 1);
        Subscription subscription = subscribe(client1, forwardStream, null, true, true);
        PubSubObserver subObserver = new PubSubObserver(1);
        subscription.addObserver(subObserver);
        mute(subscription, TrackKind.AUDIO_AND_VIDEO, subObserver, true);
        unmute(subscription, TrackKind.AUDIO_AND_VIDEO, subObserver, true);
        subscription.removeObserver(subObserver);
    }

    public void testMuteAndUnmute_videoOnSubscriptionWithVideoOnly_shouldSucceed() {
        observer1 = new ConferenceClientObserver(USER1_NAME, 1);
        client1 = createClient(observer1);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(false, capturer1);
        publish(client1, localStream1, null, observer1, true);
        int streamsN = client1.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
        RemoteStream forwardStream = getRemoteForwardStream(client1, streamsN - 1);
        SubscribeOptions subOption = createSubscribeOptions(null, new VideoCodec[]{}, null);
        Subscription subscription = subscribe(client1, forwardStream, subOption, true, true);
        PubSubObserver subObserver = new PubSubObserver(1);
        subscription.addObserver(subObserver);
        mute(subscription, TrackKind.VIDEO, subObserver, true);
        unmute(subscription, TrackKind.VIDEO, subObserver, true);
        subscription.removeObserver(subObserver);
    }

    public void testMuteAndUnmute_audioOnSubscriptionWithVideoOnly_shouldFail() {
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
        mute(subscription, TrackKind.AUDIO, null, false);
        unmute(subscription, TrackKind.AUDIO, null, false);
    }

    public void testMuteAndUnmute_audioAndVideoOnSubscriptionWithVideoOnly_shouldFail() {
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
        mute(subscription, TrackKind.AUDIO_AND_VIDEO, null, false);
        unmute(subscription, TrackKind.AUDIO_AND_VIDEO, null, false);
    }

    public void testMuteAndUnmute_audioOnSubscriptionWithAudioOnly_shouldSucceed() {
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
        PubSubObserver subObserver = new PubSubObserver(1);
        subscription.addObserver(subObserver);
        mute(subscription, TrackKind.AUDIO, subObserver, true);
        unmute(subscription, TrackKind.AUDIO, subObserver, true);
        subscription.removeObserver(subObserver);
    }

    public void testMuteAndUnmute_videoOnSubscriptionWithAudioOnly_shouldFail() {
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
        mute(subscription, TrackKind.VIDEO, null, false);
        unmute(subscription, TrackKind.VIDEO, null, false);
    }

    public void testMuteAndUnmute_audioAndVideoOnSubscriptionWithAudioOnly_shouldFail() {
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
        mute(subscription, TrackKind.AUDIO_AND_VIDEO, null, false);
        unmute(subscription, TrackKind.AUDIO_AND_VIDEO, null, false);
    }
}
