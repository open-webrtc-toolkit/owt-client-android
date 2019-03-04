/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.test.p2p.util;

import static owt.test.util.Config.SLEEP;
import static owt.test.util.Config.TIMEOUT;
import static owt.test.util.Config.TIMEOUT_LONG;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import owt.base.AudioEncodingParameters;
import owt.base.LocalStream;
import owt.base.MediaCodecs.AudioCodec;
import owt.base.MediaCodecs.VideoCodec;
import owt.base.VideoEncodingParameters;
import owt.p2p.P2PClient;
import owt.p2p.P2PClientConfiguration;
import owt.p2p.Publication;
import owt.p2p.RemoteStream;
import owt.test.util.FakeRenderer;
import owt.test.util.TestCallback;
import owt.test.util.TestObserver;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.RTCStatsReport;

import java.util.ArrayList;

public class P2PAction {

    /**
     * @param pcObserver observer for the P2PClient to be created.
     */
    public static P2PClient createPeerClient(P2PClientObserver pcObserver) {
        return createPeerClient(null, null, pcObserver);
    }

    /**
     * @param videoCodecs videoCodecs to be used for the P2PClient to be created.
     * @param audioCodecs audioCodecs to be used for the P2PClient to be created.
     * @param pcObserver observer for the P2PClient to be created.
     */
    public static P2PClient createPeerClient(ArrayList<VideoCodec> videoCodecs,
            ArrayList<AudioCodec> audioCodecs, P2PClientObserver pcObserver) {
        P2PClientConfiguration.Builder builder = P2PClientConfiguration.builder();
        if (videoCodecs != null) {
            for (VideoCodec videoCodec : videoCodecs) {
                builder.addVideoParameters(new VideoEncodingParameters(videoCodec));
            }
        }
        if (audioCodecs != null) {
            for (AudioCodec audioCodec : audioCodecs) {
                builder.addAudioParameters(new AudioEncodingParameters(audioCodec));
            }
        }
        P2PClient client = new P2PClient(builder.build(), new SocketSignalingChannel());
        if (pcObserver != null) {
            client.addObserver(pcObserver);
        }
        return client;
    }

    public static void connect(P2PClient client, String userName, String serverIp,
            boolean expectation) {
        JSONObject loginObject = new JSONObject();
        try {
            loginObject.put("host", serverIp);
            loginObject.put("token", userName);
            TestCallback<String> callback = new TestCallback<>();
            client.connect(loginObject.toString(), callback);
            assertTrue(callback.getResult(expectation, TIMEOUT_LONG));
            if (expectation) {
                JSONObject jsonObject = new JSONObject(callback.successCallbackResult);
                assertTrue(jsonObject.getString("uid").equals(userName));
            }
        } catch (JSONException e) {
            fail(e.getMessage());
        }
    }

    /**
     * @param peerObserver observer of another P2PClient rather than |client|.
     * @param checkRenderer If to check frame rendered.
     */
    public static Publication publish(P2PClient client, LocalStream stream, String desName,
            P2PClientObserver peerObserver, boolean checkRenderer, boolean expectation) {
        if (peerObserver != null) {
            peerObserver.clearStatus(1);
        }
        TestCallback<Publication> callback = new TestCallback<>();
        client.publish(desName, stream, callback);
        assertTrue(callback.getResult(expectation, TIMEOUT));
        if (expectation && peerObserver != null) {
            assertTrue(peerObserver.getResultForStreamAdded(TIMEOUT));
            RemoteStream remoteStream = peerObserver.remoteStreams.get(
                    peerObserver.remoteStreams.size() - 1);
            assertNotNull(remoteStream);
            assertTrue(stream.hasVideo() == remoteStream.hasVideo());
            assertTrue(stream.hasAudio() == remoteStream.hasAudio());
            if (checkRenderer) {
                FakeRenderer renderer = new FakeRenderer();
                remoteStream.attach(renderer);
                assertTrue(stream.hasVideo() == (renderer.getFramesRendered(SLEEP) != 0));
            }
        }
        return callback.successCallbackResult;
    }

