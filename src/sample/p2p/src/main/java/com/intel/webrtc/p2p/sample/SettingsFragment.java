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
