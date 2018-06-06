/*
 *  Copyright 2015 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package com.bailingcloud.bailingvideo;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bailingcloud.bailingvideo.base.BlinkBaseActivity;
import com.bailingcloud.bailingvideo.engine.binstack.http.BlinkHttpClient;
import com.bailingcloud.bailingvideo.engine.binstack.http.QuicHttpCallback;
import com.bailingcloud.bailingvideo.engine.binstack.json.module.StatusBean;
import com.bailingcloud.bailingvideo.engine.binstack.json.module.StatusReport;
import com.bailingcloud.bailingvideo.engine.binstack.json.module.StatusReportParser;
import com.bailingcloud.bailingvideo.engine.binstack.util.BlinkSessionManager;
import com.bailingcloud.bailingvideo.engine.binstack.util.FinLog;
import com.bailingcloud.bailingvideo.engine.context.BlinkContext;
import com.bailingcloud.bailingvideo.engine.view.BlinkVideoView;
import com.bailingcloud.bailingvideo.entity.ResolutionInfo;
import com.bailingcloud.bailingvideo.util.AppRTCUtils;
import com.bailingcloud.bailingvideo.util.AssetsFilesUtil;
import com.bailingcloud.bailingvideo.util.BlinkTalkTypeUtil;
import com.bailingcloud.bailingvideo.util.SessionManager;
import com.bailingcloud.bailingvideo.util.Utils;
import com.bailingcloud.bailingvideo.util.checkBoxDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import static com.bailingcloud.bailingvideo.SettingActivity.IS_BLINKCONNECTIONMODE;
import static com.bailingcloud.bailingvideo.SettingActivity.IS_GPUIMAGEFILTER;
import static com.bailingcloud.bailingvideo.SettingActivity.IS_SRTP;

/**
 * Activity for peer connection call setup, call waiting
 * and call view.
 */
public class CallActivity extends BlinkBaseActivity {

    //TODO 需要在百灵官网申请
    private static final String appid_ = "";
    
    private static String APPID = appid_;

    private AlertDialog ConfirmDialog = null;
    private String deviceId = "";

    public static final String EXTRA_ROOMID = "blinktalk.io.ROOMID";
    public static final String EXTRA_USER_NAME = "blinktalk.io.USER_NAME";
    public static final String EXTRA_SERVER_URL = "blinktalk.io.EXTRA_SERVER_URL";
    public static final String EXTRA_CAMERA = "blinktalk.io.EXTRA_CAMERA";
    public static final String EXTRA_OBSERVER = "blinktalk.io.EXTRA_OBSERVER";
    private static final String TAG = "CallActivity";
    private static final String AUDIOLEVELTAG = "CallAudioLevel";
    private static String Path = Environment.getExternalStorageDirectory().toString() + File.separator;

