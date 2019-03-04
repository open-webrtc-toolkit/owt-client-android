/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.test.p2p.apitest;

import static owt.test.p2p.util.P2PAction.checkRemoteStreamEnded;
import static owt.test.p2p.util.P2PAction.connect;
import static owt.test.p2p.util.P2PAction.createPeerClient;
import static owt.test.p2p.util.P2PAction.publish;
import static owt.test.p2p.util.P2PAction.stop;
import static owt.test.util.CommonAction.createDefaultCapturer;
import static owt.test.util.CommonAction.createLocalStream;
import static owt.test.util.Config.P2P_SERVER;
import static owt.test.util.Config.TIMEOUT_LONG;
import static owt.test.util.Config.USER1_NAME;
import static owt.test.util.Config.USER2_NAME;

import owt.p2p.Publication;
import owt.test.p2p.util.P2PClientObserver;
import owt.test.util.TestObserver;

public class StopTest extends TestBase {

    public void testStopClient_beforeConnect_shouldBePeaceful() {
        user1 = createPeerClient(null);
        stop(user1, "DoesNotMatter", null);
    }

    public void testStopClient_beforeDoingAnything_shouldBePeaceful() {
        user1 = createPeerClient(null);
        connect(user1, USER1_NAME, P2P_SERVER, true);
        stop(user1, "DoesNotMatter", null);
    }

    public void testStopClient_nullPeer_shouldThrowException() {
        observer1 = new P2PClientObserver(USER1_NAME);
        observer2 = new P2PClientObserver(USER2_NAME);
        user1 = createPeerClient(observer1);
        user2 = createPeerClient(observer2);
        user1.addAllowedRemotePeer(USER2_NAME);
        user2.addAllowedRemotePeer(USER1_NAME);
        connect(user1, USER1_NAME, P2P_SERVER, true);
        connect(user2, USER2_NAME, P2P_SERVER, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(true, capturer1);
        publish(user1, localStream1, USER2_NAME, observer2, true, true);
        try {
            stop(user1, null, null);
            fail("RuntimeException expected.");
        } catch (RuntimeException e) {
            stop(user1, USER2_NAME, observer2);
        }
    }

    public void testStopClient_afterPublish_checkEvents() {
        observer2 = new P2PClientObserver(USER2_NAME);
        user1 = createPeerClient(null);
        user2 = createPeerClient(observer2);
        user1.addAllowedRemotePeer(USER2_NAME);
        user2.addAllowedRemotePeer(USER1_NAME);
        connect(user1, USER1_NAME, P2P_SERVER, true);
        connect(user2, USER2_NAME, P2P_SERVER, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(true, capturer1);
        Publication publication = publish(user1, localStream1, USER2_NAME, observer2, true, true);
        stop(user1, USER2_NAME, observer2, publication);
    }

    public void testStopClient_afterStreamAdded_checkEvents() {
        observer2 = new P2PClientObserver(USER2_NAME);
        user1 = createPeerClient(null);
        user2 = createPeerClient(observer2);
        user1.addAllowedRemotePeer(USER2_NAME);
        user2.addAllowedRemotePeer(USER1_NAME);
        connect(user1, USER1_NAME, P2P_SERVER, true);
        connect(user2, USER2_NAME, P2P_SERVER, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(true, capturer1);
        Publication publication = publish(user1, localStream1, USER2_NAME, observer2, true, true);
        stop(user2, USER1_NAME, observer2, publication);
    }

    public void testStopClient_twice_shouldBePeaceful() {
        observer1 = new P2PClientObserver(USER1_NAME);
        observer2 = new P2PClientObserver(USER2_NAME);
        user1 = createPeerClient(observer1);
        user2 = createPeerClient(observer2);
        user1.addAllowedRemotePeer(USER2_NAME);
        user2.addAllowedRemotePeer(USER1_NAME);
        connect(user1, USER1_NAME, P2P_SERVER, true);
        connect(user2, USER2_NAME, P2P_SERVER, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(true, capturer1);
        Publication publication = publish(user1, localStream1, USER2_NAME, observer2, true, true);
        stop(user1, USER2_NAME, observer2, publication);
        stop(user1, USER2_NAME, null);
    }

    public void testStopPublication_checkEvents() {
        observer1 = new P2PClientObserver(USER1_NAME);
        observer2 = new P2PClientObserver(USER2_NAME);
        user1 = createPeerClient(observer1);
        user2 = createPeerClient(observer2);
        user1.addAllowedRemotePeer(USER2_NAME);
        user2.addAllowedRemotePeer(USER1_NAME);
        connect(user1, USER1_NAME, P2P_SERVER, true);
        connect(user2, USER2_NAME, P2P_SERVER, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(true, capturer1);
        Publication publication1 = publish(user1, localStream1, USER2_NAME, observer2, true, true);
        Publication publication2 = publish(user2, localStream1, USER1_NAME, observer1, true, true);
        stop(publication1, observer2, 0, true);
        stop(publication2, observer1, 0, true);
    }

    public void testStopPublication_twice_shouldBePeaceful() {
        observer2 = new P2PClientObserver(USER2_NAME);
        user1 = createPeerClient(null);
        user2 = createPeerClient(observer2);
        user1.addAllowedRemotePeer(USER2_NAME);
        user2.addAllowedRemotePeer(USER1_NAME);
        connect(user1, USER1_NAME, P2P_SERVER, true);
        connect(user2, USER2_NAME, P2P_SERVER, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(true, capturer1);
        Publication publication = publish(user1, localStream1, USER2_NAME, observer2, true, true);
        stop(publication, observer2, 0, true);
        stop(publication, null, 0, false);
    }

    public void testStopClient_simultaneouslyWithTwoClient_shouldSucceed() {
        observer2 = new P2PClientObserver(USER2_NAME);
        user1 = createPeerClient(null);
        user2 = createPeerClient(observer2);
        user1.addAllowedRemotePeer(USER2_NAME);
        user2.addAllowedRemotePeer(USER1_NAME);
        connect(user1, USER1_NAME, P2P_SERVER, true);
        connect(user2, USER2_NAME, P2P_SERVER, true);
        capturer1 = createDefaultCapturer();
        localStream1 = createLocalStream(true, capturer1);
        Publication publication = publish(user1, localStream1, USER2_NAME, observer2, true, true);
        TestObserver publicationObserver = new TestObserver();
        publication.addObserver(publicationObserver);
        user1.stop(USER2_NAME);
        user2.stop(USER1_NAME);
        assertTrue(publicationObserver.getResult(TIMEOUT_LONG));
        checkRemoteStreamEnded(observer2.remoteStreamObservers.values());
    }
}
