/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.p2p;

public enum OwtP2PError {

    P2P_CONN_SERVER_UNKNOWN(2100),
    P2P_CONN_SERVER_UNAVAILABLE(2101),
    P2P_CONN_SERVER_BUSY(2102),
    P2P_CONN_SERVER_NOT_SUPPORTED(2103),
    P2P_CONN_CLIENT_UNKNOWN(2110),
    P2P_CONN_CLIENT_NOT_INITIALIZED(2111),
    P2P_CONN_AUTH_UNKNOWN(2120),
    P2P_CONN_AUTH_FAILED(2121),
    P2P_MESSAGING_TARGET_UNREACHABLE(2201),
    P2P_CLIENT_DENIED(2202),
    P2P_CLIENT_UNKNOWN(2400),
    P2P_CLIENT_ILLEGAL_ARGUMENT(2402),
    P2P_CLIENT_INVALID_STATE(2403),
    P2P_CLIENT_NOT_ALLOWED(2404),
    P2P_WEBRTC_SDP(2502),
    P2P_WEBRTC_ICE_POLICY_UNSUPPORTED(2503),
    P2P_CODE_UNKNOWN(9999);

    public final int value;

    OwtP2PError(final int value) {
        this.value = value;
    }

    public static OwtP2PError get(int value) {
        switch (value) {
            case 2100:
                return P2P_CONN_SERVER_UNKNOWN;
            case 2101:
                return P2P_CONN_SERVER_UNAVAILABLE;
            case 2102:
                return P2P_CONN_SERVER_BUSY;
            case 2103:
                return P2P_CONN_SERVER_NOT_SUPPORTED;
            case 2121:
                return P2P_CONN_AUTH_FAILED;
            case 2201:
                return P2P_MESSAGING_TARGET_UNREACHABLE;
            default:
                return P2P_CODE_UNKNOWN;
        }
    }
}