    // List of mandatory application unGrantedPermissions.
    private static final String[] MANDATORY_PERMISSIONS = {
            "android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.RECORD_AUDIO",
            "android.permission.INTERNET",
            "android.permission.CAMERA",
            "android.permission.READ_PHONE_STATE",
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private AppRTCAudioManager audioManager = null;
    private boolean isVideoMute = true;
    Handler networkSpeedHandler;
    // Controls
    private String serverURL = "", channelID = "", iUserName = "";
    private VideoViewManager renderViewManager;
    private boolean isConnected = true;

    private TextView textViewRoomNumber;
    private TextView textViewTime;
    private TextView textViewNetSpeed;
    private Button buttonHangUp;
    private CheckBox btnRotateScreen;
    //功能按钮所在的layout
//    private LinearLayout moreContainer;
    private LinearLayout waitingTips;
    private LinearLayout titleContainer;
    private WebView whiteboardView;
    private RelativeLayout mRelativeWebView;
    private boolean isGPUImageFliter = false;
    private Handler handler = new Handler();
    private DebugInfoAdapter debugInfoAdapter;
    private ListView debugInfoListView;
    private TextView biteRateSendView, biteRateRcvView, rttSendView;
    private ProgressDialog progressDialog;
    private checkBoxDialog sideBar;
    /**
     * UpgradeToNormal邀请观察者发言,将观察升级为正常用户=0, 摄像头:1 麦克风:2
     **/
    Map<Integer, ActionState> stateMap = new LinkedHashMap<>();
    /**
     * 存储用户是否开启分享
     **/
    private HashMap<String, Boolean> sharingMap = new HashMap<>();
/*
private Handler handlerScreen;
private OrientationSensorListener listener;
private SensorManager sm;
private Sensor sensor;
*/

    public static final String CR_720x1280 = "720x1280";
    public static final String CR_1080x1920 = "1088x1920";
    public static final String CR_480x720 = "480x720";
    public static final String CR_480x640 = "480x640";
    public static final String CR_368x640 = "368x640";
    public static final String CR_368x480 = "368x480";
    public static final String CR_240x320 = "240x320";
    public static final String CR_144x256 = "144x256";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(
                new UnhandledExceptionHandler(this));

        String userAppid = AppRTCUtils.getAppID();
        if (!TextUtils.isEmpty(userAppid)) {
            APPID = userAppid;
        } else {
            APPID = appid_;
        }
        FinLog.i(TAG, "user appid=" + APPID);

        // Set window styles for fullscreen-window size. Needs to be done before
        // adding content.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.activity_call);

        // Get Intent parameters.
        final Intent intent = getIntent();
        channelID = intent.getStringExtra(EXTRA_ROOMID);
        iUserName = intent.getStringExtra(EXTRA_USER_NAME);
        serverURL = intent.getStringExtra(EXTRA_SERVER_URL);
        isVideoMute = intent.getBooleanExtra(EXTRA_CAMERA, false);
        BlinkContext.ConfigParameter.isObserver = intent.getBooleanExtra(EXTRA_OBSERVER, false);
        if (channelID == null || channelID.length() == 0) {
            Log.e(TAG, "Incorrect room ID in intent!");
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        setChangeResolutionMap();

        sideBar = new checkBoxDialog(this, R.style.Dialog);
        if (!this.isFinishing()) {
            sideBar.showSideBar();
        }

        initAudioManager();

        initViews(intent);
        setCallbacks();
        checkPermissions();
        //调试内存使用情况
//        handler.post(memoryRunnable);
        // 屏幕旋转相关
//        handlerScreen = new ChangeOrientationHandler(this);
//        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
//        sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
//        listener = new OrientationSensorListener(handlerScreen);
//        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI);
    }

    private void initAudioManager() {
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        // Create and audio manager that will take care of audio routing,
        // audio modes, audio device enumeration etc.
        audioManager = AppRTCAudioManager.create(this, new Runnable() {
                    // This method will be called each time the audio state (number and
                    // type of devices) has been changed.
                    @Override
                    public void run() {
                        onAudioManagerChangedState();
                    }
                }
        );
        // Store existing audio settings and change audio mode to
        // MODE_IN_COMMUNICATION for best possible VoIP performance.
        Log.d(TAG, "Initializing the audio manager...");
        audioManager.init();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //防止在进入该页面后，权限没请求到就切换横竖屏导致的context为nul和获取不到屏幕宽高所导致的ui error;
        if (renderViewManager != null && null != unGrantedPermissions && unGrantedPermissions.size() == 0) {
            renderViewManager.rotateView();
            if (mRelativeWebView.getVisibility() == View.VISIBLE)
                loadWhiteBoard(null, true);
        }
    }

    /**
     * 在升降级过程中，改变功能按钮的状态
     */
    private void toggleCameraMicViewStatus() {
        if (BlinkContext.ConfigParameter.isObserver) {//降级之前，先恢复到正常状态：摄像头打开/前置/未静音
            if (sideBar.getCheckBox("btnSwitchCamera").isChecked())
                sideBar.getCheckBox("btnSwitchCamera").performClick();
            if (sideBar.getCheckBox("btnMuteMic").isChecked())
                sideBar.getCheckBox("btnMuteMic").performClick();
            if (sideBar.getCheckBox("btnCloseCamera").isChecked())
                sideBar.getCheckBox("btnCloseCamera").performClick();
        }
        sideBar.getCheckBox("btnSwitchCamera").setEnabled(BlinkContext.ConfigParameter.isObserver ? false : true);
        sideBar.getCheckBox("btnCloseCamera").setEnabled(BlinkContext.ConfigParameter.isObserver ? false : true);
        sideBar.getCheckBox("btnMuteMic").setEnabled(BlinkContext.ConfigParameter.isObserver ? false : true);
        sideBar.getCheckBox("btnRaiseHand").setEnabled(BlinkContext.ConfigParameter.isObserver ? true : false);
    }

    private void initViews(Intent intent) {
        biteRateSendView = (TextView) findViewById(R.id.debug_info_bitrate_send);
        biteRateRcvView = (TextView) findViewById(R.id.debug_info_bitrate_rcv);
        rttSendView = (TextView) findViewById(R.id.debug_info_rtt_send);
        debugInfoListView = (ListView) findViewById(R.id.debug_info_list);
        debugInfoAdapter = new DebugInfoAdapter(this);
        debugInfoListView.setAdapter(debugInfoAdapter);
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("白板加载中...");
        textViewRoomNumber = (TextView) findViewById(R.id.call_room_number);
        textViewTime = (TextView) findViewById(R.id.call_time);
        textViewNetSpeed = (TextView) findViewById(R.id.call_net_speed);
        buttonHangUp = (Button) findViewById(R.id.call_btn_hangup);
        titleContainer = (LinearLayout) findViewById(R.id.call_layout_title);
        sideBar.getCheckBox("btnRaiseHand").setEnabled(BlinkContext.ConfigParameter.isObserver ? true : false);

        sideBar.getCheckBox("btnSwitchCamera").setEnabled(BlinkContext.ConfigParameter.isObserver ? false : true);
        sideBar.getCheckBox("btnCloseCamera").setChecked(isVideoMute);
        sideBar.getCheckBox("btnCloseCamera").setEnabled(BlinkContext.ConfigParameter.isObserver ? false : true);
        sideBar.getCheckBox("btnMuteMic").setEnabled(BlinkContext.ConfigParameter.isObserver ? false : true);
        waitingTips = (LinearLayout) findViewById(R.id.call_waiting_tips);
        btnRotateScreen = (CheckBox) findViewById(R.id.menu_rotate_screen);
        btnRotateScreen.setVisibility(View.GONE);
        mRelativeWebView = (RelativeLayout) findViewById(R.id.call_whiteboard);
        whiteboardView = new WebView(getApplicationContext());
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        whiteboardView.setLayoutParams(params);
        mRelativeWebView.addView(whiteboardView);
//         btnWhiteBoard.setVisibility(View.GONE);
        WebSettings settings = whiteboardView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        settings.setLoadWithOverviewMode(true);
        settings.setBlockNetworkImage(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        renderViewManager = new VideoViewManager();
        renderViewManager.setActivity(this);
        if (BuildConfig.DEBUG) {
            textViewNetSpeed.setVisibility(View.VISIBLE);
        } else {
            textViewNetSpeed.setVisibility(View.GONE);
        }

        textViewRoomNumber.setText(getText(R.string.room_number) + intent.getStringExtra(CallActivity.EXTRA_ROOMID));
        buttonHangUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intendToLeave();
            }
        });
        sideBar.getCheckBox("btnSwitchCamera").setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckBox checkBox = (CheckBox) v;
                onCameraSwitch();
            }
        });
        sideBar.getCheckBox("btnCloseCamera").setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckBox checkBox = (CheckBox) v;
                onCameraClose(checkBox.isChecked());
            }
        });
        sideBar.getCheckBox("btnMuteMic").setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckBox checkBox = (CheckBox) v;
                onToggleMic(checkBox.isChecked());
            }
        });
        sideBar.getCheckBox("btnMuteSpeaker").setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CheckBox checkBox = (CheckBox) view;
                onToggleSpeaker(checkBox.isChecked());
            }
        });
        sideBar.getCheckBox("btnWhiteBoard").setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleWhiteboard(((CheckBox) view).isChecked());
            }
        });
        sideBar.getCheckBox("btnRaiseHand").setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BlinkEngine.getInstance().observerRequestBecomeNormalUser();
            }
        });
        btnRotateScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rotateScreen(getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        });
        renderViewManager.setOnLocalVideoViewClickedListener(new VideoViewManager.OnLocalVideoViewClickedListener() {
            @Override
            public void onClick() {
                toggleActionButtons(buttonHangUp.getVisibility() == View.VISIBLE);
            }
        });
        waitingTips.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleActionButtons(buttonHangUp.getVisibility() == View.VISIBLE);
            }
        });
        sideBar.getbtnChangeResolution_up().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeVideoSize("up");
            }
        });
        sideBar.getbtnChangeResolution_down().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeVideoSize("down");
            }
        });
        setCallIdel();
    }

    /**
     * 准备离开当前房间
     */
    private void intendToLeave() {
        if (null != sharingMap) {
            sharingMap.clear();
        }

        //当前用户是观察者 或 离开房间时还有其他用户存在，直接退出
        if (BlinkContext.ConfigParameter.isObserver || BlinkEngine.getInstance().hasConnectedUser())
            disconnect();
        else //非观察者离开房间时，房间只剩自己，这时候要先去判断是否有白板存在，然后提示用户
        {
            if (BlinkEngine.getInstance().isWhiteBoardExist()) {//房间中有打开的白板，提示用户是否关闭
                FinLog.i(TAG, "还有人吗：" + BlinkEngine.getInstance().hasConnectedUser() + ",,BlinkEngine.getInstance().isWhiteBoardExist()=" + BlinkEngine.getInstance().isWhiteBoardExist());
                showConfirmDialog(getResources().getString(R.string.meeting_control_destroy_whiteBoard), null, null, null, null);
            } else disconnect();
        }
    }

    /**
     * 改变屏幕上除了视频通话之外的其他视图可见状态
     */
    private void toggleActionButtons(boolean isHidden) {
        if (isHidden) {
            buttonHangUp.setVisibility(View.GONE);
            sideBar.dismissSideBar();
            titleContainer.setVisibility(View.GONE);
        } else {
            buttonHangUp.setVisibility(View.VISIBLE);
            sideBar.showSideBar();
            titleContainer.setVisibility(View.VISIBLE);
            startTenSecondsTimer();
        }
    }

    private Timer tenSecondsTimer;

    /**
     * 启动一个持续10秒的计时器，用于隐藏除了视频以外的视图
     */
    private void startTenSecondsTimer() {
//        tenSecondsTimer = new Timer();
//        tenSecondsTimer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        toggleActionButtons(true);
//                        tenSecondsTimer = null;
//                    }
//                });
//            }
//        }, 10 * 1000);
    }

    private void setCallIdel() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                super.onCallStateChanged(state, incomingNumber);
                switch (state) {
                    case TelephonyManager.CALL_STATE_RINGING:
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        if (BlinkEngine.getInstance() != null) {
                            BlinkEngine.getInstance().muteMicrophone(true);
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        if (BlinkEngine.getInstance() != null) {
                            BlinkEngine.getInstance().muteMicrophone(false);
                        }
                        break;
                }
            }
        }, PhoneStateListener.LISTEN_CALL_STATE);
    }

    public void setWaitingTipsVisiable(boolean visiable) {
        if (visiable) {
            waitingTips.setVisibility(View.VISIBLE);
        } else {
            waitingTips.setVisibility(View.GONE);
        }
    }

    // Activity interfaces
    @Override
    public void onPause() {
//        sm.unregisterListener(listener);
        super.onPause();
        BlinkEngine.getInstance().stopCapture();
    }

    @Override
    public void onResume() {
//        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI);
        super.onResume();
        BlinkEngine.getInstance().startCapture();
    }

    @Override
    protected void onStop() {
        super.onStop();
        clearState();
    }

    @Override
    protected void onDestroy() {
        if (null != sideBar) {
            sideBar.dismissSideBar();
        }
        if (isConnected) {
            if (BlinkEngine.getInstance() != null)
                BlinkEngine.getInstance().leaveChannel();
            if (renderViewManager != null)
//                renderViewManager.destroyViews();
                if (audioManager != null) {
                    audioManager.close();
                    audioManager = null;
                }
        }
        if (handler != null)
            handler.removeCallbacks(memoryRunnable);
        super.onDestroy();
        if (null != ConfirmDialog && ConfirmDialog.isShowing()) {
            ConfirmDialog.dismiss();
            ConfirmDialog = null;
        }
        if (null != sharingMap) {
            sharingMap.clear();
        }
        destroyWebView(whiteboardView);
    }


    public void onCameraSwitch() {
        BlinkEngine.getInstance().switchCamera();
    }

    /**
     * 摄像头开关
     *
     * @param closed true  关闭摄像头
     *               false 打开摄像头
     * @return
     */
    public boolean onCameraClose(boolean closed) {
        BlinkEngine.getInstance().closeLocalVideo(closed);
        if (renderViewManager != null)
            renderViewManager.updateTalkType(getDeviceId(), closed ? BlinkTalkTypeUtil.C_CAMERA : BlinkTalkTypeUtil.O_CAMERA);
        this.isVideoMute = closed;
        return isVideoMute;
    }
