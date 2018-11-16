package oms.test.conference.apitest;

import static oms.test.conference.util.ConferenceAction.createClient;
import static oms.test.conference.util.ConferenceAction.getRemoteMixStream;
import static oms.test.conference.util.ConferenceAction.getToken;
import static oms.test.conference.util.ConferenceAction.join;
import static oms.test.conference.util.ConferenceAction.publish;
import static oms.test.conference.util.ConferenceAction.send;
import static oms.test.conference.util.ConferenceAction.stop;
import static oms.test.conference.util.ConferenceAction.subscribe;
import static oms.test.util.CommonAction.createDefaultCapturer;
import static oms.test.util.CommonAction.createLocalStream;
import static oms.test.util.Config.MESSAGE;
import static oms.test.util.Config.PRESENTER_ROLE;
import static oms.test.util.Config.USER1_NAME;

import android.test.suitebuilder.annotation.LargeTest;

import oms.base.LocalStream;
import oms.conference.Publication;
import oms.conference.RemoteStream;
import oms.conference.Subscription;
import oms.test.conference.util.ConferenceClientObserver;

public class StabilityTest extends TestBase {
    @LargeTest
    public void testPublish_200Times() {
        try {
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            client1 = createClient(null);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            capturer1 = createDefaultCapturer();
            LocalStream stream = createLocalStream(true, capturer1);
            for (int i = 0; i < 200; i++) {
                Publication publication = publish(client1, stream, null, null, true);
                stop(publication, null, true);
            }
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            client1.addObserver(observer1);
        }
    }

    @LargeTest
    public void testSubscribe_200Times() {
        try {
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            client1 = createClient(null);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            publish(client1, localStream1, null, null, true);
            RemoteStream mixSteam = getRemoteMixStream(client1);
            for (int i = 0; i < 200; i++) {
                Subscription subscription = subscribe(client1, mixSteam, null, false, true);
                subscription.stop();
            }
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            client1.addObserver(observer1);
        }
    }

    @LargeTest
    public void testPublishThenSubscribe_200Times() {
        try {
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            client1 = createClient(null);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            RemoteStream mixSteam = getRemoteMixStream(client1);
            for (int i = 0; i < 200; i++) {
                Publication publication = publish(client1, localStream1, null, null, true);
                Subscription subscription = subscribe(client1, mixSteam, null, false, true);
                publication.stop();
                subscription.stop();
            }
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            client1.addObserver(observer1);
        }
    }

    @LargeTest
    public void testSend_200Times() {
        try {
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            client1 = createClient(null);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            for (int i = 0; i < 200; i++) {
                send(client1, null, MESSAGE, null, true);
            }
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            client1.addObserver(observer1);
        }
    }
}
