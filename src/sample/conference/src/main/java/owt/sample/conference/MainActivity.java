/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package owt.sample.conference;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import static org.webrtc.PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;

import static owt.base.MediaCodecs.AudioCodec.OPUS;
import static owt.base.MediaCodecs.AudioCodec.PCMU;
import static owt.base.MediaCodecs.VideoCodec.H264;
import static owt.base.MediaCodecs.VideoCodec.H265;
import static owt.base.MediaCodecs.VideoCodec.VP8;
import static owt.base.MediaCodecs.VideoCodec.VP9;

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
import android.support.v7.app.AlertDialog;
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
import org.webrtc.PeerConnection;
import org.webrtc.RTCStatsReport;
import org.webrtc.SurfaceViewRenderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import owt.base.ActionCallback;
import owt.base.AudioCodecParameters;
import owt.base.ContextInitialization;
import owt.base.LocalStream;
import owt.base.MediaConstraints;
import owt.base.OwtError;
import owt.base.VideoCodecParameters;
import owt.base.VideoEncodingParameters;
import owt.conference.ConferenceClient;
import owt.conference.ConferenceClientConfiguration;
import owt.conference.ConferenceInfo;
import owt.conference.Participant;
import owt.conference.Publication;
import owt.conference.PublicationSettings;
import owt.conference.PublishOptions;
import owt.conference.RemoteStream;
import owt.conference.SubscribeOptions;
import owt.conference.SubscribeOptions.AudioSubscriptionConstraints;
import owt.conference.SubscribeOptions.VideoSubscriptionConstraints;
import owt.conference.Subscription;
import owt.conference.SubscriptionCapabilities;
import owt.sample.utils.OwtScreenCapturer;
import owt.sample.utils.OwtVideoCapturer;

