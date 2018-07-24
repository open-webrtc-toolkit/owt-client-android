/*
 * Intel License Header Holder
 */
package com.intel.webrtc.base;

import android.annotation.SuppressLint;
import android.content.Context;

import org.webrtc.EglBase;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.audio.AudioDeviceModule;
import org.webrtc.audio.LegacyAudioDeviceModule;

final class PCFactoryProxy {
    static int networkIgnoreMask = 0;
    //Enable Intel VP8 by default
    static String fieldTrials = "WebRTC-IntelVP8/Enabled/";
    @SuppressLint("StaticFieldLeak")
    static Context context;
    static boolean hwAcc = true;
    static VideoEncoderFactory encoderFactory = null;
    static VideoDecoderFactory decoderFactory = null;
    static AudioDeviceModule adm = null;
    static EglBase.Context localCtx = null, remoteCtx = null;
    @SuppressLint("StaticFieldLeak")
    private static PeerConnectionFactory peerConnectionFactory;

    static PeerConnectionFactory instance() {
        if (peerConnectionFactory == null) {
            PeerConnectionFactory.InitializationOptions initializationOptions =
                    PeerConnectionFactory.InitializationOptions.builder(context)
                            .setEnableVideoHwAcceleration(hwAcc)
                            .setFieldTrials(fieldTrials)
                            .createInitializationOptions();
            PeerConnectionFactory.initialize(initializationOptions);
            PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
            options.networkIgnoreMask = networkIgnoreMask;
            peerConnectionFactory = PeerConnectionFactory.builder()
                    .setOptions(options)
                    // TODO: currently using legacy adm
                    .setAudioDeviceModule(adm == null ? new LegacyAudioDeviceModule() : adm)
                    // TODO: currently using legacy factory by default
                    .setVideoEncoderFactory(encoderFactory)
                    .setVideoDecoderFactory(decoderFactory)
                    .createPeerConnectionFactory();
            if (localCtx != null || remoteCtx != null) {
                peerConnectionFactory.setVideoHwAccelerationOptions(localCtx, remoteCtx);
            }

            //dereference context or better design?
            context = null;
        }
        return peerConnectionFactory;
    }
}
