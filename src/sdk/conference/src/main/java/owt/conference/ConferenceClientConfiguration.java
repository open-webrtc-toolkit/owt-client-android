/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.conference;

import owt.base.ClientConfiguration;

import org.webrtc.PeerConnection;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

/**
 * Configuration for ConferenceClient.
 */
public final class ConferenceClientConfiguration extends ClientConfiguration {

    SSLContext sslContext = null;
    HostnameVerifier hostnameVerifier = null;

    private ConferenceClientConfiguration(PeerConnection.RTCConfiguration configuration) {
        super(configuration);
    }

    /**
     * Get a Builder for creating a ConferenceClientConfiguration.
     *
     * @return Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for building up a ConferenceClientConfiguration.
     */
    public static class Builder {
        private SSLContext sslContext = null;
        private HostnameVerifier hostnameVerifier = null;
        private PeerConnection.RTCConfiguration rtcConfiguration = null;

        Builder() {
        }

        /**
         * Set up the SSL context for the underlying socket.io communication.
         *
         * @param sslContext SSLContext to be set.
         * @return Builder
         */
        public Builder setSSLContext(SSLContext sslContext) {
            this.sslContext = sslContext;
            return this;
        }

        /**
         * Set up the hostname verifier for the underlying socket.io communication.
         *
         * @param hostnameVerifier HostnameVerifier to be set.
         * @return Builder
         */
        public Builder setHostnameVerifier(HostnameVerifier hostnameVerifier) {
            this.hostnameVerifier = hostnameVerifier;
            return this;
        }

        /**
         * Set up the RTCConfiguration for the underlying WebRTC PeerConnection
         *
         * @param rtcConfiguration RTCConfiguration to be set.
         * @return Builder
         */
        public Builder setRTCConfiguration(PeerConnection.RTCConfiguration rtcConfiguration) {
            this.rtcConfiguration = rtcConfiguration;
            return this;
        }

        /**
         * Build up the ConferenceClientConfiguration.
         *
         * @return ConferenceClientConfiguration.
         */
        public ConferenceClientConfiguration build() {
            ConferenceClientConfiguration configuration =
                    new ConferenceClientConfiguration(rtcConfiguration);
            configuration.sslContext = sslContext;
            configuration.hostnameVerifier = hostnameVerifier;
            return configuration;
        }
    }
}
