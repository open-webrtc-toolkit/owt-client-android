/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.test.conference.util;

import android.util.Log;

import owt.conference.ConferenceClient;
import owt.conference.Participant;
import owt.conference.RemoteStream;
import owt.test.util.Resultable;
import owt.test.util.TestObserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ConferenceClientObserver extends Resultable implements
        ConferenceClient.ConferenceClientObserver {
    private final static String TAG = "owt_test_conference";
    public final String name;
    public String lastSenderId = null;
    public String lastRecvMsg = null;
    public boolean disconnectedTriggered = false;
    public boolean userJoinedTriggered = false;
    public boolean streamAddedTriggered = false;
    public boolean msgRecvTriggered = false;
    public final List<RemoteStream> remoteStreams = new ArrayList<>();
    public final List<Participant> participants = new ArrayList<>();
    // key: RemoteStream.id
    public final HashMap<String, TestObserver> streamObservers = new HashMap<>();
    // key: Participant.userId
    public final HashMap<String, TestObserver> participantObservers = new HashMap<>();

    public ConferenceClientObserver(String name, int count) {
        super(count);
        this.name = name;
    }

    public void clearStatus(int count) {
        this.lastSenderId = null;
        this.lastRecvMsg = null;
        this.disconnectedTriggered = false;
        this.userJoinedTriggered = false;
        this.streamAddedTriggered = false;
        this.msgRecvTriggered = false;
        reinitLatch(count);
    }

    @Override
    public void onStreamAdded(RemoteStream remoteStream) {
        Log.d(TAG, "streamAddedTriggered.");
        streamAddedTriggered = true;
        TestObserver streamObserver = new TestObserver();
        remoteStream.addObserver(streamObserver);
        remoteStreams.add(remoteStream);
        streamObservers.put(remoteStream.id(), streamObserver);
        onResult();
    }

    @Override
    public void onParticipantJoined(Participant participant) {
        Log.d(TAG, "onParticipantJoined.");
        userJoinedTriggered = true;
        TestObserver participantObserver = new TestObserver(this.name, participant.id);
        participant.addObserver(participantObserver);
        participants.add(participant);
        participantObservers.put(participant.id, participantObserver);
        onResult();
    }

    @Override
    public void onMessageReceived(String message, String from, String to) {
        Log.d(TAG, "onMessageReceived.");
        msgRecvTriggered = true;
        lastRecvMsg = message;
        lastSenderId = from;
        onResult();
    }

    @Override
    public void onServerDisconnected() {
        Log.d(TAG, "onServerDisconnected.");
        disconnectedTriggered = true;
        onResult();
    }

    public boolean getResultForJoin(int timeout) {
        return getResult(timeout) && userJoinedTriggered;
    }

    public boolean getResultForPublish(int timeout) {
        return getResult(timeout) && streamAddedTriggered;
    }

    public boolean getResultForSend(int timeout) {
        return getResult(timeout) && msgRecvTriggered;
    }

    public boolean getResultForLeave(int timeout) {
        return getResult(timeout) && disconnectedTriggered;
    }
}
