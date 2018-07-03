# Intel Collaboration Suite for WebRTC Android Samples

## Run samples with Android Studio

In order to run sample project in the release package, dependency files need to be imported first.

    1. Open Android Studio and open the sample project by 'Open File or Project'.
    2. Import .aar dependencies by creating a new module 'Import .JAR/.AAR Package'.
    3. Import libwebrtc.jar dependency by copying it into directory src/sample/utils/libs
    4. Import .so dependencies by copying them into directory src/sample/utils/src/main/jniLibs.

### SSL/TLS

By default, conference sample trusts all certificates and doesn't verify hostname of the server, which is INSECURE yet easy for debugging. 
Please don't include conference sample default behavior in the production code. To set up a secure environment, follow steps below.

##### Server side uses a self-signed certificate
    1. Substitute conferenceSample/src/res/raw/democert.crt with the cert that server uses.
    2. Use getTokenSSLSelfsigned() to fetch the token from server in WooGeenActivity.
    3. Pass the SSLContext to join() by setting the ConnectionOptions.

##### Server side uses a certificate issued by a well-known CA
    1. Use getToken() to fetch the token from server in WooGeenActivity.
    2. No need to set up SSLContext or passing it to join().

##### Trust all certificates (WARNING: INSECURE)
This is the default behavior in conference sample. For easily debugging without configuring any certificates, conferenceSample can be set to trust all certificates, which means the Android app will not verify the certificate and hostname from server.

    1. Use getTokenSSLINSECURE() to fetch the token from server in WooGeenActivity.
    2. Pass the SSLContext and HostnameVerifier to join() by setting the ConnectionOptions.

Otherwise if you wouldn't like to use HTTPS/SSL, you should disable the ssl at the server side.

## P2P Sample

P2P sample connects to PeerServer and then it can start a session with other clients connected to the PeerServer with Intel CS for WebRTC client SDK.
