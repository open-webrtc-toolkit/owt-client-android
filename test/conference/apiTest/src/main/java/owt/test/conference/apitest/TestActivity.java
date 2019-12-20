/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.test.conference.apitest;

import android.app.Activity;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;

import org.webrtc.EglBase;

import owt.base.ContextInitialization;

public class TestActivity extends Activity {
    private final static String TAG = "owt_conference_test";
    private static boolean contextHasInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conference);
        Log.d(TAG, "PID=" + Process.myPid() + "=");
        initConferenceClient();
    }

    private void initConferenceClient() {
        if (!contextHasInitialized) {
            EglBase rootEglBase = EglBase.create();
            ContextInitialization.create().setApplicationContext(this)
                    .setVideoHardwareAccelerationOptions(rootEglBase.getEglBaseContext(),
                            rootEglBase.getEglBaseContext())
                    .initialize();
            contextHasInitialized = true;
        }
    }

}
