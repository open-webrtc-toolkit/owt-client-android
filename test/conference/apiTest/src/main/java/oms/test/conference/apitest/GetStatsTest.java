package oms.test.conference.apitest;

import static oms.test.conference.util.ConferenceAction.createClient;
import static oms.test.conference.util.ConferenceAction.getRemoteForwardStream;
import static oms.test.conference.util.ConferenceAction.getStats;
import static oms.test.conference.util.ConferenceAction.getToken;
import static oms.test.conference.util.ConferenceAction.join;
import static oms.test.conference.util.ConferenceAction.leave;
import static oms.test.conference.util.ConferenceAction.publish;
import static oms.test.conference.util.ConferenceAction.stop;
import static oms.test.conference.util.ConferenceAction.subscribe;
import static oms.test.util.CommonAction.createDefaultCapturer;
import static oms.test.util.CommonAction.createLocalStream;
import static oms.test.util.Config.MIXED_STREAM_SIZE;
import static oms.test.util.Config.PRESENTER_ROLE;
import static oms.test.util.Config.USER1_NAME;
import static oms.test.util.Config.USER2_NAME;

import android.test.suitebuilder.annotation.LargeTest;

import oms.conference.Publication;
import oms.conference.RemoteStream;
import oms.conference.Subscription;
import oms.test.conference.util.ConferenceClientObserver;

public class GetStatsTest extends TestBase {

    @LargeTest
    public void testGetStats_publicationStatsAfterPublicationStop_shouldFail() {
        try {
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            client1 = createClient(observer1);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            Publication publication = publish(client1, localStream1, null, observer1, true);
            stop(publication, observer1, true);
            getStats(publication, false);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testGetStats_subscriptionStatsAfterSubscriptionStop_shouldFail() {
        try {
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
            getStats(subscription, false);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testGetStats_publicationStatsAfterLeave_shouldFail() {
        try {
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            client1 = createClient(observer1);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            Publication publication = publish(client1, localStream1, null, observer1, true);
            leave(client1, observer1, null);
            getStats(publication, false);
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            client1 = null;
        }
    }

    @LargeTest
    public void testGetStats_subscriptionStatsAfterLeave_shouldFail() {
        try {
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            client1 = createClient(observer1);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            publish(client1, localStream1, null, observer1, true);
            int streamsN = client1.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
            RemoteStream forwardStream = getRemoteForwardStream(client1, streamsN - 1);
            Subscription subscription = subscribe(client1, forwardStream, null, true, true);
            leave(client1, observer1, null);
            getStats(subscription, false);
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            client1 = null;
        }
    }

    @LargeTest
    public void testGetStats_subscriptionStatsAfterRemoteStreamEnded_shouldSuccess() {
        try {
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            client1 = createClient(null);
            observer2 = new ConferenceClientObserver(USER2_NAME, 1);
            client2 = createClient(observer2);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            join(client2, getToken(PRESENTER_ROLE, USER2_NAME), null, null, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            Publication publication = publish(client1, localStream1, null, observer2, true);
            int streamsN = client2.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
            RemoteStream forwardStream = getRemoteForwardStream(client2, streamsN - 1);
            Subscription subscription = subscribe(client2, forwardStream, null, true, true);
            stop(publication, observer2, true);
            getStats(subscription, true);
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            client1.addObserver(observer1);
        }
    }
}
