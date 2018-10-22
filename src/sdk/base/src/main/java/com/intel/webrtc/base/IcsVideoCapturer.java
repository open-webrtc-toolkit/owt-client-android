/*
 * Intel License Header Holder
 */
package com.intel.webrtc.base;

import com.intel.webrtc.base.MediaConstraints.VideoTrackConstraints;

import org.webrtc.Camera1Capturer;

public final class IcsVideoCapturer extends Camera1Capturer implements VideoCapturer {
    private VideoTrackConstraints videoTrackConstraints;

    public IcsVideoCapturer(VideoTrackConstraints constraints) {
        super(constraints.getDeviceName(), null, constraints.captureToTexture);
        videoTrackConstraints = constraints;
    }

    @Override
    public int getWidth() {
        return videoTrackConstraints.resolutionWidth;
    }

    @Override
    public int getHeight() {
        return videoTrackConstraints.resolutionHeight;
    }

    @Override
    public int getFps() {
        return videoTrackConstraints.fps;
    }

    @Override
    public Stream.StreamSourceInfo.VideoSourceInfo getVideoSource() {
        return Stream.StreamSourceInfo.VideoSourceInfo.CAMERA;
    }

    public void switchCamera() {
        super.switchCamera(null);
    }

    public void setFilter() {

    }

    public void dispose() {
        super.dispose();
    }
}
