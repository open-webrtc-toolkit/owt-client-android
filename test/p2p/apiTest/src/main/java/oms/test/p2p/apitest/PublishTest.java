package oms.test.p2p.apitest;

import static oms.test.p2p.util.P2PAction.connect;
import static oms.test.p2p.util.P2PAction.createPeerClient;
import static oms.test.p2p.util.P2PAction.disconnect;
import static oms.test.p2p.util.P2PAction.getStats;
import static oms.test.p2p.util.P2PAction.publish;
import static oms.test.p2p.util.P2PAction.send;
import static oms.test.p2p.util.P2PAction.stop;
import static oms.test.util.CommonAction.checkRTCStats;
import static oms.test.util.CommonAction.createDefaultCapturer;
import static oms.test.util.CommonAction.createLocalStream;
import static oms.test.util.Config.MESSAGE;
import static oms.test.util.Config.P2P_SERVER;
import static oms.test.util.Config.RAW_STREAM_FILE;
import static oms.test.util.Config.TIMEOUT;
import static oms.test.util.Config.USER1_NAME;
import static oms.test.util.Config.USER2_NAME;

import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.SmallTest;

import oms.base.MediaCodecs;
import oms.p2p.P2PClient;
import oms.p2p.Publication;
import oms.test.p2p.util.P2PClientObserver;
import oms.test.util.Config;
import oms.test.util.RawCapturerForTest;
import oms.test.util.TestCallback;

