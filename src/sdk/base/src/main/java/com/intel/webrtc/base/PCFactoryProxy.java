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

final class PCFactoryProxy {
    @SuppressLint("StaticFieldLeak")
    private static PeerConnectionFactory peerConnectionFactory;
    static int networkIgnoreMask = 0;
    //Enable Intel VP8 by default
    static String fieldTrials = "WebRTC-IntelVP8/Enabled/";
    @SuppressLint("StaticFieldLeak")
    static Context context;
    static boolean hwAcc = true;
    static VideoEncoderFactory encoderFactory = null;
    static VideoDecoderFactory decoderFactory = null;
    static EglBase.Context localCtx = null, remoteCtx = null;

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
            peerConnectionFactory = new PeerConnectionFactory(options, encoderFactory,
                                                              decoderFactory);
            if (localCtx != null || remoteCtx != null) {
                peerConnectionFactory.setVideoHwAccelerationOptions(localCtx, remoteCtx);
            }

            //dereference context or better design?
            context = null;
        }
        return peerConnectionFactory;
    }
}
