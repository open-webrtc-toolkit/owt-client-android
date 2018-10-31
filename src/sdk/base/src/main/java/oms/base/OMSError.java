/*
 * Intel License Header Holder
 */
package oms.base;

/**
 * OMSError includes error code and error message.
 */
public class OMSError {
    public final int errorCode;
    public final String errorMessage;

    public OMSError(String errorMessage) {
        this.errorCode = 0;
        this.errorMessage = errorMessage;
    }

    public OMSError(int errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
