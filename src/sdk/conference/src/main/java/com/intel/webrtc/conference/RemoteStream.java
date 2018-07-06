/*
 * Intel License Header Holder
 */
package com.intel.webrtc.conference;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.MediaStream;

import com.intel.webrtc.base.Stream.StreamSourceInfo.AudioSourceInfo;
import com.intel.webrtc.base.Stream.StreamSourceInfo.VideoSourceInfo;

import java.util.HashMap;
import java.util.Iterator;

import static com.intel.webrtc.conference.JsonUtils.getObj;
import static com.intel.webrtc.conference.JsonUtils.getString;

/**
 * RemoteStream represent the stream published by other endpoints in the conference.
 */
public class RemoteStream extends com.intel.webrtc.base.RemoteStream {
    public final SubscriptionCapabilities subscriptionCapability;
    public final PublicationSettings publicationSettings;

    RemoteStream(JSONObject streamInfo) throws JSONException {
        super(getString(streamInfo, "id"),
              getString(streamInfo.getJSONObject("info"), "owner", "mixer"));

        JSONObject mediaInfo = getObj(streamInfo, "media", true);
        publicationSettings = new PublicationSettings(mediaInfo);
        subscriptionCapability = new SubscriptionCapabilities(mediaInfo);

        JSONObject video = getObj(mediaInfo, "video");
        VideoSourceInfo videoSourceInfo = null;
        if (video != null) {
            videoSourceInfo = VideoSourceInfo.get(getString(video, "source", "mixed"));
        }

        JSONObject audio = getObj(mediaInfo, "audio");
        AudioSourceInfo audioSourceInfo = null;
        if (audio != null) {
            audioSourceInfo = AudioSourceInfo.get(getString(audio, "source", "mixed"));
        }

        setStreamSourceInfo(new StreamSourceInfo(videoSourceInfo, audioSourceInfo));
        setAttributes(getObj(getObj(streamInfo, "info"), "attributes"));
    }

    void setMediaStream(MediaStream mediaStream) {
        this.mediaStream = mediaStream;
    }

    MediaStream getMediaStream() {
        return mediaStream;
    }

    private void setAttributes(JSONObject attributes) throws JSONException {
        if (attributes == null) {
            return;
        }
        HashMap<String, String> attr = new HashMap<>();
        Iterator<String> keyset = attributes.keys();

        while (keyset.hasNext()) {
            String key = keyset.next();
            String value = attributes.getString(key);
            attr.put(key, value);
        }

        setAttributes(attr);
    }

    void onEnded() {
        triggerEndedEvent();
    }
}
