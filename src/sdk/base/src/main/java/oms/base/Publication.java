/*
 * Intel License Header Holder
 */
package oms.base;

import org.webrtc.MediaStream;
import org.webrtc.RTCStatsReport;

public abstract class Publication {
    ///@cond
    protected final String id;
    protected final String mediaStreamId;
    protected boolean ended = false;

    protected Publication(String id) {
        this.id = id;
        this.mediaStreamId = "";
    }

    protected Publication(String id, String mediaStreamId) {
        this.id = id;
        this.mediaStreamId = mediaStreamId;
    }
    ///@endcond

    public String id() {
        return id;
    }

    public abstract void getStats(ActionCallback<RTCStatsReport> callback);

    public abstract void stop();
}
