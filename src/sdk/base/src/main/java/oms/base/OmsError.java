/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package oms.base;

/**
 * OmsError includes error code and error message.
 */
public class OmsError {
    public final int errorCode;
    public final String errorMessage;

    public OmsError(String errorMessage) {
        this.errorCode = 0;
        this.errorMessage = errorMessage;
    }

    public OmsError(int errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
