/*
 * Intel License Header Holder
 */
package com.intel.webrtc.base;

import org.webrtc.MediaStream;
import org.webrtc.RTCStatsReport;

public abstract class Publication {
    ///@cond
    protected final String id;
    protected final MediaStream mediaStream;
    protected boolean ended = false;

    protected Publication(String id, MediaStream mediaStream) {
        this.id = id;
        this.mediaStream = mediaStream;
    }
    ///@endcond

    public String id() {
        return id;
    }

    public abstract void getStats(ActionCallback<RTCStatsReport> callback);

    public abstract void stop();
}
