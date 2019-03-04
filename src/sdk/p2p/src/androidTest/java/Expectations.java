import android.util.Log;

import owt.p2p.P2PClient;
import owt.p2p.RemoteStream;

public class Expectations implements P2PClient.P2PClientObserver{
    private static final String TAG = Expectations.class.getSimpleName();

    @Override
    public void onServerDisconnected() {
        Log.v(TAG, "onServerDisconnected.");
    }

    @Override
    public void onStreamAdded(RemoteStream remoteStream) {
        Log.v(TAG, "onStreamAdded.");
    }

    @Override
    public void onDataReceived(String peerId, String message) {
        Log.v(TAG, "onDataReceived.");
    }
}
