/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.p2p;

import owt.base.ActionCallback;
import owt.base.OwtError;

import org.webrtc.MediaStream;
import org.webrtc.RTCStatsReport;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Publication handles the actions on a LocalStream published by a P2PClient.
 */
public final class Publication extends owt.base.Publication {

    private final P2PPeerConnectionChannel pcChannel;
    private List<PublicationObserver> observers;

    Publication(String mediaStreamId, P2PPeerConnectionChannel pcChannel) {
        super(UUID.randomUUID().toString(), mediaStreamId);
        this.pcChannel = pcChannel;
    }

    /**
     * Add a PublicationObserver.
     *
     * @param observer PublicationObserver to be added.
     */
    public void addObserver(PublicationObserver observer) {
        if (observers == null) {
            observers = new ArrayList<>();
        }
        observers.add(observer);
    }

    /**
     * Remove a PublicationObserver.
     *
     * @param observer PublicationObserver to be removed.
     */
    public void removeObserver(PublicationObserver observer) {
        if (observers != null) {
            observers.remove(observer);
        }
    }

    /**
     * Get the PeerConnection stats.
     *
     * @param callback ActionCallback.onSuccess will be invoked with RTCStatsReport when succeeds
     * to get the stats. Otherwise when fails to do so, ActionCallback.onFailure
     * will be invoked with the corresponding OwtError.
     */
    @Override
    public void getStats(final ActionCallback<RTCStatsReport> callback) {
        if (!pcChannel.disposed()) {
            pcChannel.getConnectionStats(callback);
        } else {
            if (callback != null) {
                callback.onFailure(new OwtError(OwtP2PError.P2P_CLIENT_INVALID_STATE.value, "Wrong state"));
            }
        }
    }

    /**
     * Stop publishing the LocalStream associated with the Publication to the remote P2PClient.
     */
    @Override
    public void stop() {
        if (!ended) {
            pcChannel.unpublish(mediaStreamId);
            ended = true;
            if (observers != null) {
                for (PublicationObserver observer : observers) {
                    observer.onEnded();
                }
            }
        }
    }

    void onEnded() {
        if (!ended) {
            ended = true;
            if (observers != null) {
                for (PublicationObserver observer : observers) {
                    observer.onEnded();
                }
            }
        }
    }

    /**
     * Interface for observing publication events.
     */
    public interface PublicationObserver {
        /**
         * Called upon Publication ended.
         */
        void onEnded();
    }
}
