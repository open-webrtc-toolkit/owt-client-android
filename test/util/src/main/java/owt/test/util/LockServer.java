package owt.test.util;

import static junit.framework.Assert.assertTrue;

import static owt.test.util.Config.IATF_SERVER;
import static owt.test.util.Config.IATF_WAIT_TIME;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import okhttp3.OkHttpClient;


public class LockServer {
    private final static String TAG = "iatf_test_util";
    public String controlMsg = "";
    public String targetcontrolMsg = "";
    public String targetworkFlowMsgType = "";
    public String targetworkFlowMsgData = "";
    public String workFlowMsgType = "";
    public String workFlowMsgData = "";
    private Socket mSocket = null;
    private final ExecutorService callbackExecutor = Executors.newSingleThreadExecutor();
    static SSLContext sslContext;
    static HostnameVerifier hostnameVerifier;
    private CountDownLatch latch = new CountDownLatch(1);
    public Object lockControl = new Object();
    public Object lockWorkFlow = new Object();

    public void initStatus(){
        this.controlMsg = "";
        this.targetcontrolMsg = "";
        this.targetworkFlowMsgType = "";
        this.targetworkFlowMsgData = "";
        this.workFlowMsgData = "";
        this.workFlowMsgType = "";
    }

    public static void setUpINSECURESSLContext() {
        hostnameVerifier = (hostname, session) -> true;

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

    private final Emitter.Listener connectedCallback = args -> callbackExecutor.execute(() -> {
        Log.d(TAG, "Socket connected.");
    });

    private final Emitter.Listener iatfControlCallback = args -> callbackExecutor.execute(() -> {
        synchronized (lockControl) {
            JSONObject msg = (JSONObject) args[0];
            Log.d(TAG, "iatf-control msg: " + msg.toString());
            try {
                controlMsg = msg.getString("type");
                if (latch.getCount() != 0 && targetcontrolMsg.equals(controlMsg)) {
                    latch.countDown();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    });

    private final Emitter.Listener iatfWorkFlowCallback = args -> callbackExecutor.execute(() -> {
        synchronized (lockWorkFlow) {
            JSONObject workFlowMsg = (JSONObject) args[0];
            Log.d(TAG, "iatf-workflow msg: " + workFlowMsg.toString());
            try {
                workFlowMsgData = workFlowMsg.getString("data");
                workFlowMsgType = workFlowMsg.getString("type");
                if (latch.getCount() != 0 && targetworkFlowMsgData.equals(workFlowMsgData)
                        && targetworkFlowMsgType.equals(workFlowMsgType)) {
                    latch.countDown();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    });

    public void connect(String taskId, String role) {
        try {
            setUpINSECURESSLContext();
            IO.Options opt = new IO.Options();
            opt.forceNew = true;
            opt.reconnection = true;
            opt.secure = true;
            opt.query = "taskId=" + taskId + "&role=" + role + "&type=Android";
            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
            if (sslContext != null) {
                clientBuilder.sslSocketFactory(sslContext.getSocketFactory());
            }
            if (hostnameVerifier != null) {
                clientBuilder.hostnameVerifier(hostnameVerifier);
            }
            OkHttpClient httpClient = clientBuilder.build();
            opt.callFactory = httpClient;
            opt.webSocketFactory = httpClient;
            mSocket = IO.socket(IATF_SERVER, opt);
            mSocket.on(Socket.EVENT_CONNECT, connectedCallback)
                    .on("iatf-control", iatfControlCallback)
                    .on("iatf-workflow", iatfWorkFlowCallback);
            mSocket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void disconnect(){
        mSocket.disconnect();
    }

    public void notifyControlLock(String lockTpye, JSONObject lockDate) {
        JSONObject msg = new JSONObject();
        try {
            msg.put("type", lockTpye);
            msg.put("message",lockDate);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mSocket.emit("iatf-control", msg);
    }

    public void notifyWorkflowLock(String lockTpye, String lockDate) {
        JSONObject msg = new JSONObject();
        try {
            msg.put("type", lockTpye);
            msg.put("data",lockDate);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mSocket.emit("iatf-workflow", msg);
    }

    public void waitControlLock(String lock) {
        synchronized (lockControl) {
            targetcontrolMsg = lock;
            latch = new CountDownLatch(1);
            if (controlMsg.equals(lock)) {
                latch.countDown();
            }
        }
        assertTrue("can not get " + lock, waitLock());
    }

    public void waitWorkflowLock(String lockType, String lockData) {
        synchronized (lockWorkFlow) {
            targetworkFlowMsgType = lockType;
            targetworkFlowMsgData = lockData;
            latch = new CountDownLatch(1);
            if (workFlowMsgType.equals(lockType) && workFlowMsgData.equals(lockData)) {
                latch.countDown();
            }
        }
        assertTrue(
                "can not get workFlowType:" + lockType + " workFlowData:" + lockData,
                waitLock());
    }


    private boolean waitLock() {
        try {
            if (latch.await(Integer.valueOf(IATF_WAIT_TIME), TimeUnit.MILLISECONDS)) {
                return true;
            } else {
                Log.w(TAG, "Timeout on Resultable.getResult.");
            }
        } catch (InterruptedException e) {
            Log.w(TAG, "InterruptedException during latch.await");
        }
        return false;
    }
}
