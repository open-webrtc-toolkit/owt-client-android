package com.intel.webrtc.sample.utils;

import org.webrtc.FileVideoCapturer;

import com.intel.webrtc.base.Stream;
import com.intel.webrtc.base.VideoCapturer;

import java.io.IOException;

public class IcsFileVideoCapturer extends FileVideoCapturer implements VideoCapturer {

    public IcsFileVideoCapturer(String inputFile) throws IOException {
        super(inputFile);
    }

    @Override
    public int getWidth() {
        // ignored
        return 0;
    }

    @Override
    public int getHeight() {
        // ignored
        return 0;
    }

    @Override
    public int getFps() {
        return 30;
    }

    @Override
    public Stream.StreamSourceInfo.VideoSourceInfo getVideoSource() {
        return Stream.StreamSourceInfo.VideoSourceInfo.RAW_FILE;
    }
}
