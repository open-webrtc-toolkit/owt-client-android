/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.sample.p2p;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class SettingsFragment extends Fragment implements RadioGroup.OnCheckedChangeListener {
    boolean cameraFront = true, resolutionVGA = true;
    private RadioGroup cameraRG, resolutionRG;

    public SettingsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        return mView;
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
        }
    }
}