//
//    @Override
//    public void onVideoScalingSwitch(ScalingType scalingType) {
////        this.scalingType = scalingType;
////        updateVideoView();
//    }

    public void onCaptureFormatChange(int width, int height, int framerate) {
    }

    public boolean onToggleMic(boolean mute) {
        BlinkEngine.getInstance().muteMicrophone(mute);
        return mute;
    }

    public boolean onToggleSpeaker(boolean mute) {
        try {
            audioManager.onToggleSpeaker(mute);
        } catch (Exception e) {
            e.printStackTrace();
            FinLog.i(TAG, "message=" + e.getMessage());
        }
        return mute;
    }

    private void rotateScreen(boolean isToLandscape) {
        if (isToLandscape)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    private void toggleWhiteboard(boolean open) {
        if (open) {
            BlinkEngine.getInstance().requestWhiteBoardURL();
        } else {
            mRelativeWebView.setVisibility(View.GONE);
        }
//        renderViewManager.toggleLocalView(!open);
    }

    List<String> unGrantedPermissions;

    private void checkPermissions() {
        unGrantedPermissions = new ArrayList();
        for (String permission : MANDATORY_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                unGrantedPermissions.add(permission);
            }
        }
        if (unGrantedPermissions.size() == 0) {//已经获得了所有权限，开始加入聊天室
            startCall();
        } else {//部分权限未获得，重新请求获取权限
            String[] array = new String[unGrantedPermissions.size()];
            ActivityCompat.requestPermissions(this, unGrantedPermissions.toArray(array), 0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        unGrantedPermissions.clear();
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_DENIED)
                unGrantedPermissions.add(permissions[i]);
        }
        for (String permission : unGrantedPermissions) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                showToastLengthLong("权限: " + permission + " 已被禁止，请手动开启！");
                finish();
            } else ActivityCompat.requestPermissions(this, new String[]{permission}, 0);
        }
        if (unGrantedPermissions.size() == 0) {
            AssetsFilesUtil.putAssetsToSDCard(getApplicationContext(), assetsFile, encryptFilePath);
            startCall();
        }
    }

    private String assetsFile = "EncryptData/00000001.bin";
    private String encryptFilePath=new StringBuffer().append(Environment.getExternalStorageDirectory().toString() + File.separator).append("Blink").append(File.separator).append("EncryptData").toString();
    BlinkVideoView localSurface;

    private void startCall() {
        try {
            Map<String, Object> parameters = new HashMap<String, Object>();

            //Set connection mode 这一段代码也只是在测试的时候放出
            String connetionMode = SessionManager.getInstance(this).getString(SettingActivity.CONNECTION_MODE);
            if (!TextUtils.isEmpty(connetionMode) && connetionMode.equals("P2P")) {
                BlinkContext.ConfigParameter.connectionMode = (BlinkContext.ConfigParameter.CONNECTION_MODE_P2P);
            } else {
                BlinkContext.ConfigParameter.connectionMode = (BlinkContext.ConfigParameter.CONNECTION_MODE_RELAY);
            }

            parameters.put(BlinkEngine.ParameterKey.KEY_IS_AUDIO_ONLY, isVideoMute);
            //Set max and min bitrate
            String minBitRate = SessionManager.getInstance(this).getString(SettingActivity.BIT_RATE_MIN);
            if (!TextUtils.isEmpty(minBitRate) && minBitRate.length() > 4) {
                int bitRateIntvalue = Integer.valueOf(minBitRate.substring(0, minBitRate.length() - 4));
                FinLog.i(TAG,"BIT_RATE_MIN="+bitRateIntvalue);
                parameters.put(BlinkEngine.ParameterKey.KEY_VIDEO_MIN_RATE, bitRateIntvalue);
            }
            String maxBitRate = SessionManager.getInstance(this).getString(SettingActivity.BIT_RATE_MAX);
            if (!TextUtils.isEmpty(maxBitRate) && maxBitRate.length() > 4) {
                int bitRateIntvalue = Integer.valueOf(maxBitRate.substring(0, maxBitRate.length() - 4));
                FinLog.i(TAG,"BIT_RATE_MAX="+bitRateIntvalue);
                parameters.put(BlinkEngine.ParameterKey.KEY_VIDEO_MAX_RATE, bitRateIntvalue);
            }
            //set resolution
            String resolution = SessionManager.getInstance(this).getString(SettingActivity.RESOLUTION);
            String fps = SessionManager.getInstance(this).getString(SettingActivity.FPS);
            if (SettingActivity.RESOLUTION_LOW.equals(resolution)) {
                if ("15".equals(fps)) {
                    parameters.put(BlinkEngine.ParameterKey.KEY_VIDEO_PROFILE, BlinkEngine.BlinkVideoProfile.BLINK_VIDEO_PROFILE_240P_15f);
                } else if ("24".equals(fps)) {
                    parameters.put(BlinkEngine.ParameterKey.KEY_VIDEO_PROFILE, BlinkEngine.BlinkVideoProfile.BLINK_VIDEO_PROFILE_240P_24f);
                } else if ("30".equals(fps)) {
                    parameters.put(BlinkEngine.ParameterKey.KEY_VIDEO_PROFILE, BlinkEngine.BlinkVideoProfile.BLINK_VIDEO_PROFILE_240P_30f);
                }
            } else if (SettingActivity.RESOLUTION_MEDIUM.equals(resolution)) {
                if ("15".equals(fps)) {
                    parameters.put(BlinkEngine.ParameterKey.KEY_VIDEO_PROFILE, BlinkEngine.BlinkVideoProfile.BLINK_VIDEO_PROFILE_480P_15f_1);
                } else if ("24".equals(fps)) {
                    parameters.put(BlinkEngine.ParameterKey.KEY_VIDEO_PROFILE, BlinkEngine.BlinkVideoProfile.BLINK_VIDEO_PROFILE_480P_24f_1);
                } else if ("30".equals(fps)) {
                    parameters.put(BlinkEngine.ParameterKey.KEY_VIDEO_PROFILE, BlinkEngine.BlinkVideoProfile.BLINK_VIDEO_PROFILE_480P_30f_1);
                }
            } else if (SettingActivity.RESOLUTION_HIGH.equals(resolution)) {
                if ("15".equals(fps)) {
                    parameters.put(BlinkEngine.ParameterKey.KEY_VIDEO_PROFILE, BlinkEngine.BlinkVideoProfile.BLINK_VIDEO_PROFILE_720P_15f);
                } else if ("24".equals(fps)) {
                    parameters.put(BlinkEngine.ParameterKey.KEY_VIDEO_PROFILE, BlinkEngine.BlinkVideoProfile.BLINK_VIDEO_PROFILE_720P_24f);
                } else if ("30".equals(fps)) {
                    parameters.put(BlinkEngine.ParameterKey.KEY_VIDEO_PROFILE, BlinkEngine.BlinkVideoProfile.BLINK_VIDEO_PROFILE_720P_30f);
                }
            } else if (SettingActivity.RESOLUTION_SUPER.equals(resolution)) {
                if ("15".equals(fps)) {
                    parameters.put(BlinkEngine.ParameterKey.KEY_VIDEO_PROFILE, BlinkEngine.BlinkVideoProfile.BLINK_VIDEO_PROFILE_1080P_15f);
                } else if ("24".equals(fps)) {
                    parameters.put(BlinkEngine.ParameterKey.KEY_VIDEO_PROFILE, BlinkEngine.BlinkVideoProfile.BLINK_VIDEO_PROFILE_1080P_24f);
                } else if ("30".equals(fps)) {
                    parameters.put(BlinkEngine.ParameterKey.KEY_VIDEO_PROFILE, BlinkEngine.BlinkVideoProfile.BLINK_VIDEO_PROFILE_1080P_30f);
                }
            }
            //set codecs
            String codec = SessionManager.getInstance(this).getString(SettingActivity.CODECS);
            if (!TextUtils.isEmpty(codec)) {
                if ("VP8".equals(codec))
                    parameters.put(BlinkEngine.ParameterKey.KEY_VIDEO_CODECS, BlinkEngine.BlinkVideoCodecs.VP8);
                else
                    parameters.put(BlinkEngine.ParameterKey.KEY_VIDEO_CODECS, BlinkEngine.BlinkVideoCodecs.H264);
            }

            parameters.put(BlinkEngine.ParameterKey.KEY_USER_TYPE, BlinkContext.ConfigParameter.isObserver ? BlinkEngine.UserType.Blink_User_Observer : BlinkEngine.UserType.Blink_User_Normal);
            //设置是否启用美颜模式
            isGPUImageFliter = SessionManager.getInstance(this).getBoolean(IS_GPUIMAGEFILTER);
            parameters.put(BlinkEngine.ParameterKey.KEY_IS_BEAUTY_FILETER_USED, isGPUImageFliter ? true : false);
            //设置是否使用SRTP
            parameters.put(BlinkEngine.ParameterKey.KEY_IS_SRTP_USED, SessionManager.getInstance(this).getBoolean(IS_SRTP));

            BlinkEngine.getInstance().setVideoParameters(parameters);
            BlinkEngine.getInstance().enableSendLostReport(true);
//            BlinkEngine.getInstance().setExternalEncryptFilePath(encryptFilePath + File.separator + "00000001.bin");

//            String path = new StringBuffer().append(Path).append("Blink").append(File.separator).append("Log").append(File.separator).append(getDateString()).append(".log").toString();
//            BlinkEngine.getInstance().setBlinkLog(path);

            renderViewManager.initViews(this, BlinkContext.ConfigParameter.isObserver);

            if (!BlinkContext.ConfigParameter.isObserver) {
                localSurface = BlinkEngine.createVideoView(getApplicationContext());
                BlinkEngine.getInstance().setLocalVideoView(localSurface);
                renderViewManager.setVideoView(true, getDeviceId(), iUserName, localSurface, isVideoMute ? BlinkTalkTypeUtil.C_CAMERA : BlinkTalkTypeUtil.O_CAMERA);
            }
            logonToServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void logonToServer() {
        try {
            deviceId = getDeviceId();
            if (TextUtils.isEmpty(deviceId)) {
                deviceId = BlinkSessionManager.getInstance().getString(BlinkContext.BLINK_UUID);
            }
            boolean isQuic = BlinkContext.ConfigParameter.blinkConnectionMode == BlinkEngine.BlinkConnectionMode.QUIC ? true : false;
            if (isQuic) {
                FinLog.i("BinClient", "quic方式请求token");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        BlinkHttpClient.getInstance().doGetQuic(serverURL + "?" + "uid=" + deviceId + "&appid=" + APPID, new QuicHttpCallback() {
                            @Override
                            public void onResponseReceived(String result, Exception e) {
                                if (TextUtils.isEmpty(result) || TextUtils.isEmpty(deviceId)) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(CallActivity.this, "获取token/deviceID失败，请重新尝试！", Toast.LENGTH_SHORT).show();
                                            CallActivity.this.finish();
                                        }
                                    });
                                    return;
                                }
                                FinLog.i(TAG, result);
                                BlinkEngine.getInstance().joinChannel(deviceId, iUserName, result, channelID);
                            }
                        });
                    }
                }).start();
            } else {
                FinLog.i("BinClient", "tcp方式请求token");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final String token = BlinkHttpClient.getInstance().doPost(serverURL, "uid=" + deviceId + "&appid=" + APPID);
                        if (TextUtils.isEmpty(token) || TextUtils.isEmpty(deviceId)) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(CallActivity.this, "获取token/deviceID失败，请重新尝试！", Toast.LENGTH_SHORT).show();
                                    CallActivity.this.finish();
                                }
                            });
                            return;
                        }
                        BlinkEngine.getInstance().joinChannel(deviceId, iUserName, token, channelID);
                    }
                }).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private String getDeviceId() {
        String deviceId = "";
        try {
            TelephonyManager TelephonyMgr = (TelephonyManager) getSystemService(Activity.TELEPHONY_SERVICE);
            deviceId = TelephonyMgr.getDeviceId();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (TextUtils.isEmpty(deviceId)) {
                deviceId = BlinkSessionManager.getInstance().getString(BlinkContext.BLINK_UUID);
            }
            return deviceId;
        }
    }

    private void onAudioManagerChangedState() {
        // TODO(henrika): disable video if AppRTCAudioManager.AudioDevice.EARPIECE
        // is active.
    }

    // Disconnect from remote resources, dispose of local resources, and exit.

    private void startCalculateNetSpeed() {
        if (networkSpeedHandler == null)
            networkSpeedHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    if (msg.what == 1) {
                        textViewNetSpeed.setText(getResources().getString(R.string.network_traffic_receive) + msg.getData().getLong("rcv") + "Kbps  " +
                                getResources().getString(R.string.network_traffic_send) + msg.getData().getLong("send") + "Kbps");
                        updateTimer();
                    }
                    super.handleMessage(msg);
                }
            };
