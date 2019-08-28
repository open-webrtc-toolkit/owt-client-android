/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.conference;

import static owt.base.CheckCondition.RCHECK;

import owt.base.ActionCallback;
import owt.base.OwtError;
import owt.base.MediaConstraints.TrackKind;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.RTCStatsReport;

import java.util.ArrayList;
import java.util.List;

import io.socket.client.Ack;

/**
 * Subscription handles the actions on a RemoteStream subscribed by a ConferenceClient.
 */

public final class Subscription {

    /**
     * Id of the Subscription
     */
    public final String id;
    private final ConferenceClient client;
    private List<SubscriptionObserver> observers;
    private boolean ended = false;

    Subscription(String id, ConferenceClient client) {
        this.id = id;
        this.client = client;
    }

    /**
     * Stop receiving the media track data to conference.
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
                onStatusUpdated(trackKind, false);
                if (callback != null) {
                    callback.onSuccess(null);
                }
            } else {
                client.triggerCallback(callback, new OwtError(args[1].toString()));
            }
        };

        try {
            client.sendSignalingMessage("subscription-control",
                    generateMsg(trackKind, true),
                    ack);
        } catch (JSONException e) {
            callback.onFailure(new OwtError(e.getMessage()));
        }
    }

    /**
     * Start to receive the media track data that has been stopped before to conference.
     *
     * @param trackKind TrackKind of the media to be started.
     * @param callback ActionCallback.onSuccess will be invoked when succeeds to unmute. Otherwise
     * when fails to do so, ActionCallback.onFailure will be invoked with the
     * corresponding OwtError.
     */
    public void unmute(final TrackKind trackKind, final ActionCallback<Void> callback) {
        if (ended) {
            client.triggerCallback(callback, new OwtError(0, "Wrong state"));
            return;
        }
        Ack ack = args -> {
            if (args[0].equals("ok")) {
                onStatusUpdated(trackKind, true);
                if (callback != null) {
                    callback.onSuccess(null);
                }
            } else {
                client.triggerCallback(callback, new OwtError(args[1].toString()));
            }
        };

        try {
            client.sendSignalingMessage("subscription-control",
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
     * Update the media track parameters of the Subscription. Currently only updating for video
     * tracks is supported.
     *
     * @param updateOptions UpdateOptions
     * @param callback ActionCallback.onSuccess will be invoked when succeeds to get the
     * update. Otherwise when fails to do so, ActionCallback
     * .onFailure will be invoked with the corresponding OwtError.
     */
    public void applyOptions(VideoUpdateOptions updateOptions,
            final ActionCallback<Void> callback) {
        if (ended) {
            client.triggerCallback(callback, new OwtError(0, "Wrong state"));
            return;
        }
        RCHECK(updateOptions);
        JSONObject msg = new JSONObject();
        try {
            msg.put("id", id);
            msg.put("operation", "update");
            msg.put("data", updateOptions.generateOptionMsg());
        } catch (JSONException e) {
            client.triggerCallback(callback, new OwtError(e.getMessage()));
        }

        client.sendSignalingMessage("subscription-control", msg, args -> {
            if (args[0].equals("ok")) {
                if (callback != null) {
                    callback.onSuccess(null);
                }
            } else {
                client.triggerCallback(callback, new OwtError(args[1].toString()));
            }
        });
    }

    /**
     * Get the PeerConnection stats.
     *
     * @param callback ActionCallback.onSuccess will be invoked with RTCStatsReport when succeeds
     * to get the stats. Otherwise when fails to do so, ActionCallback.onFailure
     * will be invoked with the corresponding OwtError.
     */
    public void getStats(ActionCallback<RTCStatsReport> callback) {
        if (ended) {
            client.triggerCallback(callback, new OwtError("Wrong state"));
            return;
        }
        client.getStats(id, callback);
    }

    /**
     * Stop subscribing the RemoteStream associated with the Subscription from the conference.
     */
    public void stop() {
        if (!ended) {
            client.unsubscribe(id, this);
        }
    }

    /**
     * Add a SubscriptionObserver.
     *
     * @param observer SubscriptionObserver to be added.
     */
    public void addObserver(SubscriptionObserver observer) {
        if (observers == null) {
            observers = new ArrayList<>();
        }
        observers.add(observer);
    }

    /**
     * Removed a SubscriptionObserver.
     *
     * @param observer SubscriptionObserver to be removed.
     */
    public void removeObserver(SubscriptionObserver observer) {
        if (observers != null) {
            observers.remove(observer);
        }
    }

    void onEnded() {
        if (!ended) {
            ended = true;
            if (observers != null) {
                for (SubscriptionObserver observer : observers) {
                    observer.onEnded();
                }
            }
        }
    }

    void onError(final OwtError error){
        if (!ended) {
            if (observers != null) {
                for (SubscriptionObserver observer : observers) {
                    observer.onError(error);
                }
            }
        }
    }

    ///@cond
    void onStatusUpdated(TrackKind trackKind, boolean active) {
        if (observers != null) {
            for (SubscriptionObserver observer : observers) {
                if (active) {
                    observer.onUnmute(trackKind);
                } else {
                    observer.onMute(trackKind);
                }
            }
        }
    }

    /**
     * Interface for observing the subscription events.
     */
    public interface SubscriptionObserver {
        /**
         * Called upon Subscription ended.
         */
        void onEnded();

        /**
         * Called upon an error occurred on the subscription.
         */
        void onError(final OwtError error);

        /**
         * Called upon media track associated with the subscription muted.
         *
         * @param trackKind TrackKind muted.
         */
        void onMute(TrackKind trackKind);

        /**
         * Called upon media track associated with the subscription unmute.
         *
         * @param trackKind TrackKind unmute.
         */
        void onUnmute(TrackKind trackKind);
    }

    /**
     * Options for updating the video track parameters for current Subscription.
     */
    public static final class VideoUpdateOptions {
        public int resolutionHeight = 0, resolutionWidth = 0, fps = 0, keyframeInterval = 0;
        public double bitrateMultiplier = 0;

        JSONObject generateOptionMsg() throws JSONException {
            JSONObject optionMsg = new JSONObject();
            JSONObject video = new JSONObject();
            JSONObject parameters = new JSONObject();
            if (resolutionWidth != 0 && resolutionHeight != 0) {
                JSONObject reso = new JSONObject();
                reso.put("width", resolutionWidth);
                reso.put("height", resolutionHeight);
                parameters.put("resolution", reso);
            }
            if (fps != 0) {
                parameters.put("framerate", fps);
            }
            if (keyframeInterval != 0) {
                parameters.put("keyFrameInterval", keyframeInterval);
            }
            if (bitrateMultiplier != 0) {
                parameters.put("bitrate", "x" + bitrateMultiplier);
            }

            video.put("parameters", parameters);
            optionMsg.put("video", video);
            return optionMsg;
        }
    }
    ///@endcond
}
