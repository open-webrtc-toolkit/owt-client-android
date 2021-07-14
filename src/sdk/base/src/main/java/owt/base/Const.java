/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.base;

import android.os.Build;

///@cond
public class Const {
    public static final String LOG_TAG = "OWT";
    public static final String CLIENT_VERSION = "5.0";
    public static final String userAgent = "{" +
            "'sdk':{'type':'Android','version':" + CLIENT_VERSION + "}," +
            "'capabilities':{'continualIceGathering': true," +
                            "'unifiedPlan': true," +
                            "'streamRemovable': true}" +
            "}";
    public static final String PROTOCOL_VERSION = "1.1";
}
///@endcond
