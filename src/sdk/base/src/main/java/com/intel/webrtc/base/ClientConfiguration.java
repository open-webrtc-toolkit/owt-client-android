/*
 * Intel License Header Holder
 */
package com.intel.webrtc.base;

import org.webrtc.PeerConnection.RTCConfiguration;

import java.util.LinkedList;

///@cond
public abstract class ClientConfiguration {
    // default RTCConfiguration will contain an empty IceServer list.
    public final RTCConfiguration rtcConfiguration;

    protected ClientConfiguration(RTCConfiguration rtcConf) {
        if (rtcConf == null) {
            rtcConf = new RTCConfiguration(new LinkedList<>());
            rtcConf.enableDtlsSrtp = true;
        }
        this.rtcConfiguration = rtcConf;
    }
}
///@endcond
