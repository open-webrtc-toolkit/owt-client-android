
import static junit.framework.Assert.assertTrue;

import android.util.Log;

import owt.base.ActionCallback;
import owt.base.OwtError;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Callback<T> implements ActionCallback<T> {
    private static final String TAG = Callback.class.getSimpleName();
    private CountDownLatch latch = new CountDownLatch(1);
    private boolean onSuccessTriggered = false, onFailureTriggered = false;
    T result;

    @Override
    public void onSuccess(T result) {
        Log.v(TAG, "Callback.onSuccess.");
        onSuccessTriggered = true;
        this.result = result;
        onResult();
    }

    @Override
    public void onFailure(OwtError error) {
        Log.v(TAG, "Callback.onFailure: " + error.errorMessage);
        onFailureTriggered = true;
        onResult();
    }

    private void onResult() {
        assertTrue("Unexpected event triggered.", latch.getCount() > 0);
        latch.countDown();
    }

    boolean getResult(boolean expected) {
        try {
            if (latch.await(5000, TimeUnit.MILLISECONDS)) {
                return expected ? onSuccessTriggered : onFailureTriggered;
            } else {
                Log.e(TAG, "Timeout on Callback.getResult.");
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "InterruptedException during latch.await");
        }
        return false;
    }
}
