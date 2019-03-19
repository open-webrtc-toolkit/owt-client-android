/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.base;

import static owt.base.CheckCondition.RCHECK;

/**
 * Encoding parameters for sending a video track.
 */
public final class VideoEncodingParameters {
    /**
     * Maximum bitrate for sending a video track.
     * *NOTE* currently setting different bitrates for different video codecs is not supported.
     */
    public static int maxBitrate = 0;
    /**
     * Video codec.
     */
    public final VideoCodecParameters codec;

    public VideoEncodingParameters(MediaCodecs.VideoCodec codec) {
        RCHECK(codec);
        this.codec = new VideoCodecParameters(codec);
    }

    public VideoEncodingParameters(VideoCodecParameters videoCodecParameters) {
        RCHECK(videoCodecParameters);
        codec = videoCodecParameters;
    }

    public VideoEncodingParameters(VideoCodecParameters videoCodecParameters, int maxBitrateKbps) {
        RCHECK(videoCodecParameters);
        RCHECK(maxBitrateKbps > 0);
        this.codec = videoCodecParameters;
        maxBitrate = maxBitrateKbps;
    }
}
