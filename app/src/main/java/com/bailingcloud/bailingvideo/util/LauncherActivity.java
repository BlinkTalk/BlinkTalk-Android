package com.bailingcloud.bailingvideo.util;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import com.bailingcloud.bailingvideo.MainPageActivity;
import com.bailingcloud.bailingvideo.R;

import static com.bailingcloud.bailingvideo.SettingActivity.IS_BLINKCONNECTIONMODE;
import com.bailingcloud.bailingvideo.MainPageActivity;
import com.bailingcloud.bailingvideo.R;
import com.bailingcloud.bailingvideo.base.BlinkBaseActivity;

/**
 * Created by suancaicai on 2016/9/27.
 */
public class LauncherActivity extends BlinkBaseActivity {
    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_launcher);
        ((TextView)findViewById(R.id.launcher_loading)).setTextColor(getResources().getColor(R.color.blink_launcher_grey));

        if(!SessionManager.getInstance(this).contains(IS_BLINKCONNECTIONMODE)){
            SessionManager.getInstance(this).put(IS_BLINKCONNECTIONMODE,true);
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                skipToMainPage();
            }
        },1000);
    }

    private void skipToMainPage()
    {
        Intent intent = new Intent(this, MainPageActivity.class);
        startActivity(intent);
        finish();
    }
}
