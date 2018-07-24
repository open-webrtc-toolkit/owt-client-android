/*
 * Intel License Header Holder
 */
package com.intel.webrtc.base;

import static com.intel.webrtc.base.CheckCondition.RCHECK;

/**
 * Encoding parameters for sending a audio track.
 */
public class AudioEncodingParameters {
    /**
     * Maximum bitrate for sending an audio track.
     * *NOTE* currently setting different bitrates for different audio codecs is not supported.
     */
    public static int maxBitrate = 0;
    /**
     * Audio codec.
     */
    public final AudioCodecParameters codec;

    public AudioEncodingParameters(MediaCodecs.AudioCodec codec) {
        RCHECK(codec);
        this.codec = new AudioCodecParameters(codec);
    }

    public AudioEncodingParameters(AudioCodecParameters audioCodecParameters) {
        RCHECK(audioCodecParameters);
        this.codec = audioCodecParameters;
    }

    public AudioEncodingParameters(AudioCodecParameters audioCodecParameters, int maxBitrateKbps) {
        RCHECK(audioCodecParameters);
        RCHECK(maxBitrateKbps > 0);
        this.codec = audioCodecParameters;
        maxBitrate = maxBitrateKbps;
    }
}
