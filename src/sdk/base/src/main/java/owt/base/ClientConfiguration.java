/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.base;

import static org.webrtc.PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
import static org.webrtc.PeerConnection.SdpSemantics.UNIFIED_PLAN;
import org.webrtc.PeerConnection;
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
            rtcConf.continualGatheringPolicy = GATHER_CONTINUALLY;
        }
        rtcConf.sdpSemantics = UNIFIED_PLAN;
        this.rtcConfiguration = rtcConf;
    }
}
///@endcond
