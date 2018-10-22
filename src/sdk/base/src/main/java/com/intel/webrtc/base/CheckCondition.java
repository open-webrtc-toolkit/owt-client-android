/*
 * Intel License Header Holder
 */
package com.intel.webrtc.base;

import android.util.Log;

///@cond
public final class CheckCondition {
    private static final String TAG = "ICS";
    //TODO: As project variable BuildConfig.DEBUG isn't reliable reportedly, we set up ourselves.
    //TODO: DO NOT forget to disable ICS_DEBUG when exporting release jar files.
    private static boolean ICS_DEBUG = true;

    public static void DCHECK(Object obj) {
        if (ICS_DEBUG) {
            RCHECK(obj);
        }
    }

    public static void RCHECK(Object object) {
        if (object == null) {
            String error = "Object of " + Object.class + "is not expected to be null.";
            Log.e(TAG, error);
            printStackTrace();
            throw new RuntimeException(error);
        }
    }

    public static void DCHECK(boolean condition) {
        if (ICS_DEBUG) {
            RCHECK(condition);
        }
    }

    public static void RCHECK(boolean condition) {
        if (!condition) {
            String error = "Wrong condition.";
            Log.d(TAG, error);
            printStackTrace();
            throw new RuntimeException(error);
        }
    }

    public static void DCHECK(Exception e) {
        if (ICS_DEBUG) {
            RCHECK(e);
        }
    }

    public static void RCHECK(Exception e) {
        e.printStackTrace();
        throw new RuntimeException(e.getCause());
    }

    private static void printStackTrace() {
        Thread thread = Thread.currentThread();
        StackTraceElement[] traceElements = thread.getStackTrace();
        for (StackTraceElement traceElement : traceElements) {
            Log.d(TAG, traceElement.toString());
        }
    }
    ///@endcond
}
