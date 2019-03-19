/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.base;

/**
 * Codec parameters for an video track
 */
public final class VideoCodecParameters {
    /**
     * Codec name.
     */
    public final MediaCodecs.VideoCodec name;
    /**
     * The profile of a codec. Only supported for H264.
     */
    public final MediaCodecs.H264Profile profile;

    /**
     * Create a VideoCodecParameters specified by the codec name supporting all profiles if
     * applicable.
     */
    public VideoCodecParameters(MediaCodecs.VideoCodec codecName) {
        this.name = codecName;
        profile = null;
    }

    /**
     * Create a VideoCodecParameters specified by the codec name and profile if applicable.
     */
    public VideoCodecParameters(MediaCodecs.VideoCodec codecName, MediaCodecs.H264Profile profile) {
        this.name = codecName;
        this.profile = profile;
    }
}
