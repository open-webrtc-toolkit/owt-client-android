/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.conference;

import static owt.base.CheckCondition.DCHECK;
import static owt.conference.JsonUtils.getObj;
import static owt.conference.JsonUtils.getString;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * RemoteMixedStream represents the stream mixed by the conference server.
 */
public final class RemoteMixedStream extends RemoteStream {

    /**
     * View label of the RemoteMixedStream.
     */
    public final String view;
    private String activeAudioInput;
    /**
     * List of Region%s that indicates the video layout of the RemoteStream.
     */
    private List<Region> regions;

    RemoteMixedStream(JSONObject streamInfo) throws JSONException {
        super(streamInfo);
        JSONObject info = getObj(streamInfo, "info", true);
        view = getString(info, "label", "");
        activeAudioInput = getString(info, "activeInput", "");
        regions = new ArrayList<>();
        updateRegions(info.getJSONArray("layout"));
    }

    /**
     * Get the region information of this RemoteMixedStream.
     *
     * @return list of Region%s in this RemoteMixedStream.
     */
    public List<Region> regions() {
        return Collections.unmodifiableList(regions);
    }

    public String activeAudioInput() {
        return activeAudioInput;
    }

    void updateRegions(JSONArray regionsInfo) {
        regions.clear();
        try {
            for (int i = 0; i < regionsInfo.length(); i++) {
                JSONObject region = regionsInfo.getJSONObject(i);
                regions.add(new Region(region));
            }
        } catch (JSONException e) {
            DCHECK(e);
        }
        triggerLayoutChange();
    }

    private void triggerLayoutChange() {
        if (observers != null) {
            for (StreamObserver observer : observers) {
                if (observer instanceof RemoteMixedStreamObserver) {
                    ((RemoteMixedStreamObserver) observer).onLayoutChange(regions);
                }
            }
        }
    }

    void updateActiveInput(String activeInput) {
        this.activeAudioInput = activeInput;
        triggerActiveInputChange(activeInput);
    }

    private void triggerActiveInputChange(String activeInput) {
        if (observers != null) {
            for (StreamObserver observer : observers) {
                if (observer instanceof RemoteMixedStreamObserver) {
                    ((RemoteMixedStreamObserver) observer).onActiveAudioInputChange(activeInput);
                }
            }
        }
    }

    /**
     * Interface for observing remote mixed stream events.
     */
    public interface RemoteMixedStreamObserver extends StreamObserver {
        void onLayoutChange(List<Region> regions);

        void onActiveAudioInputChange(String activeAudioInput);
    }

    /**
     * Region information of a RemoteStream in this RemoteMixedStream.
     */
    public static class Region {
        /**
         * Id of this region.
         */
        public final String regionId;

        /**
         * Id of the RemoteStream corresponding to this Region.
         */
        public final String streamId;

        /**
         * Shape of this Region.
         */
        public final String shape;

        /**
         * Shape specific parameters of this Region.
         */
        public final HashMap<String, String> parameters = new HashMap<>();

        Region(JSONObject regionObj) {
            streamId = getString(regionObj, "stream", "");
            JSONObject region = getObj(regionObj, "region");
            if (region != null) {
                regionId = getString(region, "id");
                shape = getString(region, "shape");
                JSONObject area = getObj(region, "area");
                for (Iterator<String> it = area.keys(); it.hasNext(); ) {
                    String key = it.next();
                    String value = getString(area, key);
                    parameters.put(key, value);
                }
            } else {
                regionId = null;
                shape = null;
            }
        }
    }
}
