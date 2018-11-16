package oms.test.p2p.apitest;

import android.test.ActivityInstrumentationTestCase2;

import oms.base.LocalStream;
import oms.base.VideoCapturer;
import oms.p2p.P2PClient;
import oms.test.p2p.util.P2PClientObserver;
import oms.test.util.Config;

public class TestBase extends ActivityInstrumentationTestCase2<TestActivity> {
    P2PClient user1 = null;
    P2PClient user2 = null;
    P2PClientObserver observer1 = null;
    P2PClientObserver observer2 = null;
    VideoCapturer capturer1 = null;
    VideoCapturer capturer2 = null;
    LocalStream localStream1 = null;
    LocalStream localStream2 = null;
    TestActivity act = null;

    public TestBase() {
        super(TestActivity.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
        act = getActivity();
    }

    protected void tearDown() throws Exception {
        try {
            finishTest();
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            super.tearDown();
        }
    }

    private void finishTest() {
        if (observer1 != null) {
            observer1.clearStatus(1);
        }
        if (observer2 != null) {
            observer2.clearStatus(1);
        }
        if (user1 != null) {
            user1.disconnect();
        }
        if (user2 != null) {
            user2.disconnect();
        }
        if (observer1 != null) {
            observer1.getResultForServerDisconnected(Config.TIMEOUT);
        }
        if (observer2 != null) {
            observer2.getResultForServerDisconnected(Config.TIMEOUT);
        }
        try {
            if (capturer1 != null) {
                capturer1.stopCapture();
                capturer1.dispose();
            }
            if (capturer2 != null) {
                capturer2.stopCapture();
                capturer2.dispose();
            }
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        if (localStream1 != null) {
            localStream1.dispose();
        }
        if (localStream2 != null) {
            localStream2.dispose();
        }
        observer1 = null;
        observer2 = null;
        user1 = null;
        user2 = null;
        localStream1 = null;
        localStream2 = null;
        assertFalse(act.isExceptionCaught());
    }
}
