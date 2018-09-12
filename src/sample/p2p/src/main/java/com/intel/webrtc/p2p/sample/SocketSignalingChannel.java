/*
 * Copyright © 2017 Intel Corporation. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.intel.webrtc.p2p.sample;

import static com.intel.webrtc.p2p.IcsP2PError.P2P_CLIENT_ILLEGAL_ARGUMENT;
import static com.intel.webrtc.p2p.IcsP2PError.P2P_CONN_SERVER_UNKNOWN;

import android.util.Log;

import com.intel.webrtc.base.ActionCallback;
import com.intel.webrtc.base.IcsConst;
import com.intel.webrtc.base.IcsError;
import com.intel.webrtc.p2p.IcsP2PError;
import com.intel.webrtc.p2p.SignalingChannelInterface;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter.Listener;

/**
 * Socket.IO implementation of P2P signaling channel.
 */
public class SocketSignalingChannel implements SignalingChannelInterface {
    private static final String TAG = "ICS-SocketClient";
    private final String CLIENT_CHAT_TYPE = "ics-message";
    private final String SERVER_AUTHENTICATED = "server-authenticated";
    private final String FORCE_DISCONNECT = "server-disconnect";
    private final String CLIENT_TYPE = "&clientType=";
    private final String CLIENT_TYPE_VALUE = "Android";
    private final String CLIENT_VERSION = "&clientVersion=";
    private final String CLIENT_VERSION_VALUE = IcsConst.CLIENT_VERSION;

    private final int MAX_RECONNECT_ATTEMPTS = 5;
    private int reconnectAttempts = 0;
    private Socket socketIOClient;
    private List<SignalingChannelObserver> signalingChannelObservers;
    private ActionCallback<String> connectCallback;

    // Socket.IO events.
    private Listener onConnectErrorCallback = args -> {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            if (connectCallback != null) {
                connectCallback.onFailure(
                        new IcsError(P2P_CONN_SERVER_UNKNOWN.value, "connect failed"));
                connectCallback = null;
            } else {
                for (SignalingChannelObserver observer : signalingChannelObservers) {
                    observer.onServerDisconnected();
                }
            }
            reconnectAttempts = 0;
        }
    };

    private Listener onErrorCallback = args -> {
        if (connectCallback != null) {
            Pattern pattern = Pattern.compile("[0-9]*");
            if (pattern.matcher(args[0].toString()).matches()) {
                connectCallback.onFailure(
                        new IcsError(IcsP2PError.get(Integer.parseInt((String) args[0])).value,
                                "Server error"));
            } else {
                connectCallback.onFailure(new IcsError(args[0].toString()));
            }
        }
    };

    private Listener onReconnectingCallback = args -> {
        reconnectAttempts++;
    };

    private Listener onDisconnectCallback = args -> {
        for (SignalingChannelObserver observer : signalingChannelObservers) {
            observer.onServerDisconnected();
        }
    };

    // P2P server events.
    private Listener onServerAuthenticatedCallback = args -> {
        if (connectCallback != null) {
            connectCallback.onSuccess(args[0].toString());
            connectCallback = null;
        }
    };

    private Listener onForceDisconnectCallback = args -> {
        if (socketIOClient != null) {
            socketIOClient.on(Socket.EVENT_DISCONNECT, onDisconnectCallback);
            socketIOClient.io().reconnection(false);
        }
    };

    private Listener onMessageCallback = args -> {
        JSONObject argumentJsonObject = (JSONObject) args[0];
        for (SignalingChannelObserver observer : signalingChannelObservers) {
            try {
                observer.onMessage(argumentJsonObject.getString("from"),
                        argumentJsonObject.getString("data"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    SocketSignalingChannel() {
        this.signalingChannelObservers = new ArrayList<>();
    }

    @Override
    public void addObserver(SignalingChannelObserver observer) {
        this.signalingChannelObservers.add(observer);
    }

    @Override
    public void removeObserver(SignalingChannelObserver observer) {
        this.signalingChannelObservers.remove(observer);
    }

    @Override
    public void connect(String userInfo, ActionCallback<String> callback) {
        JSONObject loginObject;
        String token;
        String url;
        try {
            connectCallback = callback;
            loginObject = new JSONObject(userInfo);
            token = URLEncoder.encode(loginObject.getString("token"), "UTF-8");
            url = loginObject.getString("host");
            url += "?token=" + token + CLIENT_TYPE + CLIENT_TYPE_VALUE + CLIENT_VERSION
                    + CLIENT_VERSION_VALUE;
            if (!isValid(url)) {
                callback.onFailure(new IcsError(P2P_CLIENT_ILLEGAL_ARGUMENT.value, "Invalid URL"));
                return;
            }
            IO.Options opt = new IO.Options();
            opt.forceNew = true;
            opt.reconnection = true;
            opt.reconnectionAttempts = MAX_RECONNECT_ATTEMPTS;
            if (socketIOClient != null) {
                Log.d(TAG, "stop reconnecting the former url");
                socketIOClient.disconnect();
            }
            socketIOClient = IO.socket(url, opt);

            socketIOClient.on(Socket.EVENT_CONNECT_ERROR, onConnectErrorCallback)
                    .on(Socket.EVENT_ERROR, onErrorCallback)
                    .on(Socket.EVENT_RECONNECTING, onReconnectingCallback)
                    .on(CLIENT_CHAT_TYPE, onMessageCallback)
                    .on(SERVER_AUTHENTICATED, onServerAuthenticatedCallback)
                    .on(FORCE_DISCONNECT, onForceDisconnectCallback);

            socketIOClient.connect();

        } catch (JSONException e) {
            if (callback != null) {
                callback.onFailure(new IcsError(P2P_CLIENT_ILLEGAL_ARGUMENT.value, e.getMessage()));
            }
        } catch (URISyntaxException e) {
            if (callback != null) {
                callback.onFailure(new IcsError(P2P_CLIENT_ILLEGAL_ARGUMENT.value, e.getMessage()));
            }
        } catch (UnsupportedEncodingException e) {
            if (callback != null) {
                callback.onFailure(new IcsError(P2P_CLIENT_ILLEGAL_ARGUMENT.value, e.getMessage()));
            }
        }
    }

    private boolean isValid(String urlString) {
        try {
            URL url = new URL(urlString);
            return url.getPort() <= 65535;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    @Override
    public void disconnect() {
        if (socketIOClient != null) {
            Log.d(TAG, "Socket IO Disconnect.");
            socketIOClient.on(Socket.EVENT_DISCONNECT, onDisconnectCallback);
            socketIOClient.disconnect();
            socketIOClient = null;
        }
    }

    @Override
    public void sendMessage(String peerId, String message, final ActionCallback<Void> callback) {
        if (socketIOClient == null) {
            Log.d(TAG, "socketIOClient is not established.");
            return;
        }
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("to", peerId);
            jsonObject.put("data", message);
            socketIOClient.emit(CLIENT_CHAT_TYPE, jsonObject, (Ack) args -> {
                if (args == null || args.length != 0) {
                    if (callback != null) {
                        callback.onFailure(new IcsError("Failed to send message."));
                    }
                } else {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
