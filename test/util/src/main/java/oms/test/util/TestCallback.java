package oms.test.util;

import android.util.Log;

import oms.base.ActionCallback;
import oms.base.OMSError;

public class TestCallback<T> extends Resultable implements ActionCallback<T> {
    private final String TAG = "ics_test_util";
    public T successCallbackResult;
    private boolean onSuccessTriggered = false;
    private boolean onFailureTriggered = false;
    public long resultTsMs = 0;

    /**
     * Callback should be triggered once and only. And renew a latch for callback is not allowed.
     */
    public TestCallback() {
        super(1);
    }

    @Override
    public void onSuccess(T result) {
        Log.d(TAG, "onSuccess.");
        this.successCallbackResult = result;
        onSuccessTriggered = true;
        resultTsMs = System.currentTimeMillis();
        onResult();
    }

    @Override
    public void onFailure(OMSError error) {
        Log.d(TAG, "onFailure: " + error.errorMessage);
        onFailureTriggered = true;
        onResult();
    }

    public boolean getResult(boolean expectation, int timeout) {
        return super.getResult(timeout) && (expectation ? onSuccessTriggered : onFailureTriggered);
    }
}
