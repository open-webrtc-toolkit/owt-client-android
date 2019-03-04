/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.test.util;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

import android.os.SystemClock;

import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

import java.util.ArrayList;

public class FakeRenderer implements VideoSink {
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

    public int getFramesRendered(int timeout) {
        SystemClock.sleep(timeout);
        synchronized (lock) {
            return framesRendered;
        }
    }

    public void checkLocalStreamFrame(boolean isBackFrame) {
        // TODO
    }

    private void checkVideoFrame(byte[] bytes, int value, boolean isblack) {
        // TODO
    }

    @Override
    public void onFrame(VideoFrame videoFrame) {
        synchronized (lock) {
            ++framesRendered;
            width = videoFrame.getRotatedWidth();
            height = videoFrame.getRotatedHeight();
        }
        videoFrame.release();
    }
}
