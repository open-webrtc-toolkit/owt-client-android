/*
 * Intel License Header Holder
 */
package com.intel.webrtc.p2p;

import org.webrtc.MediaStream;

public final class RemoteStream extends com.intel.webrtc.base.RemoteStream {

    RemoteStream(String origin, MediaStream mediaStream) {
        super(mediaStream.getId(), origin);
        this.mediaStream = mediaStream;
    }

    void setInfo(StreamSourceInfo streamSourceInfo) {
        setStreamSourceInfo(streamSourceInfo);
    }

    void onEnded() {
        triggerEndedEvent();
    }
}
