package com.intel.webrtc.test.base;

import static com.intel.webrtc.test.util.CommonAction.createDefaultCapturer;
import static com.intel.webrtc.test.util.CommonAction.createLocalStream;
import static com.intel.webrtc.test.util.Config.TIMEOUT;

import android.test.ActivityInstrumentationTestCase2;

import com.intel.webrtc.base.IcsVideoCapturer;
import com.intel.webrtc.base.LocalStream;
import com.intel.webrtc.base.MediaConstraints;
import com.intel.webrtc.base.VideoCapturer;
import com.intel.webrtc.test.util.FakeRenderer;

public class LocalStreamTest extends ActivityInstrumentationTestCase2<TestActivity> {
    private TestActivity act = null;

    public LocalStreamTest() {
        super(TestActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        act = getActivity();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        act = null;
    }

    public void testStream_createStreamWithICSCameraCapturer_shouldSucceed() {
        VideoCapturer videoCapturer = createDefaultCapturer();
        LocalStream localStream = createLocalStream(true, videoCapturer);
        FakeRenderer fakeRenderer = new FakeRenderer();
        localStream.attach(fakeRenderer);
        assertTrue(fakeRenderer.getFramesRendered(TIMEOUT) > 0);
        assertEquals(1280, fakeRenderer.frameWidth());
        assertEquals(720, fakeRenderer.frameHeight());
        videoCapturer.dispose();
        localStream.dispose();
    }

    public void testStream_createVideoOnlyStream_shouldSucceed() {
        VideoCapturer videoCapturer = createDefaultCapturer();
        LocalStream localStream = createLocalStream(false, videoCapturer);
        FakeRenderer fakeRenderer = new FakeRenderer();
        localStream.attach(fakeRenderer);
        assertTrue(fakeRenderer.getFramesRendered(TIMEOUT) > 0);
        assertEquals(1280, fakeRenderer.frameWidth());
        assertEquals(720, fakeRenderer.frameHeight());
        videoCapturer.dispose();
        localStream.dispose();
    }

    public void testStream_createAudioOnlyStream_shouldSucceed() {
        createLocalStream(true, null);
    }

    public void testStream_createStreamWithoutVideoOrAudio_shouldThrowException() {
        try {
            createLocalStream(false, null);
            fail("RuntimeException expected.");
        } catch (RuntimeException ignored) {
        }
    }

    public void testStream_disableVideoOnStreamWithVideo_shouldSucceed() {
        MediaConstraints.VideoTrackConstraints vmc
                = MediaConstraints.VideoTrackConstraints.create(false);
        vmc.setResolution(1280, 720);
        vmc.setFramerate(20);
        vmc.setCameraFacing(MediaConstraints.VideoTrackConstraints.CameraFacing.FRONT);
        VideoCapturer videoCapturer = new IcsVideoCapturer(vmc);
        LocalStream localStream = createLocalStream(true, videoCapturer);
        localStream.disableVideo();
        FakeRenderer fakeRenderer = new FakeRenderer();
        localStream.attach(fakeRenderer);
        assertTrue(fakeRenderer.getFramesRendered(TIMEOUT) > 0);
        fakeRenderer.checkLocalStreamFrame(true);
        videoCapturer.dispose();
        localStream.dispose();
    }

    public void testStream_disableVideoOnStreamWithoutVideo_shouldBePeaceful() {
        LocalStream localStream = createLocalStream(true, null);
        localStream.disableVideo();
        localStream.dispose();
    }

    public void testStream_disableAudioOnStreamWithAudio_shouldSucceed() {
        VideoCapturer videoCapturer = createDefaultCapturer();
        LocalStream localStream = createLocalStream(true, videoCapturer);
        localStream.disableAudio();
        videoCapturer.dispose();
        localStream.dispose();
    }

    public void testStream_disableAudioOnStreamWithoutAudio_shouldBePeaceful() {
        VideoCapturer videoCapturer = createDefaultCapturer();
        LocalStream localStream = createLocalStream(false, videoCapturer);
        localStream.disableAudio();
        videoCapturer.dispose();
        localStream.dispose();
    }

    public void testStream_createStreamWithDifferentResolution_shouldSucceed() {
        String[] resolutions = new String[]{"1920x1080", "1280x720", "640x480", "352x288"};
        for (String resolution : resolutions) {
            int width = Integer.valueOf(resolution.split("x")[0]);
            int height = Integer.valueOf(resolution.split("x")[1]);
            MediaConstraints.VideoTrackConstraints vmc
                    = MediaConstraints.VideoTrackConstraints.create(false);
            vmc.setResolution(width, height);
            vmc.setFramerate(20);
            vmc.setCameraFacing(MediaConstraints.VideoTrackConstraints.CameraFacing.FRONT);
            VideoCapturer videoCapturer = new IcsVideoCapturer(vmc);
            LocalStream localStream = createLocalStream(true, videoCapturer);
            FakeRenderer fakeRenderer = new FakeRenderer();
            localStream.attach(fakeRenderer);
            assertTrue(fakeRenderer.getFramesRendered(TIMEOUT) > 0);
            assertEquals(width, fakeRenderer.frameWidth());
            assertEquals(height, fakeRenderer.frameHeight());
            videoCapturer.dispose();
            localStream.dispose();
        }
    }
}
