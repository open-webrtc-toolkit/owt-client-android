/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.base;

/**
 * Codec parameters for an audio track
 */
public final class AudioCodecParameters {
    /**
     * Codec name.
     */
    public final MediaCodecs.AudioCodec name;
    /**
     * channel numbers.
     */
    public final int channelNum;
    /**
     * sample rate.
     */
    public final int sampleRate;

    /**
     * Create a AudioCodecParameters specified by the codec name supporting all channel numbers
     * and sample rates.
     */
    public AudioCodecParameters(MediaCodecs.AudioCodec codecName) {
        this.name = codecName;
        channelNum = 0;
        sampleRate = 0;
    }

    /**
     * Create a AudioCodecParameters specified by the codec name, channel number and sample rate.
     */
    public AudioCodecParameters(MediaCodecs.AudioCodec codecName, int channelNum, int sampleRate) {
        this.name = codecName;
        this.channelNum = channelNum;
        this.sampleRate = sampleRate;
    }
}
