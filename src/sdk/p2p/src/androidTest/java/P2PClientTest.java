import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import oms.base.ContextInitialization;
import oms.p2p.P2PClient;
import oms.p2p.P2PClientConfiguration;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

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

    // TODO: add more test cases.
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
        client.disconnect();
    }

    @Test
    public void testSend() {
        P2PClient client1 = new P2PClient(defaultConf, new MockSignalingChannel());
        P2PClient client2 = new P2PClient(defaultConf, new MockSignalingChannel());
        String token1 = "{'token':" + UID1 + "}";
        String token2 = "{'token':" + UID2 + "}";
        Callback<String> connectCallback1 = new Callback<>();
        Callback<String> connectCallback2 = new Callback<>();
        client1.connect(token1, connectCallback1);
        assertTrue(connectCallback1.getResult(true));
        client2.connect(token2, connectCallback2);
        assertTrue(connectCallback2.getResult(true));
        client1.addAllowedRemotePeer(UID2);
        client2.addAllowedRemotePeer(UID1);
        Callback<Void> sendCallback = new Callback<>();
        client1.send(UID2, "message", sendCallback);
        assertTrue(sendCallback.getResult(true));
        client1.disconnect();
        client2.disconnect();
    }
}
