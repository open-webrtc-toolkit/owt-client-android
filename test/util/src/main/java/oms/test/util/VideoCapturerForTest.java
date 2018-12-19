/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package oms.test.util;

import org.webrtc.Camera1Capturer;
import org.webrtc.Camera1Enumerator;
import org.webrtc.CameraEnumerator;

import oms.base.Stream;
import oms.base.VideoCapturer;

public final class VideoCapturerForTest extends Camera1Capturer implements VideoCapturer {
    private int width, height, fps;

    private VideoCapturerForTest(String deviceName, boolean captureToTexture) {
        super(deviceName, null, captureToTexture);
    }

    public static VideoCapturerForTest create() {
        String deviceName = getDeviceName(true);
        VideoCapturerForTest capturer = new VideoCapturerForTest(deviceName, true);
        capturer.width = 640;
        capturer.height = 480;
        capturer.fps = 20;
        return capturer;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getFps() {
        return fps;
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

    private static String getDeviceName(boolean captureToTexture) {
        CameraEnumerator enumerator = new Camera1Enumerator(captureToTexture);

        String deviceName = null;
        for (String device : enumerator.getDeviceNames()) {
            if (enumerator.isFrontFacing(device)) {
                deviceName = device;
                break;
            }
        }

        return deviceName == null ? enumerator.getDeviceNames()[0] : deviceName;
    }
}