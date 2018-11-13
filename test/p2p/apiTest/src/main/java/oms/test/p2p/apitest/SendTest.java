package oms.test.p2p.apitest;

import static oms.test.p2p.util.P2PAction.connect;
import static oms.test.p2p.util.P2PAction.createPeerClient;
import static oms.test.p2p.util.P2PAction.disconnect;
import static oms.test.p2p.util.P2PAction.publish;
import static oms.test.p2p.util.P2PAction.send;
import static oms.test.p2p.util.P2PAction.stop;
import static oms.test.util.CommonAction.createDefaultCapturer;
import static oms.test.util.CommonAction.createLocalStream;
import static oms.test.util.Config.MESSAGE;
import static oms.test.util.Config.P2P_SERVER;
import static oms.test.util.Config.SPECIAL_CHARACTER;
import static oms.test.util.Config.TIMEOUT;
import static oms.test.util.Config.USER1_NAME;
import static oms.test.util.Config.USER2_NAME;

import android.test.suitebuilder.annotation.LargeTest;

import oms.base.MediaCodecs;
import oms.p2p.Publication;
import oms.test.p2p.util.P2PClientObserver;
import oms.test.util.Config;
import oms.test.util.TestCallback;

import java.util.ArrayList;
import java.util.Arrays;

public class SendTest extends TestBase {