    /**
     * @param peerObserver observer of another P2PClient rather than |client|.
     */
    public static void send(P2PClient client, String msg, String receiver,
            P2PClientObserver peerObserver, boolean expectation) {
        int initDataNumber = 0;
        if (peerObserver != null) {
            peerObserver.clearStatus(1);
            initDataNumber = peerObserver.dataReceived.size();
        }
        TestCallback<Void> callback = new TestCallback<>();
        client.send(receiver, msg, callback);
        assertTrue(callback.getResult(expectation, TIMEOUT));
        if (expectation && peerObserver != null) {
            assertTrue(peerObserver.getResultForDataReceived(TIMEOUT));
            assertEquals(msg, peerObserver.dataReceived.get(initDataNumber));
            assertEquals(client.id(), peerObserver.dataSenders.get(initDataNumber));
        }
    }

    /**
     * @param peerObserver observer of another P2PClient rather than |client|.
     */
    public static void disconnect(P2PClient client, P2PClientObserver peerObserver) {
        if (peerObserver != null) {
            peerObserver.clearStatus(1);
        }
        client.disconnect();
        if (peerObserver != null) {
            assertTrue(peerObserver.getResultForServerDisconnected(TIMEOUT));
        }
    }

    public static RTCStatsReport getStats(P2PClient client, String peerId,
            boolean expectation) {
        TestCallback<RTCStatsReport> callback = new TestCallback<>();
        client.getStats(peerId, callback);
        assertTrue(callback.getResult(expectation, TIMEOUT));
        return callback.successCallbackResult;
    }

    public static RTCStatsReport getStats(Publication publication, boolean expectation) {
        TestCallback<RTCStatsReport> callback = new TestCallback<>();
        publication.getStats(callback);
        assertTrue(callback.getResult(expectation, TIMEOUT));
        return callback.successCallbackResult;
    }

    /**
     * @param peerObserver observer of which StreamObserver will be watched.
     * @param index index of RemoteStream published by |publication|, on P2PClient whose observer
     * is |peerObserver|.
     */
    public static void stop(Publication publication, P2PClientObserver peerObserver, int index,
            boolean checkPublicationObserver) {
        TestObserver publicationObserver = new TestObserver();
        publication.addObserver(publicationObserver);
        publication.stop();
        if (checkPublicationObserver) {
            assertTrue(publicationObserver.getResult(TIMEOUT));
        }
        if (peerObserver != null) {
            peerObserver.remoteStreamObservers.get(peerObserver.remoteStreams.get(index)).getResult(
                    TIMEOUT);
        }
        publication.removeObserver(publicationObserver);
    }

    /**
     * @param peerObserver observer of another P2PClient rather than |client|.
     * @param publications Publications to watch onEnded event.
     */
    public static void stop(P2PClient client, String peer, P2PClientObserver peerObserver,
            Publication... publications) {
        ArrayList<TestObserver> pubObservers = new ArrayList<>();
        for (Publication publication : publications) {
            TestObserver publicationObserver = new TestObserver();
            publication.addObserver(publicationObserver);
            pubObservers.add(publicationObserver);
        }
        client.stop(peer);
        for (TestObserver pubObserver : pubObservers) {
            assertTrue(pubObserver.getResult(TIMEOUT_LONG));
        }
        if (peerObserver != null) {
            checkRemoteStreamEnded(peerObserver.remoteStreamObservers.values());
        }
        int i = 0;
        for (Publication publication : publications) {
            publication.removeObserver(pubObservers.get(i));
            i++;
        }
    }

    public static void checkRemoteStreamEnded(Iterable<TestObserver> streamObservers) {
        for (TestObserver streamObserver : streamObservers) {
            assertTrue(streamObserver.getResult(TIMEOUT));
        }
    }
}
