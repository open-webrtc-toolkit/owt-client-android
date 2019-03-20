/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.sample.p2p;

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

    private static final String TAG = "OWT_P2P";
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
                                .x(event.getRawX() + dX >= event.getRawY() + dY ? event.getRawX()
                                        + dX : 0)
                                .y(event.getRawX() + dX >= event.getRawY() + dY ? 0
                                        : event.getRawY() + dY)
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
        getActivity().runOnUiThread(() -> {
            isPublishing = succeed;
            publishBtn.setText(succeed ? R.string.unpublish : R.string.publish);
            publishBtn.setEnabled(true);
        });
    }

    public interface CallFragmentListener {
        void onReady(SurfaceViewRenderer localRenderer, SurfaceViewRenderer remoteRenderer);

        void onPublishRequest();

        void onUnpublishRequest(boolean stop);
    }
}
