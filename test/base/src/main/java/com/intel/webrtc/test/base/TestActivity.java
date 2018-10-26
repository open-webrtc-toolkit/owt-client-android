package com.intel.webrtc.test.base;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.os.Process;

import com.intel.webrtc.base.ContextInitialization;

import org.webrtc.EglBase;

public class TestActivity extends Activity {
    private final static String TAG = "ics_base_test";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        Log.d(TAG, "PID=" + Process.myPid() + "=");
        initConferenceClient();
    }

    private void initConferenceClient() {
        EglBase rootEglBase = EglBase.create();
        ContextInitialization.create().setApplicationContext(this)
                .setCodecHardwareAccelerationEnabled(true)
                .setVideoHardwareAccelerationOptions(rootEglBase.getEglBaseContext(),
                        rootEglBase.getEglBaseContext())
                .initialize();
    }
}
