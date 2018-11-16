package oms.test.p2p.apitest;

import android.app.Activity;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;

import oms.base.ContextInitialization;

import org.webrtc.EglBase;

import java.io.PrintWriter;
import java.io.StringWriter;

public class TestActivity extends Activity {
    private final static String TAG = "ics_p2p_test";
    private static boolean initialized = false;
    private boolean exceptionCaught = false;

    public boolean isExceptionCaught() {
        return exceptionCaught;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        Log.d(TAG, "PID=" + Process.myPid() + "=");
        createUI();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw, true));
            String str = sw.toString();
            Log.d(TAG, str);
            exceptionCaught = true;
        });
    }

    private void createUI() {
        // Initialization work.
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
