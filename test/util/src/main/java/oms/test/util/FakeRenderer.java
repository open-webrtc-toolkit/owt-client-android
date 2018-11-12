package oms.test.util;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

import android.os.SystemClock;

import org.webrtc.VideoRenderer;

import java.util.ArrayList;

public class FakeRenderer implements VideoRenderer.Callbacks {
    private int framesRendered = 0;
    private int width = 0;
    private int height = 0;
    private ArrayList<byte[]> oneFrameByte = null;
    private final Object lock = new Object();

    public int frameWidth() {
        synchronized (lock) {
            return width;
        }
    }

    public int frameHeight() {
        synchronized (lock) {
            return height;
        }
    }

    @Override
    public void renderFrame(VideoRenderer.I420Frame i420Frame) {
        synchronized (lock) {
            ++framesRendered;
            width = i420Frame.width;
            height = i420Frame.height;
            if (i420Frame.yuvPlanes != null) {
                oneFrameByte = new ArrayList<>();
                for (int i = 0; i < i420Frame.yuvPlanes.length; i++) {
                    byte[] bytes;
                    if (i420Frame.yuvPlanes[i].hasArray()) {
                        bytes = i420Frame.yuvPlanes[i].array();
                    } else {
                        bytes = new byte[i420Frame.yuvPlanes[i].remaining()];
                        i420Frame.yuvPlanes[i].get(bytes);
                    }
                    oneFrameByte.add(bytes);
                }
            }
        }
        VideoRenderer.renderFrameDone(i420Frame);
    }

    public int getFramesRendered(int timeout) {
        SystemClock.sleep(timeout);
        synchronized (lock) {
            return framesRendered;
        }
    }

    public void checkLocalStreamFrame(boolean isBackFrame) {
        synchronized (lock) {
            checkVideoFrame(oneFrameByte.get(1), -128, isBackFrame);
            checkVideoFrame(oneFrameByte.get(0), 0, isBackFrame);
            checkVideoFrame(oneFrameByte.get(2), -128, isBackFrame);
        }
    }

    private void checkVideoFrame(byte[] bytes, int value, boolean isblack) {
        for (byte b : bytes) {
            if (isblack) {
                assertEquals((int) b, value);
            } else {
                if ((int) b == value) {
                    fail("Frame expected.");
                }
            }
        }
    }
}
