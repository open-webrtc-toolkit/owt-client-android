package com.intel.webrtc.test.p2p.apitest;

import static com.intel.webrtc.test.p2p.util.P2PAction.connect;
import static com.intel.webrtc.test.p2p.util.P2PAction.createPeerClient;
import static com.intel.webrtc.test.p2p.util.P2PAction.disconnect;
import static com.intel.webrtc.test.util.Config.P2P_SERVER;
import static com.intel.webrtc.test.util.Config.P2P_SERVER_INCORRECT;
import static com.intel.webrtc.test.util.Config.SPECIAL_CHARACTER;
import static com.intel.webrtc.test.util.Config.USER1_NAME;

import com.intel.webrtc.test.p2p.util.P2PClientObserver;

public class ConnectTest extends TestBase {

    public void testConnect_toCorrectServer_shouldSucceed() {
        user1 = createPeerClient(null);
        connect(user1, USER1_NAME, P2P_SERVER, true);
    }

    public void testConnect_toIncorrectServer_shouldFail() {
        user1 = createPeerClient(null);
        connect(user1, USER1_NAME, P2P_SERVER_INCORRECT, false);
    }

    public void testConnect_toIncorrectThenCorrectServer_shouldSucceed() {
        user1 = createPeerClient(null);
        connect(user1, USER1_NAME, P2P_SERVER_INCORRECT, false);
        connect(user1, USER1_NAME, P2P_SERVER, true);
    }

    public void testConnect_withNullUsername_shouldFail() {
        user1 = createPeerClient(null);
        connect(user1, null, P2P_SERVER, false);
    }

    public void testConnect_withSpecialUsername_shouldSucceed() {
        user1 = createPeerClient(null);
        connect(user1, SPECIAL_CHARACTER, P2P_SERVER, true);
    }

    public void testConnect_twice_shouldFailAt2nd() {
        user1 = createPeerClient(null);
        connect(user1, USER1_NAME, P2P_SERVER, true);
        connect(user1, USER1_NAME, P2P_SERVER, false);
    }

    public void testConnect_toCorrectServerThenDisconnectThenConnect_shouldSucceed() {
        observer1 = new P2PClientObserver(USER1_NAME);
        user1 = createPeerClient(observer1);
        connect(user1, USER1_NAME, P2P_SERVER, true);
        disconnect(user1, observer1);
        connect(user1, USER1_NAME, P2P_SERVER, true);
    }
}
