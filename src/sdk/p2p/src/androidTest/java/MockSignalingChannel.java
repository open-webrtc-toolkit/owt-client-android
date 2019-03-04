import static owt.p2p.OwtP2PError.P2P_MESSAGING_TARGET_UNREACHABLE;

import static junit.framework.Assert.fail;

import android.util.Log;

import owt.base.ActionCallback;
import owt.base.OwtError;
import owt.p2p.SignalingChannelInterface;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

class MockSignalingChannel implements SignalingChannelInterface {
    private static final String TAG = MockSignalingChannel.class.getSimpleName();
    // global signaling pool.
    // key: uid
    private static HashMap<String, SignalingChannelObserver> observers = new HashMap<>();
    private static final Object lock = new Object();

    private String uid;
    private SignalingChannelObserver observer;

    @Override
    public void connect(String token, ActionCallback<String> callback) {
        JSONObject result = new JSONObject();
        try {
            JSONObject tokenObj = new JSONObject(token);
            String tokenId = tokenObj.getString("token");

            synchronized (lock) {
                if (observers.containsKey(tokenId)) {
                    Log.e(TAG, "Duplicated uid.");
                    callback.onFailure(new OwtError("Duplicated uid."));
                    return;
                }
                observers.put(tokenId, observer);
            }

            result.put("uid", tokenId);
            this.uid = tokenId;
            Log.v(TAG, "User: " + tokenId + " connected to server.");
            callback.onSuccess(result.toString());
        } catch (JSONException e) {
            fail(e.getMessage());
        }
    }

    @Override
    public void disconnect() {
        synchronized (lock) {
            observers.get(uid).onServerDisconnected();
            observers.remove(uid);
        }
    }

    @Override
    public void sendMessage(String peerId, String message, ActionCallback<Void> callback) {
        synchronized (lock) {
            if (!observers.containsKey(peerId)) {
                callback.onFailure(new OwtError(P2P_MESSAGING_TARGET_UNREACHABLE.value, ""));
                return;
            }
        }
        Log.v(TAG, uid + " sends message to " + peerId + ".");
        observers.get(peerId).onMessage(uid, message);
        callback.onSuccess(null);
    }

    @Override
    public void addObserver(SignalingChannelObserver observer) {
        this.observer = observer;
    }

    @Override
    public void removeObserver(SignalingChannelObserver observer) {
        // useless
    }
}
