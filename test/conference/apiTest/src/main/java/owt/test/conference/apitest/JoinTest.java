/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.test.conference.apitest;

import static owt.test.conference.util.ConferenceAction.createClient;
import static owt.test.conference.util.ConferenceAction.getToken;
import static owt.test.conference.util.ConferenceAction.join;
import static owt.test.conference.util.ConferenceAction.leave;
import static owt.test.util.Config.CHINESE_CHARACTER;
import static owt.test.util.Config.CONFERENCE_ROOM_ID;
import static owt.test.util.Config.CONFERENCE_ROOM_ID_INCORRECT;
import static owt.test.util.Config.CONFERENCE_SERVER_HTTP;
import static owt.test.util.Config.CONFERENCE_SERVER_HTTPS;
import static owt.test.util.Config.CONFERENCE_SERVER_INCORRECT;
import static owt.test.util.Config.CONFERENCE_TOKEN_FAKE;
import static owt.test.util.Config.CONFERENCE_TOKEN_INCORRECT;
import static owt.test.util.Config.ERROR_ROLE;
import static owt.test.util.Config.PRESENTER_ROLE;
import static owt.test.util.Config.SPECIAL_CHARACTER;
import static owt.test.util.Config.TIMEOUT;
import static owt.test.util.Config.USER1_NAME;
import static owt.test.util.Config.USER2_NAME;
import static owt.test.util.Config.USER3_NAME;
import static owt.test.util.Config.VIEWER_ROLE;

import owt.conference.ConferenceInfo;
import owt.test.conference.util.ConferenceClientObserver;
import owt.test.conference.util.HttpUtils;
import owt.test.util.TestCallback;

public class JoinTest extends TestBase {

