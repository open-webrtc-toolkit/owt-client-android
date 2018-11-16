package oms.test.conference.apitest;

import android.test.ActivityInstrumentationTestCase2;

import oms.base.LocalStream;
import oms.base.VideoCapturer;
import oms.conference.ConferenceClient;
import oms.test.conference.util.ConferenceClientObserver;
import oms.test.util.Config;

public class TestBase extends ActivityInstrumentationTestCase2<TestActivity> {
    ConferenceClient client1 = null;
    ConferenceClient client2 = null;
    ConferenceClient client3 = null;
    ConferenceClientObserver observer1 = null;
    ConferenceClientObserver observer2 = null;
    ConferenceClientObserver observer3 = null;
    VideoCapturer capturer1 = null;
    VideoCapturer capturer2 = null;
    LocalStream localStream1 = null;
    LocalStream localStream2 = null;
    TestActivity act = null;

    public TestBase() {
        super(TestActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        act = getActivity();
    }

    @Override
    public void tearDown() throws Exception {
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
        if (observer3 != null) {
            observer3.clearStatus(1);
        }
        if (client1 != null) {
            client1.leave();
            if (observer1 != null) {
                observer1.getResultForLeave(Config.TIMEOUT);
            }
        }
        if (client2 != null) {
            client2.leave();
            if (observer2 != null) {
                observer2.getResultForLeave(Config.TIMEOUT);
            }
        }
        if (client3 != null) {
            client3.leave();
            if (observer3 != null) {
                observer3.getResultForLeave(Config.TIMEOUT);
            }
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
        client1 = null;
        client2 = null;
        client3 = null;
        localStream1 = null;
        localStream2 = null;
        assertFalse(act.isExceptionCaught());
    }

}
