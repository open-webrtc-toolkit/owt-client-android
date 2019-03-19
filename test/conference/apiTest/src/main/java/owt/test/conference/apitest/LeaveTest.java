/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.test.conference.apitest;

import static owt.test.conference.util.ConferenceAction.createClient;
import static owt.test.conference.util.ConferenceAction.getRemoteForwardStream;
import static owt.test.conference.util.ConferenceAction.getToken;
import static owt.test.conference.util.ConferenceAction.join;
import static owt.test.conference.util.ConferenceAction.leave;
import static owt.test.conference.util.ConferenceAction.publish;
import static owt.test.util.CommonAction.createDefaultCapturer;
import static owt.test.util.CommonAction.createLocalStream;
import static owt.test.util.Config.MIXED_STREAM_SIZE;
import static owt.test.util.Config.PRESENTER_ROLE;
import static owt.test.util.Config.TIMEOUT;
import static owt.test.util.Config.USER1_NAME;
import static owt.test.util.Config.USER2_NAME;

import owt.conference.RemoteStream;
import owt.test.conference.util.ConferenceClientObserver;

public class LeaveTest extends TestBase {

    public void testLeave_shouldBePeaceful() {
        client1 = createClient(null);
        client1.join(getToken(PRESENTER_ROLE, USER1_NAME), null);
        client1.leave();
        client1 = null;
    }

    public void testLeave_checkEventsTriggered() {
        client1 = createClient(null);
        observer2 = new ConferenceClientObserver(USER2_NAME, 1);
        client2 = createClient(observer2);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        join(client2, getToken(PRESENTER_ROLE, USER2_NAME), observer2, null, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(true, capturer1);
        observer1 = new ConferenceClientObserver(USER1_NAME, 1);
        client1.addObserver(observer1);
        publish(client1, localStream1, null, observer2, true);
        int streamsN = client2.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
        RemoteStream forwardStream = getRemoteForwardStream(client2, streamsN - 1);
        assertTrue(observer1.getResultForPublish(TIMEOUT));
        leave(client1, observer1, observer2);
        assertTrue(observer2.streamObservers.get(forwardStream.id()).getResult(TIMEOUT));
        client1 = null;
    }
}
