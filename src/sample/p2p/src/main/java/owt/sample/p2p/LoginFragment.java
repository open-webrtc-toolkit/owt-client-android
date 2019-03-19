/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.sample.p2p;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class LoginFragment extends Fragment implements View.OnClickListener {

    private Button connectBtn, callBtn;
    private EditText serverText, myIdText, peerIdText;
    private TextView errorTV;
    private LinearLayout peerContainer;
    private LoginFragmentListener mListener;
    private boolean loggedIn = false;

    public LoginFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View mView = inflater.inflate(R.layout.fragment_login, container, false);
        connectBtn = mView.findViewById(R.id.connect_btn);
        connectBtn.setText(loggedIn ? R.string.disconnect : R.string.connect);
        connectBtn.setOnClickListener(this);
        callBtn = mView.findViewById(R.id.call_btn);
        callBtn.setOnClickListener(this);
        serverText = mView.findViewById(R.id.server_url);
        serverText.setText("http://example.com");
        myIdText = mView.findViewById(R.id.my_id);
        peerIdText = mView.findViewById(R.id.peer_id);
        peerContainer = mView.findViewById(R.id.peer_container);
        peerContainer.setVisibility(loggedIn ? View.VISIBLE : View.INVISIBLE);
        errorTV = mView.findViewById(R.id.error_msg);
        return mView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof LoginFragmentListener) {
            mListener = (LoginFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement LoginFragmentListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onClick(View v) {
        InputMethodManager inputManager = (InputMethodManager)
                getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

        if (getActivity().getCurrentFocus() != null) {
            inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }
        switch (v.getId()) {
            case R.id.connect_btn:
                if (loggedIn) {
                    loggedIn = false;
                    mListener.onDisconnectRequest();
                    connectBtn.setText(R.string.connect);
                    peerContainer.setVisibility(View.INVISIBLE);
                } else {
                    connectBtn.setEnabled(false);
                    errorTV.setText("");
                    mListener.onConnectRequest(serverText.getText().toString(),
                            myIdText.getText().toString());
                }
                break;
            case R.id.call_btn:
                mListener.onCallRequest(peerIdText.getText().toString());
                break;
        }
    }

    void onConnected() {
        loggedIn = true;
        getActivity().runOnUiThread(() -> {
            connectBtn.setText(R.string.disconnect);
            connectBtn.setEnabled(true);
            errorTV.setText("");
            peerContainer.setVisibility(View.VISIBLE);
        });
    }

    void onConnectFailed(final String errorMsg) {
        loggedIn = false;
        getActivity().runOnUiThread(() -> {
            connectBtn.setEnabled(true);
            errorTV.setText(errorMsg);
        });
    }

    public interface LoginFragmentListener {
        void onConnectRequest(String server, String myId);

        void onDisconnectRequest();

        void onCallRequest(String peerId);
    }
}
