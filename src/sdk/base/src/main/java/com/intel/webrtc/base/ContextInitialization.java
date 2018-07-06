/*
 * Intel License Header Holder
 */
package com.intel.webrtc.base;

import android.content.Context;

import org.webrtc.EglBase;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.audio.AudioDeviceModule;

import static com.intel.webrtc.base.CheckCondition.RCHECK;

/**
 * Initialization settings. ContextInitialization.initialize must be called before creating
 * P2PClient or ConferenceClient.
 */
public class ContextInitialization {

    /**
     * Network types: Ethernet, wifi, cellular, vpn and loopback.
     */
    public enum NetworkType {
        ETHERNET(1),
        WIFI(1 << 1),
        CELLULAR(1 << 2),
        VPN(1 << 3),
        LOOPBACK(1 << 4);
        //BLUETOOTH();

        private final int value;

        ///@cond
        NetworkType(int v) {
            value = v;
        }
        ///@endcond
    }

    private static boolean initialized = false;
    private static ContextInitialization instance = null;

    private ContextInitialization() {
    }

    /**
     * Get the ContextInitialization instance.
     *
     * @return ContextInitialization
     */
    public static ContextInitialization create() {
        if (instance == null) {
            instance = new ContextInitialization();
        }
        return instance;
    }

    /**
     * Add the network type to be ignored during video or audio call. This method can be
     * called multiple times in case there are multiple network types that needed to be ignored.
     *
     * @param ignoreNetworkType network type to be ignored.
     * @return ContextInitialization
     */
    public ContextInitialization addIgnoreNetworkType(NetworkType ignoreNetworkType) {
        RCHECK(!initialized);
        RCHECK(ignoreNetworkType);
        PCFactoryProxy.networkIgnoreMask |= ignoreNetworkType.value;
        return this;
    }

    /**
     * Set up Android application context to WebRTC runtime.
     *
     * @param ctx Android application context.
     * @return ContextInitialization
     */
    public ContextInitialization setApplicationContext(Context ctx) {
        RCHECK(!initialized);
        PCFactoryProxy.context = ctx;
        return this;
    }

    /**
     * Set a flag to indicate whether video codec hardware acceleration should be enabled.
     * By default, VP8 hardware acceleration is enabled. H.264 only has hardware accelerated
     * codecs. Actual result depends on platform and hardware support.
     *
     * @param enabled a flag to indicate whether codec hardware acceleration is enabled.
     * @return ContextInitialization
     */
    public ContextInitialization setCodecHardwareAccelerationEnabled(boolean enabled) {
        RCHECK(!initialized);
        PCFactoryProxy.hwAcc = enabled;
        return this;
    }

    /**
     * Set the EGL context used by hardware video encoder and decoder.
     *
     * @param localEglCtx Must be the same as that used by VideoCapturerAndroid and any local video
     * renderer.
     * @param remoteEglCtx Must be the same as that used by any remote video renderer.
     * @return ContextInitialization
     */
    public ContextInitialization setVideoHardwareAccelerationOptions(EglBase.Context localEglCtx,
            EglBase.Context remoteEglCtx) {
        RCHECK(!initialized);
        PCFactoryProxy.localCtx = localEglCtx;
        PCFactoryProxy.remoteCtx = remoteEglCtx;
        return this;
    }

    /**
     * Add a field trial used for enabling some features in WebRTC.
     * This method can be called multiple times in case there are multiple field trials that
     * needed to be added.
     *
     * @param fieldTrial fieldTrial to be added.
     * @return ContextInitialization
     */
    public ContextInitialization addFieldTrials(String fieldTrial) {
        RCHECK(!initialized);
        PCFactoryProxy.fieldTrials += fieldTrial;
        return this;
    }

    /**
     * Set the customized video encoder factory.
     *
     * @param encoderFactory VideoEncoderFactory to be set.
     * @return ContextInitialization
     */
    public ContextInitialization setCustomizedVideoEncoderFactory(
            VideoEncoderFactory encoderFactory) {
        RCHECK(!initialized);
        PCFactoryProxy.encoderFactory = encoderFactory;
        return this;
    }

    /**
     * Set the customized video decoder factory.
     *
     * @param decoderFactory VideoDecoderFactory to be set.
     * @return ContextInitialization
     */
    public ContextInitialization setCustomizedVideoDecoderFactory(
            VideoDecoderFactory decoderFactory) {
        RCHECK(!initialized);
        PCFactoryProxy.decoderFactory = decoderFactory;
        return this;
    }

    /**
     * Set the customized audio device module.
     *
     * @param adm AudioDeviceModule to be set.
     * @return ContextInitialization
     */
    public ContextInitialization setCustomizedAudioDeviceModule(AudioDeviceModule adm) {
        RCHECK(!initialized);
        PCFactoryProxy.adm = adm;
        return this;
    }

    /**
     * Initialize context settings.
     */
    public void initialize() {
        RCHECK(!initialized);
        initialized = true;
        PCFactoryProxy.instance();
    }
}
