package oms.test.conference.apitest;

import static oms.test.conference.util.ConferenceAction.createClient;
import static oms.test.conference.util.ConferenceAction.getRemoteForwardStream;
import static oms.test.conference.util.ConferenceAction.getToken;
import static oms.test.conference.util.ConferenceAction.join;
import static oms.test.conference.util.ConferenceAction.publish;
import static oms.test.conference.util.ConferenceAction.stop;
import static oms.test.conference.util.ConferenceAction.subscribe;
import static oms.test.util.CommonAction.createDefaultCapturer;
import static oms.test.util.CommonAction.createLocalStream;
import static oms.test.util.Config.MIXED_STREAM_SIZE;
import static oms.test.util.Config.PRESENTER_ROLE;
import static oms.test.util.Config.USER1_NAME;

import oms.conference.Publication;
import oms.conference.RemoteStream;
import oms.conference.Subscription;
import oms.test.conference.util.ConferenceClientObserver;

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
