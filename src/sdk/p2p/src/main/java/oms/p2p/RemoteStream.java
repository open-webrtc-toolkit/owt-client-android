/*
 * Intel License Header Holder
 */
package oms.p2p;

import org.webrtc.MediaStream;

public final class RemoteStream extends oms.base.RemoteStream {

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
