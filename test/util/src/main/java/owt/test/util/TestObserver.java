/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.test.util;

import android.util.Log;

import owt.conference.Participant;
import owt.p2p.Publication;
import owt.base.RemoteStream;

/**
 * A general observer which only listens on one single event.
 */
public class TestObserver extends Resultable implements RemoteStream.StreamObserver,
        Publication.PublicationObserver, Participant.ParticipantObserver {
    private final static String TAG = "owt_test_util";
    public final String id;
    public final String name;

    public TestObserver() {
        this("N/A", "N/A");
    }

    public TestObserver(String name, String id) {
        super(1);
        this.id = id;
        this.name = name;
    }

    public boolean getResult(int timeout) {
        return super.getResult(timeout);
    }

    @Override
    public void onUpdated() {
        Log.d(TAG, "StreamObserver.onUpdated: " + id + "@" + name);
        // TODO: not yet implemented.
    }

    @Override
    public void onEnded() {
        Log.d(TAG, "PublicationObserver/StreamObserver.onEnded: " + id + "@" + name);
        onResult();
    }

    @Override
    public void onLeft() {
        Log.d(TAG, "ParticipantObserver.onLeft: " + id + "@" + name);
        onResult();
    }
}
