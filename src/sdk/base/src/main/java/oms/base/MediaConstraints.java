/*
 * Intel License Header Holder
 */
package oms.base;

import static oms.base.CheckCondition.RCHECK;
import static oms.base.MediaConstraints.VideoTrackConstraints.CameraFacing.BACK;
import static oms.base.MediaConstraints.VideoTrackConstraints.CameraFacing.FRONT;

import org.webrtc.Camera1Enumerator;
import org.webrtc.CameraEnumerator;

public abstract class MediaConstraints {

    public enum TrackKind {
        AUDIO("audio"),
        VIDEO("video"),
        AUDIO_AND_VIDEO("av");

        ///@cond
        public final String kind;
        ///@endcond

        TrackKind(String kind) {
            this.kind = kind;
        }
    }

    public final static class VideoTrackConstraints {
        public int resolutionWidth = 640, resolutionHeight = 480, fps = 30;
        public boolean captureToTexture = true;
        public CameraFacing cameraFacing = CameraFacing.FRONT;

        private VideoTrackConstraints(boolean captureToTexture) {
            this.captureToTexture = captureToTexture;
        }

        public static VideoTrackConstraints create(boolean captureToTexture) {
            return new VideoTrackConstraints(captureToTexture);
        }

        public VideoTrackConstraints setResolution(int width, int height) {
            CheckCondition.RCHECK(width > 0 && height > 0);
            resolutionWidth = width;
            resolutionHeight = height;
            return this;
        }

        public VideoTrackConstraints setFramerate(int framerate) {
            CheckCondition.RCHECK(framerate > 0);
            fps = framerate;
            return this;
        }

        public VideoTrackConstraints setCameraFacing(CameraFacing facing) {
            CheckCondition.RCHECK(facing);
            cameraFacing = facing;
            return this;
        }

        public String getDeviceName() {
            CameraEnumerator enumerator = new Camera1Enumerator(captureToTexture);

            String deviceName = null;
            for (String device : enumerator.getDeviceNames()) {
                if (cameraFacing == FRONT && enumerator.isFrontFacing(device)) {
                    deviceName = device;
                    break;
                } else if (cameraFacing == BACK && enumerator.isBackFacing(device)) {
                    deviceName = device;
                    break;
                }
            }

            return deviceName == null ? enumerator.getDeviceNames()[0] : deviceName;
        }

        public enum CameraFacing {
            FRONT,
            BACK
        }
    }

    public final static class AudioTrackConstraints {
        public boolean echoCancellation = false;
        public boolean extendedFilterEchoCancellation = false;
        public boolean delayAgnosticEchoCancellation = false;

        org.webrtc.MediaConstraints convertToWebRTCConstraints() {
            org.webrtc.MediaConstraints result = new org.webrtc.MediaConstraints();
            if (echoCancellation) {
                result.mandatory.add(
                        new org.webrtc.MediaConstraints.KeyValuePair("googEchoCancellation",
                                "true"));
            }
            if (extendedFilterEchoCancellation) {
                result.mandatory.add(
                        new org.webrtc.MediaConstraints.KeyValuePair("googEchoCancellation2",
                                "true"));
            }
            if (delayAgnosticEchoCancellation) {
                result.mandatory.add(
                        new org.webrtc.MediaConstraints.KeyValuePair("googDAEchoCancellation",
                                "true"));
            }
            return result;
        }
    }
}
