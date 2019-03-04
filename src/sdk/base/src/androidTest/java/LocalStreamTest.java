import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import owt.base.ContextInitialization;
import owt.base.LocalStream;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class LocalStreamTest {
    static {
        ContextInitialization.create().setApplicationContext(
                InstrumentationRegistry.getTargetContext()).initialize();
    }
    // TODO: add more test cases.
    @Test
    public void testCreateLocalStream() {
        new LocalStream(new MockVideoCapturer());
    }
}
