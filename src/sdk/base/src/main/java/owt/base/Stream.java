/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.base;

import static owt.base.CheckCondition.RCHECK;

import org.webrtc.MediaStream;
import org.webrtc.VideoSink;

import java.util.HashMap;

public abstract class Stream {

    ///@cond
    protected MediaStream mediaStream;
    ///@endcond
    protected StreamSourceInfo streamSourceInfo = new StreamSourceInfo();
    private HashMap<String, String> attributes;

    abstract public String id();

    /**
     * Whether this Stream has a video track.
     *
     * @return true if it has a video track; otherwise, false.
     */
    public boolean hasVideo() {
        return mediaStream != null && !mediaStream.videoTracks.isEmpty();
    }

    /**
     * Whether this Stream has an audio track.
     *
     * @return true if it has an audio track; otherwise, false.
     */
    public boolean hasAudio() {
        return mediaStream != null && !mediaStream.audioTracks.isEmpty();
    }

    /**
     * Get the StreamSourceInfo of the Stream.
     *
     * @return StreamSourceInfo
     */
    public StreamSourceInfo getStreamSourceInfo() {
        return streamSourceInfo;
    }

    ///@cond
    protected void setStreamSourceInfo(StreamSourceInfo info) {
        streamSourceInfo = info;
    }

    /**
     * Get attributes that has been set to the Stream.
     *
     * @return attributes
     */
    public HashMap<String, String> getAttributes() {
        return attributes;
    }
    ///@endcond

    /**
     * Set attributes for the Stream.
     *
     * @param attributes attributes to be set.
     */
    public void setAttributes(HashMap<String, String> attributes) {
        this.attributes = attributes;
    }

    /**
     * Disable the video track of the Stream.
     */
    public void disableVideo() {
        if (hasVideo()) {
            mediaStream.videoTracks.get(0).setEnabled(false);
        }
    }

    /**
     * Enable the video track of the Stream.
     */
    public void enableVideo() {
        if (hasVideo()) {
            mediaStream.videoTracks.get(0).setEnabled(true);
        }
    }

    /**
     * Disable the audio track of the Stream.
     */
    public void disableAudio() {
        if (hasAudio()) {
            mediaStream.audioTracks.get(0).setEnabled(false);
        }
    }

    /**
     * Enable the audio track of the Stream.
     */
    public void enableAudio() {
        if (hasAudio()) {
            mediaStream.audioTracks.get(0).setEnabled(true);
        }
    }

    /**
     * Attach the video track of the media stream to videosink in order to display the video
     * content.
     *
     * @param videoSink VideoSink
     */
    public void attach(VideoSink videoSink) {
        if (mediaStream == null) {
            return;
        }
        RCHECK(videoSink);
        CheckCondition.RCHECK(!mediaStream.videoTracks.isEmpty());
        mediaStream.videoTracks.get(0).addSink(videoSink);
    }

    /**
     * Detach the video track of the media stream from a videosink.
     *
     * @param videoSink VideoSink
     */
    public void detach(VideoSink videoSink) {
        if (mediaStream == null) {
            return;
        }
        RCHECK(videoSink);
        CheckCondition.RCHECK(!mediaStream.videoTracks.isEmpty());
        mediaStream.videoTracks.get(0).removeSink(videoSink);
    }

    ///@cond
    public String videoTrackId() {
        if (hasVideo()) {
            return mediaStream.videoTracks.get(0).id();
        }
        return null;
    }

    public String audioTrackId() {
        if (hasAudio()) {
            return mediaStream.audioTracks.get(0).id();
        }
        return null;
    }

    /**
     * Check if the Stream is disposed
     * @return true if Stream is disposed; otherwise, false.
     */
    public boolean disposed() {
        return mediaStream == null;
    }

    /**
     * Information of the source of a Stream.
     */
    public static class StreamSourceInfo {
        /**
         * Video source info of the Stream.
         */
        public VideoSourceInfo videoSourceInfo;
        /**
         * Audio source info of the Stream.
         */
        public AudioSourceInfo audioSourceInfo;

        public StreamSourceInfo() {
            this.videoSourceInfo = null;
            this.audioSourceInfo = null;
        }

        public StreamSourceInfo(VideoSourceInfo videoSourceInfo, AudioSourceInfo audioSourceInfo) {
            this.videoSourceInfo = videoSourceInfo;
            this.audioSourceInfo = audioSourceInfo;
        }

        public enum VideoSourceInfo {
            CAMERA("camera"),
            SCREEN_CAST("screen-cast"),
            RAW_FILE("raw-file"),
            ENCODED_FILE("encoded-file"),
            MIXED("mixed"),
            OTHERS("other");

            ///@cond
            public final String type;

            VideoSourceInfo(String type) {
                this.type = type;
            }

            public static VideoSourceInfo get(String type) {
                switch (type) {
                    case "camera":
                        return CAMERA;
                    case "screen-cast":
                        return SCREEN_CAST;
                    case "raw-file":
                        return RAW_FILE;
                    case "encoded-file":
                        return ENCODED_FILE;
                    case "mixed":
                        return MIXED;
                    default:
                        return OTHERS;
                }
            }
            ///@endcond
        }

        public enum AudioSourceInfo {
            MIC("mic"),
            FILE("file"),
            OTHERS("other"),
            MIXED("mixed");

            ///@cond
            public final String type;

            AudioSourceInfo(String type) {
                this.type = type;
            }

            public static AudioSourceInfo get(String info) {
                switch (info) {
                    case "mic":
                        return MIC;
                    case "file":
                        return FILE;
                    case "mixed":
                        return MIXED;
                    default:
                        return OTHERS;
                }
            }
            ///@endcond
        }
    }
    ///@endcond
}
