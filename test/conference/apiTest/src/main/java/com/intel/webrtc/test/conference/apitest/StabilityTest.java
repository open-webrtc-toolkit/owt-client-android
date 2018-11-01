package com.intel.webrtc.test.conference.apitest;

import static com.intel.webrtc.test.conference.util.ConferenceAction.createClient;
import static com.intel.webrtc.test.conference.util.ConferenceAction.getRemoteMixStream;
import static com.intel.webrtc.test.conference.util.ConferenceAction.getToken;
import static com.intel.webrtc.test.conference.util.ConferenceAction.join;
import static com.intel.webrtc.test.conference.util.ConferenceAction.publish;
import static com.intel.webrtc.test.conference.util.ConferenceAction.send;
import static com.intel.webrtc.test.conference.util.ConferenceAction.stop;
import static com.intel.webrtc.test.conference.util.ConferenceAction.subscribe;
import static com.intel.webrtc.test.util.CommonAction.createDefaultCapturer;
import static com.intel.webrtc.test.util.CommonAction.createLocalStream;
import static com.intel.webrtc.test.util.Config.MESSAGE;
import static com.intel.webrtc.test.util.Config.PRESENTER_ROLE;
import static com.intel.webrtc.test.util.Config.USER1_NAME;

import com.intel.webrtc.base.LocalStream;
import com.intel.webrtc.conference.Publication;
import com.intel.webrtc.conference.RemoteStream;
import com.intel.webrtc.conference.Subscription;

public class StabilityTest extends TestBase {
    public void testPublish_200Times() {
        client1 = createClient(null);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        capturer1 = createDefaultCapturer();
        LocalStream stream = createLocalStream(true, capturer1);
        for (int i = 0; i < 200; i++) {
            Publication publication = publish(client1, stream, null, null, true);
            stop(publication, null, true);
        }
    }

    public void testSubscribe_200Times() {
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
    }

    public void testPublishThenSubscribe_200Times() {
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
    }

    public void testSend_200Times() {
        client1 = createClient(null);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        for (int i = 0; i < 200; i++) {
            send(client1, null, MESSAGE, null, true);
        }
    }
}
