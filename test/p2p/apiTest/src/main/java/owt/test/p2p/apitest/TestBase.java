/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.test.p2p.apitest;

import android.test.ActivityInstrumentationTestCase2;

import owt.base.LocalStream;
import owt.base.VideoCapturer;
import owt.p2p.P2PClient;
import owt.test.p2p.util.P2PClientObserver;

public class TestBase extends ActivityInstrumentationTestCase2<TestActivity> {
    P2PClient user1 = null;
    P2PClient user2 = null;
    P2PClientObserver observer1 = null;
    P2PClientObserver observer2 = null;
    VideoCapturer capturer1 = null;
    VideoCapturer capturer2 = null;
    LocalStream localStream1 = null;
    LocalStream localStream2 = null;

    public TestBase() {
        super(TestActivity.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
        getActivity();
    }

    protected void tearDown() throws Exception {
        finishTest();
        super.tearDown();
    }

    private void finishTest() {
        if (observer1 != null) {
            observer1.clearStatus(1);
        }
        if (observer2 != null) {
            observer2.clearStatus(1);
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
    }
}
