/*
 * Intel License Header Holder
 */
package oms.base;

import org.webrtc.Camera1Capturer;

public final class OMSVideoCapturer extends Camera1Capturer implements VideoCapturer {
    private MediaConstraints.VideoTrackConstraints videoTrackConstraints;

    public OMSVideoCapturer(MediaConstraints.VideoTrackConstraints constraints) {
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
