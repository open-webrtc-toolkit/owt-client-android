package oms.test.util;

import android.util.Log;

import oms.conference.Participant;
import oms.p2p.Publication;
import oms.base.RemoteStream;

/**
 * A general observer which only listens on one single event.
 */
public class TestObserver extends Resultable implements RemoteStream.StreamObserver,
        Publication.PublicationObserver, Participant.ParticipantObserver {
    private final static String TAG = "ics_test_util";
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
