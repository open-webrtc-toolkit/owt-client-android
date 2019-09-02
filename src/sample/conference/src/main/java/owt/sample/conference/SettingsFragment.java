/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.sample.conference;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class SettingsFragment extends Fragment implements RadioGroup.OnCheckedChangeListener {
    boolean cameraFront = true, resolutionVGA = true;
    boolean VideoEncodingVP8 = false, VideoEncodingVP9 = false, VideoEncodingH264 = false, VideoEncodingH265 = false;
    private RadioGroup cameraRG, resolutionRG, videoCodecRG;
    private EditText roomIdEditText;

    public SettingsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View mView = inflater.inflate(R.layout.fragment_settings, container, false);
        cameraRG = mView.findViewById(R.id.camera_rg);
        cameraRG.setOnCheckedChangeListener(this);
        RadioButton frontCam = mView.findViewById(R.id.front);
        frontCam.setChecked(true);

        resolutionRG = mView.findViewById(R.id.resolution_rg);
        resolutionRG.setOnCheckedChangeListener(this);
        RadioButton vgaReso = mView.findViewById(R.id.vga);
        vgaReso.setChecked(true);

        videoCodecRG = mView.findViewById(R.id.pub_videoCodec_rg);
        videoCodecRG.setOnCheckedChangeListener(this);
        roomIdEditText = mView.findViewById(R.id.room_id);
        return mView;
    }

    String getRoomId() {
        return roomIdEditText.getText().toString();
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (group == cameraRG) {
            switch (checkedId) {
                case R.id.front:
                    cameraFront = true;
                    break;
                case R.id.back:
                    cameraFront = false;
                    break;
            }
        } else if (group == resolutionRG) {
            switch (checkedId) {
                case R.id.vga:
                    resolutionVGA = true;
                    break;
                case R.id.p720:
                    resolutionVGA = false;
                    break;
            }
        } else if (group == videoCodecRG) {
            switch (checkedId) {
                case R.id.pub_vp8:
                    VideoEncodingVP8 = true;
                    VideoEncodingVP9 = false;
                    VideoEncodingH264 = false;
                    VideoEncodingH265= false;
                    break;
                case R.id.pub_vp9:
                    VideoEncodingVP8 = false;
                    VideoEncodingVP9 = true;
                    VideoEncodingH264 = false;
                    VideoEncodingH265= false;
                    break;
                case R.id.pub_h264:
                    VideoEncodingVP8 = false;
                    VideoEncodingVP9 = false;
                    VideoEncodingH264 = true;
                    VideoEncodingH265= false;
                    break;
                case R.id.pub_h265:
                    VideoEncodingVP8 = false;
                    VideoEncodingVP9 = false;
                    VideoEncodingH264 = false;
                    VideoEncodingH265= true;
                    break;
            }
        }
    }
}
