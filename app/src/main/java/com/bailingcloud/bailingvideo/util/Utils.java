package com.bailingcloud.bailingvideo.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.bailingcloud.bailingvideo.BlinkEngine;
import com.bailingcloud.bailingvideo.engine.binstack.util.FinLog;
import com.bailingcloud.bailingvideo.engine.context.BlinkContext;

import java.math.BigDecimal;

import static com.bailingcloud.bailingvideo.SettingActivity.IS_BLINKCONNECTIONMODE;

/**
 * @Author DengXudong.
 * @Time 2018/1/22.
 * @Description:
 */
public class Utils {
    private static Context mContext=null;

    public static final String KEY_screeHeight="screeHeight";
    public static final String KEY_screeWidth="screeWidth";

    private Utils(){ throw new UnsupportedOperationException("BlinkUtils Error!");}

    public static void init(Context context){Utils.mContext=context.getApplicationContext();}

    public static Context getContext(){
        if(null!=mContext){
            return mContext;
        }
        throw new NullPointerException("u should context init first");
    }

    /**
     * 判断网络是否可用
     *
     * @param context
     * @return
     */
    public static boolean isNetWorkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    public static final  String QUIC="quic://",TCP="tcp://";
    //默认使用的取token地址 release
    public static final String TOKEN_SERVER_URL_EXTERNAL = "https://api.blinkcloud.cn:8800/token";
    //默认使用的cmp地址 release
    public static final String CMP_SERVER_URL_EXTERNAL = "cmp.blinkcloud.cn:80";

    /**
     * 判断用户输入的cmp地址是否包含标识
     * @param val
     * @return
     */
    public static boolean isQuicOrTcp(String val){
        if(val.indexOf(QUIC)==-1 && val.indexOf(TCP)==-1){
            return false;
        }else{
            return true;
        }
    }

    /**
     * 根据地址标识修改连接方式
     * @param val
     * @return 根据返回bool去更新开关值
     */
    public static boolean connectionModeConfig(String val){
        boolean isQuic= BlinkContext.ConfigParameter.blinkConnectionMode==BlinkEngine.BlinkConnectionMode.QUIC?true:false;
        if(val.indexOf(QUIC)!=-1){
            BlinkEngine.getInstance().setBlinkConnectionMode(true);
            isQuic= true;
        }else if(val.indexOf(TCP)!=-1){
            BlinkEngine.getInstance().setBlinkConnectionMode(false);
            isQuic= false;
        }
        SessionManager.getInstance(Utils.getContext()).put(IS_BLINKCONNECTIONMODE,isQuic);
        FinLog.i("BinClient","输入的是："+(isQuic?"Quic":"Tcp"+"地址"));
        return isQuic;
    }

    /**
     * 当请求configlistr列表出现异常 或为局域网等情况 使用默认地址初始化
     * @param mHandler
     */
    public static void getConfigListError(Handler mHandler){
        Message message=new Message();
        message.what=2;
        Bundle bundle=new Bundle();
        bundle.putString("cmpServer",CMP_SERVER_URL_EXTERNAL);
        bundle.putString("tokenUrl",TOKEN_SERVER_URL_EXTERNAL);
        message.setData(bundle);
        mHandler.sendMessage(message);
    }

    /**
     * 重置所有控制连接的 状态为指定的bool
     * @param val false:tcp true:quic
     */
    public static void resetConnectionMode(boolean val){
        BlinkEngine.getInstance().setBlinkConnectionMode(val);
        SessionManager.getInstance(Utils.getContext()).put(IS_BLINKCONNECTIONMODE, val ? true : false);
    }
}
