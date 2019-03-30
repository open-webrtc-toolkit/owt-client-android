/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.base;

import android.util.Log;

///@cond
public final class CheckCondition {
    private static final String TAG = "OWT";
    //TODO: As project variable BuildConfig.DEBUG isn't reliable reportedly, we set up ourselves.
    //TODO: DO NOT forget to disable OWT_DEBUG when exporting release jar files.
    private static final boolean OWT_DEBUG = true;

    public static void DCHECK(Object obj) {
        if (OWT_DEBUG) {
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
        if (OWT_DEBUG) {
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
        if (OWT_DEBUG) {
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
