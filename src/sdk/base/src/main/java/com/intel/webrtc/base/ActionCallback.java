/*
 * Intel License Header Holder
 */
package com.intel.webrtc.base;

/**
 * Callback interface for receiving information about an action execution result.
 */
public interface ActionCallback<T> {

    /**
     * Called when it has succeeded to execute this action.
     *
     * @param result result returned by this action.
     */
    void onSuccess(final T result);

    /**
     * Called when it has failed to execute this action.
     *
     * @param error error occurred when executes this action.
     */
    void onFailure(final IcsError error);
}
