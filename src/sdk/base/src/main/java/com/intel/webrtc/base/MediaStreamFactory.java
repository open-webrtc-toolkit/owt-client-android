/*
 * Intel License Header Holder
 */
package com.intel.webrtc.base;

import static com.intel.webrtc.base.CheckCondition.DCHECK;
import static com.intel.webrtc.base.CheckCondition.RCHECK;

import com.intel.webrtc.base.MediaConstraints.AudioTrackConstraints;

import org.webrtc.AudioSource;
import org.webrtc.MediaStream;
import org.webrtc.VideoSource;

import java.util.HashMap;
import java.util.UUID;

final class MediaStreamFactory {

    private static MediaStreamFactory instance;
    private final HashMap<String, VideoSource> unsharedVideoSources = new HashMap<>();
    private AudioSource sharedAudioSource;
    private int audioSourceRef = 0;

    private MediaStreamFactory() {
    }

    synchronized static MediaStreamFactory instance() {
        if (instance == null) {
            instance = new MediaStreamFactory();
        }
        return instance;
    }

    MediaStream createMediaStream(VideoCapturer videoCapturer,
            AudioTrackConstraints audioMediaConstraints) {
        RCHECK(videoCapturer != null || audioMediaConstraints != null);

        String label = UUID.randomUUID().toString();
        MediaStream mediaStream = PCFactoryProxy.instance().createLocalMediaStream(label);

        if (videoCapturer != null) {
            VideoSource videoSource = PCFactoryProxy.instance().createVideoSource(videoCapturer);
            videoCapturer.startCapture(videoCapturer.getWidth(),
                    videoCapturer.getHeight(),
                    videoCapturer.getFps());
            mediaStream.addTrack(
                    PCFactoryProxy.instance().createVideoTrack(label + "v0", videoSource));
            unsharedVideoSources.put(label, videoSource);
        }

        if (audioMediaConstraints != null) {
            if (sharedAudioSource == null) {
                sharedAudioSource = PCFactoryProxy.instance().createAudioSource(
                        audioMediaConstraints.convertToWebRTCConstraints());
            }
            audioSourceRef++;
            mediaStream.addTrack(
                    PCFactoryProxy.instance().createAudioTrack(label + "a0", sharedAudioSource));
        }

        return mediaStream;
    }

    void onAudioSourceRelease() {
        DCHECK(audioSourceRef > 0);
        if (--audioSourceRef == 0) {
            sharedAudioSource.dispose();
            sharedAudioSource = null;
        }
    }

    void onVideoSourceRelease(String label) {
        DCHECK(unsharedVideoSources.containsKey(label));
        VideoSource videoSource = unsharedVideoSources.get(label);
        unsharedVideoSources.remove(label);
        videoSource.dispose();
    }

}
