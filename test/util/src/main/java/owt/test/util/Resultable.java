/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.test.util;

import static junit.framework.Assert.assertTrue;

import android.util.Log;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public abstract class Resultable {
    private final String TAG = "owt_test_util";
    private CountDownLatch latch;

    public Resultable(int count) {
        latch = new CountDownLatch(count);
    }

    protected void reinitLatch(int count) {
        if (latch.getCount() != 0) {
            Log.w(TAG, "Renew a latch before its count reaches to zero.");
        }
        latch = new CountDownLatch(count);
    }

    protected void onResult() {
        assertTrue("Unexpected event triggered.", latch.getCount() > 0);
        latch.countDown();
    }

    /**
     * @param timeout timeout
     * @return return false upon timeout or interrupted, otherwise return true.
     */
    protected boolean getResult(int timeout) {
        try {
            if (latch.await(timeout, TimeUnit.MILLISECONDS)) {
                return true;
            } else {
                Log.w(TAG, "Timeout on Resultable.getResult.");
            }
        } catch (InterruptedException e) {
            Log.w(TAG, "InterruptedException during latch.await");
        }
        return false;
    }
}
