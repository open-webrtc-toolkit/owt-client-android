/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.p2p;

import owt.base.ActionCallback;

/**
 * Interface for signaling channel implementation that P2PClient relies on for sending and
 * receiving data. Member methods and SignalingChannelObserver are expected to be implemented and
 * triggered by app level.
 */
public interface SignalingChannelInterface {

    /**
     * Connect to signaling server. Since signaling channel can be customized, this method does not
     * define how a token should look like. Token will be passed into SignalingChannelInterface
     * implemented by the app without any changes.
     */
    void connect(String token, ActionCallback<String> callback);

    /**
     * Disconnect from the signaling server.
     */
    void disconnect();

    /**
     * Send a message through signaling channel.
     */
    void sendMessage(String peerId, String message, ActionCallback<Void> callback);

    /**
     * Add a SignalingChannelObserver.
     */
    void addObserver(SignalingChannelObserver observer);

    /**
     * Remove a SignalingChannelObserver.
     */
    void removeObserver(SignalingChannelObserver observer);

    /**
     * Interface for observing signaling channel events.
     */
    interface SignalingChannelObserver {
        /**
         * Called upon receiving a message.
         *
         * @param peerId id of message sender.
         * @param message message received.
         */
        void onMessage(String peerId, String message);

        /**
         * Called upon server disconnected.
         */
        void onServerDisconnected();
    }
}
