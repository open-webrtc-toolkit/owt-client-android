/*
 * Intel License Header Holder
 */
package com.intel.webrtc.conference;

import com.intel.webrtc.base.MediaCodecs.AudioCodec;
import com.intel.webrtc.base.AudioCodecParameters;
import com.intel.webrtc.base.MediaCodecs.VideoCodec;
import com.intel.webrtc.base.VideoCodecParameters;

import org.json.JSONObject;

import static com.intel.webrtc.base.CheckCondition.DCHECK;
import static com.intel.webrtc.conference.JsonUtils.getInt;
import static com.intel.webrtc.conference.JsonUtils.getObj;
import static com.intel.webrtc.conference.JsonUtils.getString;

/**
 * The settings for a publication.
 */
public final class PublicationSettings {

    /**
     * Audio settings for a publication.
     */
    public static class AudioPublicationSettings {
        public final AudioCodecParameters codec;

        AudioPublicationSettings(JSONObject audioObj) {
            JSONObject format = getObj(audioObj, "format", true);
            AudioCodec audioCodec = AudioCodec.get(getString(format, "codec", ""));
            int sampleRate = getInt(format, "sampleRate", 0);
            int channelNum = getInt(format, "channelNum", 0);
            codec = new AudioCodecParameters(audioCodec, sampleRate, channelNum);
        }
    }

    /**
     * Video settings for a publication.
     */
    public static class VideoPublicationSettings {
        public final VideoCodecParameters codec;
        public final int resolutionWidth, resolutionHeight, frameRate;
        public final int bitrate, keyFrameInterval;

        VideoPublicationSettings(JSONObject videoObj) {
            JSONObject format = getObj(videoObj, "format", true);
            VideoCodec videoCodec = VideoCodec.get(getString(format, "codec", ""));
            codec = new VideoCodecParameters(videoCodec);

            JSONObject param = getObj(videoObj, "parameters");
            if (param != null) {
                resolutionWidth = getInt(getObj(param, "resolution"), "width", 0);
                resolutionHeight = getInt(getObj(param, "resolution"), "height", 0);
                frameRate = getInt(param, "framerate", 0);
                bitrate = getInt(param, "bitrate", 0);
                keyFrameInterval = getInt(param, "keyFrameInterval", 0);
            } else {
                resolutionWidth = 0;
                resolutionHeight = 0;
                frameRate = 0;
                bitrate = 0;
                keyFrameInterval = 0;
            }
        }
    }

    public final AudioPublicationSettings audioPublicationSettings;
    public final VideoPublicationSettings videoPublicationSettings;

    PublicationSettings(JSONObject mediaInfo) {
        DCHECK(mediaInfo);

        JSONObject audio = getObj(mediaInfo, "audio");
        audioPublicationSettings = audio == null ? null : new AudioPublicationSettings(audio);

        JSONObject video = getObj(mediaInfo, "video");
        videoPublicationSettings = video == null ? null : new VideoPublicationSettings(video);
    }

}
