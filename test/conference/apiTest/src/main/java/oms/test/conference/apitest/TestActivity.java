package oms.test.conference.apitest;

import android.app.Activity;
import android.os.Bundle;

import android.os.Process;
import android.util.Log;

import oms.base.ContextInitialization;

import org.webrtc.EglBase;

import java.io.PrintWriter;
import java.io.StringWriter;

public class TestActivity extends Activity {
    private final static String TAG = "ics_conference_test";
    private static boolean initialized = false;
    private boolean exception = false;

    public boolean isException() {
        return exception;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conference);
        Log.d(TAG, "PID=" + Process.myPid() + "=");
        initConferenceClient();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw, true));
            String str = sw.toString();
            Log.d(TAG, str);
            exception = true;
        });
    }

    private void initConferenceClient() {
        if (!initialized) {
            EglBase rootEglBase = EglBase.create();
            ContextInitialization.create().setApplicationContext(this)
                    .setCodecHardwareAccelerationEnabled(true)
                    .setVideoHardwareAccelerationOptions(rootEglBase.getEglBaseContext(),
                            rootEglBase.getEglBaseContext())
                    .initialize();
            initialized = true;
        }
    }

}
