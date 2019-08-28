/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.conference;

import static owt.base.CheckCondition.DCHECK;

import owt.base.ActionCallback;
import owt.base.OwtError;
import owt.base.MediaConstraints.TrackKind;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.MediaStream;
import org.webrtc.RTCStatsReport;

import java.util.ArrayList;
import java.util.List;

import io.socket.client.Ack;

/**
 * Publication handles the actions on a LocalStream published by a ConferenceClient.
 */
public final class Publication extends owt.base.Publication {

    private final ConferenceClient client;
    private List<PublicationObserver> observers;

    Publication(String id, ConferenceClient client) {
        super(id);
        this.client = client;
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
     * Stop sending the media track data to conference.
     *
     * @param trackKind TrackKind of the media to be stopped.
     * @param callback ActionCallback.onSuccess will be invoked when succeeds to mute. Otherwise
     * when fails to do so, ActionCallback.onFailure will be invoked with the
     * corresponding OwtError.
     */
    public void mute(final TrackKind trackKind, final ActionCallback<Void> callback) {
        if (ended) {
            client.triggerCallback(callback, new OwtError("Wrong state"));
            return;
        }
        Ack ack = args -> {
            if (args[0].equals("ok")) {
                if (callback != null) {
                    callback.onSuccess(null);
                }
            } else {
                client.triggerCallback(callback, new OwtError(args[1].toString()));
            }
        };

        try {
            client.sendSignalingMessage("stream-control",
                    generateMsg(trackKind, true),
                    ack);
        } catch (JSONException e) {
            callback.onFailure(new OwtError(e.getMessage()));
        }
    }

    /**
     * Start to send the media track data that has been stopped before to conference.
     *
     * @param trackKind TrackKind of the media to be started.
     * @param callback ActionCallback.onSuccess will be invoked when succeeds to unmute. Otherwise
     * when fails to do so, ActionCallback.onFailure will be invoked with the
     * corresponding OwtError.
     */
    public void unmute(final TrackKind trackKind, final ActionCallback<Void> callback) {
        if (ended) {
            client.triggerCallback(callback, new OwtError("Wrong state"));
            return;
        }
        Ack ack = args -> {
            if (args[0].equals("ok")) {
                if (callback != null) {
                    callback.onSuccess(null);
                }
            } else {
                client.triggerCallback(callback, new OwtError(args[1].toString()));
            }
        };

        try {
            client.sendSignalingMessage("stream-control",
                    generateMsg(trackKind, false),
                    ack);
        } catch (JSONException e) {
            callback.onFailure(new OwtError(e.getMessage()));
        }
    }

    private JSONObject generateMsg(TrackKind trackKind, boolean mute) throws JSONException {
        JSONObject msg = new JSONObject();
        msg.put("id", id);
        msg.put("operation", mute ? "pause" : "play");
        msg.put("data", trackKind.kind);
        return msg;
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
        if (ended) {
            client.triggerCallback(callback, new OwtError("Publication has stopped."));
            return;
        }
        client.getStats(id, callback);
    }

    /**
     * Stop publishing the LocalStream associated with the Publication to the conference.
     */
    @Override
    public void stop() {
        if (!ended) {
            client.unpublish(id, this);
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

    void onError(final OwtError error){
        if (!ended) {
            if (observers != null) {
                for (PublicationObserver observer : observers) {
                    observer.onError(error);
                }
            }
        }
    }

    ///@cond
    void onStatusUpdated(TrackKind trackKind, boolean active) {
        if (observers != null) {
            for (PublicationObserver observer : observers) {
                if (active) {
                    observer.onUnmute(trackKind);
                } else {
                    observer.onMute(trackKind);
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

        /**
         * Called upon an error occurred on the publication.
         */
        void onError(final OwtError error);

        /**
         * Called upon media track associated with the publication muted.
         *
         * @param trackKind TrackKind muted.
         */
        void onMute(TrackKind trackKind);

        /**
         * Called upon media track associated with the publication unmute.
         *
         * @param trackKind TrackKind unmute.
         */
        void onUnmute(TrackKind trackKind);
    }
    ///@endcond
}
