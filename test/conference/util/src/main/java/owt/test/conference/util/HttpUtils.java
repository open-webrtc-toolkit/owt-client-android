/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.test.conference.util;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public final class HttpUtils {
    private final static String TAG = "owt_conference_test";
    static SSLContext sslContext;
    static HostnameVerifier hostnameVerifier;

    public static String getToken(String basicServer, String role, String username, String roomId) {
        JSONObject joinBody = new JSONObject();
        try {
            joinBody.put("role", role);
            joinBody.put("username", username);
            joinBody.put("room", roomId.equals("") ? "" : roomId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String uri = basicServer + "/createToken/";
        String token = request(uri, "POST", joinBody.toString(), false);
        Log.d(TAG, "getToken: " + token);
        return token;
    }

    public static String getTokenSSLINSECURE(String basicServer, String role, String username,
            String roomId) {
        JSONObject joinBody = new JSONObject();
        try {
            joinBody.put("role", role);
            joinBody.put("username", username);
            joinBody.put("room", roomId.equals("") ? "" : roomId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String uri = basicServer + "/createToken/";
        String token = request(uri, "POST", joinBody.toString(), true);
        Log.d(TAG, "getTokenSSLINSECURE: " + token);
        return token;
    }

    public static void setUpINSECURESSLContext() {
        hostnameVerifier = new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };

        TrustManager[] trustManagers = new TrustManager[]{new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws
                    CertificateException {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws
                    CertificateException {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        }};

        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagers, null);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }
    }


    public static String request(String uri, String method, String body, boolean isSecure) {
        StringBuilder response = new StringBuilder("");
        URL url;
        HttpURLConnection httpURLConnection = null;
        try {
            url = new URL(uri);
            if (isSecure) {
                httpURLConnection = (HttpsURLConnection) url.openConnection();
                ((HttpsURLConnection) httpURLConnection).setSSLSocketFactory(
                        sslContext.getSocketFactory());
                if (hostnameVerifier != null) {
                    //******************WARNING****************
                    //DO NOT IMPLEMENT THIS IN PRODUCTION CODE
                    //*****************************************
                    ((HttpsURLConnection) httpURLConnection).setHostnameVerifier(hostnameVerifier);
                }
            } else {
                httpURLConnection = (HttpURLConnection) url.openConnection();
            }
            httpURLConnection.setDoInput(true);
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setRequestProperty("Content-Type", "application/json");
            httpURLConnection.setRequestProperty("Accept", "application/json");
            httpURLConnection.setConnectTimeout(5000);
            httpURLConnection.setRequestMethod(method);

            DataOutputStream out = new DataOutputStream(httpURLConnection.getOutputStream());
            out.write(body.getBytes("UTF-8"));
            out.flush();
            out.close();

            if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(httpURLConnection.getInputStream()));
                String lines;
                while ((lines = reader.readLine()) != null) {
                    lines = new String(lines.getBytes(), "UTF-8");
                    response.append(lines);
                }
                reader.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }
        return response.toString();
    }

}
