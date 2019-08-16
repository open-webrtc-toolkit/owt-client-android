/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.conference;

import static owt.base.CheckCondition.DCHECK;
import static owt.base.CheckCondition.RCHECK;
import static owt.base.Const.LOG_TAG;

import android.util.Base64;
import android.util.Log;

import okhttp3.OkHttpClient;
import owt.base.Const;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter.Listener;

final class SignalingChannel {

    interface SignalingChannelObserver {

        void onRoomConnected(JSONObject info);

        void onRoomConnectFailed(String errorMsg);

        void onReconnecting();

        void onRoomDisconnected();

        void onProgressMessage(JSONObject message);

        void onTextMessage(String message, String from, String to);

        void onStreamAdded(RemoteStream remoteStream);

        void onStreamRemoved(String streamId);

        void onStreamUpdated(String id, JSONObject updateInfo);

        void onParticipantJoined(JSONObject participantInfo);

        void onParticipantLeft(String participantId);
    }

    private SignalingChannelObserver observer;
    // Base64 encoded token.
    private final String token;
    private final ExecutorService callbackExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService refreshExecutor = Executors.newSingleThreadExecutor();
    private final int MAX_RECONNECT_ATTEMPTS = 5;
    private String reconnectionTicket;
    private int reconnectAttempts = 0;
    // No lock is guarding loggedIn so void access and modify it on threads other than
    // |callbackExecutor|.
    private boolean loggedIn = false;
    private Socket socketClient;
    // [{'name': name, 'msg': message, 'ack': ack}]
    private final ArrayList<HashMap<String, Object>> cache = new ArrayList<>();

