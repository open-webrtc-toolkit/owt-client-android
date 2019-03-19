/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.test.conference.apitest;

import static owt.test.conference.util.ConferenceAction.leave;

import android.test.ActivityInstrumentationTestCase2;

import owt.base.LocalStream;
import owt.base.VideoCapturer;
import owt.conference.ConferenceClient;
import owt.test.conference.util.ConferenceClientObserver;

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

    public TestBase() {
        super(TestActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        getActivity();
    }

    @Override
    public void tearDown() throws Exception {
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
        if (observer2 != null) {
            observer2.clearStatus(1);
        }
        if (client1 != null) {
            leave(client1, observer1, null);
        }
        if (client2 != null) {
            leave(client2, observer2, null);
        }
        if (client3 != null) {
            leave(client3, observer3, null);
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
    }
}