import org.webrtc.RTCStatsReport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class PublishTest extends TestBase {

    @LargeTest
    public void testPublish_beforeConnect_shouldFail() {
        try {
            user1 = createPeerClient(null);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            publish(user1, localStream1, USER1_NAME, null, false, false);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testPublish_toUnallowedUser_shouldFail() {
        try {
            observer2 = new P2PClientObserver(USER2_NAME);
            user1 = createPeerClient(null);
            P2PClient user2 = createPeerClient(observer2);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            publish(user1, localStream1, USER2_NAME, observer2, false, false);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testPublish_toUserNotAllowsMe_shouldFail() {
        try {
            observer2 = new P2PClientObserver(USER2_NAME);
            user1 = createPeerClient(null);
            user2 = createPeerClient(observer2);
            user1.addAllowedRemotePeer(USER2_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            publish(user1, localStream1, USER2_NAME, observer2, false, false);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testPublish_toNullUser_shouldFail() {
        try {
            user1 = createPeerClient(null);
            user2 = createPeerClient(null);
            user1.addAllowedRemotePeer(USER2_NAME);
            user2.addAllowedRemotePeer(USER1_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            publish(user1, localStream1, null, null, false, false);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testPublish_nullStream_shouldThrowException() {
        try {
            user1 = createPeerClient(null);
            user2 = createPeerClient(null);
            user1.addAllowedRemotePeer(USER2_NAME);
            user2.addAllowedRemotePeer(USER1_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            try {
                publish(user1, null, USER2_NAME, null, false, false);
                fail("RuntimeException expected.");
            } catch (RuntimeException ignored) {
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testPublish_toMyself_shouldFail() {
        try {
            user1 = createPeerClient(null);
            user2 = createPeerClient(null);
            user1.addAllowedRemotePeer(USER2_NAME);
            user2.addAllowedRemotePeer(USER1_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            publish(user1, localStream1, USER1_NAME, null, false, false);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    public void testPublish_twice_shouldFailAt2nd() {
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
            publish(user1, localStream1, USER2_NAME, observer2, false, false);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @SmallTest
    @LargeTest
    public void testPublish_eachOtherWithVP8_shouldSucceedAndCheckStats() {
        try {
            ArrayList<MediaCodecs.VideoCodec> videoCodecs = new ArrayList<>();
            videoCodecs.add(MediaCodecs.VideoCodec.VP8);
            observer1 = new P2PClientObserver(USER1_NAME);
            observer2 = new P2PClientObserver(USER2_NAME);
            user1 = createPeerClient(videoCodecs, null, observer1);
            user2 = createPeerClient(videoCodecs, null, observer2);
            user1.addAllowedRemotePeer(USER2_NAME);
            user2.addAllowedRemotePeer(USER1_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            Publication publication1 = publish(user1, localStream1, USER2_NAME, observer2, true,
                    true);
            Publication publication2 = publish(user2, localStream1, USER1_NAME, observer1, true,
                    true);
            RTCStatsReport statsReport1 = getStats(publication1, true);
            RTCStatsReport statsReport2 = getStats(publication2, true);
            HashMap<String, String> expectation = new HashMap<>();
            expectation.put("videoCodec", "vp8");
            checkRTCStats(statsReport1, expectation, true, true, true);
            checkRTCStats(statsReport2, expectation, true, true, true);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @SmallTest
    @LargeTest
    public void testPublish_eachOtherWithH264_shouldSucceedAndCheckStats() {
        try {
            ArrayList<MediaCodecs.VideoCodec> videoCodecs = new ArrayList<>();
            videoCodecs.add(MediaCodecs.VideoCodec.H264);
            observer1 = new P2PClientObserver(USER1_NAME);
            observer2 = new P2PClientObserver(USER2_NAME);
            user1 = createPeerClient(videoCodecs, null, observer1);
            user2 = createPeerClient(videoCodecs, null, observer2);
            user1.addAllowedRemotePeer(USER2_NAME);
            user2.addAllowedRemotePeer(USER1_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            Publication publication1 = publish(user1, localStream1, USER2_NAME, observer2, true,
                    true);
            Publication publication2 = publish(user2, localStream1, USER1_NAME, observer1, true,
                    true);
            RTCStatsReport statsReport1 = getStats(publication1, true);
            RTCStatsReport statsReport2 = getStats(publication2, true);
            HashMap<String, String> expectation = new HashMap<>();
            expectation.put("videoCodec", "H264");
            checkRTCStats(statsReport1, expectation, true, true, true);
            checkRTCStats(statsReport2, expectation, true, true, true);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testPublish_eachOtherWithVP9_shouldSucceedAndCheckStats() {
        try {
            ArrayList<MediaCodecs.VideoCodec> videoCodecs = new ArrayList<>();
            videoCodecs.add(MediaCodecs.VideoCodec.VP9);
            observer1 = new P2PClientObserver(USER1_NAME);
            observer2 = new P2PClientObserver(USER2_NAME);
            user1 = createPeerClient(videoCodecs, null, observer1);
            user2 = createPeerClient(videoCodecs, null, observer2);
            user1.addAllowedRemotePeer(USER2_NAME);
            user2.addAllowedRemotePeer(USER1_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            Publication publication1 = publish(user1, localStream1, USER2_NAME, observer2, true,
                    true);
            Publication publication2 = publish(user2, localStream1, USER1_NAME, observer1, true,
                    true);
            RTCStatsReport statsReport1 = getStats(publication1, true);
            RTCStatsReport statsReport2 = getStats(publication2, true);
            HashMap<String, String> expectation = new HashMap<>();
            expectation.put("videoCodec", "VP9");
            checkRTCStats(statsReport1, expectation, true, true, true);
            checkRTCStats(statsReport2, expectation, true, true, true);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testPublish_videoOnlyStream_shouldSucceed() {
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
            publish(user1, localStream1, USER2_NAME, observer2, true, true);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testPublish_audioOnlyStream_shouldSucceed() {
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
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testPublish_afterSend_shouldSucceed() {
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
            send(user1, MESSAGE, USER2_NAME, observer2, true);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testPublish_afterRemoteClientSend_shouldSucceed() {
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
            publish(user1, localStream1, USER2_NAME, observer2, true, true);
            send(user2, MESSAGE, USER1_NAME, observer1, true);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testPublish_stopPublicationThenPublishAgain_shouldSucceed() {
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
            publish(user1, localStream1, USER2_NAME, observer2, true, true);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testPublish_stopClientThenPublishAgain_shouldSucceed() {
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
            publish(user1, localStream1, USER2_NAME, observer2, true, true);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testPublish_stopPeerThenPublishAgain_shouldSucceed() {
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
            publish(user1, localStream1, USER2_NAME, observer2, true, true);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testPublish_simultaneouslyPublishTwoStream_shouldSucceed() {
        try {
            observer2 = new P2PClientObserver(USER2_NAME, 2);
            user1 = createPeerClient(null);
            user2 = createPeerClient(observer2);
            user1.addAllowedRemotePeer(USER2_NAME);
            user2.addAllowedRemotePeer(USER1_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            try {
                capturer1 = new RawCapturerForTest(RAW_STREAM_FILE);
            } catch (IOException e) {
                fail(e.getMessage());
            }
            localStream1 = createLocalStream(true, capturer1);
            capturer2 = createDefaultCapturer();
            localStream2 = createLocalStream(true, capturer2);
            TestCallback<Publication> callback1 = new TestCallback<>();
            TestCallback<Publication> callback2 = new TestCallback<>();
            user1.publish(USER2_NAME, localStream1, callback1);
            user1.publish(USER2_NAME, localStream2, callback2);
            assertTrue(callback1.getResult(true, TIMEOUT));
            assertTrue(callback2.getResult(true, TIMEOUT));
            assertTrue(observer2.getResultForStreamAdded(TIMEOUT));
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testPublish_simultaneouslyTwoClientPublish_shouldSucceed() {
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
            TestCallback<Publication> callback1 = new TestCallback<>();
            TestCallback<Publication> callback2 = new TestCallback<>();
            user1.publish(USER2_NAME, localStream1, callback1);
            user2.publish(USER1_NAME, localStream1, callback2);
            assertTrue(callback1.getResult(true, TIMEOUT));
            assertTrue(callback2.getResult(true, TIMEOUT));
            assertTrue(observer1.getResultForStreamAdded(TIMEOUT));
            assertTrue(observer2.getResultForStreamAdded(TIMEOUT));
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testPublish_withDifferentCodecSupportClient_shouldFail() {
        try {
            ArrayList<MediaCodecs.VideoCodec> videoCodec1 = new ArrayList<>();
            videoCodec1.add(MediaCodecs.VideoCodec.VP8);
            ArrayList<MediaCodecs.VideoCodec> videoCodec2 = new ArrayList<>();
            videoCodec2.add(MediaCodecs.VideoCodec.H264);
            observer1 = new P2PClientObserver(USER1_NAME);
            observer2 = new P2PClientObserver(USER2_NAME);
            user1 = createPeerClient(videoCodec1, null, observer1);
            user2 = createPeerClient(videoCodec2, null, observer2);
            user1.addAllowedRemotePeer(USER2_NAME);
            user2.addAllowedRemotePeer(USER1_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            publish(user1, localStream1, USER2_NAME, null, false, false);
            publish(user2, localStream1, USER1_NAME, null, false, false);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testPublish_afterSendMsgWithoutWaitCallBack_shouldSucceed() {
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
            user1.send(USER2_NAME, MESSAGE, sendTestCallback);
            user1.publish(USER2_NAME, localStream1, publicationTestCallback);
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
    public void testPublish_afterRemoteClientSendMsgWithoutWaitCallBack_shouldSucceed() {
        try {
            observer1 = new P2PClientObserver(USER2_NAME);
            observer2 = new P2PClientObserver(USER2_NAME);
            user1 = createPeerClient(observer1);
            user2 = createPeerClient(observer2);
            user1.addAllowedRemotePeer(USER2_NAME);
            user2.addAllowedRemotePeer(USER1_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            int initDataReceive = observer1.dataReceived.size();
            TestCallback<Publication> publicationTestCallback = new TestCallback<>();
            TestCallback<Void> sendTestCallback = new TestCallback<>();
            user2.send(USER1_NAME, MESSAGE, sendTestCallback);
            user1.publish(USER2_NAME, localStream1, publicationTestCallback);
            assertTrue(publicationTestCallback.getResult(true, TIMEOUT));
            assertTrue(sendTestCallback.getResult(true, TIMEOUT));
            assertTrue(observer2.getResultForStreamAdded(Config.TIMEOUT));
            assertTrue(observer1.getResultForDataReceived(Config.TIMEOUT));
            assertEquals(MESSAGE, observer1.dataReceived.get(initDataReceive));
            assertEquals(user2.id(), observer1.dataSenders.get(initDataReceive));
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testPublish_rePublishAfterPublicationStopWithoutWaitEvent_shouldSucceed() {
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
            publish(user1, localStream1, USER2_NAME, observer2, true, true);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testPublish_stopClientThenPublishAgainWithoutWaitEvent_shouldSucceed() {
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
            publish(user1, localStream1, USER2_NAME, observer2, true, true);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testPublish_stopPeerThenPublishAgainWithoutWaitEvent_shouldSucceed() {
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
            publish(user1, localStream1, USER2_NAME, observer2, true, true);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testPublish_afterDisconnect_shouldFail() {
        try {
            observer1 = new P2PClientObserver(USER2_NAME);
            user1 = createPeerClient(observer1);
            user2 = createPeerClient(null);
            user1.addAllowedRemotePeer(USER2_NAME);
            user2.addAllowedRemotePeer(USER1_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            disconnect(user1, observer1);
            publish(user1, localStream1, USER2_NAME, null, false, false);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
