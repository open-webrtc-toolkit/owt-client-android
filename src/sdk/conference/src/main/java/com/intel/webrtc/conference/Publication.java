/*
 * Intel License Header Holder
 */
package com.intel.webrtc.conference;

import com.intel.webrtc.base.ActionCallback;
import com.intel.webrtc.base.IcsError;
import com.intel.webrtc.base.MediaConstraints.TrackKind;

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
public final class Publication extends com.intel.webrtc.base.Publication
        implements MuteEventObserver {

    /**
     * Interface for observing publication events.
     */
    public interface PublicationObserver {
        /**
         * Called upon Publication ended.
         */
        void onEnded();

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


    private final ConferenceClient client;
    private List<PublicationObserver> observers;

    Publication(String id, MediaStream mediaStream, ConferenceClient client) {
        super(id, mediaStream);
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
     * @param callback  ActionCallback.onSuccess will be invoked when succeeds to mute. Otherwise
     *                  when fails to do so, ActionCallback.onFailure will be invoked with the
     *                  corresponding IcsError.
     */
    public void mute(final TrackKind trackKind, final ActionCallback<Void> callback) {
        if (ended) {
            client.triggerCallback(callback, new IcsError("Wrong state"));
            return;
        }
        Ack ack = new Ack() {
            @Override
            public void call(Object... args) {
                if (args[0].equals("ok")) {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                } else {
                    client.triggerCallback(callback, new IcsError(args[1].toString()));
                }
            }
        };

        try {
            client.sendSignalingMessage("stream-control",
                                        generateMsg(trackKind, true),
                                        ack);
        } catch (JSONException e) {
            callback.onFailure(new IcsError(e.getMessage()));
        }
    }

    /**
     * Start to send the media track data that has been stopped before to conference.
     *
     * @param trackKind TrackKind of the media to be started.
     * @param callback  ActionCallback.onSuccess will be invoked when succeeds to unmute. Otherwise
     *                  when fails to do so, ActionCallback.onFailure will be invoked with the
     *                  corresponding IcsError.
     */
    public void unmute(final TrackKind trackKind, final ActionCallback<Void> callback) {
        if (ended) {
            client.triggerCallback(callback, new IcsError("Wrong state"));
            return;
        }
        Ack ack = new Ack() {
            @Override
            public void call(Object... args) {
                if (args[0].equals("ok")) {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                } else {
                    client.triggerCallback(callback, new IcsError(args[1].toString()));
                }
            }
        };

        try {
            client.sendSignalingMessage("stream-control",
                                        generateMsg(trackKind, false),
                                        ack);
        } catch (JSONException e) {
            callback.onFailure(new IcsError(e.getMessage()));
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
     *                 to get the stats. Otherwise when fails to do so, ActionCallback.onFailure
     *                 will be invoked with the corresponding IcsError.
     */
    @Override
    public void getStats(final ActionCallback<RTCStatsReport> callback) {
        if (ended) {
            client.triggerCallback(callback, new IcsError("Publication has stopped."));
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
        ended = true;
        if (observers != null) {
            for (PublicationObserver observer : observers) {
                observer.onEnded();
            }
        }
    }

    ///@cond
    @Override
    public void onStatusUpdated(TrackKind trackKind, boolean active) {
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
    ///@endcond
}
