/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.conference;

import static owt.base.CheckCondition.RCHECK;

import owt.base.AudioEncodingParameters;
import owt.base.VideoEncodingParameters;

import java.util.ArrayList;
import java.util.List;

/**
 * Options for publishing a LocalStream to the conference.
 */
public final class PublishOptions {

    final List<AudioEncodingParameters> audioEncodingParameters;
    final List<VideoEncodingParameters> videoEncodingParameters;

    private PublishOptions(List<AudioEncodingParameters> audioParameters,
            List<VideoEncodingParameters> videoParameters) {
        audioEncodingParameters = audioParameters;
        videoEncodingParameters = videoParameters;
    }

    /**
     * Get a Builder for creating a PublishOptions.
     *
     * @return Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for building up a PublishOptions.
     */
    public static class Builder {
        final List<AudioEncodingParameters> audioEncodingParameters = new ArrayList<>();
        final List<VideoEncodingParameters> videoEncodingParameters = new ArrayList<>();

        Builder() {
        }

        /**
         * Add a VideoEncodingParameters to be supported for publishing a LocalStream.
         * PublishOptions without any VideoEncodingParameters specified, it will support all
         * video codecs supported by the hardware devices.
         *
         * @param parameter VideoEncodingParameters to be added.
         * @return Builder
         */
        public Builder addVideoParameter(VideoEncodingParameters parameter) {
            RCHECK(parameter);
            videoEncodingParameters.add(parameter);
            return this;
        }

        /**
         * Add an AudioEncodingParameters to be supported for publishing a LocalStream.
         * PublishOptions without any VideoEncodingParameters specified, it will support all
         * audio codecs supported by the hardware devices.
         *
         * @param parameter AudioEncodingParameters to be added.
         * @return Builder
         */
        public Builder addAudioParameter(AudioEncodingParameters parameter) {
            RCHECK(parameter);
            audioEncodingParameters.add(parameter);
            return this;
        }

        /**
         * Build up a PublishOptions.
         *
         * @return PublishOptions
         */
        public PublishOptions build() {
            return new PublishOptions(audioEncodingParameters, videoEncodingParameters);
        }
    }
}
