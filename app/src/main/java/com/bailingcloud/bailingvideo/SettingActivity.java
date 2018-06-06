package com.bailingcloud.bailingvideo;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bailingcloud.bailingvideo.base.BlinkBaseActivity;
import com.bailingcloud.bailingvideo.engine.context.BlinkContext;

import com.bailingcloud.bailingvideo.util.SessionManager;

/**
 * Created by suancai on 2016/11/4.
 */

public class SettingActivity extends BlinkBaseActivity {

    private LinearLayout testSettingOptionsContainer;

    private LinearLayout backButton;
    private static final String TAG = "SettingActivitytag";

    public static final String FPS = "FPS";
    public static final String CONNECTION_MODE = "CONNECTION_MODE";
    public static final String RESOLUTION = "RESOLUTION";
    public static final String CODECS = "CODECS";
    public static final String BIT_RATE_MIN = "BIT_RATE_MIN";
    public static final String BIT_RATE_MAX = "BIT_RATE_MAX";
    public static final String IS_OBSERVER = "IS_OBSERVER";
    public static final String IS_GPUIMAGEFILTER = "IS_GPUIMAGEFILTER";
    public static final String IS_SRTP = "IS_SRTP";
    public static final String IS_BLINKCONNECTIONMODE = "IS_BLINKCONNECTIONMODE";

    public static final String RESOLUTION_LOW = "240x320";
    public static final String RESOLUTION_MEDIUM = "480x640";
    public static final String RESOLUTION_HIGH = "720x1280";
    public static final String RESOLUTION_SUPER = "1080x1920(仅部分手机支持)";
    private String[] list_resolution = new String[]{RESOLUTION_LOW, RESOLUTION_MEDIUM, RESOLUTION_HIGH, RESOLUTION_SUPER};
    private String[] list_fps = new String[]{"15", "24", "30"};
    private String[] list_bitrate_max = new String[]{};
    private String[] list_bitrate_min = new String[]{};
    private String[] list_connectionMode = new String[]{"Relay", "P2P"};
    private String[] list_format = new String[]{"H264", "VP8", "VP9"};
    private String[] list_observer;
    private String[] list_gpuImageFilter;
    private String[] list_connectionType;

    private int defaultBitrateMinIndex = 0;
    private int defaultBitrateMaxIndex = 0;

    private static final int REQUEST_CODE_RESOLUTION = 12;
    private static final int REQUEST_CODE_FPS = 13;
    private static final int REQUEST_CODE_BITRATE_MAX = 14;
    private static final int REQUEST_CODE_MODE = 15;
    private static final int REQUEST_CODE_FORMAT = 16;
    private static final int REQUEST_CODE_BITRATE_MIN = 17;
    private static final int REQUEST_CODE_IS_OBSERVER = 18;
    private static final int REQUEST_CODE_IS_GPUIMAGEFILTER = 19;
    private static final int REQUEST_CODE_IS_SRTP = 20;
    private static final int REQUEST_CODE_IS_CONNECTIONTYPE = 21;

    private int tapStep = 0;
    private long lastClickTime = 0;
    private TextView settingOptionText1, settingOptionText2, settingOptionText3, settingOptionText4, settingOptionText5, settingOptionText6, settingOptionText7, settingOptionText8, settingOptionSRTP, settingOptionConnectionType;
    private LinearLayout settings_Modify;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        initViews();

