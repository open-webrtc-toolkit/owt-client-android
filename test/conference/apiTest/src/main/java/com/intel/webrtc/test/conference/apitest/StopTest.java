package com.intel.webrtc.test.conference.apitest;

import static com.intel.webrtc.test.conference.util.ConferenceAction.createClient;
import static com.intel.webrtc.test.conference.util.ConferenceAction.getRemoteForwardStream;
import static com.intel.webrtc.test.conference.util.ConferenceAction.getToken;
import static com.intel.webrtc.test.conference.util.ConferenceAction.join;
import static com.intel.webrtc.test.conference.util.ConferenceAction.publish;
import static com.intel.webrtc.test.conference.util.ConferenceAction.stop;
import static com.intel.webrtc.test.conference.util.ConferenceAction.subscribe;
import static com.intel.webrtc.test.util.CommonAction.createDefaultCapturer;
import static com.intel.webrtc.test.util.CommonAction.createLocalStream;
import static com.intel.webrtc.test.util.Config.MIXED_STREAM_SIZE;
import static com.intel.webrtc.test.util.Config.PRESENTER_ROLE;
import static com.intel.webrtc.test.util.Config.USER1_NAME;

import com.intel.webrtc.conference.Publication;
import com.intel.webrtc.conference.RemoteStream;
import com.intel.webrtc.conference.Subscription;
import com.intel.webrtc.test.conference.util.ConferenceClientObserver;

public class StopTest extends TestBase {

    public void testStop_publication_checkEventTriggered() {
        observer1 = new ConferenceClientObserver(USER1_NAME, 1);
        client1 = createClient(observer1);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(true, capturer1);
        Publication publication = publish(client1, localStream1, null, observer1, true);
        stop(publication, observer1, true);
    }

    public void testStop_stoppedPublication_shouldBePeaceful() {
        observer1 = new ConferenceClientObserver(USER1_NAME, 1);
        client1 = createClient(observer1);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(true, capturer1);
        Publication publication1 = publish(client1, localStream1, null, observer1, true);
        stop(publication1, observer1, true);
        publication1.stop();
    }

    public void testStop_subscription_checkEventTriggered() {
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
    }

    public void testStop_stoppedSubscription_shouldBePeaceful() {
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
        subscription.stop();
    }

}