public class MainActivity extends AppCompatActivity
        implements VideoFragment.VideoFragmentListener,
        ActivityCompat.OnRequestPermissionsResultCallback,
        ConferenceClient.ConferenceClientObserver {

    static final int STATS_INTERVAL_MS = 5000;
    private static final String TAG = "OWT_CONF";
    private static final int OWT_REQUEST_CODE = 100;
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
    private Button leftBtn, rightBtn, middleBtn, subscribeBtn, unSubscribeBtn;
    private ConferenceClient conferenceClient;
    private ConferenceInfo conferenceInfo;
    private Publication publication;
    private Subscription subscription;
    private LocalStream localStream;
    private RemoteStream stream2Sub;
    private OwtVideoCapturer capturer;
    private LocalStream screenStream;
    private OwtScreenCapturer screenCapturer;
    private Publication screenPublication;
    private SurfaceViewRenderer localRenderer, remoteRenderer;
    private RemoteStream remoteForwardStream = null;
    private int subscribeRemoteStreamChoice = 0;
    private int subscribeVideoCodecChoice = 0;
    private int subscribeSimulcastRidChoice = 0;
    private ArrayList<String> remoteStreamIdList = new ArrayList<>();
    private HashMap<String, RemoteStream> remoteStreamMap = new HashMap<>();
    private HashMap<String, List<String>> videoCodecMap = new HashMap<>();
    private HashMap<String, List<String>> simulcastStreamMap = new HashMap<>();

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

    private View.OnClickListener subscribe = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            subscribeRemoteStreamChoice = 0;
            subscribeVideoCodecChoice = 0;
            subscribeSimulcastRidChoice = 0;
            final String[] items = (String[]) remoteStreamIdList.toArray(new String[0]);
            AlertDialog.Builder singleChoiceDialog =
                    new AlertDialog.Builder(MainActivity.this);
            singleChoiceDialog.setTitle("Remote Stream List");
            singleChoiceDialog.setSingleChoiceItems(items, 0,
                    (dialog, which) -> subscribeRemoteStreamChoice = which);
            singleChoiceDialog.setPositiveButton("ok",
                    (dialog, which) -> chooseCodec(
                            remoteStreamMap.get(items[subscribeRemoteStreamChoice])));
            singleChoiceDialog.show();
        }
    };

    private View.OnClickListener leaveRoom = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            executor.execute(() -> conferenceClient.leave());
        }
    };

    private View.OnClickListener unSubscribe = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (remoteForwardStream != null) {
                subscription.stop();
                remoteForwardStream.detach(remoteRenderer);
                runOnUiThread(() -> {
                    unSubscribeBtn.setVisibility(View.GONE);
                    subscribeBtn.setVisibility(View.VISIBLE);
                });
                subscribeRemoteStreamChoice = 0;
                subscribeVideoCodecChoice = 0;
            }

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
                boolean isCameraFront = settingsFragment == null || settingsFragment.cameraFront;
                capturer = OwtVideoCapturer.create(vga ? 640 : 1280, vga ? 480 : 720, 30, true,
                        isCameraFront);
                localStream = new LocalStream(capturer,
                        new MediaConstraints.AudioTrackConstraints());
                localStream.attach(localRenderer);

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
                    public void onFailure(final OwtError error) {
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

                conferenceClient.publish(localStream, setPublishOptions(), callback);
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
                        for (RemoteStream remoteStream : conferenceInfo.getRemoteStreams()) {
                            remoteStreamIdList.add(remoteStream.id());
                            remoteStreamMap.put(remoteStream.id(), remoteStream);
                            getParameterByRemoteStream(remoteStream);
                            remoteStream.addObserver(new owt.base.RemoteStream.StreamObserver() {
                                @Override
                                public void onEnded() {
                                    remoteStreamIdList.remove(remoteStream.id());
                                    remoteStreamMap.remove(remoteStream.id());
                                }

                                @Override
                                public void onUpdated() {

                                }
                            });
                        }
                        requestPermission();
                    }

                    @Override
                    public void onFailure(OwtError e) {
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
                startActivityForResult(manager.createScreenCaptureIntent(), OWT_REQUEST_CODE);
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
        subscribeBtn = findViewById(R.id.multi_func_btn_subscribe);
        subscribeBtn.setOnClickListener(subscribe);
        subscribeBtn.setVisibility(View.GONE);
        unSubscribeBtn = findViewById(R.id.multi_func_btn_unsubscribe);
        unSubscribeBtn.setOnClickListener(unSubscribe);
        unSubscribeBtn.setVisibility(View.GONE);
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

    @Override
    protected void onDestroy() {
        conferenceClient.removeObserver(this);

        super.onDestroy();
    }

    private void initConferenceClient() {
        rootEglBase = EglBase.create();

        if (!contextHasInitialized) {
            ContextInitialization.create()
                    .setApplicationContext(this)
                    .addIgnoreNetworkType(ContextInitialization.NetworkType.LOOPBACK)
                    .setVideoHardwareAccelerationOptions(
                            rootEglBase.getEglBaseContext(),
                            rootEglBase.getEglBaseContext())
                    .initialize();
            contextHasInitialized = true;
        }

        PeerConnection.IceServer iceServer = PeerConnection.IceServer.builder(
                "turn:example.com?transport=tcp").setUsername("userName").setPassword(
                "passward").createIceServer();
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        iceServers.add(iceServer);
        PeerConnection.RTCConfiguration rtcConfiguration = new PeerConnection.RTCConfiguration(
                iceServers);
        HttpUtils.setUpINSECURESSLContext();
        rtcConfiguration.continualGatheringPolicy = GATHER_CONTINUALLY;
        ConferenceClientConfiguration configuration
                = ConferenceClientConfiguration.builder()
                .setHostnameVerifier(HttpUtils.hostnameVerifier)
                .setSSLContext(HttpUtils.sslContext)
                .setRTCConfiguration(rtcConfiguration)
                .build();
        conferenceClient = new ConferenceClient(configuration);
        conferenceClient.addObserver(this);
    }

    private void requestPermission() {
        String[] permissions = new String[]{Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE};

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    permission) != PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        permissions,
                        OWT_REQUEST_CODE);
                return;
            }
        }

        onConnectSucceed();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
            int[] grantResults) {
        if (requestCode == OWT_REQUEST_CODE
                && grantResults.length == 3
                && grantResults[0] == PERMISSION_GRANTED
                && grantResults[1] == PERMISSION_GRANTED
                && grantResults[2] == PERMISSION_GRANTED) {
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
            subscribeBtn.setVisibility(View.VISIBLE);

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
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        screenCapturer = new OwtScreenCapturer(data, 1280, 720);
        screenStream = new LocalStream(screenCapturer,
                new MediaConstraints.AudioTrackConstraints());

        executor.execute(
                () -> conferenceClient.publish(screenStream, setPublishOptions(),
                        new ActionCallback<Publication>() {
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
                            public void onFailure(OwtError error) {
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

    public PublishOptions setPublishOptions() {
        PublishOptions options = null;
        VideoEncodingParameters h264 = new VideoEncodingParameters(H264);
        VideoEncodingParameters vp8 = new VideoEncodingParameters(VP8);
        VideoEncodingParameters vp9 = new VideoEncodingParameters(VP9);
        VideoEncodingParameters h265 = new VideoEncodingParameters(H265);
        if (settingsFragment != null && settingsFragment.VideoEncodingVP8) {
            options = PublishOptions.builder()
                    .addVideoParameter(vp8)
                    .build();
        } else if (settingsFragment != null && settingsFragment.VideoEncodingH264) {
            options = PublishOptions.builder()
                    .addVideoParameter(h264)
                    .build();
        } else if (settingsFragment != null && settingsFragment.VideoEncodingVP9) {
            options = PublishOptions.builder()
                    .addVideoParameter(vp9)
                    .build();
        } else if (settingsFragment != null && settingsFragment.VideoEncodingH265) {
            options = PublishOptions.builder()
                    .addVideoParameter(h265)
                    .build();
        } else {
            options = PublishOptions.builder()
                    .addVideoParameter(vp8)
                    .addVideoParameter(h264)
                    .addVideoParameter(vp9)
                    .build();
        }
        return options;
    }

    private void getStats() {
        if (publication != null) {
            publication.getStats(new ActionCallback<RTCStatsReport>() {
                @Override
                public void onSuccess(RTCStatsReport result) {
                    videoFragment.updateStats(result, true);
                }

                @Override
                public void onFailure(OwtError error) {

                }
            });
        }
        if (screenPublication != null) {
            screenPublication.getStats(new ActionCallback<RTCStatsReport>() {
                @Override
                public void onSuccess(RTCStatsReport result) {
                    videoFragment.updateStats(result, true);
                }

                @Override
                public void onFailure(OwtError error) {

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
                public void onFailure(OwtError error) {

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

    public void chooseCodec(RemoteStream remoteStream) {
        List<String> videoCodecList = videoCodecMap.get(remoteStream.id());
        removeDuplicate(videoCodecList);
        final String[] items = videoCodecList.toArray(new String[0]);
        AlertDialog.Builder singleChoiceDialog =
                new AlertDialog.Builder(MainActivity.this);
        singleChoiceDialog.setTitle("VideoCodec List");
        singleChoiceDialog.setSingleChoiceItems(items, 0,
                (dialog, which) -> subscribeVideoCodecChoice = which);
        singleChoiceDialog.setPositiveButton("ok",
                (dialog, which) -> {
                    String chooseVideoCodec = items[subscribeVideoCodecChoice];
                    if (simulcastStreamMap.containsKey(remoteStream.id())) {
                        chooseRid(remoteStream, chooseVideoCodec);
                    } else {
                        subscribeForward(remoteStream, chooseVideoCodec, null);
                    }

                });
        singleChoiceDialog.show();
    }

    public void chooseRid(RemoteStream remoteStream, String videoCodec) {
        List<String> ridList = simulcastStreamMap.get(remoteStream.id());
        removeDuplicate(ridList);
        final String[] items = (String[]) ridList.toArray(new String[0]);
        AlertDialog.Builder singleChoiceDialog =
                new AlertDialog.Builder(MainActivity.this);
        singleChoiceDialog.setTitle("Rid List");
        singleChoiceDialog.setSingleChoiceItems(items, 0,
                (dialog, which) -> subscribeSimulcastRidChoice = which);
        singleChoiceDialog.setPositiveButton("ok",
                (dialog, which) -> subscribeForward(remoteStream,videoCodec, items[subscribeSimulcastRidChoice]));
        singleChoiceDialog.show();
    }

    public void subscribeForward(RemoteStream remoteStream, String videoCodec, String rid) {
        VideoSubscriptionConstraints.Builder videoOptionBuilder =
                VideoSubscriptionConstraints.builder();

        VideoCodecParameters vcp = new VideoCodecParameters(H264);

        if (videoCodec.equals("VP8")) {
            vcp = new VideoCodecParameters(VP8);
        } else if (videoCodec.equals("H264")) {
            vcp = new VideoCodecParameters(H264);
        } else if (videoCodec.equals("VP9")) {
            vcp = new VideoCodecParameters(VP9);
        } else if (videoCodec.equals("H265")) {
            vcp = new VideoCodecParameters(H265);
        }
        if (rid != null) {
            videoOptionBuilder.setRid(rid);
        }
        VideoSubscriptionConstraints videoOption = videoOptionBuilder
                .addCodec(vcp)
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

        conferenceClient.subscribe(remoteStream, options,
                new ActionCallback<Subscription>() {
                    @Override
                    public void onSuccess(Subscription result) {
                        MainActivity.this.subscription = result;
                        MainActivity.this.remoteForwardStream = remoteStream;
                        remoteStream.attach(remoteRenderer);
                        runOnUiThread(() -> {
                            subscribeBtn.setVisibility(View.GONE);
                            unSubscribeBtn.setVisibility(View.VISIBLE);
                        });

                    }

                    @Override
                    public void onFailure(OwtError error) {
                        Log.e(TAG, "Failed to subscribe "
                                + error.errorMessage);
                    }
                });
    }

    @Override
    public void onRenderer(SurfaceViewRenderer localRenderer, SurfaceViewRenderer remoteRenderer) {
        this.localRenderer = localRenderer;
        this.remoteRenderer = remoteRenderer;
    }

    @Override
    public void onStreamAdded(RemoteStream remoteStream) {
        remoteStreamIdList.add(remoteStream.id());
        remoteStreamMap.put(remoteStream.id(), remoteStream);
        getParameterByRemoteStream(remoteStream);
        remoteStream.addObserver(new owt.base.RemoteStream.StreamObserver() {
            @Override
            public void onEnded() {
                remoteStreamIdList.remove(remoteStream.id());
                remoteStreamMap.remove(remoteStream.id());
            }

            @Override
            public void onUpdated() {
                getParameterByRemoteStream(remoteStream);
            }
        });
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
            subscribeBtn.setVisibility(View.GONE);
            unSubscribeBtn.setVisibility(View.GONE);
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

        remoteStreamIdList.clear();
        remoteStreamMap.clear();
    }

    public void getParameterByRemoteStream(RemoteStream remoteStream) {
        List<String> videoCodecList = new ArrayList<>();
        List<String> ridList = new ArrayList<>();
        SubscriptionCapabilities.VideoSubscriptionCapabilities videoSubscriptionCapabilities
                = remoteStream.extraSubscriptionCapability.videoSubscriptionCapabilities;
        for (VideoCodecParameters videoCodec : videoSubscriptionCapabilities.videoCodecs) {
            videoCodecList.add(videoCodec.name.name());
            videoCodecMap.put(remoteStream.id(), videoCodecList);
        }

        for (PublicationSettings.VideoPublicationSettings videoPublicationSetting :
                remoteStream.publicationSettings.videoPublicationSettings) {
            if (videoCodecMap.containsKey(remoteStream.id())){
                videoCodecMap.get(remoteStream.id()).add(videoPublicationSetting.codec.name.name());
            }else{
                videoCodecList.add(videoPublicationSetting.codec.name.name());
                videoCodecMap.put(remoteStream.id(), videoCodecList);
            }

            if (videoPublicationSetting.rid != null) {
                ridList.add(videoPublicationSetting.rid);
            }
        }

        if (ridList.size() != 0) {
            simulcastStreamMap.put(remoteStream.id(), ridList);
        }
    }

    public void removeDuplicate(List<String> list) {
        LinkedHashSet<String> set = new LinkedHashSet<String>(list.size());
        set.addAll(list);
        list.clear();
        list.addAll(set);
    }
}
