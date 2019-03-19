/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.base;

import static owt.base.CheckCondition.DCHECK;

/**
 * LocalStream is a Stream that created by current client.
 */
public final class LocalStream extends Stream {
    public final int resolutionWidth, resolutionHeight, frameRate;

    /**
     * Create a LocalStream with video track ONLY.
     *
     * @param videoCapturer VideoCapturer that the video content is captured from.
     */
    public LocalStream(VideoCapturer videoCapturer) {
        this(videoCapturer, null);
    }

    /**
     * Create a LocalStream with audio track ONLY.
     *
     * @param audioMediaConstraints Audio options for the audio track.
     */
    public LocalStream(MediaConstraints.AudioTrackConstraints audioMediaConstraints) {
        this(null, audioMediaConstraints);
    }

    /**
     * Create a LocalStream with a video track and an audio track.
     *
     * @param videoCapturer VideoCapturer that the video content is captured from.
     * @param audioMediaConstraints Audio options for the audio track.
     */
    public LocalStream(VideoCapturer videoCapturer,
            MediaConstraints.AudioTrackConstraints audioMediaConstraints) {
        //TODO: update audio source info.
        streamSourceInfo = new StreamSourceInfo(
                videoCapturer == null ? null : videoCapturer.getVideoSource(), StreamSourceInfo
                .AudioSourceInfo.MIC);
        mediaStream = MediaStreamFactory.instance().createMediaStream(videoCapturer,
                audioMediaConstraints);
        resolutionWidth = videoCapturer == null ? 0 : videoCapturer.getWidth();
        resolutionHeight = videoCapturer == null ? 0 : videoCapturer.getHeight();
        frameRate = videoCapturer == null ? 0 : videoCapturer.getFps();
    }

    /**
     * Id of the LocalStream.
     *
     * @return id of LocalStream.
     */
    @Override
    public String id() {
        DCHECK(mediaStream);
        return mediaStream.getId();
    }

    /**
     * Close and dispose all the sources associated with the LocalStream.
     * ATTENTION: Be sure that there is no active Publication using this LocalStream before
     * calling this method.
     */
    public void dispose() {
        DCHECK(mediaStream);
        if (hasVideo()) {
            MediaStreamFactory.instance().onVideoSourceRelease(mediaStream.getId());
        }
        if (hasAudio()) {
            MediaStreamFactory.instance().onAudioSourceRelease();
        }

        mediaStream.dispose();
        mediaStream = null;
    }
}