//        startTenSecondsTimer();
    }

    private int time = 0;

    private void updateTimer() {
        time++;
        textViewTime.setText(getFormatTime(time));
    }

    private String getFormatTime(int time) {
        if (time < 10)
            return "00:0" + time;
        else if (time < 60)
            return "00:" + time;
        else if (time % 60 < 10) {
            if (time / 60 < 10) {
                return "0" + time / 60 + ":0" + time % 60;
            } else {
                return time / 60 + ":0" + time % 60;
            }
        } else {
            if (time / 60 < 10) {
                return "0" + time / 60 + ":" + time % 60;
            } else {
                return time / 60 + ":" + time % 60;
            }
        }
    }

    // Log |msg| and Toast about it.
    private void logAndToast(String msg) {
        Log.d(TAG, msg);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void setCallbacks() {
        BlinkEngine.getInstance().setBlinkEngineEventHandler(new BlinkEngineEventHandler() {

            @Override
            public void onStartCaptureResult(int resultCode) {
                super.onStartCaptureResult(resultCode);
                FinLog.i("onStartCaptureResult", "onStartCapture  Result=" + resultCode + "\n threadName=" + Thread.currentThread().getName());
            }

            @Override
            public void onConnectionStateChanged(int connectionState) {
                FinLog.i("connectionState", "Connection State Code=" + connectionState + "\n threadName=" + Thread.currentThread().getName());
                switch (connectionState) {
                    case BlinkResponseCode.Connection_BlinkConnectionFactory_InitFailed:
                    case BlinkResponseCode.Connection_Socket_InitFailed:
                    case BlinkResponseCode.Connection_JoinFailed:
                    case BlinkResponseCode.Connection_DNSFailed:
                    case BlinkResponseCode.Connection_Disconnected:
                    case BlinkResponseCode.Connection_KeepAliveFailed:
                    case BlinkResponseCode.Connection_InsufficientPermissions:
                        showToastLengthLong("onConnectionStateChanged error code=" + connectionState);
                        disconnect();
                        break;
                    case BlinkResponseCode.Connection_JoinComplete:
                        break;
                }
            }

            /**
             * 自己是否成功离开某一聊天室, leaveChannel() 方法的结果反馈。
             *
             * @param success 是否成功
             */
            @Override
            public void onLeaveComplete(boolean success) {
            }

            @Override
            public void onUserJoined(String userId, String userName, BlinkEngine.UserType type, long talkType, int screenSharingStatus) {
                FinLog.i("userJoined", "----onUserJoined-------\n userId=" + userId + "," + "type == BlinkEngine.UserType.Blink_User_Observer=" + (type == BlinkEngine.UserType.Blink_User_Observer) + "：Name=" + userName + ",talkType=" + talkType + ",screenSharingStatus=" + screenSharingStatus + "\n threadName=" + Thread.currentThread().getName());
                if (type == BlinkEngine.UserType.Blink_User_Observer) {
                    return;
                }
                renderViewManager.userJoin(userId, userName, userJoinTaikType(talkType));
            }

            @Override
            public void onNotifyUserVideoCreated(String userId, String userName, BlinkEngine.UserType type, long talkType, int screenSharingStatus) {
                FinLog.i("userJoined", "----onNotifyUserVideoCreated-------\n userId=" + userId + "," + "type == " + (type == BlinkEngine.UserType.Blink_User_Observer) + "：Name=" + userName + ",talkType=" + talkType + ",screenSharingStatus=" + screenSharingStatus + "\n threadName=" + Thread.currentThread().getName());
                if (!userId.equals(getDeviceId())) {
                    boolean sharing_status = screenSharingStatus == 1 ? true : false;
                    sharingMap.put(userId, sharing_status);
                    if (BlinkContext.ConfigParameter.isObserver && sharing_status) {
                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                showToastLengthLong(getResources().getString(R.string.meeting_control_OpenWiteBoard));
                            }
                        }, 1100);
                    }
                }
                if (!renderViewManager.hasConnectedUser())
                    startCalculateNetSpeed();

                BlinkVideoView remoteView = BlinkEngine.createVideoView(CallActivity.this.getApplicationContext());
                BlinkEngine.getInstance().setRemoteVideoView(remoteView, userId);
                renderViewManager.setVideoView(false, userId, userName, remoteView, userJoinTaikType(talkType));
            }

            /**
             * 某一用户从当前所在的聊天室退出
             * 房间中只有两个人,不存在流退不退出 直接left  ，人多的时候会执行left videoDestory;
             * @param userId 退出聊天室的用户ID
             */
            @Override
            public void onUserLeft(String userId) {
                FinLog.i("userLeft", "onUserLeft---\nuserid=" + userId + ",,connectedRemote size=" + renderViewManager.connetedRemoteRenders.size());
                exitRoom(userId);
            }

            @Override
            public void OnNotifyUserVideoDestroyed(String userId) {
                FinLog.i("userLeft", "OnNotifyUserVideoDestroyed----\nuserId=" + userId);
                //用户被降级会回调
                exitRoom(userId);
            }

            /**
             * 自己已在聊天室中且聊天室中至少还有一个远程用户, requestWhiteBoardURL() 请求白板页面的HTTP URL之后的回调
             *
             * @param url 白板页面的url
             */
            @Override
            public void onWhiteBoardURL(String url) {
                loadWhiteBoard(url, false);
            }

            @Override
            public void onNetworkSentLost(int lossRate) {
//                FinLog.e("lossRate = " + lossRate);
            }

            @Override
            public void onNetworkReceiveLost(int lossRate) {

            }

            @Override
            public int onTextureFrameCaptured(int width, int height, int oesTextureId) {
//                FinLog.e(TAG,"oesTextureId == "+oesTextureId);
                return 0;
            }

            @Override
            public void onAudioInputLevel(String audioLevel) {
                super.onAudioInputLevel(audioLevel);
                try {
                    if (!TextUtils.isEmpty(audioLevel)) {
                        int val = Integer.valueOf(audioLevel);
                        audiolevel(val, BlinkContext.ConfigParameter.userID);
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onAudioReceivedLevel(HashMap<String, String> audioLevel) {
                super.onAudioReceivedLevel(audioLevel);
                try {
                    Iterator iter = audioLevel.entrySet().iterator();
                    while (iter.hasNext()) {
                        Map.Entry entry = (Map.Entry) iter.next();
                        String key = "";
                        int val = 0;
                        if (null != entry.getKey()) {
                            key = entry.getKey().toString();
                        }
                        if (null != entry.getValue()) {
                            val = Integer.valueOf(entry.getValue().toString());
                        }
                        audiolevel(val, key);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void onControlAudioVideoDevice(int code) {
            }

            @Override
            public void onNotifyControlAudioVideoDevice(String userId, BlinkEngine.BlinkDeviceType type, boolean isOpen) {
//                if (renderViewManager != null && (type == BlinkEngine.BlinkDeviceType.Camera || type == BlinkEngine.BlinkDeviceType.CameraAndMicrophone))
//                    renderViewManager.updateTalkType(userId, isOpen ? 1 : 0);
                deviceCover(userId, isOpen, type);
            }

            @Override
            public void onNotifyCreateWhiteBoard(String userId) {
                toastMessage(userId + "创建了白板！");
            }

            @Override
            public void onNotifySharingScreen(String userId, boolean isOpen) {
                FinLog.i(TAG, userId + (isOpen ? "打开了屏幕共享" : "关闭了屏幕共享"));
                sharingMap.put(userId, isOpen);
                if (isOpen && renderViewManager.isBig(userId) && isSharing(userId)) {
                    showToastLengthLong(getResources().getString(R.string.meeting_control_OpenWiteBoard));
                }
            }
        });

        BlinkEngine.getInstance().setChannelManageEventHandler(new BlinkEngineChannelManageEventHandler() {

            @Override
            public void onObserverRequestBecomeNormalUser(int code) {
                if (code == 0)
                    toastMessage("请求发送成功！");
                else toastMessage("请求发送失败！Code=" + code);

            }

            @Override
            public void onUpgradeObserverToNormalUser(int code) {
            }

            @Override
            public void onDegradeNormalUserToObserver(int code) {
            }

            @Override
            public void onRemoveUser(int code) {
            }

            @Override
            public void onHostControlUserDevice(String userId, BlinkEngine.BlinkDeviceType dType, int code) {
            }

            @Override
            public void onGetInviteURL(String url, int code) {
                if (code == 0)
                    copyInviteUrlToClipboard(url);
                else toastMessage("获取邀请链接失败！");
            }

            @Override
            public void onNormalUserRequestHostAuthority(int code) {
                if (code == 0)
                    toastMessage("您成为会议主持人");
            }

            @Override
            public void onNotifyNormalUserRequestHostAuthority(String userId) {
                toastMessage("用户:" + userId + "成为会议主持人");
            }

            @Override
            public void onNotifyDegradeNormalUserToObserver(String hostUid, String userId) {
                if (userId.equals(getDeviceId())) {
                    changeToObserverOrNormal(true);
                    toastMessage("您已被主持人降级为观察者");
                    BlinkEngine.getInstance().answerDegradeNormalUserToObserver(hostUid, true);
                } else {
                    toastMessage(userId + "已被主持人降级为观察者");
                    sharingMap.put(userId, false);
                }
            }

            @Override
            public void onNotifyUpgradeObserverToNormalUser(String hostUid, String userId) {
                if (addActionState(0, hostUid, userId)) {
                    return;
                }
                showConfirmDialog(getResources().getString(R.string.meeting_control_inviteToUpgrade), hostUid, userId, BlinkEngine.BlinkActionType.UpgradeToNormal, null);
            }

            @Override
            public void onNotifyRemoveUser(String userId) {
                toastMessage("您已被主持人移出会话！");
                disconnect();
            }

            @Override
            public void onNotifyObserverRequestBecomeNormalUser(String userId) {
                showConfirmDialog("用户:" + userId + "请求发言", "", userId, BlinkEngine.BlinkActionType.RequestUpgradeToNormal, null);
            }

            @Override
            public void onNotifyHostControlUserDevice(String userId, String hostId, BlinkEngine.BlinkDeviceType type, boolean isOpen) {
                if (isOpen) {
                    if (addActionState(type.getValue(), hostId, userId)) {
                        return;
                    }
                    String deviceType = "";
                    if (type == BlinkEngine.BlinkDeviceType.Camera)
                        deviceType = getResources().getString(R.string.meeting_control_inviteToOpen_camera);
                    if (type == BlinkEngine.BlinkDeviceType.Microphone)
                        deviceType = getResources().getString(R.string.meeting_control_inviteToOpen_microphone);
                    showConfirmDialog(deviceType, hostId, userId, BlinkEngine.BlinkActionType.InviteToOpen, type);
                } else {
                    if (userId.equals(getDeviceId())) {
                        if (type == BlinkEngine.BlinkDeviceType.Camera && !sideBar.getCheckBox("btnCloseCamera").isChecked())
                            sideBar.getCheckBox("btnCloseCamera").performClick();
                        if (type == BlinkEngine.BlinkDeviceType.Microphone && !sideBar.getCheckBox("btnMuteMic").isChecked())
                            sideBar.getCheckBox("btnMuteMic").performClick();
                        if (type == BlinkEngine.BlinkDeviceType.CameraAndMicrophone) {
                            if (!sideBar.getCheckBox("btnCloseCamera").isChecked())
                                sideBar.getCheckBox("btnCloseCamera").performClick();
                            if (!sideBar.getCheckBox("btnMuteMic").isChecked())
                                sideBar.getCheckBox("btnMuteMic").performClick();
                        }
                        BlinkEngine.getInstance().answerHostControlUserDevice(hostId, type, isOpen, true);
                    } //else toastMessage("主持人关闭了" + userId + "的:" + type.name());
                }
            }

            @Override
            public void onNotifyAnswerUpgradeObserverToNormalUser(String userId, boolean isAccept) {
                String statusString = "";
                if (isAccept)
                    statusString = " 同意";
                else
                    statusString = " 拒绝";
                toastMessage("用户:" + userId + statusString + "升级成正常用户");
            }

            @Override
            public void onNotifyAnswerObserverRequestBecomeNormalUser(String userId, long status) {
                if (status == BlinkEngine.BlinkAnswerActionType.Busy.getValue()) {
                    showToastLengthLong("主持人忙，请稍后再拨");
                } else if (status == BlinkEngine.BlinkAnswerActionType.Accept.getValue()) {
//                    Toast.makeText(CallActivity.this, "主持人同意", Toast.LENGTH_SHORT).show();
                    if (userId.equals(getDeviceId())) {
                        changeToObserverOrNormal(false);
                    } else {
//                        FinLog.i("","主持人同意了"+userId+"成为正常用户");
                    }
                } else if (status == BlinkEngine.BlinkAnswerActionType.Deny.getValue()) {
//                    Toast.makeText(CallActivity.this, "主持人拒绝", Toast.LENGTH_SHORT).show();
                }
            }

            /**
             * 主持人将其他与会人降级成为观察者时
             * @param userId   用户ID
             * @param isAccept 是否接受 true：被降级的与会人默认同意
             */
            @Override
            public void onNotifyAnswerDegradeNormalUserToObserver(String userId, boolean isAccept) {
                sharingMap.put(userId, false);
            }

            /**
             * @param userId   用户ID 3947CD61-BBFE-4623-8BA7-D5FD5D7E9162
             * @param isOpen   操作类型 false
             * @param dType    设备类型 1:摄像头 2 麦克风 3 摄像头+麦克风 -1无效
             * @param isAccept 是否接受 true
             */
            @Override
            public void onNotifyAnswerHostControlUserDevice(String userId, boolean isOpen, BlinkEngine.BlinkDeviceType dType, boolean isAccept) {
//                toastMessage("用户:" + userId + (isAccept ? " 同意" : " 拒绝") + "了你的请求:" + (isOpen ? " 打开" : " 关闭") + dType.name());
//                int talkType=-2; //0-只有音频；1-视频；2-音频+视频；3-无 // 0 or 3摄像头被关闭
//                if(isOpen){
//                    talkType=2;
//                }else{
//                   if(dType==BlinkEngine.BlinkDeviceType.Camera){
//                       talkType=0;
//                   }else if(dType==BlinkEngine.BlinkDeviceType.Microphone){
//                       talkType=1;
//                   }
//
//                }
//                renderViewManager.updateTalkType(userId,talkType);

                deviceCover(userId, isOpen, dType);
            }
        });

        StatusReportParser.debugCallbacks = new StatusReportParser.BlinkDebugCallbacks() {
            @Override
            public void onConnectionStats(final StatusReport statusReport) {
                updateNetworkSpeedInfo(statusReport);
                //只有Debug模式下才显示详细的调试信息
                if (renderViewManager == null || !BuildConfig.DEBUG)
                    return;
                parseToList(statusReport);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateDebugInfo(statusReport);
                    }
                });
            }
        };
    }

    private void updateNetworkSpeedInfo(StatusReport statusReport) {
        if (networkSpeedHandler != null) {
            Message message = new Message();
            Bundle bundle = new Bundle();
            message.what = 1;
            bundle.putLong("send", statusReport.bitRateSend);
            bundle.putLong("rcv", statusReport.bitRateRcv);
            message.setData(bundle);
            networkSpeedHandler.sendMessage(message);
        }
    }

    private void updateDebugInfo(StatusReport statusReport) {
        biteRateSendView.setText(statusReport.bitRateSend + "");
        biteRateRcvView.setText(statusReport.bitRateRcv + "");
        rttSendView.setText(statusReport.rtt + "");
        debugInfoAdapter.setStatusBeanList(statusBeanList);
        debugInfoAdapter.notifyDataSetChanged();
    }

    List<StatusBean> statusBeanList = new ArrayList<>();

    private void parseToList(StatusReport statusReport) {
        statusBeanList.clear();
        for (Map.Entry<String, StatusBean> entry : statusReport.statusVideoRcvs.entrySet()) {
            statusBeanList.add(entry.getValue());
        }
        for (Map.Entry<String, StatusBean> entry : statusReport.statusVideoSends.entrySet()) {
            statusBeanList.add(entry.getValue());
        }
        if (null != statusReport.statusAudioSend) {
            statusBeanList.add(statusReport.statusAudioSend);
        }
        for (Map.Entry<String, StatusBean> entry : statusReport.statusAudioRcvs.entrySet()) {
            statusBeanList.add(entry.getValue());
        }
    }

    private void loadWhiteBoard(String url, boolean isReload) {
        if (isReload) {
            whiteboardView.reload();
            progressDialog.show();
            return;
        }

        if (TextUtils.isEmpty(url)) {
            //重置白板按钮的状态
            sideBar.resetCbState();
//            btnWhiteBoard.performClick();
            showToastLengthLong(getResources().getString(R.string.meeting_control_no_whiteBoard));
            return;
        }

        progressDialog.show();
        showToastLengthLong(getResources().getString(R.string.meeting_control_OpenWiteBoard));
        mRelativeWebView.setVisibility(View.VISIBLE);
        FinLog.i(TAG, url);
        whiteboardView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        // https://web.blinktalk.online/ewbweb/blink-wb.html?roomKey=1234567890abcdefg1dg%40blinktest&token=eyJhbGciOiJIUzUxMiJ9.eyJyb29tS2V5IjoiMTIzNDU2Nzg5MGFiY2RlZmcxZGdAYmxpbmt0ZXN0IiwiZXhwIjoxNTE2MzQ0MTc1fQ.6izAdEW6yfYns7ACmKBVL6ymASq_28crvseMCIsjv-ITjfCXB2S8O7gcKv1CUclkSSfCGOvgfo4Pycl_Z0yM0Q&type=android

        // https://web.blinkcloud.cn/ewbweb/blink-wb.html?roomKey=1234567890abcdefg1dg%40blink&token=eyJhbGciOiJIUzUxMiJ9.eyJyb29tS2V5IjoiMTIzNDU2Nzg5MGFiY2RlZmcxZGdAYmxpbmsiLCJleHAiOjE1MTYzNDM3NjJ9.DJCa1mt67xW_5sfzxHUWi5O143UjgFl-LDNLfc8GlWp-khWACXIzYipA_L-9SIU7h8_16N2Pu-fLmePOeRX6pA&type=android
        whiteboardView.loadUrl(url);
        whiteboardView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // TODO Auto-generated method stub
                //返回值是true的时候控制去WebView打开，为false调用系统浏览器或第三方浏览器
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                progressDialog.dismiss();
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
//                super.onReceivedSslError(view, handler, error);
                try {
                    FinLog.i(TAG, "Ignore the certificate error.");
                    handler.proceed();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void copyInviteUrlToClipboard(String url) {
        toastMessage(getResources().getString(R.string.meeting_control_invite_tips));
        ClipboardManager mClipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData data = ClipData.newPlainText("meeting", String.format(getResources().getString(R.string.meeting_control_invite_url), channelID, url));
        mClipboardManager.setPrimaryClip(data);
    }

    private void toastMessage(String message) {
        //Toast.makeText(this,message,Toast.LENGTH_SHORT).show();
    }

    private void showConfirmDialog(final String message, final String hostUid, final String managedUid, final BlinkEngine.BlinkActionType action, final BlinkEngine.BlinkDeviceType type) {
        TextView msg = new TextView(this);
        msg.setText(message);
        msg.setPadding(10, 10, 10, 10);
        msg.setGravity(Gravity.CENTER);
        msg.setTextSize(18);
        ConfirmDialog = new AlertDialog.Builder(this).setView(msg)
                .setPositiveButton(getResources().getString(R.string.settings_text_observer_yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (action == null) {
                            disconnect();
                        } else if (action == BlinkEngine.BlinkActionType.InviteToOpen) {
                            removeActionState(type.getValue());
                            if (type == BlinkEngine.BlinkDeviceType.Camera && sideBar.getCheckBox("btnCloseCamera").isChecked())
                                sideBar.getCheckBox("btnCloseCamera").performClick();
                            if (type == BlinkEngine.BlinkDeviceType.Microphone && sideBar.getCheckBox("btnMuteMic").isChecked())
                                sideBar.getCheckBox("btnMuteMic").performClick();
                            if (type == BlinkEngine.BlinkDeviceType.CameraAndMicrophone) {
                                if (sideBar.getCheckBox("btnCloseCamera").isChecked())
                                    sideBar.getCheckBox("btnCloseCamera").performClick();
                                if (sideBar.getCheckBox("btnMuteMic").isChecked())
                                    sideBar.getCheckBox("btnMuteMic").performClick();
                            }
                            BlinkEngine.getInstance().answerHostControlUserDevice(hostUid, type, true, true);
                        } else if (action == BlinkEngine.BlinkActionType.RequestUpgradeToNormal) {
                            BlinkEngine.getInstance().answerObserverRequestBecomeNormalUser(managedUid, true);
                        } else if (action == BlinkEngine.BlinkActionType.UpgradeToNormal) {
                            removeActionState(0);
                            BlinkEngine.getInstance().answerUpgradeObserverToNormalUser(hostUid, true);
                            changeToObserverOrNormal(false);
                        }
                    }
                })
                .setNegativeButton(getResources().getString(R.string.settings_text_observer_no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (action == BlinkEngine.BlinkActionType.InviteToOpen) {
                            removeActionState(type.getValue());
                            BlinkEngine.getInstance().answerHostControlUserDevice(hostUid, type, true, false);
                        } else if (action == BlinkEngine.BlinkActionType.RequestUpgradeToNormal) {
                            BlinkEngine.getInstance().answerObserverRequestBecomeNormalUser(managedUid, false);
                        } else if (action == BlinkEngine.BlinkActionType.UpgradeToNormal) {
                            removeActionState(0);
                            BlinkEngine.getInstance().answerUpgradeObserverToNormalUser(hostUid, false);
                        }
                    }
                })
                .create();
        ConfirmDialog.setCanceledOnTouchOutside(false);
        ConfirmDialog.setCancelable(false);
        ConfirmDialog.show();
    }

    /**
     * 这是本地用户升降级回调
     *
     * @param toObserver false：观察者升级成正常用户
     */
    private void changeToObserverOrNormal(boolean toObserver) {
        if (toObserver) {
            BlinkContext.ConfigParameter.isObserver = true;
            renderViewManager.isObserver = true;
            //把自己的本地视图删除
            renderViewManager.removeVideoView(getDeviceId());

        } else {
            BlinkContext.ConfigParameter.isObserver = false;
            renderViewManager.isObserver = false;
            if (localSurface == null) {
                localSurface = BlinkEngine.createVideoView(getApplicationContext());
                BlinkEngine.getInstance().setLocalVideoView(localSurface);
            }
            FinLog.i(TAG, "用户：" + iUserName + " 升级成正常用户！");
            isVideoMute = false;
            renderViewManager.setVideoView(true, getDeviceId(), iUserName, localSurface, isVideoMute ? BlinkTalkTypeUtil.C_CAMERA : BlinkTalkTypeUtil.O_CAMERA);
//            BlinkEngine.getInstance().upgradeToNormalUser();//升级成正常用户
        }
        toggleCameraMicViewStatus();
    }

    /**
     * Initialize the UI to "waiting user join" status
     */
    private void initUIForWaitingStatus() {
        time = 0;
        textViewTime.setText(getResources().getText(R.string.connection_duration));
        textViewNetSpeed.setText(getResources().getText(R.string.network_traffic));
    }

    private void disconnect() {
        isConnected = false;
        BlinkEngine.getInstance().leaveChannel();
        if (audioManager != null) {
            audioManager.close();
            audioManager = null;
        }
        finish();
    }

    private Runnable memoryRunnable = new Runnable() {
        @Override
        public void run() {
            getSystemMemory();
            if (handler != null)
                handler.postDelayed(memoryRunnable, 1000);
        }
    };

    /**
     * @param type true:不弹窗  false：弹窗
     * @return
     */
    private boolean addActionState(int type, String hostUid, String userid) {
        if (null == stateMap) {
            stateMap = new LinkedHashMap<>();
        }
        boolean state = false;
        if (stateMap.containsKey(type)) {
            state = true;
        } else {
            ActionState bean = null;
            if (stateMap.size() > 0) {//之前有弹窗 保存key 不继续执行
                bean = new ActionState(type, hostUid, userid);
                stateMap.put(type, bean);
                state = true;
            } else {  //当前没有弹窗 保存 继续当前的执行（弹窗）
                bean = new ActionState(type, hostUid, userid);
                stateMap.put(type, bean);
                state = false;
            }
        }
        return state;
    }

    //将观察升级为正常用户=0, 摄像头:1 麦克风:2
    private void removeActionState(int keyType) {
        stateMap.remove((Integer) keyType);
        for (Map.Entry<Integer, ActionState> val : stateMap.entrySet()) {
            ActionState state = val.getValue();
            if (state.getType() == 0) {
                showConfirmDialog(getResources().getString(R.string.meeting_control_inviteToUpgrade), state.getHostUid(), state.getUserid(), BlinkEngine.BlinkActionType.UpgradeToNormal, null);
            } else {
                InviteToOpen(state.getType(), state.getUserid(), state.getHostUid());
            }
            return;
        }
    }

    private void InviteToOpen(int type, String userId, String hostId) {
        String deviceType = "";
        if (type == BlinkEngine.BlinkDeviceType.Camera.getValue())
            deviceType = getResources().getString(R.string.meeting_control_inviteToOpen_camera);
        if (type == BlinkEngine.BlinkDeviceType.Microphone.getValue())
            deviceType = getResources().getString(R.string.meeting_control_inviteToOpen_microphone);
        showConfirmDialog(deviceType, hostId, userId, BlinkEngine.BlinkActionType.InviteToOpen, type == BlinkEngine.BlinkDeviceType.Camera.getValue() ? BlinkEngine.BlinkDeviceType.Camera : BlinkEngine.BlinkDeviceType.Microphone);
    }

    private void clearState() {
        if (null != stateMap && stateMap.size() > 0) {
            stateMap.clear();
        }
    }

    private void getSystemMemory() {
        final ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(info);
        Runtime rt = Runtime.getRuntime();
        long maxMemory = rt.maxMemory();
        FinLog.e("max Memory:" + Long.toString(maxMemory / (1024 * 1024)));
        FinLog.e("free Memory:" + rt.freeMemory() / (1024 * 1024) + "m");
        FinLog.e("total Memory:" + rt.totalMemory() / (1024 * 1024) + "m");
        FinLog.e("系统是否处于低Memory运行：" + info.lowMemory);
        FinLog.e("当系统剩余Memory低于" + (info.threshold >> 10) / 1024 + "m时就看成低内存运行");
    }

    public void destroyWebView(WebView mWebView) {
        if (mWebView != null) {
            try {
                ViewParent parent = mWebView.getParent();
                if (parent != null) {
                    ((ViewGroup) parent).removeView(mWebView);
                }
                mWebView.stopLoading();
                mWebView.getSettings().setJavaScriptEnabled(false);
                mWebView.clearHistory();
                mWebView.clearView();
                mWebView.removeAllViews();

                mWebView.destroy();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 主持人、与会人 关闭or打开设备，显示封面视图设置
     *
     * @param userId
     * @param isOpen 打开/关闭
     * @param dType  0x01：摄像头 0x02：麦克风 0x03：摄像头+麦克风
     */
    private void deviceCover(String userId, boolean isOpen, BlinkEngine.BlinkDeviceType dType) {
//        int talkType = 3; //0-只有音频；1-视频；2-音频+视频；3-无 // 0 or 3摄像头被关闭
        renderViewManager.updateTalkType(userId, blinkTalkType(isOpen, dType));
    }

    private String blinkTalkType(boolean isOpen, BlinkEngine.BlinkDeviceType dType) {
        String talkType = "";
        if (isOpen) {
            if (dType == BlinkEngine.BlinkDeviceType.Camera) {
                talkType = BlinkTalkTypeUtil.O_CAMERA;
            } else if (dType == BlinkEngine.BlinkDeviceType.Microphone) {
                talkType = BlinkTalkTypeUtil.O_MICROPHONE;
            } else if (dType == BlinkEngine.BlinkDeviceType.CameraAndMicrophone) {
                talkType = BlinkTalkTypeUtil.O_CM;
            }
        } else {//
            if (dType == BlinkEngine.BlinkDeviceType.Camera) {
                talkType = BlinkTalkTypeUtil.C_CAMERA;
            } else if (dType == BlinkEngine.BlinkDeviceType.Microphone) {
                talkType = BlinkTalkTypeUtil.C_MICROPHONE;
            } else if (dType == BlinkEngine.BlinkDeviceType.CameraAndMicrophone) {
                talkType = BlinkTalkTypeUtil.C_CM;
            }
        }
        return talkType;
    }

    private void showToastLengthLong(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    /**
     * 横竖屏检测
     *
     * @param config
     */
    private void screenCofig(Configuration config) {
        try {
            Configuration configuration = null;
            if (config == null) {
                configuration = this.getResources().getConfiguration();
            } else {
                configuration = config;
            }
            int ori = configuration.orientation;
            if (ori == configuration.ORIENTATION_LANDSCAPE) {

            } else if (ori == configuration.ORIENTATION_PORTRAIT) {//v

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Boolean isSharing(String userid) {
        if (sharingMap.size() == 0) {
            return false;
        }
        if (sharingMap.containsKey(userid)) {
            return sharingMap.get(userid);
        } else {
            return false;
        }
    }

    private void exitRoom(String userId) {
        sharingMap.remove(userId);
        renderViewManager.delSelect(userId);
        //
        renderViewManager.removeVideoView(userId);
        if (!renderViewManager.hasConnectedUser()) {//除我以为,无外人
            initUIForWaitingStatus();
        }
    }

    /**
     * userjoin onNotifyUserVideoCreated
     *
     * @param talkType
     * @return
     */
    public String userJoinTaikType(long talkType) {
        String talk = "";
        if (talkType == 0) {
            talk = BlinkTalkTypeUtil.C_CAMERA;
        } else if (talkType == 1) {
            talk = BlinkTalkTypeUtil.O_CM;
        } else if (talkType == 2) {
            talk = BlinkTalkTypeUtil.C_MICROPHONE;
        } else if (talkType == 3) {
            talk = BlinkTalkTypeUtil.C_CM;
        }
        return talk;
    }
    /*--------------------------------------------------------------------------切换分辨率---------------------------------------------------------------------------*/

    /**
     * 构造分辨率对应的BlinkVideoProfile对象
     *
     * @param resolutionStr
     * @return
     */
    private BlinkEngine.BlinkVideoProfile selectiveResolution(String resolutionStr) {
        BlinkEngine.BlinkVideoProfile profile = null;
        String fpsStr = SessionManager.getInstance(this).getString(SettingActivity.FPS);
        if (TextUtils.isEmpty(fpsStr)) {
            fpsStr = "15";
        }
        if (CR_144x256.equals(resolutionStr)) {
            if ("15".equals(fpsStr)) {
                profile = BlinkEngine.BlinkVideoProfile.BLINK_VIDEO_PROFILE_144P_15f;
            } else if ("24".equals(fpsStr)) {
                profile = BlinkEngine.BlinkVideoProfile.BLINK_VIDEO_PROFILE_144P_24f;
            } else if ("30".equals(fpsStr)) {
                profile = BlinkEngine.BlinkVideoProfile.BLINK_VIDEO_PROFILE_144P_30f;
            }
        } else if (CR_240x320.equals(resolutionStr)) {
            if ("15".equals(fpsStr)) {
                profile = BlinkEngine.BlinkVideoProfile.BLINK_VIDEO_PROFILE_240P_15f;
            } else if ("24".equals(fpsStr)) {
                profile = BlinkEngine.BlinkVideoProfile.BLINK_VIDEO_PROFILE_240P_24f;
            } else if ("30".equals(fpsStr)) {
                profile = BlinkEngine.BlinkVideoProfile.BLINK_VIDEO_PROFILE_240P_30f;
            }
        } else if (CR_368x480.equals(resolutionStr)) {
            if ("15".equals(fpsStr)) {
                profile = BlinkEngine.BlinkVideoProfile.BLINK_VIDEO_PROFILE_360P_15f_1;
            } else if ("24".equals(fpsStr)) {
                profile = BlinkEngine.BlinkVideoProfile.BLINK_VIDEO_PROFILE_360P_24f_1;
            } else if ("30".equals(fpsStr)) {
                profile = BlinkEngine.BlinkVideoProfile.BLINK_VIDEO_PROFILE_360P_30f_1;
            }
        } else if (CR_368x640.equals(resolutionStr)) {
            if ("15".equals(fpsStr)) {
                profile = BlinkEngine.BlinkVideoProfile.BLINK_VIDEO_PROFILE_360P_15f_2;
            } else if ("24".equals(fpsStr)) {
                profile = BlinkEngine.BlinkVideoProfile.BLINK_VIDEO_PROFILE_360P_24f_2;
            } else if ("30".equals(fpsStr)) {
                profile = BlinkEngine.BlinkVideoProfile.BLINK_VIDEO_PROFILE_360P_30f_2;
            }
        } else if (CR_480x640.equals(resolutionStr)) {
            if ("15".equals(fpsStr)) {
                profile = BlinkEngine.BlinkVideoProfile.BLINK_VIDEO_PROFILE_480P_15f_1;
            } else if ("24".equals(fpsStr)) {
                profile = BlinkEngine.BlinkVideoProfile.BLINK_VIDEO_PROFILE_480P_24f_1;
            } else if ("30".equals(fpsStr)) {
                profile = BlinkEngine.BlinkVideoProfile.BLINK_VIDEO_PROFILE_480P_30f_1;
            }
        } else if (CR_480x720.equals(resolutionStr)) {
            if ("15".equals(fpsStr)) {
                profile = BlinkEngine.BlinkVideoProfile.BLINK_VIDEO_PROFILE_480P_15f_2;
            } else if ("24".equals(fpsStr)) {
                profile = BlinkEngine.BlinkVideoProfile.BLINK_VIDEO_PROFILE_480P_24f_2;
            } else if ("30".equals(fpsStr)) {
                profile = BlinkEngine.BlinkVideoProfile.BLINK_VIDEO_PROFILE_480P_30f_2;
            }
        } else if (CR_720x1280.equals(resolutionStr)) {
            if ("15".equals(fpsStr)) {
                profile = BlinkEngine.BlinkVideoProfile.BLINK_VIDEO_PROFILE_720P_15f;
            } else if ("24".equals(fpsStr)) {
                profile = BlinkEngine.BlinkVideoProfile.BLINK_VIDEO_PROFILE_720P_24f;
            } else if ("30".equals(fpsStr)) {
                profile = BlinkEngine.BlinkVideoProfile.BLINK_VIDEO_PROFILE_720P_30f;
            }
        } else if (CR_1080x1920.equals(resolutionStr)) {
            if ("15".equals(fpsStr)) {
                profile = BlinkEngine.BlinkVideoProfile.BLINK_VIDEO_PROFILE_1080P_15f;
            } else if ("24".equals(fpsStr)) {
                profile = BlinkEngine.BlinkVideoProfile.BLINK_VIDEO_PROFILE_1080P_24f;
            } else if ("30".equals(fpsStr)) {
                profile = BlinkEngine.BlinkVideoProfile.BLINK_VIDEO_PROFILE_1080P_30f;
            }
        } else if (CR_720x1280.equals(resolutionStr)) {
            if ("15".equals(fpsStr)) {
                profile = BlinkEngine.BlinkVideoProfile.BLINK_VIDEO_PROFILE_720P_15f;
            } else if ("24".equals(fpsStr)) {
                profile = BlinkEngine.BlinkVideoProfile.BLINK_VIDEO_PROFILE_720P_24f;
            } else if ("30".equals(fpsStr)) {
                profile = BlinkEngine.BlinkVideoProfile.BLINK_VIDEO_PROFILE_720P_30f;
            }
        }
        return profile;
    }

    private Map<String, ResolutionInfo> changeResolutionMap = null;
    private String[] resolution;

    private void setChangeResolutionMap() {
        ResolutionInfo info = null;
        changeResolutionMap = new HashMap<>();
        String key = "";
        resolution = new String[]{CR_144x256, CR_240x320, CR_368x480, CR_368x640, CR_480x640, CR_480x720, CR_720x1280, CR_1080x1920};
        try {
            for (int i = 0; i < resolution.length; i++) {
                key = resolution[i];
                info = new ResolutionInfo(key, i);
                changeResolutionMap.put(key, info);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void changeVideoSize(String action) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(BlinkContext.ConfigParameter.videoWidth);
        stringBuffer.append("x").append(BlinkContext.ConfigParameter.videoHeight);
        String resolutionStr = stringBuffer.toString();
        int index = -1;

        try {
            if (changeResolutionMap.containsKey(resolutionStr)) {
                index = changeResolutionMap.get(resolutionStr).getIndex();
            }
            if (action.equals("down")) {
                if (index != 0) {
                    //上一个的拿到了 需要获取到下一个index对应的分辨率
                    String str = resolution[index - 1];
                    FinLog.i("videoProfile", "降级至：" + str);
                    BlinkEngine.BlinkVideoProfile profile = selectiveResolution(str);
                    BlinkEngine.getInstance().changeVideoSize(profile);
                } else {
                    Toast.makeText(CallActivity.this, "已经降到最低!", Toast.LENGTH_SHORT).show();
                }
            } else if (action.equals("up")) {
                if (index != 7) {
                    //上一个的拿到了 需要获取到下一个index对应的分辨率
                    String str = resolution[index + 1];
                    FinLog.i("videoProfile", "升级至：" + str);
                    BlinkEngine.BlinkVideoProfile profile = selectiveResolution(str);
                    BlinkEngine.getInstance().changeVideoSize(profile);
                } else {
                    Toast.makeText(CallActivity.this, "已经升到最高!", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            FinLog.i(TAG, "error：" + e.getMessage());
        }
    }

    /*--------------------------------------------------------------------------AudioLevel---------------------------------------------------------------------------*/

    private void audiolevel(final int val, final String key) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (null != renderViewManager && null != renderViewManager.connetedRemoteRenders &&
                        renderViewManager.connetedRemoteRenders.containsKey(key)) {
                    if (val > 0) {
                        renderViewManager.connetedRemoteRenders.get(key).coverView.showAudioLevel();
                    } else {
                        renderViewManager.connetedRemoteRenders.get(key).coverView.closeAudioLevel();
                    }
                }
            }
        });
    }
}