    // Socket.IO events.
    private final Listener connectedCallback = args -> callbackExecutor.execute(() -> {
        Log.d(LOG_TAG, "Socket connected.");
        if (loggedIn) {
            relogin();
        } else {
            try {
                login();
            } catch (JSONException e) {
                observer.onRoomConnectFailed(e.getMessage());
            }
        }
    });
    private final Listener connectErrorCallback = (Object... args) -> callbackExecutor.execute(
            () -> {
                Log.d(LOG_TAG, "Socket connect error.");
                String msg = extractMsg(0, args);
                if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
                    if (loggedIn) {
                        triggerDisconnected();
                    } else {
                        observer.onRoomConnectFailed("Socket.IO connected failed: " + msg);
                    }
                }
            });
    private final Listener reconnectingCallback = args -> callbackExecutor.execute(() -> {
        Log.d(LOG_TAG, "Socket reconnecting.");
        reconnectAttempts++;
        // trigger onReconnecting, ONLY when already logged in and first time to reconnect.
        if (loggedIn && reconnectAttempts == 1) {
            observer.onReconnecting();
        }
    });
    // disconnectCallback will be bound ONLY when disconnect() is called actively.
    private final Listener disconnectCallback = args -> callbackExecutor.execute(
            this::triggerDisconnected);

    // MCU events.
    private final Listener progressCallback = (Object... args) -> callbackExecutor.execute(() -> {
        JSONObject msg = (JSONObject) args[0];
        observer.onProgressMessage(msg);
    });
    private final Listener participantCallback = (Object... args) -> callbackExecutor.execute(
            () -> {
                JSONObject msg = (JSONObject) args[0];
                try {
                    switch (msg.getString("action")) {
                        case "join":
                            observer.onParticipantJoined(msg.getJSONObject("data"));
                            break;
                        case "leave":
                            observer.onParticipantLeft(msg.getString("data"));
                            break;
                        default:
                            DCHECK(false);
                    }
                } catch (JSONException e) {
                    DCHECK(e);
                }
            });
    private final Listener streamCallback = (Object... args) -> callbackExecutor.execute(() -> {
        try {
            JSONObject msg = (JSONObject) args[0];
            String status = msg.getString("status");
            String streamId = msg.getString("id");
            switch (status) {
                case "add":
                    JSONObject data = msg.getJSONObject("data");
                    RemoteStream remoteStream = new RemoteStream(data);
                    observer.onStreamAdded(remoteStream);
                    break;
                case "remove":
                    observer.onStreamRemoved(streamId);
                    break;
                case "update":
                    observer.onStreamUpdated(streamId, msg.getJSONObject("data"));
                    break;
                default:
                    DCHECK(false);
            }

        } catch (JSONException e) {
            DCHECK(e);
        }
    });
    private final Listener textCallback = (Object... args) -> callbackExecutor.execute(() -> {
        JSONObject data = (JSONObject) args[0];
        try {
            observer.onTextMessage(data.getString("message"), data.getString("from"),
                    data.has("to") ? data.getString("to") : "");
        } catch (JSONException e) {
            DCHECK(false);
        }
    });
    private final Listener dropCallback = args -> triggerDisconnected();

    SignalingChannel(String token, SignalingChannelObserver observer) {
        this.token = token;
        this.observer = observer;
    }

    void connect(final ConferenceClientConfiguration configuration) {
        try {
            RCHECK(token);
            JSONObject jsonToken = new JSONObject(
                    new String(Base64.decode(token, Base64.DEFAULT)));

            boolean isSecure = jsonToken.getBoolean("secure");
            String host = jsonToken.getString("host");
            final String url = (isSecure ? "https" : "http") + "://" + host;

            IO.Options opt = new IO.Options();
            opt.forceNew = true;
            opt.reconnection = true;
            opt.reconnectionAttempts = MAX_RECONNECT_ATTEMPTS;
            opt.secure = isSecure;
            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
            if (configuration.sslContext != null) {
                clientBuilder.sslSocketFactory(configuration.sslContext.getSocketFactory());
            }
            if (configuration.hostnameVerifier != null) {
                clientBuilder.hostnameVerifier(configuration.hostnameVerifier);
            }
            OkHttpClient httpClient = clientBuilder.build();
            opt.callFactory = httpClient;
            opt.webSocketFactory = httpClient;
            socketClient = IO.socket(url, opt);

            // Do not listen EVENT_DISCONNECT event on this phase.
            socketClient.on(Socket.EVENT_CONNECT, connectedCallback)
                    .on(Socket.EVENT_CONNECT_ERROR, connectErrorCallback)
                    .on(Socket.EVENT_RECONNECTING, reconnectingCallback)
                    .on("progress", progressCallback)
                    .on("participant", participantCallback)
                    .on("stream", streamCallback)
                    .on("text", textCallback)
                    .on("drop", dropCallback);
            socketClient.connect();

        } catch (JSONException e) {
            observer.onRoomConnectFailed(e.getMessage());
        } catch (URISyntaxException e) {
            observer.onRoomConnectFailed(e.getMessage());
        }
    }

    void disconnect() {
        if (socketClient != null) {
            socketClient.on(Socket.EVENT_DISCONNECT, disconnectCallback);
            socketClient.disconnect();
        }
    }

    void sendMsg(String type, JSONObject msg, Ack ack) {
        if (!socketClient.connected()) {
            HashMap<String, Object> msg2cache = new HashMap<>();
            msg2cache.put("type", type);
            msg2cache.put("msg", msg);
            msg2cache.put("ack", ack);
            cache.add(msg2cache);
        } else {
            if (msg != null) {
                socketClient.emit(type, msg, ack);
            } else {
                socketClient.emit(type, ack);
            }
        }
    }

    private void login() throws JSONException {
        Log.d(LOG_TAG, "Logging in the conference room.");
        JSONObject loginInfo = new JSONObject();
        loginInfo.put("token", token);
        loginInfo.put("userAgent", new JSONObject(Const.userAgent));
        loginInfo.put("protocol", Const.PROTOCOL_VERSION);

        socketClient.emit("login", loginInfo,
                (Ack) (Object... args) -> callbackExecutor.execute(() -> {
                    if (extractMsg(0, args).equals("ok")) {
                        loggedIn = true;
                        try {
                            reconnectionTicket = ((JSONObject) args[1]).getString(
                                    "reconnectionTicket");
                        } catch (JSONException e) {
                            DCHECK(e);
                        }
                        observer.onRoomConnected((JSONObject) args[1]);
                        onRefreshReconnectionTicket();
                    } else {
                        observer.onRoomConnectFailed(extractMsg(1, args));
                    }

                }));
    }

    private void relogin() {
        DCHECK(reconnectionTicket);
        socketClient.emit("relogin", reconnectionTicket, (Ack) (Object... args) -> {
            if (extractMsg(0, args).equals("ok")) {
                reconnectionTicket = (String) args[1];
                reconnectAttempts = 0;
                flushCachedMsg();
                onRefreshReconnectionTicket();
            } else {
                triggerDisconnected();
            }
        });
    }

    private void flushCachedMsg() {
        for (HashMap<String, Object> msg : cache) {
            try {
                sendMsg((String) msg.get("type"), (JSONObject) msg.get("msg"),
                        (Ack) msg.get("ack"));
            } catch (Exception exception) {
                DCHECK(exception);
            }

        }
        cache.clear();
    }

    private void triggerDisconnected() {
        loggedIn = false;
        reconnectAttempts = 0;
        cache.clear();
        observer.onRoomDisconnected();
    }

    private String extractMsg(int position, Object... args) {
        if (position < 0 || args == null || args.length < position + 1
                || args[position] == null) {
            DCHECK(false);
            return "";
        }
        return args[position].toString();
    }

    private void onRefreshReconnectionTicket() {
        Log.d(LOG_TAG, "refresh connection ticket");
        socketClient.emit("refreshReconnectionTicket", null,
                (Object... args) -> callbackExecutor.execute(() -> {
                    if (extractMsg(0, args).equals("ok")) {
                        String message = args[1].toString();
                        onReconnectionTicket(message);
                    }
                }));
    }

    private void onReconnectionTicket(String ticket){
        try {
            reconnectionTicket = ticket;
            JSONObject jsonTicketTicket = new JSONObject(
                    new String(Base64.decode(ticket, Base64.DEFAULT)));

            String expiredStr = jsonTicketTicket.getString("notAfter");
            long delay = Long.parseLong(expiredStr) - System.currentTimeMillis();

            if (delay < 0){
                delay = 5*60*1000;
            }

            long finalDelay = delay;
            refreshExecutor.execute(()->{
                try {
                    Thread.sleep(finalDelay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                onRefreshReconnectionTicket();
            });


        } catch (Exception e) {
            Log.e(LOG_TAG, Log.getStackTraceString(e));
        }
    }
}

