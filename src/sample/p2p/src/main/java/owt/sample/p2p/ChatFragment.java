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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

public class ChatFragment extends Fragment implements View.OnClickListener {

    private ScrollView chatSV;
    private TextView chatTV;
    private EditText chatET;
    private Button sendBtn;
    private ChatFragmentListener mListener;
    private StringBuilder chatHistory = new StringBuilder();

    public ChatFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View mView = inflater.inflate(R.layout.fragment_chat, container, false);
        chatSV = mView.findViewById(R.id.chat_history);
        chatTV = mView.findViewById(R.id.chat_history_content);
        chatET = mView.findViewById(R.id.chat_content);
        sendBtn = mView.findViewById(R.id.send_btn);
        sendBtn.setOnClickListener(this);

        updateMessageView();
        return mView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ChatFragmentListener) {
            mListener = (ChatFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement LoginFragmentListener");
        }
    }

    @Override
    public void onClick(View v) {
        mListener.onSendMessage(chatET.getText().toString());
        chatET.setText("");
    }

    void onMessage(String peerId, String message) {
        String chat = "\n" + peerId + ": " + message;
        chatHistory.append(chat);
        updateMessageView();
    }

    private void updateMessageView() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> chatTV.setText(chatHistory.toString()));
        }
    }

    public interface ChatFragmentListener {
        void onSendMessage(String message);
    }

}
