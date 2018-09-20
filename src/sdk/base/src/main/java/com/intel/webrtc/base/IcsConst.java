/*
 * Intel License Header Holder
 */
package com.intel.webrtc.base;

import android.os.Build;

///@cond
public class IcsConst {
    public static final String LOG_TAG = "ICS";
    public static final String CLIENT_VERSION = "4.1";
    public static final String userAgent = "{" +
            "'runtime':{'name':'webrtc','version':'67'}," +
            "'sdk':{'type':'Android','version':" + CLIENT_VERSION + "}," +
            "'os':{'name':'Android', 'version':" + Build.VERSION.RELEASE + "}," +
            "'capabilities':{'continualIceGathering': true," +
                            "'unifiedPlan': false," +
                            "'streamRemovable': true}" +
            "}";
    public static final String PROTOCOL_VERSION = "1.0";
}
///@endcond