package com.intel.webrtc.conference;

import com.intel.webrtc.base.MediaConstraints;

///@cond
interface MuteEventObserver {
    void onStatusUpdated(MediaConstraints.TrackKind trackKind, boolean active);
}
///@endcond
