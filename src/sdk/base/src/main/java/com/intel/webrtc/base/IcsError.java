/*
 * Intel License Header Holder
 */
package com.intel.webrtc.base;

/**
 * IcsError includes error code and error message.
 */
public class IcsError {
    public final int errorCode;
    public final String errorMessage;

    public IcsError(String errorMessage) {
        this.errorCode = 0;
        this.errorMessage = errorMessage;
    }

    public IcsError(int errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
