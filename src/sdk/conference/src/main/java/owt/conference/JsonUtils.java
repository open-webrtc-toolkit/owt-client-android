/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.conference;

import static owt.base.CheckCondition.DCHECK;

import org.json.JSONException;
import org.json.JSONObject;

final class JsonUtils {

    static String getString(JSONObject jsonObject, String key) {
        return getString(jsonObject, key, null);
    }

    static String getString(JSONObject jsonObject, String key, String defaultVal) {
        if (jsonObject == null) {
            return defaultVal;
        }

        try {
            if (defaultVal == null) {
                DCHECK(jsonObject.has(key));
            }
            return jsonObject.has(key) ? jsonObject.getString(key) : defaultVal;
        } catch (JSONException e) {
            return defaultVal;
        }
    }

    static int getInt(JSONObject jsonObject, String key, int defaultVal) {
        if (jsonObject == null) {
            return defaultVal;
        }

        int result = defaultVal;
        try {
            result = jsonObject.has(key) ? jsonObject.getInt(key) : defaultVal;
        } catch (JSONException e) {
            DCHECK(e);
        }

        return result;
    }

    static JSONObject getObj(JSONObject jsonObject, String key) {
        return getObj(jsonObject, key, false);
    }

    // mandatory true means the key is expected in the jsonObject
    static JSONObject getObj(JSONObject jsonObject, String key, boolean mandatory) {
        DCHECK(jsonObject);
        DCHECK(key);

        try {
            if (mandatory) {
                DCHECK(jsonObject.has(key));
            }
            return jsonObject.getJSONObject(key);
        } catch (JSONException e) {
            //mandatory false, return null
            return null;
        }
    }
}