        setupListeners();

    }

    private void initViews() {
        settings_Modify = (LinearLayout) findViewById(R.id.settings_Modify);

        list_observer = new String[]{getResources().getString(R.string.settings_text_observer_no), getResources().getString(R.string.settings_text_observer_yes)};
        list_gpuImageFilter = new String[]{getResources().getString(R.string.settings_text_gpufliter_no), getResources().getString(R.string.settings_text_gpufliter_yes)};
        list_connectionType = new String[]{getResources().getString(R.string.settings_text_connectionType_quic), getResources().getString(R.string.settings_text_connectionType_tcp)};

        settingOptionText1 = ((TextView) findViewById(R.id.setting_option_1_txt));
        String resolution = SessionManager.getInstance(this).getString(RESOLUTION);
        if (TextUtils.isEmpty(resolution))
            resolution = SessionManager.getInstance(this).put(RESOLUTION, list_resolution[1]);
        settingOptionText1.setText(resolution);
        reInitBitrates(settingOptionText1.getText().toString());

        settingOptionText2 = ((TextView) findViewById(R.id.setting_option_2_txt));
        String fps = SessionManager.getInstance(this).getString(FPS);
        if (TextUtils.isEmpty(fps))
            fps = SessionManager.getInstance(this).put(FPS, list_fps[0]);
        settingOptionText2.setText(fps);

        settingOptionText3 = ((TextView) findViewById(R.id.setting_option_3_txt));
        String biterate = SessionManager.getInstance(this).getString(BIT_RATE_MAX);
        if (TextUtils.isEmpty(biterate))
            biterate = SessionManager.getInstance(this).put(BIT_RATE_MAX, list_bitrate_max[defaultBitrateMaxIndex]);
        settingOptionText3.setText(biterate);

        settingOptionText4 = ((TextView) findViewById(R.id.setting_option_4_txt));
        String connectionMode = SessionManager.getInstance(this).getString(CONNECTION_MODE);
        if (TextUtils.isEmpty(connectionMode))
            connectionMode = SessionManager.getInstance(this).put(CONNECTION_MODE, list_connectionMode[0]);
        settingOptionText4.setText(connectionMode);

        settingOptionText5 = ((TextView) findViewById(R.id.setting_option_5_txt));
        String format = SessionManager.getInstance(this).getString(CODECS);
        if (TextUtils.isEmpty(format))
            format = SessionManager.getInstance(this).put(CODECS, list_format[0]);
        settingOptionText5.setText(format);

        settingOptionText6 = ((TextView) findViewById(R.id.setting_option_6_txt));
        String biterateMin = SessionManager.getInstance(this).getString(BIT_RATE_MIN);
        if (TextUtils.isEmpty(biterateMin))
            biterateMin = SessionManager.getInstance(this).put(BIT_RATE_MIN, list_bitrate_min[defaultBitrateMinIndex]);
        settingOptionText6.setText(biterateMin);

        settingOptionText7 = (TextView) findViewById(R.id.setting_option_7_txt);
        boolean isObserver = SessionManager.getInstance(this).getBoolean(IS_OBSERVER);
        String observerMode = list_observer[isObserver ? 1 : 0];
        settingOptionText7.setText(observerMode);

        settingOptionText8 = (TextView) findViewById(R.id.setting_option_8_txt);
        boolean isGpuImageFilter = SessionManager.getInstance(this).getBoolean(IS_GPUIMAGEFILTER);
        String gpuImageFiliter = list_gpuImageFilter[isGpuImageFilter ? 1 : 0];
        settingOptionText8.setText(gpuImageFiliter);

        settingOptionSRTP = (TextView) findViewById(R.id.setting_option_9_txt);
        boolean isSrtp = SessionManager.getInstance(this).getBoolean(IS_SRTP);
        String srtpOption = list_gpuImageFilter[isSrtp ? 1 : 0];
        settingOptionSRTP.setText(srtpOption);

        backButton = (LinearLayout) findViewById(R.id.settings_back);
        testSettingOptionsContainer = (LinearLayout) findViewById(R.id.setting_test_list);

        //connection type
        settingOptionConnectionType = (TextView) findViewById(R.id.setting_option_connectiontype_txt);
        boolean isQuic = SessionManager.getInstance(this).getBoolean(IS_BLINKCONNECTIONMODE);
        settingOptionConnectionType.setText(list_connectionType[isQuic ? 0 : 1]);
    }

    private void setupListeners() {
        findViewById(R.id.setting_option_1).setOnClickListener(new OnOptionViewClickListener(R.string.settings_text_resolution, list_resolution, REQUEST_CODE_RESOLUTION));
        findViewById(R.id.setting_option_2).setOnClickListener(new OnOptionViewClickListener(R.string.settings_text_fps, list_fps, REQUEST_CODE_FPS));
        findViewById(R.id.setting_option_3).setOnClickListener(new OnOptionViewClickListener(R.string.settings_text_rate, list_bitrate_max, REQUEST_CODE_BITRATE_MAX));
        findViewById(R.id.setting_option_4).setOnClickListener(new OnOptionViewClickListener(R.string.settings_text_connection_mode, list_connectionMode, REQUEST_CODE_MODE));
        findViewById(R.id.setting_option_5).setOnClickListener(new OnOptionViewClickListener(R.string.settings_text_coding_mode, list_format, REQUEST_CODE_FORMAT));
        findViewById(R.id.setting_option_6).setOnClickListener(new OnOptionViewClickListener(R.string.settings_text_min_rate, list_bitrate_min, REQUEST_CODE_BITRATE_MIN));
        findViewById(R.id.setting_option_7).setOnClickListener(new OnOptionViewClickListener(R.string.settings_text_observer, list_observer, REQUEST_CODE_IS_OBSERVER));
        findViewById(R.id.setting_option_8).setOnClickListener(new OnOptionViewClickListener(R.string.settings_text_gpufliter, list_gpuImageFilter, REQUEST_CODE_IS_GPUIMAGEFILTER));
        findViewById(R.id.setting_option_9).setOnClickListener(new OnOptionViewClickListener(R.string.settings_text_srtp, list_gpuImageFilter, REQUEST_CODE_IS_SRTP));
        findViewById(R.id.setting_option_connectiontype).setOnClickListener(new OnOptionViewClickListener(R.string.settings_text_connection, list_connectionType, REQUEST_CODE_IS_CONNECTIONTYPE));
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private class OnOptionViewClickListener implements View.OnClickListener {
        String title;
        String[] datas;
        int requestCode;

        public OnOptionViewClickListener(int resId, String[] datas, int requestCode) {
            this.title = getResources().getString(resId);
            this.datas = datas;
            this.requestCode = requestCode;
        }

        @Override
        public void onClick(View view) {
            Intent intent = new Intent(SettingActivity.this, OptionsPickActivity.class);
            intent.putExtra(OptionsPickActivity.BUNDLE_KEY_DATAS, datas);
            intent.putExtra(OptionsPickActivity.BUNDLE_KEY_TITLE, title);
            startActivityForResult(intent, requestCode);
        }
    }


    private void reInitBitrates(String resolution) {
        String kbps = "Kbps";
        try {
            //数组长度 間隔  最小
            int length = 0, parameters = 0, min = 0;//
            if (!TextUtils.isEmpty(resolution)) {
                if (RESOLUTION_MEDIUM.equals(resolution)) {
                    parameters = 10;
                    //                min=200;
                    length = (1000 - min) / parameters;
                } else if (RESOLUTION_HIGH.equals(resolution)) {
                    parameters = 10;
                    //                min=500;
                    length = (2000 - min) / parameters;
                } else if (RESOLUTION_LOW.equals(resolution)) {
                    parameters = 10;
                    //                min=100;
                    length = (600 - min) / parameters;
                } else if (RESOLUTION_SUPER.equals(resolution)) {
                    parameters = 10;
                    //                min=1500;
                    length = (4000 - min) / parameters;
                }
            }
            list_bitrate_max = new String[length + 1];
            list_bitrate_min = new String[length + 1];
            for (int i = 0; i <= length; i++) {
                int bitrate = i * parameters + min;
                list_bitrate_max[i] = bitrate + kbps;
                list_bitrate_min[i] = bitrate + kbps;
            }
            //設置默認
            if (!TextUtils.isEmpty(resolution) && RESOLUTION_MEDIUM.equals(resolution)) {
                defaultBitrateMinIndex = 10;//100
                defaultBitrateMaxIndex = 50;//500
            } else if (!TextUtils.isEmpty(resolution) && RESOLUTION_HIGH.equals(resolution)) {
                defaultBitrateMinIndex = 10;//100
                defaultBitrateMaxIndex = 150;//1500
            } else if (!TextUtils.isEmpty(resolution) && RESOLUTION_LOW.equals(resolution)) {
                defaultBitrateMinIndex = 10;//100
                defaultBitrateMaxIndex = 32;//320
            } else if (!TextUtils.isEmpty(resolution) && RESOLUTION_SUPER.equals(resolution)) {
                defaultBitrateMinIndex = 10;//100
                defaultBitrateMaxIndex = 250;//2500
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null)
            return;

        String result = data.getStringExtra(OptionsPickActivity.BUNDLE_KEY_RESULT);
        if (TextUtils.isEmpty(result))
            return;
        switch (requestCode) {
            case REQUEST_CODE_RESOLUTION:
                settingOptionText1.setText(result);
                SessionManager.getInstance(this).put(RESOLUTION, result);
                break;
            case REQUEST_CODE_FPS:
                SessionManager.getInstance(this).put(FPS, result);
                settingOptionText2.setText(result);
                break;
            case REQUEST_CODE_BITRATE_MAX:
                settingOptionText3.setText(result);
                SessionManager.getInstance(this).put(BIT_RATE_MAX, result);
                break;
            case REQUEST_CODE_MODE:
                settingOptionText4.setText(result);
                SessionManager.getInstance(this).put(CONNECTION_MODE, result);
                break;
            case REQUEST_CODE_FORMAT:
                settingOptionText5.setText(result);
                SessionManager.getInstance(this).put(CODECS, result);
                break;
            case REQUEST_CODE_BITRATE_MIN:
                settingOptionText6.setText(result);
                SessionManager.getInstance(this).put(BIT_RATE_MIN, result);
                break;
            case REQUEST_CODE_IS_OBSERVER:
                settingOptionText7.setText(result);
                SessionManager.getInstance(this).put(IS_OBSERVER, result.equals(list_observer[1]));
                break;
            case REQUEST_CODE_IS_GPUIMAGEFILTER:
                settingOptionText8.setText(result);
                SessionManager.getInstance(this).put(IS_GPUIMAGEFILTER, result.equals(list_gpuImageFilter[1]));
                break;
            case REQUEST_CODE_IS_SRTP:
                settingOptionSRTP.setText(result);
                SessionManager.getInstance(this).put(IS_SRTP, result.equals(list_gpuImageFilter[1]));
                break;
            case REQUEST_CODE_IS_CONNECTIONTYPE:
                settingOptionConnectionType.setText(result);
                if (list_connectionType[0].equals(result)) {
                    BlinkEngine.getInstance().setBlinkConnectionMode(true);
                } else if (list_connectionType[1].equals(result)) {
                    BlinkEngine.getInstance().setBlinkConnectionMode(false);
                }
                SessionManager.getInstance(this).put(IS_BLINKCONNECTIONMODE, BlinkContext.ConfigParameter.blinkConnectionMode == BlinkEngine.BlinkConnectionMode.QUIC ? true : false);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
