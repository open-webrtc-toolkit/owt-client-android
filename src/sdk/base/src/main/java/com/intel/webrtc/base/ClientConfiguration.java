/*
 * Intel License Header Holder
 */
package com.intel.webrtc.base;

import org.webrtc.PeerConnection.RTCConfiguration;
import org.webrtc.PeerConnection.IceServer;

import java.util.LinkedList;

///@cond
public abstract class ClientConfiguration {
    // default RTCConfiguration will contain an empty IceServer list.
    public final RTCConfiguration rtcConfiguration;

    protected ClientConfiguration(RTCConfiguration rtcConfiguration) {
        this.rtcConfiguration =
                rtcConfiguration == null ? new RTCConfiguration(new LinkedList<IceServer>())
                                         : rtcConfiguration;
    }
}
///@endcond
