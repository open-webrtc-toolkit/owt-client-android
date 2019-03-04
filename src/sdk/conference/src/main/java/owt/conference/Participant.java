/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.conference;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Information of the participant in the conference.
 */
public final class Participant {

    /**
     * Id of this Participant instance.
     */
    public final String id;
    /**
     * Role of the participant.
     */
    public final String role;
    /**
     * User id of the participant.
     */
    public final String userId;
    private List<ParticipantObserver> observers;

    Participant(JSONObject participantInfo) throws JSONException {
        id = participantInfo.getString("id");
        role = participantInfo.getString("role");
        userId = participantInfo.getString("user");
    }

    /**
     * Add a ParticipantObserver.
     *
     * @param observer ParticipantObserver to be added.
     */
    public void addObserver(ParticipantObserver observer) {
        if (observers == null) {
            observers = new ArrayList<>();
        }
        observers.add(observer);
    }

    /**
     * Remove a ParticipantObserver.
     *
     * @param observer ParticipantObserver to be removed.
     */
    public void removeObserver(ParticipantObserver observer) {
        if (observers != null) {
            observers.remove(observer);
        }
    }

    void onLeft() {
        if (observers != null) {
            for (ParticipantObserver observer : observers) {
                observer.onLeft();
            }
        }
    }

    /**
     * Interface for observing participant events.
     */
    public interface ParticipantObserver {
        /**
         * Called upon the participant leaves the conference.
         */
        void onLeft();
    }
}
