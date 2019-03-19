import static junit.framework.Assert.fail;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import owt.base.ContextInitialization;
import owt.conference.ConferenceClient;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ConferenceClientTest {
    static {
        ContextInitialization.create().setApplicationContext(
                InstrumentationRegistry.getTargetContext()).initialize();
    }

    // TODO: add more test cases.
    @Test
    public void testCreationWithNullConfig() {
        try {
            new ConferenceClient(null);
            fail("Exception expected.");
        } catch (Exception ignored) {
        }
    }

}
