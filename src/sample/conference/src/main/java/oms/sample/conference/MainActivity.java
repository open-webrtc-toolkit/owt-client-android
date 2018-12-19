/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package oms.sample.conference;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import static oms.base.MediaCodecs.AudioCodec.OPUS;
import static oms.base.MediaCodecs.AudioCodec.PCMU;
import static oms.base.MediaCodecs.VideoCodec.H264;
import static oms.base.MediaCodecs.VideoCodec.VP8;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.EglBase;
import org.webrtc.RTCStatsReport;
import org.webrtc.SurfaceViewRenderer;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import oms.base.ActionCallback;
import oms.base.AudioCodecParameters;
import oms.base.ContextInitialization;
import oms.base.LocalStream;
import oms.base.MediaConstraints;
import oms.base.OmsError;
import oms.base.VideoCodecParameters;
import oms.base.VideoEncodingParameters;
import oms.conference.ConferenceClient;
import oms.conference.ConferenceClientConfiguration;
import oms.conference.ConferenceInfo;
import oms.conference.Participant;
import oms.conference.Publication;
import oms.conference.PublishOptions;
import oms.conference.RemoteMixedStream;
import oms.conference.RemoteStream;
import oms.conference.SubscribeOptions;
import oms.conference.SubscribeOptions.AudioSubscriptionConstraints;
import oms.conference.SubscribeOptions.VideoSubscriptionConstraints;
import oms.conference.Subscription;
import oms.sample.utils.OmsScreenCapturer;
import oms.sample.utils.OmsVideoCapturer;

