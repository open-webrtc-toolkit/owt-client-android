/*
 * Copyright © 2017 Intel Corporation. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.intel.webrtc.p2p.sample;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

public class CallFragment extends Fragment implements View.OnClickListener {

    public interface CallFragmentListener {
        void onReady(SurfaceViewRenderer localRenderer, SurfaceViewRenderer remoteRenderer);

        void onPublishRequest();

        void onUnpublishRequest(boolean stop);
    }

    private static final String TAG = "ICS_P2P";
    private SurfaceViewRenderer fullRenderer, smallRenderer;
    private Button publishBtn, backBtn;

    private CallFragmentListener mListener;

    private float dX, dY;
    private boolean isPublishing = false;
    private View.OnTouchListener touchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (v.getId() == R.id.small_renderer) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = v.getX() - event.getRawX();
                        dY = v.getY() - event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        v.animate()
                         .x(event.getRawX() + dX)
                         .y(event.getRawY() + dY)
                         .setDuration(0)
                         .start();
                        break;
                    case MotionEvent.ACTION_UP:
                        v.animate()
                         .x(event.getRawX() + dX >= event.getRawY() + dY ? event.getRawX() + dX : 0)
                         .y(event.getRawX() + dX >= event.getRawY() + dY ? 0 : event.getRawY() + dY)
                         .setDuration(10)
                         .start();
                        break;
                }
            }
            return true;
        }
    };

    public CallFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View mView = inflater.inflate(R.layout.fragment_call, container, false);
        publishBtn = mView.findViewById(R.id.publish_btn);
        publishBtn.setText(isPublishing ? R.string.unpublish : R.string.publish);
        publishBtn.setOnClickListener(this);
        backBtn = mView.findViewById(R.id.back_btn);
        backBtn.setOnClickListener(this);
        fullRenderer = mView.findViewById(R.id.full_renderer);
        fullRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        fullRenderer.setEnableHardwareScaler(true);
        smallRenderer = mView.findViewById(R.id.small_renderer);
        smallRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        smallRenderer.setOnTouchListener(touchListener);
        smallRenderer.setEnableHardwareScaler(true);
        smallRenderer.setZOrderMediaOverlay(true);

        mListener.onReady(smallRenderer, fullRenderer);
        return mView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mListener = (CallFragmentListener) context;
    }

    @Override
    public void onDetach() {
        fullRenderer.release();
        fullRenderer = null;
        smallRenderer.release();
        smallRenderer = null;
        super.onDetach();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.publish_btn:
                publishBtn.setEnabled(false);
                if (publishBtn.getText().toString().equals("Publish")) {
                    mListener.onPublishRequest();
                } else {
                    mListener.onUnpublishRequest(false);
                    publishBtn.setText(R.string.publish);
                    isPublishing = false;
                    publishBtn.setEnabled(true);
                }
                break;
            case R.id.back_btn:
                mListener.onUnpublishRequest(true);
                isPublishing = false;
                break;
        }
    }

    void onPublished(final boolean succeed) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                isPublishing = succeed;
                publishBtn.setText(succeed ? R.string.unpublish : R.string.publish);
                publishBtn.setEnabled(true);
            }
        });
    }
}
