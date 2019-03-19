import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import owt.base.ContextInitialization;
import owt.p2p.P2PClient;
import owt.p2p.P2PClientConfiguration;

@RunWith(AndroidJUnit4.class)
public class P2PClientTest {

    private static final String UID1 = "PCTest1";
    private static final String UID2 = "PCTest2";
    private P2PClientConfiguration defaultConf;

    static {
        ContextInitialization.create().setApplicationContext(
                InstrumentationRegistry.getTargetContext()).initialize();
    }

    @Before
    public void setUp() {
        defaultConf = P2PClientConfiguration.builder().build();
    }

    // TODO: add more test cases and more checking.
    @Test
    public void testCreationWithNullConfig() {
        try {
            new P2PClient(null, new MockSignalingChannel());
            fail("Exception expected.");
        } catch (Exception ignored) {
        }
    }

    @Test
    public void testCreationWithNullSignaling() {
        try {
            new P2PClient(defaultConf, null);
            fail("Exception expected.");
        } catch (Exception ignored) {
        }
    }

    @Test
    public void testConnect() {
        P2PClient client = new P2PClient(defaultConf, new MockSignalingChannel());
        String token = "{'token':" + UID1 + "}";
        Callback<String> callback = new Callback<>();
        client.connect(token, callback);
        assertTrue(callback.getResult(true));
        client.disconnect();
    }
}
