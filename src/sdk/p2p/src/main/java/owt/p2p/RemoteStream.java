/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.p2p;

import org.webrtc.MediaStream;

public final class RemoteStream extends owt.base.RemoteStream {

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