public class MainActivity extends AppCompatActivity
        implements VideoFragment.VideoFragmentListener,
        ActivityCompat.OnRequestPermissionsResultCallback,
        ConferenceClient.ConferenceClientObserver {

    static final int STATS_INTERVAL_MS = 5000;
    private static final String TAG = "ICS_CONF";
    private static final int ICS_REQUEST_CODE = 100;
    private static boolean contextHasInitialized = false;
    EglBase rootEglBase;
    private boolean fullScreen = false;
    private boolean settingsCurrent = false;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Timer statsTimer;
    private LoginFragment loginFragment;
    private VideoFragment videoFragment;
    private SettingsFragment settingsFragment;
    private View fragmentContainer;
    private View bottomView;
    private Button leftBtn, rightBtn, middleBtn;
    private ConferenceClient conferenceClient;
    private ConferenceInfo conferenceInfo;
    private Publication publication;
    private Subscription subscription;
    private LocalStream localStream;
    private RemoteStream stream2Sub;
    private OmsVideoCapturer capturer;
    private LocalStream screenStream;
    private OmsScreenCapturer screenCapturer;
    private Publication screenPublication;
    private SurfaceViewRenderer localRenderer, remoteRenderer;

    private View.OnClickListener screenControl = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (fullScreen) {
                ActionBar actionBar = getSupportActionBar();
                if (actionBar != null) {
                    actionBar.hide();
                }
                bottomView.setVisibility(View.GONE);
                fullScreen = false;
            } else {
                ActionBar actionBar = getSupportActionBar();
                if (actionBar != null) {
                    actionBar.show();
                }
                bottomView.setVisibility(View.VISIBLE);
                fullScreen = true;
            }
        }
    };

    private View.OnClickListener settings = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (settingsCurrent) {
                switchFragment(loginFragment);
                rightBtn.setText(R.string.settings);
            } else {
                if (settingsFragment == null) {
                    settingsFragment = new SettingsFragment();
                }
                switchFragment(settingsFragment);

                rightBtn.setText(R.string.back);
            }
            settingsCurrent = !settingsCurrent;
        }
    };
    private View.OnClickListener leaveRoom = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            executor.execute(() -> conferenceClient.leave());
        }
    };
    private View.OnClickListener unpublish = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            localRenderer.setVisibility(View.GONE);
            rightBtn.setText(R.string.publish);
            rightBtn.setOnClickListener(publish);
            videoFragment.clearStats(true);

            executor.execute(() -> {
                publication.stop();
                localStream.detach(localRenderer);

                capturer.stopCapture();
                capturer.dispose();
                capturer = null;

                localStream.dispose();
                localStream = null;
            });
        }
    };
    private View.OnClickListener publish = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            rightBtn.setEnabled(false);
            rightBtn.setTextColor(Color.DKGRAY);
            executor.execute(() -> {
                boolean vga = settingsFragment == null || settingsFragment.resolutionVGA;
                capturer = OmsVideoCapturer.create(vga ? 640 : 1280, vga ? 480 : 720, 30, true);
                localStream = new LocalStream(capturer,
                        new MediaConstraints.AudioTrackConstraints());
                localStream.attach(localRenderer);

                VideoEncodingParameters h264 = new VideoEncodingParameters(H264);
                VideoEncodingParameters vp8 = new VideoEncodingParameters(VP8);

                PublishOptions options = PublishOptions.builder()
                        .addVideoParameter(h264)
                        .addVideoParameter(vp8)
                        .build();

                ActionCallback<Publication> callback = new ActionCallback<Publication>() {
                    @Override
                    public void onSuccess(final Publication result) {
                        runOnUiThread(() -> {
                            localRenderer.setVisibility(View.VISIBLE);

                            rightBtn.setEnabled(true);
                            rightBtn.setTextColor(Color.WHITE);
                            rightBtn.setText(R.string.unpublish);
                            rightBtn.setOnClickListener(unpublish);
                        });

                        publication = result;

                        try {
                            JSONArray mixBody = new JSONArray();
                            JSONObject body = new JSONObject();
                            body.put("op", "add");
                            body.put("path", "/info/inViews");
                            body.put("value", "common");
                            mixBody.put(body);

                            String serverUrl = loginFragment.getServerUrl();
                            String uri = serverUrl
                                    + "/rooms/" + conferenceInfo.id()
                                    + "/streams/" + result.id();
                            HttpUtils.request(uri, "PATCH", mixBody.toString(), true);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(final OmsError error) {
                        runOnUiThread(() -> {
                            rightBtn.setEnabled(true);
                            rightBtn.setTextColor(Color.WHITE);
                            rightBtn.setText(R.string.publish);
                            Toast.makeText(MainActivity.this,
                                    "Failed to publish " + error.errorMessage,
                                    Toast.LENGTH_SHORT).show();
                        });

                    }
                };

                conferenceClient.publish(localStream, options, callback);
            });
        }
    };
    private View.OnClickListener joinRoom = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            leftBtn.setEnabled(false);
            leftBtn.setTextColor(Color.DKGRAY);
            leftBtn.setText(R.string.connecting);
            rightBtn.setEnabled(false);
            rightBtn.setTextColor(Color.DKGRAY);

            executor.execute(() -> {
                String serverUrl = loginFragment.getServerUrl();
                String roomId = settingsFragment == null ? "" : settingsFragment.getRoomId();

                JSONObject joinBody = new JSONObject();
                try {
                    joinBody.put("role", "presenter");
                    joinBody.put("username", "user");
                    joinBody.put("room", roomId.equals("") ? "" : roomId);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String uri = serverUrl + "/createToken/";
                String token = HttpUtils.request(uri, "POST", joinBody.toString(), true);

                conferenceClient.join(token, new ActionCallback<ConferenceInfo>() {
                    @Override
                    public void onSuccess(ConferenceInfo conferenceInfo) {
                        MainActivity.this.conferenceInfo = conferenceInfo;
                        requestPermission();
                    }

                    @Override
                    public void onFailure(OmsError e) {
                        runOnUiThread(() -> {
                            leftBtn.setEnabled(true);
                            leftBtn.setTextColor(Color.WHITE);
                            leftBtn.setText(R.string.connect);
                            rightBtn.setEnabled(true);
                            rightBtn.setTextColor(Color.WHITE);
                        });
                    }
                });
            });
        }
    };
    private View.OnClickListener shareScreen = new View.OnClickListener() {
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onClick(View v) {
            middleBtn.setEnabled(false);
            middleBtn.setTextColor(Color.DKGRAY);
            if (middleBtn.getText().equals("ShareScreen")) {
                MediaProjectionManager manager =
                        (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
                startActivityForResult(manager.createScreenCaptureIntent(), ICS_REQUEST_CODE);
            } else {
                executor.execute(() -> {
                    if (screenPublication != null) {
                        screenPublication.stop();
                        screenPublication = null;
                    }
                });
                middleBtn.setEnabled(true);
                middleBtn.setTextColor(Color.WHITE);
                middleBtn.setText(R.string.share_screen);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        fullScreen = true;
        bottomView = findViewById(R.id.bottom_bar);
        fragmentContainer = findViewById(R.id.fragment_container);
        leftBtn = findViewById(R.id.multi_func_btn_left);
        leftBtn.setOnClickListener(joinRoom);
        rightBtn = findViewById(R.id.multi_func_btn_right);
        rightBtn.setOnClickListener(settings);
        middleBtn = findViewById(R.id.multi_func_btn_middle);
        middleBtn.setOnClickListener(shareScreen);
        middleBtn.setVisibility(View.GONE);

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

        loginFragment = new LoginFragment();
        switchFragment(loginFragment);

        initConferenceClient();
    }

    @Override
    protected void onPause() {
        if (localStream != null) {
            localStream.detach(localRenderer);
        }
        if (stream2Sub != null) {
            stream2Sub.detach(remoteRenderer);
        }

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (localStream != null) {
            localStream.attach(localRenderer);
        }
        if (stream2Sub != null) {
            stream2Sub.attach(remoteRenderer);
        }
    }

    private void initConferenceClient() {
        rootEglBase = EglBase.create();

        if (!contextHasInitialized) {
            ContextInitialization.create()
                    .setApplicationContext(this)
                    .setVideoHardwareAccelerationOptions(
                            rootEglBase.getEglBaseContext(),
                            rootEglBase.getEglBaseContext())
                    .initialize();
            contextHasInitialized = true;
        }

        HttpUtils.setUpINSECURESSLContext();
        ConferenceClientConfiguration configuration
                = ConferenceClientConfiguration.builder()
                .setHostnameVerifier(HttpUtils.hostnameVerifier)
                .setSSLContext(HttpUtils.sslContext)
                .build();
        conferenceClient = new ConferenceClient(configuration);
        conferenceClient.addObserver(this);
    }

    private void requestPermission() {
        String[] permissions = new String[]{Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO};

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    permission) != PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        permissions,
                        ICS_REQUEST_CODE);
                return;
            }
        }

        onConnectSucceed();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
            int[] grantResults) {
        if (requestCode == ICS_REQUEST_CODE
                && grantResults.length == 2
                && grantResults[0] == PERMISSION_GRANTED
                && grantResults[1] == PERMISSION_GRANTED) {
            onConnectSucceed();
        }
    }

    private void onConnectSucceed() {
        runOnUiThread(() -> {
            if (videoFragment == null) {
                videoFragment = new VideoFragment();
            }
            videoFragment.setListener(MainActivity.this);
            switchFragment(videoFragment);
            leftBtn.setEnabled(true);
            leftBtn.setTextColor(Color.WHITE);
            leftBtn.setText(R.string.disconnect);
            leftBtn.setOnClickListener(leaveRoom);
            rightBtn.setEnabled(true);
            rightBtn.setTextColor(Color.WHITE);
            rightBtn.setText(R.string.publish);
            rightBtn.setOnClickListener(publish);
            fragmentContainer.setOnClickListener(screenControl);
        });

        if (statsTimer != null) {
            statsTimer.cancel();
            statsTimer = null;
        }
        statsTimer = new Timer();
        statsTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                getStats();
            }
        }, 0, STATS_INTERVAL_MS);
        subscribeMixedStream();
    }

    private void subscribeMixedStream() {
        executor.execute(() -> {
            for (RemoteStream remoteStream : conferenceClient.info().getRemoteStreams()) {
                if (remoteStream instanceof RemoteMixedStream
                        && ((RemoteMixedStream) remoteStream).view.equals("common")) {
                    stream2Sub = remoteStream;
                    break;
                }
            }
            final RemoteStream finalStream2bSub = stream2Sub;
            VideoSubscriptionConstraints videoOption =
                    VideoSubscriptionConstraints.builder()
                            .setResolution(640, 480)
                            .setFrameRate(24)
                            .addCodec(new VideoCodecParameters(H264))
                            .addCodec(new VideoCodecParameters(VP8))
                            .build();

            AudioSubscriptionConstraints audioOption =
                    AudioSubscriptionConstraints.builder()
                            .addCodec(new AudioCodecParameters(OPUS))
                            .addCodec(new AudioCodecParameters(PCMU))
                            .build();

            SubscribeOptions options = SubscribeOptions.builder(true, true)
                    .setAudioOption(audioOption)
                    .setVideoOption(videoOption)
                    .build();

            conferenceClient.subscribe(stream2Sub, options,
                    new ActionCallback<Subscription>() {
                        @Override
                        public void onSuccess(Subscription result) {
                            MainActivity.this.subscription = result;
                            finalStream2bSub.attach(remoteRenderer);
                        }

                        @Override
                        public void onFailure(OmsError error) {
                            Log.e(TAG, "Failed to subscribe "
                                    + error.errorMessage);
                        }
                    });
        });
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        screenCapturer = new OmsScreenCapturer(data, 1280, 720);
        screenStream = new LocalStream(screenCapturer);
        executor.execute(
                () -> conferenceClient.publish(screenStream, new ActionCallback<Publication>() {
                    @Override
                    public void onSuccess(Publication result) {
                        runOnUiThread(() -> {
                            middleBtn.setEnabled(true);
                            middleBtn.setTextColor(Color.WHITE);
                            middleBtn.setText(R.string.stop_screen);
                        });
                        screenPublication = result;
                    }

                    @Override
                    public void onFailure(OmsError error) {
                        runOnUiThread(() -> {
                            middleBtn.setEnabled(true);
                            middleBtn.setTextColor(Color.WHITE);
                            middleBtn.setText(R.string.share_screen);
                        });
                        screenCapturer.stopCapture();
                        screenCapturer.dispose();
                        screenCapturer = null;
                        screenStream.dispose();
                        screenStream = null;
                    }
                }));
    }

    private void getStats() {
        if (publication != null) {
            publication.getStats(new ActionCallback<RTCStatsReport>() {
                @Override
                public void onSuccess(RTCStatsReport result) {
                    videoFragment.updateStats(result, true);
                }

                @Override
                public void onFailure(OmsError error) {

                }
            });
        }
        if (subscription != null) {
            subscription.getStats(new ActionCallback<RTCStatsReport>() {
                @Override
                public void onSuccess(RTCStatsReport result) {
                    videoFragment.updateStats(result, false);
                }

                @Override
                public void onFailure(OmsError error) {

                }
            });
        }
    }

    private void switchFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commitAllowingStateLoss();
        if (fragment instanceof VideoFragment) {
            middleBtn.setVisibility(View.VISIBLE);
        } else {
            middleBtn.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRenderer(SurfaceViewRenderer localRenderer, SurfaceViewRenderer remoteRenderer) {
        this.localRenderer = localRenderer;
        this.remoteRenderer = remoteRenderer;
    }

    @Override
    public void onStreamAdded(RemoteStream remoteStream) {

    }

    @Override
    public void onParticipantJoined(Participant participant) {

    }

    @Override
    public void onMessageReceived(String message, String from, String to) {

    }

    @Override
    public void onServerDisconnected() {
        runOnUiThread(() -> {
            switchFragment(loginFragment);
            leftBtn.setEnabled(true);
            leftBtn.setTextColor(Color.WHITE);
            leftBtn.setText(R.string.connect);
            leftBtn.setOnClickListener(joinRoom);
            rightBtn.setEnabled(true);
            rightBtn.setTextColor(Color.WHITE);
            rightBtn.setText(R.string.settings);
            rightBtn.setOnClickListener(settings);
            fragmentContainer.setOnClickListener(null);
        });

        if (statsTimer != null) {
            statsTimer.cancel();
            statsTimer = null;
        }

        if (capturer != null) {
            capturer.stopCapture();
            capturer.dispose();
            capturer = null;
        }

        if (localStream != null) {
            localStream.dispose();
            localStream = null;
        }

        publication = null;
        subscription = null;
        stream2Sub = null;
    }
}
