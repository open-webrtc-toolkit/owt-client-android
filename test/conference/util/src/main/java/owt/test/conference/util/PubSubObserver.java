/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.test.conference.util;

import static owt.base.MediaConstraints.TrackKind.AUDIO;
import static owt.base.MediaConstraints.TrackKind.AUDIO_AND_VIDEO;
import static owt.base.MediaConstraints.TrackKind.VIDEO;

import android.util.Log;

import owt.base.MediaConstraints;
import owt.conference.Publication;
import owt.conference.Subscription;
import owt.test.util.Resultable;
import owt.base.OwtError;

public class PubSubObserver extends Resultable implements Publication.PublicationObserver,
        Subscription.SubscriptionObserver {
    private final static String TAG = "owt_test_conference";
    private boolean onEndedTriggered = false;
    private boolean onMuteVideoTriggered = false;
    private boolean onMuteAudioTriggered = false;
    private boolean onUnMuteVideoTriggered = false;
    private boolean onUnMuteAudioTriggered = false;

    public PubSubObserver(int count) {
        super(count);
    }

    public void clearStatus(int count) {
        this.onEndedTriggered = false;
        this.onMuteVideoTriggered = false;
        this.onMuteAudioTriggered = false;
        this.onUnMuteVideoTriggered = false;
        this.onUnMuteAudioTriggered = false;
        reinitLatch(count);
    }

    @Override
    public void onEnded() {
        Log.d(TAG, "onEnded.");
        onEndedTriggered = true;
        onResult();
    }

    @Override
    public void onMute(MediaConstraints.TrackKind trackKind) {
        Log.d(TAG, "onMute.");
        if (trackKind == VIDEO || trackKind == AUDIO_AND_VIDEO) {
            onMuteVideoTriggered = true;
        }
        if (trackKind == AUDIO || trackKind == AUDIO_AND_VIDEO) {
            onMuteAudioTriggered = true;
        }
        onResult();
    }

    @Override
    public void onUnmute(MediaConstraints.TrackKind trackKind) {
        Log.d(TAG, "onUnmute.");
        if (trackKind == VIDEO || trackKind == AUDIO_AND_VIDEO) {
            onUnMuteVideoTriggered = true;
        }
        if (trackKind == AUDIO || trackKind == AUDIO_AND_VIDEO) {
            onUnMuteAudioTriggered = true;
        }
        onResult();
    }

    @Override
    public void onError(OwtError error) {
        Log.d(TAG, "onError.");
    }

    public boolean getResultForMute(MediaConstraints.TrackKind trackKind, int timeout) {
        boolean audio = trackKind == AUDIO || trackKind == AUDIO_AND_VIDEO;
        boolean video = trackKind == VIDEO || trackKind == AUDIO_AND_VIDEO;
        return getResult(timeout) && (!audio || onMuteAudioTriggered)
                && (!video || onMuteVideoTriggered);
    }

    public boolean getResultForUnmute(MediaConstraints.TrackKind trackKind, int timeout) {
        boolean audio = trackKind == AUDIO || trackKind == AUDIO_AND_VIDEO;
        boolean video = trackKind == VIDEO || trackKind == AUDIO_AND_VIDEO;
        return getResult(timeout) && (!audio || onUnMuteAudioTriggered)
                && (!video || onUnMuteVideoTriggered);
    }

    public boolean getResultForEnded(int timeout) {
        return getResult(timeout) && onEndedTriggered;
    }
}
