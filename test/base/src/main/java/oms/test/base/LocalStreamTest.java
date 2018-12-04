package oms.test.base;

import static oms.test.util.CommonAction.createDefaultCapturer;
import static oms.test.util.CommonAction.createLocalStream;
import static oms.test.util.Config.TIMEOUT;

import android.test.ActivityInstrumentationTestCase2;

import oms.base.LocalStream;
import oms.base.MediaConstraints;
import oms.base.VideoCapturer;
import oms.test.util.FakeRenderer;
import oms.test.util.VideoCapturerForTest;

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

    public void testStream_createStreamWithOMSCameraCapturer_shouldSucceed() {
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
        VideoCapturer videoCapturer = VideoCapturerForTest.create();
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
        // TODO
    }
}
