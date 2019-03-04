/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.conference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Information of the conference.
 */
public final class ConferenceInfo {
    // package access here, as the getter methods return an immutable list.
    final List<Participant> participants;
    private final Object parLock = new Object();
    final List<RemoteStream> remoteStreams;
    private final Object streamLock = new Object();
    private String id;
    private Participant self;

    ConferenceInfo(JSONObject conferenceInfo) throws JSONException {
        participants = Collections.synchronizedList(new ArrayList<>());
        remoteStreams = Collections.synchronizedList(new ArrayList<>());
        updateInfo(conferenceInfo);
    }

    private void updateInfo(JSONObject conferenceInfo) throws JSONException {

        JSONObject room = conferenceInfo.getJSONObject("room");
        id = room.getString("id");

        // dealing with participants
        JSONArray participantsInfo = room.getJSONArray("participants");
        for (int i = 0; i < participantsInfo.length(); i++) {
            JSONObject participantInfo = participantsInfo.getJSONObject(i);
            Participant participant = new Participant(participantInfo);
            synchronized (parLock) {
                participants.add(participant);
            }

            if (participant.id.equals(conferenceInfo.getString("id"))) {
                self = participant;
            }
        }

        // dealing with remote streams
        JSONArray streamsInfo = room.getJSONArray("streams");
        for (int i = 0; i < streamsInfo.length(); i++) {
            JSONObject streamInfo = streamsInfo.getJSONObject(i);
            RemoteStream remoteStream;
            if (streamInfo.getString("type").equals("mixed")) {
                remoteStream = new RemoteMixedStream(streamInfo);
            } else {
                remoteStream = new RemoteStream(streamInfo);
            }
            synchronized (streamLock) {
                remoteStreams.add(remoteStream);
            }
        }
    }

    /**
     * Conference room id.
     *
     * @return room id.
     */
    public String id() {
        return id;
    }

    /**
     * Information of the ConferenceClient itself.
     *
     * @return Participant information of the ConferenceClient itself.
     */
    public Participant self() {
        return self;
    }

    /**
     * Get the information of all the Participant%s in the conference.
     *
     * @return list of Participant%s in the conference.
     */
    public List<Participant> getParticipants() {
        synchronized (parLock) {
            return Collections.unmodifiableList(participants);
        }
    }

    /**
     * Get the information of all the RemoteStream%s in the conference.
     *
     * @return list of the RemoteStream%s in the conference.
     */
    public List<RemoteStream> getRemoteStreams() {
        synchronized (streamLock) {
            return Collections.unmodifiableList(remoteStreams);
        }
    }

}
