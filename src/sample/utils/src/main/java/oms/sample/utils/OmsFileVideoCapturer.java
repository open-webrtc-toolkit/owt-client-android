package oms.sample.utils;

import org.webrtc.FileVideoCapturer;

import oms.base.Stream;
import oms.base.VideoCapturer;

import java.io.IOException;

public class OmsFileVideoCapturer extends FileVideoCapturer implements VideoCapturer {

    public OmsFileVideoCapturer(String inputFile) throws IOException {
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
