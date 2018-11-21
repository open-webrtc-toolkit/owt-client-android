/*
 * Intel License Header Holder
 */
package oms.base;

import android.os.Build;

///@cond
public class Const {
    public static final String LOG_TAG = "OMS";
    public static final String CLIENT_VERSION = "4.1";
    public static final String userAgent = "{" +
            "'runtime':{'name':'webrtc','version':'67'}," +
            "'sdk':{'type':'Android','version':" + CLIENT_VERSION + "}," +
            "'os':{'name':'Android', 'version':" + Build.VERSION.RELEASE + "}," +
            "'capabilities':{'continualIceGathering': true," +
                            "'unifiedPlan': true," +
                            "'streamRemovable': true}" +
            "}";
    public static final String PROTOCOL_VERSION = "1.0";
}
///@endcond