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
package oms.sample.conference;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.webrtc.RTCStats;
import org.webrtc.RTCStatsReport;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

import java.math.BigInteger;
import java.util.Map;


public class VideoFragment extends Fragment {

    private VideoFragmentListener listener;
    private SurfaceViewRenderer fullRenderer, smallRenderer;
    private TextView statsInView, statsOutView;
    private float dX, dY;
    private BigInteger lastBytesSent = BigInteger.valueOf(0);
    private BigInteger lastBytesReceived = BigInteger.valueOf(0);
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

    public VideoFragment() {
    }

    public void setListener(VideoFragmentListener listener) {
        this.listener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View mView = inflater.inflate(R.layout.fragment_video, container, false);

        statsInView = mView.findViewById(R.id.stats_in);
        statsInView.setVisibility(View.GONE);
        statsOutView = mView.findViewById(R.id.stats_out);
        statsOutView.setVisibility(View.GONE);

        fullRenderer = mView.findViewById(R.id.full_renderer);
        smallRenderer = mView.findViewById(R.id.small_renderer);

        smallRenderer.init(((MainActivity) getActivity()).rootEglBase.getEglBaseContext(), null);
        smallRenderer.setMirror(true);
        smallRenderer.setOnTouchListener(touchListener);
        smallRenderer.setEnableHardwareScaler(true);
        smallRenderer.setZOrderMediaOverlay(true);

        fullRenderer.init(((MainActivity) getActivity()).rootEglBase.getEglBaseContext(), null);
        fullRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        fullRenderer.setEnableHardwareScaler(true);
        fullRenderer.setZOrderMediaOverlay(true);

        listener.onRenderer(smallRenderer, fullRenderer);
        clearStats(true);
        clearStats(false);
        return mView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    void clearStats(boolean outbound) {
        final TextView statsView = outbound ? statsOutView : statsInView;
        if (outbound) {
            lastBytesSent = BigInteger.valueOf(0);
        } else {
            lastBytesReceived = BigInteger.valueOf(0);
        }
        final String statsReport = (outbound ? "\n--- OUTBOUND ---" : "\n--- INBOUND ---")
                + "\nCodec: "
                + "\nResolution: "
                + "\nBitrate: "
                + "\nPackets: ";
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                statsView.setVisibility(View.VISIBLE);
                statsView.setText(statsReport);
            }
        });
    }

    void updateStats(RTCStatsReport report, boolean outbound) {
        final TextView statsView = outbound ? statsOutView : statsInView;
        String codecId = null;
        String codec = "";
        long bytesSR = 0;
        long width = 0, height = 0;
        long packetsSR = 0;
        for (RTCStats stats : report.getStatsMap().values()) {
            if (stats.getType().equals(outbound ? "outbound-rtp" : "inbound-rtp")) {
                Map<String, Object> members = stats.getMembers();
                if (members.get("mediaType").equals("video")) {
                    codecId = (String) members.get("codecId");
                    if (outbound) {
                        BigInteger bytes = (BigInteger) members.get("bytesSent");
                        bytesSR = bytes.longValue() - lastBytesSent.longValue();
                        lastBytesSent = bytes;
                    } else {
                        BigInteger bytes = (BigInteger) members.get("bytesReceived");
                        bytesSR = bytes.longValue() - lastBytesReceived.longValue();
                        lastBytesReceived = bytes;
                    }

                    packetsSR = (long) members.get(outbound ? "packetsSent" : "packetsReceived");
                }
            }
            if (stats.getType().equals("track")) {
                Map<String, Object> members = stats.getMembers();
                if (members.get("kind").equals("video")) {
                    width = (long) members.get("frameWidth");
                    height = (long) members.get("frameHeight");
                }
            }
        }
        if (codecId != null) {
            codec = (String) report.getStatsMap().get(codecId).getMembers().get("mimeType");
        }

        final String statsReport = (outbound ? "\n--- OUTBOUND ---" : "\n--- INBOUND ---")
                + "\nCodec: " + codec
                + "\nResolution: " + width + "x" + height
                + "\nBitrate: " + bytesSR * 8 / MainActivity.STATS_INTERVAL_MS + "kbps"
                + "\nPackets: " + packetsSR;
        getActivity().runOnUiThread(() -> {
            statsView.setVisibility(View.VISIBLE);
            statsView.setText(statsReport);
        });
    }

    public interface VideoFragmentListener {
        void onRenderer(SurfaceViewRenderer localRenderer, SurfaceViewRenderer remoteRenderer);
    }
}
