package com.bailingcloud.bailingvideo;

import android.app.Application;

import com.bailingcloud.bailingvideo.util.Utils;
import com.tencent.bugly.crashreport.CrashReport;

/**
 * Created by suancai on 2016/11/22.
 */

public class BlinkApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Utils.init(this);
        // 内测时设置为true ， 发布时修改为false
        CrashReport.initCrashReport(getApplicationContext(), "ef48d6a01a", true);
    }
}
