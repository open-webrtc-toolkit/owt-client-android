/*
 * Intel License Header Holder
 */
package com.intel.webrtc.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * RemoteStream is a Stream that sent to the current client from remote client.
 */
public abstract class RemoteStream extends Stream {

    private final String id;
    private final String origin;
    ///@cond
    protected List<StreamObserver> observers;
    ///@endcond
    private boolean ended = false;

    ///@cond
    protected RemoteStream(String id, String origin) {
        this.id = id;
        this.origin = origin;
    }

    /**
     * Id of the RemoteStream.
     *
     * @return id of RemoteStream.
     */
    @Override
    public String id() {
        return id;
    }
    ///@endcond

    /**
     * Id of the remote client that published this stream.
     *
     * @return id of remote client.
     */
    public String origin() {
        return origin;
    }

    /**
     * Add a StreamObserver.
     *
     * @param observer StreamObserver to be added.
     */
    public void addObserver(StreamObserver observer) {
        if (observers == null) {
            observers = Collections.synchronizedList(new ArrayList<StreamObserver>());
        }
        observers.add(observer);
    }

    /**
     * Remove a StreamObserver.
     *
     * @param observer StreamObserver to be removed.
     */
    public void removeObserver(StreamObserver observer) {
        if (observers != null) {
            observers.remove(observer);
        }
    }

    ///@cond
    protected void triggerEndedEvent() {
        ended = true;
        if (observers != null) {
            for (StreamObserver observer : observers) {
                observer.onStreamEnded();
            }
        }
    }

    /**
     * Interface for observing stream events.
     */
    public interface StreamObserver {
        /**
         * Called upon stream ended.
         */
        void onStreamEnded();
    }
    ///@endcond
}
