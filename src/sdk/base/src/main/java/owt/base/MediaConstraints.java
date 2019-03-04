/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.base;

public abstract class MediaConstraints {

    public enum TrackKind {
        AUDIO("audio"),
        VIDEO("video"),
        AUDIO_AND_VIDEO("av");

        ///@cond
        public final String kind;
        ///@endcond

        TrackKind(String kind) {
            this.kind = kind;
        }
    }

    public final static class AudioTrackConstraints {
        public boolean echoCancellation = false;
        public boolean extendedFilterEchoCancellation = false;
        public boolean delayAgnosticEchoCancellation = false;

        org.webrtc.MediaConstraints convertToWebRTCConstraints() {
            org.webrtc.MediaConstraints result = new org.webrtc.MediaConstraints();
            if (echoCancellation) {
                result.mandatory.add(
                        new org.webrtc.MediaConstraints.KeyValuePair("googEchoCancellation",
                                "true"));
            }
            if (extendedFilterEchoCancellation) {
                result.mandatory.add(
                        new org.webrtc.MediaConstraints.KeyValuePair("googEchoCancellation2",
                                "true"));
            }
            if (delayAgnosticEchoCancellation) {
                result.mandatory.add(
                        new org.webrtc.MediaConstraints.KeyValuePair("googDAEchoCancellation",
                                "true"));
            }
            return result;
        }
    }
}
