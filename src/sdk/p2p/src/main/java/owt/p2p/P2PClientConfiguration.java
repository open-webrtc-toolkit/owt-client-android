/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.p2p;

import static owt.base.CheckCondition.RCHECK;

import owt.base.AudioEncodingParameters;
import owt.base.ClientConfiguration;
import owt.base.VideoEncodingParameters;

import org.webrtc.PeerConnection;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for P2PClient.
 */
public final class P2PClientConfiguration extends ClientConfiguration {

    final List<VideoEncodingParameters> videoEncodings;
    final List<AudioEncodingParameters> audioEncodings;

    private P2PClientConfiguration(PeerConnection.RTCConfiguration rtcConfiguration,
            List<AudioEncodingParameters> audioEncodings,
            List<VideoEncodingParameters> videoEncodings) {
        super(rtcConfiguration);
        this.audioEncodings = audioEncodings;
        this.videoEncodings = videoEncodings;
    }

    /**
     * Get a Builder for creating a P2PClientConfiguration.
     *
     * @return Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for building up a P2PClientConfiguration.
     */
    public static class Builder {
        private final List<VideoEncodingParameters> videoEncodings = new ArrayList<>();
        private final List<AudioEncodingParameters> audioEncodings = new ArrayList<>();
        private PeerConnection.RTCConfiguration rtcConfiguration = null;

        Builder() {
        }

        /**
         * Add a VideoEncodingParameters to be supported.
         * P2PClientConfiguration without any VideoEncodingParameters specified, it will support all
         * video codecs supported by the hardware devices.
         *
         * @param videoEncodingParameter VideoEncodingParameters to be added.
         * @return Builder
         */
        public Builder addVideoParameters(
                VideoEncodingParameters videoEncodingParameter) {
            RCHECK(videoEncodingParameter);
            videoEncodings.add(videoEncodingParameter);
            return this;
        }

        /**
         * Add a AudioEncodingParameters to be supported.
         * P2PClientConfiguration without any AudioEncodingParameters specified, it will support all
         * video codecs supported by the hardware devices.
         *
         * @param audioEncodingParameter AudioEncodingParameters to be added.
         * @return Builder
         */
        public Builder addAudioParameters(
                AudioEncodingParameters audioEncodingParameter) {
            RCHECK(audioEncodingParameter);
            audioEncodings.add(audioEncodingParameter);
            return this;
        }

        /**
         * Set up the RTCConfiguration for the underlying WebRTC PeerConnection
         *
         * @param rtcConfiguration RTCConfiguration to be set.
         * @return Builder
         */
        public Builder setRTCConfiguration(PeerConnection.RTCConfiguration rtcConfiguration) {
            this.rtcConfiguration = rtcConfiguration;
            return this;
        }

        /**
         * Build up the P2PClientConfiguration.
         *
         * @return P2PClientConfiguration.
         */
        public P2PClientConfiguration build() {
            return new P2PClientConfiguration(rtcConfiguration, audioEncodings, videoEncodings);
        }
    }


}
