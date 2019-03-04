/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.base;

/**
 * OwtError includes error code and error message.
 */
public class OwtError {
    public final int errorCode;
    public final String errorMessage;

    public OwtError(String errorMessage) {
        this.errorCode = 0;
        this.errorMessage = errorMessage;
    }

    public OwtError(int errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
