/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.test.p2p.util;

import android.util.Log;

import owt.p2p.P2PClient;
import owt.p2p.RemoteStream;
import owt.test.util.Resultable;
import owt.test.util.TestObserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class P2PClientObserver extends Resultable implements P2PClient.P2PClientObserver {
    private final static String TAG = "owt_test_p2p";
    public final String name;
    public final List<String> dataReceived = new ArrayList<>();
    public final List<String> dataSenders = new ArrayList<>();
    public final List<RemoteStream> remoteStreams = new ArrayList<>();
    // key: RemoteStream
    public final HashMap<RemoteStream, TestObserver> remoteStreamObservers = new HashMap<>();
    private boolean dataRecvTriggered = false;
    private boolean disconnectedTriggered = false;
    private boolean streamAddedTriggered = false;

    public P2PClientObserver(String name) {
        this(name, 1);
    }

    public P2PClientObserver(String name, int count) {
        super(count);
        this.name = name;
    }

    /**
     * This will only clear the flags and counts, objects will not be cleared.
     */
    public void clearStatus(int count) {
        dataRecvTriggered = false;
        disconnectedTriggered = false;
        streamAddedTriggered = false;
        reinitLatch(count);
    }

    @Override
    public void onDataReceived(String peerId, String msg) {
        Log.d(TAG, "onDataReceived from " + peerId);
        dataRecvTriggered = true;
        dataReceived.add(msg);
        dataSenders.add(peerId);
        onResult();
    }

    @Override
    public void onServerDisconnected() {
        Log.d(TAG, "onServerDisconnected.");
        disconnectedTriggered = true;
        onResult();
    }

    @Override
    public void onStreamAdded(RemoteStream remoteStream) {
        Log.d(TAG, "onStreamAdded.");
        streamAddedTriggered = true;
        remoteStreams.add(remoteStream);
        TestObserver streamObserver = new TestObserver();
        remoteStream.addObserver(streamObserver);
        remoteStreamObservers.put(remoteStream, streamObserver);
        onResult();
    }

    // Remember to call clearStatus when calling below methods multiple times.
    public boolean getResultForDataReceived(int timeout) {
        return getResult(timeout) && dataRecvTriggered;
    }

    public boolean getResultForServerDisconnected(int timeout) {
        return getResult(timeout) && disconnectedTriggered;
    }

    public boolean getResultForStreamAdded(int timeout) {
        return getResult(timeout) && streamAddedTriggered;
    }
}
