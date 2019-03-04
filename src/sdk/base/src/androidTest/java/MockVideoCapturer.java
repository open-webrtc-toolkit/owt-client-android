import android.content.Context;

import owt.base.Stream;
import owt.base.VideoCapturer;

import org.webrtc.CapturerObserver;
import org.webrtc.SurfaceTextureHelper;

public class MockVideoCapturer implements VideoCapturer {
    @Override
    public int getWidth() {
        return 640;
    }

    @Override
    public int getHeight() {
        return 480;
    }

    @Override
    public int getFps() {
        return 30;
    }

    @Override
    public Stream.StreamSourceInfo.VideoSourceInfo getVideoSource() {
        return Stream.StreamSourceInfo.VideoSourceInfo.CAMERA;
    }

    @Override
    public void initialize(SurfaceTextureHelper surfaceTextureHelper, Context context,
            CapturerObserver capturerObserver) {

    }

    @Override
    public void startCapture(int i, int i1, int i2) {

    }

    @Override
    public void stopCapture() throws InterruptedException {

    }

    @Override
    public void changeCaptureFormat(int i, int i1, int i2) {

    }

    @Override
    public void dispose() {

    }

    @Override
    public boolean isScreencast() {
        return false;
    }
}
