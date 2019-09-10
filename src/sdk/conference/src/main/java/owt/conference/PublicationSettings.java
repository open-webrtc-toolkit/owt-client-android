/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.conference;

import static owt.base.CheckCondition.DCHECK;
import static owt.conference.JsonUtils.getInt;
import static owt.conference.JsonUtils.getObj;
import static owt.conference.JsonUtils.getString;

import owt.base.AudioCodecParameters;
import owt.base.MediaCodecs.AudioCodec;
import owt.base.MediaCodecs.VideoCodec;
import owt.base.VideoCodecParameters;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * The settings for a publication.
 */
public final class PublicationSettings {

    public final List<AudioPublicationSettings> audioPublicationSettings;
    public final List<VideoPublicationSettings> videoPublicationSettings;

    PublicationSettings(JSONObject mediaInfo) throws JSONException {
        DCHECK(mediaInfo);

        JSONObject audio = getObj(mediaInfo, "audio");
        if (audio != null) {
            audioPublicationSettings = new ArrayList<>();
            audioPublicationSettings.add(new AudioPublicationSettings(audio));
        } else {
            audioPublicationSettings = null;
        }

        JSONObject video = getObj(mediaInfo, "video");
        if (video != null) {
            videoPublicationSettings = new ArrayList<>();
            JSONArray videoOrigins = video.getJSONArray("original");
            for (int i = 0; i < videoOrigins.length(); i++) {
                JSONObject videoObj = videoOrigins.getJSONObject(i);
                videoPublicationSettings.add(new VideoPublicationSettings(videoObj));
            }
        } else {
            videoPublicationSettings = null;
        }
    }

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
        public final String rid;

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
            if (videoObj.has("simulcastRid")) {
                rid = getString(videoObj, "simulcastRid");
            } else {
                rid = null;
            }
        }
    }

}
