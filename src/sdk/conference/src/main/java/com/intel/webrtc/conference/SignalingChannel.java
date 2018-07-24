/*
 * Intel License Header Holder
 */
package com.intel.webrtc.conference;

import static com.intel.webrtc.base.CheckCondition.DCHECK;

import android.util.Base64;

import com.intel.webrtc.base.IcsConst;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter.Listener;

final class SignalingChannel {

    private final SignalingChannelObserver observer;
    //Base64 encoded token.
    private final String token;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final int MAX_RECONNECT_ATTEMPTS = 5;
    private final Object timerLock = new Object();
    private final Listener progressCallback = new Listener() {
        @Override
        public void call(final Object... args) {
            executor.execute(() -> {
                JSONObject msg = (JSONObject) args[0];
                observer.onProgressMessage(msg);
            });
        }
    };
    private final Listener participantCallback = new Listener() {
        @Override
        public void call(final Object... args) {
            executor.execute(() -> {
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
                    DCHECK(false);
                }
            });
        }
    };
    private final Listener streamCallback = new Listener() {
        @Override
        public void call(final Object... args) {
            executor.execute(() -> {
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
        }
    };
    private final Listener textCallback = new Listener() {
        @Override
        public void call(final Object... args) {
            executor.execute(() -> {
                JSONObject data = (JSONObject) args[0];
                try {
                    observer.onTextMessage(data.getString("from"), data.getString("message"));
                } catch (JSONException e) {
                    DCHECK(false);
                }
            });
        }
    };
    private final Listener dropCallback = args -> {
        //TODO
    };
    private Socket socketClient;
    private final Listener connectedCallback = new Listener() {
        @Override
        public void call(Object... args) {
            try {
                login();
            } catch (JSONException e) {
                observer.onRoomConnectFailed(e.getMessage());
            }

        }
    };
    private String reconnectionTicket;
    private int reconnectAttempts = 0;
    private Timer refreshTimer;
    private boolean loggedIn = false;
    private final Listener connectErrorCallback = new Listener() {
        @Override
        public void call(final Object... args) {
            executor.execute(() -> {
                String msg = extractMsg(0, args);
                if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
                    observer.onRoomConnectFailed("Socket.IO connected failed: " + msg);
                    if (loggedIn) {
                        triggerDisconnected();
                    }
                }
            });

        }
    };
    private final Listener reconnectingCallback = new Listener() {
        @Override
        public void call(Object... args) {
            executor.execute(() -> {
                reconnectAttempts++;
                //trigger onReconnecting, ONLY when already logged in and first time to reconnect
                if (loggedIn && reconnectAttempts == 1) {
                    observer.onReconnecting();
                }
            });
        }
    };
    private final Listener disconnectCallback = args -> triggerDisconnected();

    SignalingChannel(String token, SignalingChannelObserver observer) {
        this.token = token;
        this.observer = observer;
    }

    void connect(final ConferenceClientConfiguration configuration) {
        DCHECK(executor);
        executor.execute(() -> {
            try {
                DCHECK(token);
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
                if (configuration.sslContext != null) {
                    opt.sslContext = configuration.sslContext;
                }
                if (configuration.hostnameVerifier != null) {
                    opt.hostnameVerifier = configuration.hostnameVerifier;
                }

                socketClient = IO.socket(url, opt);

                socketClient.on(Socket.EVENT_CONNECT, connectedCallback)
                        .on(Socket.EVENT_CONNECT_ERROR, connectErrorCallback)
                        .on(Socket.EVENT_RECONNECTING, reconnectingCallback)
                        .on(Socket.EVENT_DISCONNECT, disconnectCallback)
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
        });
    }

    private void login() throws JSONException {
        JSONObject loginInfo = new JSONObject();
        loginInfo.put("token", token);
        loginInfo.put("userAgent", new JSONObject(IcsConst.userAgent));
        loginInfo.put("protocol", IcsConst.PROTOCOL_VERSION);

        socketClient.emit("login", loginInfo, (Ack) args -> executor.execute(() -> {
            if (extractMsg(0, args).equals("ok")) {
                observer.onRoomConnected((JSONObject) args[1]);
            } else {
                observer.onRoomConnectFailed(extractMsg(1, args));
            }

        }));
    }

    void disconnect() {
        if (socketClient != null) {
            socketClient.disconnect();
        }
    }

    private void triggerDisconnected() {
        loggedIn = false;
        reconnectAttempts = 0;
        synchronized (timerLock) {
            if (refreshTimer != null) {
                refreshTimer.cancel();
                refreshTimer = null;
            }
        }
        observer.onRoomDisconnected();
    }

    void sendMsg(String name, JSONObject args, Ack acknowledge) {
        if (args != null) {
            socketClient.emit(name, args, acknowledge);
        } else {
            socketClient.emit(name, acknowledge);
        }
    }

    private String extractMsg(int position, Object... args) {
        if (position < 0 || args == null || args.length < position + 1
                || args[position] == null) {
            DCHECK(false);
            return "";
        }
        return args[position].toString();
    }

    interface SignalingChannelObserver {

        void onRoomConnected(JSONObject info);

        void onRoomConnectFailed(String errorMsg);

        void onReconnecting();

        void onRoomDisconnected();

        void onProgressMessage(JSONObject message);

        void onTextMessage(String participantId, String message);

        void onStreamAdded(RemoteStream remoteStream);

        void onStreamRemoved(String streamId);

        void onStreamUpdated(String id, JSONObject updateInfo);

        void onParticipantJoined(JSONObject participantInfo);

        void onParticipantLeft(String participantId);
    }

}

