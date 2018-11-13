package oms.test.conference.apitest;

import static oms.test.conference.util.ConferenceAction.createClient;
import static oms.test.conference.util.ConferenceAction.getToken;
import static oms.test.conference.util.ConferenceAction.join;
import static oms.test.conference.util.ConferenceAction.send;
import static oms.test.util.Config.MESSAGE;
import static oms.test.util.Config.PRESENTER_ROLE;
import static oms.test.util.Config.TIMEOUT;
import static oms.test.util.Config.USER1_NAME;
import static oms.test.util.Config.USER2_NAME;

import android.test.suitebuilder.annotation.LargeTest;

import oms.conference.ConferenceInfo;
import oms.test.conference.util.ConferenceClientObserver;
import oms.test.util.Config;
import oms.test.util.TestCallback;

public class SendTest extends TestBase {
    @LargeTest
    public void testSend_beforeJoin_shouldFail() {
        try {
            client1 = createClient(null);
            send(client1, null, Config.MESSAGE, null, false);
        } catch (Exception e) {
            fail(e.getMessage());
        }finally {
            client1 = null;
        }
    }

    @LargeTest
    public void testSend_withEmptyMessage_shouldSucceed() {
        try {
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            client1 = createClient(null);
            client2 = createClient(null);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            ConferenceInfo info = join(client2, getToken(PRESENTER_ROLE, USER2_NAME), null, null,
                    true);
            observer2 = new ConferenceClientObserver(USER2_NAME, 1);
            client1.addObserver(observer1);
            client2.addObserver(observer2);
            send(client1, info.self().id, "", observer2, true);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSend_nullMsg_shouldFail() {
        try {
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            client1 = createClient(null);
            client2 = createClient(null);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            ConferenceInfo info = join(client2, getToken(PRESENTER_ROLE, USER2_NAME), null, null,
                    true);
            observer2 = new ConferenceClientObserver(USER2_NAME, 1);
            client1.addObserver(observer1);
            client2.addObserver(observer2);
            try {
                send(client1, info.self().id, null, observer2, false);
                fail("RuntimeException expected.");
            } catch (RuntimeException ignored) {
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSend_withSpecialCharacterMessage_shouldSucceed() {
        try {
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            client1 = createClient(null);
            observer2 = new ConferenceClientObserver(USER2_NAME, 1);
            client2 = createClient(observer2);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            ConferenceInfo info = join(client2, getToken(PRESENTER_ROLE, USER2_NAME), null, null,
                    true);
            client1.addObserver(observer1);
            send(client1, info.self().id, Config.SPECIAL_CHARACTER, observer2, true);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSend_withLargeSizeMessage_shouldSucceed() {
        try {
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            client1 = createClient(null);
            observer2 = new ConferenceClientObserver(USER2_NAME, 1);
            client2 = createClient(observer2);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            ConferenceInfo info = join(client2, getToken(PRESENTER_ROLE, USER2_NAME), null, null,
                    true);
            client1.addObserver(observer1);
            String message = new String(new byte[1024]);
            send(client1, info.self().id, message, observer2, true);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSend_toOneJoinedUser_shouldSucceed() {
        try {
            client1 = createClient(null);
            client2 = createClient(null);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            ConferenceInfo info = join(client2, getToken(PRESENTER_ROLE, USER2_NAME), null, null,
                    true);
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            observer2 = new ConferenceClientObserver(USER2_NAME, 1);
            client1.addObserver(observer1);
            client2.addObserver(observer2);
            send(client1, info.self().id, MESSAGE, observer2, true);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSend_toAllUsers_shouldSucceed() {
        try {
            client1 = createClient(null);
            client2 = createClient(null);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            join(client2, getToken(PRESENTER_ROLE, USER2_NAME), null, null, true);
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            observer2 = new ConferenceClientObserver(USER2_NAME, 1);
            client1.addObserver(observer1);
            client2.addObserver(observer2);
            send(client1, null, MESSAGE, observer2, true);
            assertTrue(observer1.getResultForSend(TIMEOUT));
            assertEquals(MESSAGE, observer1.lastRecvMsg);
            assertEquals(client1.info().self().id, observer1.lastSenderId);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSend_toUnjoinedUser_shouldFail() {
        try {
            client1 = createClient(null);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            send(client1, Config.USER2_NAME, Config.MESSAGE, null, false);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSend_toMyself_shouldSucceed() {
        try {
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            client1 = createClient(observer1);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            send(client1, client1.info().self().id, Config.MESSAGE, observer1, true);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSend_multipleTimes_shouldSucceed() {
        try {
            observer1 = new ConferenceClientObserver(USER1_NAME, 30);
            client1 = createClient(null);
            observer2 = new ConferenceClientObserver(USER2_NAME, 30);
            client2 = createClient(null);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            join(client2, getToken(PRESENTER_ROLE, USER2_NAME), null, null, true);
            client1.addObserver(observer1);
            client2.addObserver(observer2);
            for (int i = 0; i < 30; i++) {
                TestCallback<Void> sendCallback = new TestCallback<>();
                client2.send(Config.CHINESE_CHARACTER, sendCallback);
            }
            assertTrue(observer1.getResultForSend(Config.TIMEOUT));
            assertTrue(observer2.getResultForSend(Config.TIMEOUT));
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