    @LargeTest
    public void testSend_beforeConnect_shouldFail() {
        try {
            user1 = createPeerClient(null);
            user2 = createPeerClient(null);
            user1.addAllowedRemotePeer(USER2_NAME);
            user2.addAllowedRemotePeer(USER1_NAME);
            send(user1, MESSAGE, USER2_NAME, null, false);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSend_toAllowedAndConnectedPeer_shouldSucceed() {
        try {
            observer1 = new P2PClientObserver(USER1_NAME);
            user1 = createPeerClient(observer1);
            observer2 = new P2PClientObserver(USER2_NAME);
            user2 = createPeerClient(observer2);
            user1.addAllowedRemotePeer(USER2_NAME);
            user2.addAllowedRemotePeer(USER1_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            send(user1, MESSAGE, USER2_NAME, observer2, true);
            send(user2, MESSAGE, USER1_NAME, observer1, true);
            send(user1, MESSAGE, USER2_NAME, observer2, true);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSend_toUnallowedAndConnectedPeer_shouldFail() {
        try {
            user1 = createPeerClient(null);
            user2 = createPeerClient(null);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            send(user1, MESSAGE, USER2_NAME, null, false);
            send(user2, MESSAGE, USER1_NAME, null, false);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSend_toConnectedPeerButNotAllowsMe_shouldFail() {
        try {
            user1 = createPeerClient(null);
            user2 = createPeerClient(null);
            user1.addAllowedRemotePeer(USER2_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            send(user1, MESSAGE, USER2_NAME, null, false);
            send(user2, MESSAGE, USER1_NAME, null, false);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSend_toUnconnectedPeer_shouldFail() {
        try {
            observer1 = new P2PClientObserver(USER1_NAME);
            user1 = createPeerClient(observer1);
            user1.addAllowedRemotePeer(USER2_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            send(user1, MESSAGE, USER2_NAME, null, false);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSend_toMyself_shouldFail() {
        try {
            observer1 = new P2PClientObserver(USER1_NAME);
            user1 = createPeerClient(observer1);
            user1.addAllowedRemotePeer(USER2_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            user1.addAllowedRemotePeer(USER1_NAME);
            send(user1, MESSAGE, USER1_NAME, null, false);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSend_toNullUsername_shouldFail() {
        try {
            observer1 = new P2PClientObserver(USER1_NAME);
            user1 = createPeerClient(observer1);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            send(user1, MESSAGE, null, null, false);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSend_withNullMessage_shouldThrowException() {
        try {
            observer1 = new P2PClientObserver(USER1_NAME);
            observer2 = new P2PClientObserver(USER2_NAME);
            user1 = createPeerClient(observer1);
            user2 = createPeerClient(observer2);
            user1.addAllowedRemotePeer(USER2_NAME);
            user2.addAllowedRemotePeer(USER1_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            try {
                send(user1, null, USER2_NAME, null, false);
                fail("RuntimeException expected.");
            } catch (RuntimeException ignored) {
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSend_withLargeSizeMessage_shouldSucceed() {
        try {
            observer1 = new P2PClientObserver(USER1_NAME);
            user1 = createPeerClient(observer1);
            observer2 = new P2PClientObserver(USER2_NAME);
            user2 = createPeerClient(observer2);
            user1.addAllowedRemotePeer(USER2_NAME);
            user2.addAllowedRemotePeer(USER1_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            String message = Arrays.toString(new Byte[1024]);
            send(user1, message, USER2_NAME, observer2, true);
            send(user2, message, USER1_NAME, observer1, true);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSend_withSpecialMessage_shouldSucceed() {
        try {
            observer1 = new P2PClientObserver(USER1_NAME);
            user1 = createPeerClient(observer1);
            observer2 = new P2PClientObserver(USER2_NAME);
            user2 = createPeerClient(observer2);
            user1.addAllowedRemotePeer(USER2_NAME);
            user2.addAllowedRemotePeer(USER1_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            send(user1, SPECIAL_CHARACTER, USER2_NAME, observer2, true);
            send(user2, SPECIAL_CHARACTER, USER1_NAME, observer1, true);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSend_afterPublish_shouldSucceed() {
        try {
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
            publish(user1, localStream1, USER2_NAME, observer2, false, true);
            send(user1, MESSAGE, USER2_NAME, observer2, true);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSend_afterPublicationStop_shouldSucceed() {
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
            Publication publication = publish(user1, localStream1, USER2_NAME, observer2, false,
                    true);
            stop(publication, observer2, 0, true);
            send(user1, MESSAGE, USER2_NAME, observer2, true);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSend_afterClientStop_shouldSucceed() {
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
            send(user1, MESSAGE, USER2_NAME, observer2, true);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSend_afterPeerClientStop_shouldSucceed() {
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
            send(user1, MESSAGE, USER2_NAME, observer2, true);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSend_simultaneouslySendTwoMessages_shouldSucceed() {
        try {
            user1 = createPeerClient(null);
            observer2 = new P2PClientObserver(USER2_NAME, 2);
            user2 = createPeerClient(observer2);
            user1.addAllowedRemotePeer(USER2_NAME);
            user2.addAllowedRemotePeer(USER1_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            int initDataReceive = observer2.dataReceived.size();
            TestCallback<Void> callback1 = new TestCallback<>();
            TestCallback<Void> callback2 = new TestCallback<>();
            user1.send(USER2_NAME, MESSAGE, callback1);
            user1.send(USER2_NAME, MESSAGE, callback2);
            assertTrue(callback1.getResult(true, TIMEOUT));
            assertTrue(callback2.getResult(true, TIMEOUT));
            assertTrue(observer2.getResultForDataReceived(Config.TIMEOUT));
            assertEquals(MESSAGE, observer2.dataReceived.get(initDataReceive));
            assertEquals(user1.id(), observer2.dataSenders.get(initDataReceive));
            assertEquals(MESSAGE, observer2.dataReceived.get(initDataReceive + 1));
            assertEquals(user1.id(), observer2.dataSenders.get(initDataReceive + 1));
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSend_simultaneouslyTwoClientSend_shouldSucceed() {
        try {
            observer1 = new P2PClientObserver(USER1_NAME);
            user1 = createPeerClient(observer1);
            observer2 = new P2PClientObserver(USER2_NAME);
            user2 = createPeerClient(observer2);
            user1.addAllowedRemotePeer(USER2_NAME);
            user2.addAllowedRemotePeer(USER1_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            int user1initDataReceive = observer1.dataReceived.size();
            int user2initDataReceive = observer2.dataReceived.size();
            TestCallback<Void> callback1 = new TestCallback<>();
            TestCallback<Void> callback2 = new TestCallback<>();
            user1.send(USER2_NAME, MESSAGE, callback1);
            user2.send(USER1_NAME, MESSAGE, callback2);
            assertTrue(callback1.getResult(true, TIMEOUT));
            assertTrue(callback2.getResult(true, TIMEOUT));
            assertTrue(observer1.getResultForDataReceived(Config.TIMEOUT));
            assertEquals(MESSAGE, observer1.dataReceived.get(user1initDataReceive));
            assertEquals(user2.id(), observer1.dataSenders.get(user1initDataReceive));
            assertTrue(observer2.getResultForDataReceived(Config.TIMEOUT));
            assertEquals(MESSAGE, observer2.dataReceived.get(user2initDataReceive));
            assertEquals(user1.id(), observer2.dataSenders.get(user2initDataReceive));
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSend_afterDisconnect_shouldFail() {
        try {
            observer1 = new P2PClientObserver(USER2_NAME);
            user1 = createPeerClient(observer1);
            user2 = createPeerClient(null);
            user1.addAllowedRemotePeer(USER2_NAME);
            user2.addAllowedRemotePeer(USER1_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            disconnect(user1, observer1);
            send(user1, MESSAGE, USER2_NAME, null, false);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSend_withDifferentCodecSupportClient_shouldSucceed() {
        try {
            ArrayList<MediaCodecs.VideoCodec> videoCodec1 = new ArrayList<>();
            videoCodec1.add(MediaCodecs.VideoCodec.VP8);
            ArrayList<MediaCodecs.VideoCodec> videoCodec2 = new ArrayList<>();
            videoCodec2.add(MediaCodecs.VideoCodec.H264);
            observer1 = new P2PClientObserver(USER1_NAME);
            user1 = createPeerClient(videoCodec1, null, observer1);
            observer2 = new P2PClientObserver(USER2_NAME);
            user2 = createPeerClient(videoCodec2, null, observer2);
            user1.addAllowedRemotePeer(USER2_NAME);
            user2.addAllowedRemotePeer(USER1_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            send(user1, SPECIAL_CHARACTER, USER2_NAME, observer2, true);
            send(user2, SPECIAL_CHARACTER, USER1_NAME, observer1, true);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSend_afterPublishWithoutWaitCallBack_shouldSucceed() {
        try {
            observer2 = new P2PClientObserver(USER2_NAME, 2);
            user1 = createPeerClient(null);
            user2 = createPeerClient(observer2);
            user1.addAllowedRemotePeer(USER2_NAME);
            user2.addAllowedRemotePeer(USER1_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            int initDataReceive = observer2.dataReceived.size();
            TestCallback<Publication> publicationTestCallback = new TestCallback<>();
            TestCallback<Void> sendTestCallback = new TestCallback<>();
            user1.publish(USER2_NAME, localStream1, publicationTestCallback);
            user1.send(USER2_NAME, MESSAGE, sendTestCallback);
            assertTrue(publicationTestCallback.getResult(true, TIMEOUT));
            assertTrue(sendTestCallback.getResult(true, TIMEOUT));
            assertTrue(observer2.getResultForStreamAdded(Config.TIMEOUT));
            assertTrue(observer2.getResultForDataReceived(Config.TIMEOUT));
            assertEquals(MESSAGE, observer2.dataReceived.get(initDataReceive));
            assertEquals(user1.id(), observer2.dataSenders.get(initDataReceive));
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSend_stopClientThenSendAgainWithoutWaitEvent_shouldSucceed() {
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
            publish(user1, localStream1, USER2_NAME, observer2, true, true);
            user1.stop(USER2_NAME);
            send(user1, SPECIAL_CHARACTER, USER2_NAME, observer2, true);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSend_stopPeerThenSendAgainWithoutWaitEvent_shouldSucceed() {
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
            publish(user1, localStream1, USER2_NAME, observer2, true, true);
            user2.stop(USER1_NAME);
            send(user1, SPECIAL_CHARACTER, USER2_NAME, observer2, true);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSend_AfterPublicationStopWithoutWaitEvent_shouldSucceed() {
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
            publication.stop();
            send(user1, SPECIAL_CHARACTER, USER2_NAME, observer2, true);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
