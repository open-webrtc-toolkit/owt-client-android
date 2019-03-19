/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.test.util;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.InputStream;

public class Config {
    // User names for test cases.
    public static final String USER1_NAME = "User1";
    public static final String USER2_NAME = "User2";
    public static final String USER3_NAME = "User3";

    // P2P related variables.
    public static final String P2P_SERVER;
    public static final String P2P_SERVER_INCORRECT = "https://this.is.an.incorrect.ip";

    // Conference related variables.
    public static final String CONFERENCE_SERVER_HTTP;
    public static final String CONFERENCE_SERVER_HTTPS;
    public static final String CONFERENCE_SERVER_INCORRECT = "https://this.is.an.incorrect.ip";
    public static final String CONFERENCE_ROOM_ID;
    public static final String CONFERENCE_ROOM_ID_INCORRECT = "incorrect_room_id";
    public static final String PRESENTER_ROLE = "presenter";
    public static final String VIEWER_ROLE = "viewer";
    public static final String ERROR_ROLE = "error_role";
    public static final String VIDEO_ONLY_VIEWER_ROLE = "video_only_viewer";
    public static final String AUDIO_ONLY_PRESENTER_ROLE = "audio_only_presenter";

    // a token with incorrect values in the correct format.
    public static final String CONFERENCE_TOKEN_FAKE =
            "{'tokenId':'tokenId', 'host':'host'," + "'secure':false, 'signature':'signature'}";
    // a token with unexpected format.
    public static final String CONFERENCE_TOKEN_INCORRECT = "incorrect_token";
    public static final int MIXED_STREAM_SIZE;

    // messages for challenging the api.
    public static final String MESSAGE = "nihao";
    public static final String SPECIAL_CHARACTER = "!@#$%^&*()_+-=|}{[]?/><,.':;";
    public static final String CHINESE_CHARACTER = "中文";

    // try to avoid using SLEEP at the best effort.
    public static final int SLEEP = 2000;
    public static final int TIMEOUT = 5000;
    //p2p wait publication ended event
    public static final int TIMEOUT_LONG = 30000;

    public static final String RAW_STREAM_FILE;

    static {
        InputStream config = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                "config.xml");
        if (config == null) {
            throw new RuntimeException("config.xml expected.");
        }
        try {
            SAXReader reader = new SAXReader();
            Document document = reader.read(config);
            Element rootElement = document.getRootElement();

            Element p2pServerElement = rootElement.element("p2pServer");
            P2P_SERVER = p2pServerElement == null ? "" : p2pServerElement.getText();

            Element roomIdElement = rootElement.element("roomId");
            CONFERENCE_ROOM_ID = roomIdElement == null ? "" : roomIdElement.getText();

            Element mcuElement = rootElement.element("basicServer");
            CONFERENCE_SERVER_HTTP = mcuElement == null ? "" : mcuElement.getText();

            Element mcuHttpsElement = rootElement.element("basicServerHttps");
            CONFERENCE_SERVER_HTTPS = mcuHttpsElement == null ? "" : mcuHttpsElement.getText();

            Element mixedSizeElement = rootElement.element("mixedStreamSize");
            MIXED_STREAM_SIZE = mixedSizeElement == null ? 0 : Integer.valueOf(
                    mixedSizeElement.getText());

            Element rawStreamFileElement = rootElement.element("rawStreamFile");
            RAW_STREAM_FILE = rawStreamFileElement == null ? "" : rawStreamFileElement.getText();
        } catch (DocumentException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
