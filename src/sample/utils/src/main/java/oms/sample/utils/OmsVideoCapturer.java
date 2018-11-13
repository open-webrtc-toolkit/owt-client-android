package oms.sample.utils;

import org.webrtc.Camera1Capturer;

import oms.base.MediaConstraints;
import oms.base.Stream;
import oms.base.VideoCapturer;

public final class OmsVideoCapturer extends Camera1Capturer implements VideoCapturer {
    private MediaConstraints.VideoTrackConstraints videoTrackConstraints;

    public OmsVideoCapturer(MediaConstraints.VideoTrackConstraints constraints) {
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

    public void dispose() {
        super.dispose();
    }
}
