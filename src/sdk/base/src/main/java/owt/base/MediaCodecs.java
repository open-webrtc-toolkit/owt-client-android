/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.base;

import android.annotation.SuppressLint;

public final class MediaCodecs {

    public enum VideoCodec {
        VP8("VP8"), VP9("VP9"), H264("H264"), H265("H265"),
        ///@cond
        INVALID("")
        ///@endcond
        ;

        ///@cond
        final String name;

        VideoCodec(String codecName) {
            name = codecName;
        }

        @SuppressLint("DefaultLocale")
        public static VideoCodec get(String codecName) {
            // codec names returned from MCU are lower case but other clients using upper case.
            switch (codecName.toUpperCase()) {
                case "VP8":
                    return VP8;
                case "VP9":
                    return VP9;
                case "H264":
                    return H264;
                case "H265":
                    return H265;
                default:
                    return INVALID;
            }
        }
        ///@endcond
    }

    ///@cond
    public enum H264Profile {
        CONSTRAINED_BASELINE("constrained_baseline"),
        BASELINE("baseline"),
        MAIN("main"),
        HIGH("high");

        final String profile;

        H264Profile(String profile) {
            this.profile = profile;
        }
    }
    ///@endcond

    public enum AudioCodec {
        PCMU("PCMU"), PCMA("PCMA"), OPUS("opus"), G722("G722"), ISAC("ISAC"), ILBC("ILBC"),
        AAC("AAC"), AC3("AC3"), ASAO("ASAO"),
        ///@cond
        INVALID("")
        ///@endcond
        ;

        ///@cond
        final String name;

        AudioCodec(String codecName) {
            name = codecName;
        }

        @SuppressLint("DefaultLocale")
        public static AudioCodec get(String codecName) {
            // codec names returned from MCU are lower case but other clients using upper case.
            switch (codecName.toUpperCase()) {
                case "PCMU":
                    return PCMU;
                case "PCMA":
                    return PCMA;
                case "OPUS":
                    return OPUS;
                case "G722":
                    return G722;
                case "ISAC":
                    return ISAC;
                case "ILBC":
                    return ILBC;
                case "AAC":
                    return AAC;
                case "AC3":
                    return AC3;
                case "ASAO":
                    return ASAO;
                default:
                    return INVALID;
            }
        }
        ///@endcond
    }
}