    public void testJoin_multipleClients_checkEventsAndInfo() {
        observer1 = new ConferenceClientObserver(USER1_NAME, 1);
        client1 = createClient(observer1);
        observer2 = new ConferenceClientObserver(USER2_NAME, 1);
        client2 = createClient(observer2);
        client3 = createClient(null);
        ConferenceInfo info1 = join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null,
                true);
        assertEquals(PRESENTER_ROLE, info1.self().role);
        assertEquals(USER1_NAME, info1.self().userId);
        ConferenceInfo info2 = join(client2, getToken(PRESENTER_ROLE, USER2_NAME), null, observer1,
                true);
        assertEquals(USER2_NAME, observer1.participants.get(0).userId);
        assertEquals(PRESENTER_ROLE, observer1.participants.get(0).role);
        assertEquals(PRESENTER_ROLE, info2.self().role);
        assertEquals(USER2_NAME, info2.self().userId);
        ConferenceInfo info3 = join(client3, getToken(PRESENTER_ROLE, USER3_NAME), null, observer1,
                true);
        assertEquals(USER3_NAME, observer1.participants.get(1).userId);
        assertEquals(PRESENTER_ROLE, observer1.participants.get(1).role);
        assertEquals(USER3_NAME, observer2.participants.get(0).userId);
        assertEquals(PRESENTER_ROLE, observer2.participants.get(0).role);
        assertEquals(PRESENTER_ROLE, info3.self().role);
        assertEquals(USER3_NAME, info3.self().userId);
    }

    public void testJoin_withInsecureSSL_shouldSucceed() {
        client1 = createClient(null);
        String token = HttpUtils.getTokenSSLINSECURE(CONFERENCE_SERVER_HTTPS, PRESENTER_ROLE,
                USER1_NAME, CONFERENCE_ROOM_ID);
        ConferenceInfo info = join(client1, token, null, null, true);
        assertEquals(PRESENTER_ROLE, info.self().role);
        assertEquals(USER1_NAME, info.self().userId);
    }

    public void testJoin_withIncorrectThenCorrectServer_shouldSucceed() {
        client1 = createClient(null);
        String token = HttpUtils.getToken(CONFERENCE_SERVER_INCORRECT, PRESENTER_ROLE,
                USER1_NAME, CONFERENCE_ROOM_ID);
        join(client1, token, null, null, false);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
    }

    public void testJoin_withIncorrectThenCorrectRoomId_shouldSucceed() {
        client1 = createClient(null);
        String token = HttpUtils.getToken(CONFERENCE_SERVER_HTTP, PRESENTER_ROLE, USER1_NAME,
                CONFERENCE_ROOM_ID_INCORRECT);
        join(client1, token, null, null, false);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
    }

    public void testJoin_withNullToken_shouldThrowException() {
        client1 = createClient(null);
        try {
            join(client1, null, null, null, false);
            fail("RuntimeException expected.");
        } catch (RuntimeException ignored) {
        }
        client1 = null;
    }

    public void testJoin_withIncorrectFormattedToken_shouldFail() {
        client1 = createClient(null);
        join(client1, CONFERENCE_TOKEN_INCORRECT, null, null, false);
        client1 = null;
    }

    public void testJoin_withIncorrectContentedToken_shouldFail() {
        client1 = createClient(null);
        join(client1, CONFERENCE_TOKEN_FAKE, null, null, false);
        client1 = null;
    }

    public void testJoin_twoClientsWithSameToken_shouldFailAt2nd() {
        client1 = createClient(null);
        client2 = createClient(null);
        String token = getToken(PRESENTER_ROLE, USER1_NAME);
        join(client1, token, null, null, true);
        join(client2, token, null, null, false);
        client2 = null;
    }

    public void testJoin_thenLeaveThenJoinWithSameToken_shouldFail() {
        client1 = createClient(null);
        String token = getToken(PRESENTER_ROLE, USER1_NAME);
        join(client1, token, null, null, true);
        observer1 = new ConferenceClientObserver(USER1_NAME, 1);
        client1.addObserver(observer1);
        leave(client1, observer1, null);
        join(client1, token, null, null, false);
        client1 = null;
    }

    public void testJoin_thenLeaveThenJoinWithDifferentToken_shouldSucceed() {
        client1 = createClient(null);
        join(client1, getToken(VIEWER_ROLE, USER1_NAME), null, null, true);
        observer1 = new ConferenceClientObserver(USER1_NAME, 1);
        client1.addObserver(observer1);
        leave(client1, observer1, null);
        join(client1, getToken(VIEWER_ROLE, USER1_NAME), null, null, true);
    }

    public void testJoin_withNullRole_shouldFail() {
        client1 = createClient(null);
        String token = HttpUtils.getToken(CONFERENCE_SERVER_HTTP, null, USER1_NAME,
                CONFERENCE_ROOM_ID);
        join(client1, token, null, null, false);
        client1 = null;
    }

    public void testJoin_withUnsupportedRole_shouldFail() {
        client1 = createClient(null);
        join(client1, getToken(ERROR_ROLE, USER1_NAME), null, null, false);
        client1 = null;
    }

    public void testJoin_thenLeaveThenJoinWithDifferentRole_shouldSucceed() {
        client1 = createClient(null);
        join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
        observer1 = new ConferenceClientObserver(USER1_NAME, 1);
        client1.addObserver(observer1);
        leave(client1, observer1, null);
        ConferenceInfo info = join(client1, getToken(VIEWER_ROLE, USER1_NAME), null, null, true);
        assertEquals(VIEWER_ROLE, info.self().role);
    }

    public void testJoin_withNullUsername_shouldFail() {
        client1 = createClient(null);
        String token = HttpUtils.getToken(CONFERENCE_SERVER_HTTP, PRESENTER_ROLE, null,
                CONFERENCE_ROOM_ID);
        join(client1, token, null, null, false);
        client1 = null;
    }

    public void testJoin_withSpecialCharacterUsername_shouldSucceed() {
        client1 = createClient(null);
        String token = HttpUtils.getToken(CONFERENCE_SERVER_HTTP, PRESENTER_ROLE,
                SPECIAL_CHARACTER, CONFERENCE_ROOM_ID);
        ConferenceInfo info = join(client1, token, null, null, true);
        assertEquals(SPECIAL_CHARACTER, info.self().userId);
        assertEquals(PRESENTER_ROLE, info.self().role);
    }

    public void testJoin_withChineseUsername_shouldSucceed() {
        client1 = createClient(null);
        String token = HttpUtils.getToken(CONFERENCE_SERVER_HTTP, PRESENTER_ROLE,
                CHINESE_CHARACTER, CONFERENCE_ROOM_ID);
        ConferenceInfo info = join(client1, token, null, null, true);
        assertEquals(CHINESE_CHARACTER, info.self().userId);
        assertEquals(PRESENTER_ROLE, info.self().role);
    }

    public void testJoin_twiceWithoutWaitingForCallback_shouldSucceedOnce() {
        client1 = createClient(null);
        String token1 = getToken(PRESENTER_ROLE, USER1_NAME);
        String token2 = getToken(PRESENTER_ROLE, USER2_NAME);
        TestCallback<ConferenceInfo> callback1 = new TestCallback<>();
        TestCallback<ConferenceInfo> callback2 = new TestCallback<>();
        client1.join(token1, callback1);
        client1.join(token2, callback2);
        assertTrue(callback1.getResult(true, TIMEOUT));
        assertTrue(callback2.getResult(false, TIMEOUT));
    }
}
