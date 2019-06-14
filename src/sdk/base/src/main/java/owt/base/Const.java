/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.base;

import android.os.Build;

///@cond
public class Const {
    public static final String LOG_TAG = "OWT";
    public static final String CLIENT_VERSION = "4.2.1";
    public static final String userAgent = "{" +
            "'runtime':{'name':'webrtc','version':'70'}," +
            "'sdk':{'type':'Android','version':" + CLIENT_VERSION + "}," +
            "'os':{'name':'Android', 'version':" + Build.VERSION.RELEASE + "}," +
            "'capabilities':{'continualIceGathering': true," +
                            "'unifiedPlan': true," +
                            "'streamRemovable': true}" +
            "}";
    public static final String PROTOCOL_VERSION = "1.0";
}
///@endcond
