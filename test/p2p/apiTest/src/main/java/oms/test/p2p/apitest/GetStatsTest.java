package oms.test.p2p.apitest;

import static oms.test.p2p.util.P2PAction.connect;
import static oms.test.p2p.util.P2PAction.createPeerClient;
import static oms.test.p2p.util.P2PAction.disconnect;
import static oms.test.p2p.util.P2PAction.getStats;
import static oms.test.p2p.util.P2PAction.publish;
import static oms.test.p2p.util.P2PAction.stop;
import static oms.test.util.CommonAction.createDefaultCapturer;
import static oms.test.util.CommonAction.createLocalStream;
import static oms.test.util.Config.P2P_SERVER;
import static oms.test.util.Config.USER1_NAME;
import static oms.test.util.Config.USER2_NAME;
import static oms.test.util.Config.USER3_NAME;

import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.SmallTest;

import oms.p2p.Publication;
import oms.test.p2p.util.P2PClientObserver;

public class GetStatsTest extends TestBase {

    @SmallTest
    @LargeTest
    public void testGetStats_p2pClientStatsAfterStopClient_shouldFail() {
        try {
            observer2 = new P2PClientObserver(USER2_NAME);
            user1 = createPeerClient(null);
            user2 = createPeerClient(observer2);
            user1.addAllowedRemotePeer(USER2_NAME);
            user2.addAllowedRemotePeer(USER1_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            Publication publication = publish(user1, localStream1, USER2_NAME, observer2, true,
                    true);
            stop(user1, USER2_NAME, observer2, publication);
            getStats(user1, USER2_NAME, false);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @SmallTest
    @LargeTest
    public void testGetStats_p2pClientStatsAfterStopPeer_shouldFail() {
        try {
            observer2 = new P2PClientObserver(USER2_NAME);
            user1 = createPeerClient(null);
            user2 = createPeerClient(observer2);
            user1.addAllowedRemotePeer(USER2_NAME);
            user2.addAllowedRemotePeer(USER1_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            Publication publication = publish(user1, localStream1, USER2_NAME, observer2, true,
                    true);
            stop(user2, USER1_NAME, observer2, publication);
            getStats(user1, USER2_NAME, false);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testGetStats_publicationStatsAfterStopClient_shouldFail() {
        try {
            observer2 = new P2PClientObserver(USER2_NAME);
            user1 = createPeerClient(null);
            user2 = createPeerClient(observer2);
            user1.addAllowedRemotePeer(USER2_NAME);
            user2.addAllowedRemotePeer(USER1_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            Publication publication = publish(user1, localStream1, USER2_NAME, observer2, true,
                    true);
            stop(user1, USER2_NAME, observer2, publication);
            getStats(publication, false);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testGetStats_publicationStatsAfterStopPeer_shouldFail() {
        try {
            observer2 = new P2PClientObserver(USER2_NAME);
            user1 = createPeerClient(null);
            user2 = createPeerClient(observer2);
            user1.addAllowedRemotePeer(USER2_NAME);
            user2.addAllowedRemotePeer(USER1_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            Publication publication = publish(user1, localStream1, USER2_NAME, observer2, true,
                    true);
            stop(user2, USER1_NAME, observer2, publication);
            getStats(publication, false);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testGetStats_publicationStatsAfterStopPublication_shouldSucceed() {
        try {
            observer2 = new P2PClientObserver(USER2_NAME);
            user1 = createPeerClient(null);
            user2 = createPeerClient(observer2);
            user1.addAllowedRemotePeer(USER2_NAME);
            user2.addAllowedRemotePeer(USER1_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            Publication publication = publish(user1, localStream1, USER2_NAME, observer2, true,
                    true);
            stop(publication, observer2, 0, true);
            getStats(publication, true);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testGetStats_publicationStatsAfterLeave_shouldFail() {
        try {
            observer1 = new P2PClientObserver(USER2_NAME);
            user1 = createPeerClient(observer1);
            observer2 = new P2PClientObserver(USER2_NAME);
            user2 = createPeerClient(observer2);
            user1.addAllowedRemotePeer(USER2_NAME);
            user2.addAllowedRemotePeer(USER1_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            Publication publication = publish(user1, localStream1, USER2_NAME, observer2, true,
                    true);
            disconnect(user1, observer1);
            getStats(publication, false);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testGetStats_p2pClientStatsAfterLeave_shouldFail() {
        try {
            observer1 = new P2PClientObserver(USER2_NAME);
            user1 = createPeerClient(observer1);
            observer2 = new P2PClientObserver(USER2_NAME);
            user2 = createPeerClient(observer2);
            user1.addAllowedRemotePeer(USER2_NAME);
            user2.addAllowedRemotePeer(USER1_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            publish(user1, localStream1, USER2_NAME, observer2, true, true);
            disconnect(user1, observer1);
            getStats(user1, USER2_NAME, false);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testGetStats_p2pClientStatsUnconnectPeer_shouldFail() {
        try {
            user1 = createPeerClient(null);
            observer2 = new P2PClientObserver(USER2_NAME);
            user2 = createPeerClient(observer2);
            user1.addAllowedRemotePeer(USER2_NAME);
            user2.addAllowedRemotePeer(USER1_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            publish(user1, localStream1, USER2_NAME, observer2, true, true);
            getStats(user1, USER3_NAME, false);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testGetStats_p2pClientStatsToMyself_shouldFail() {
        try {
            user1 = createPeerClient(null);
            observer2 = new P2PClientObserver(USER2_NAME);
            user2 = createPeerClient(observer2);
            user1.addAllowedRemotePeer(USER2_NAME);
            user2.addAllowedRemotePeer(USER1_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            publish(user1, localStream1, USER2_NAME, observer2, true, true);
            getStats(user1, USER1_NAME, false);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testGetStats_p2pClientStatsNullUsername_shouldThrowException() {
        try {
            user1 = createPeerClient(null);
            observer2 = new P2PClientObserver(USER2_NAME);
            user2 = createPeerClient(observer2);
            user1.addAllowedRemotePeer(USER2_NAME);
            user2.addAllowedRemotePeer(USER1_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            publish(user1, localStream1, USER2_NAME, observer2, true, true);
            try {
                getStats(user1, null, false);
                fail("RuntimeException expected.");
            } catch (RuntimeException ignored) {
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testGetStats_p2pClientStatsAfterRemotePublicationStop_shouldSucceed() {
        try {
            user1 = createPeerClient(null);
            observer2 = new P2PClientObserver(USER2_NAME);
            user2 = createPeerClient(observer2);
            user1.addAllowedRemotePeer(USER2_NAME);
            user2.addAllowedRemotePeer(USER1_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            Publication publication = publish(user1, localStream1, USER2_NAME, observer2, true,
                    true);
            stop(publication, observer2, 0, true);
            getStats(user2, USER1_NAME, true);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
