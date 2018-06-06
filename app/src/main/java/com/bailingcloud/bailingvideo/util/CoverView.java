package com.bailingcloud.bailingvideo.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bailingcloud.bailingvideo.R;
import com.bailingcloud.bailingvideo.VideoViewManager;
import com.bailingcloud.bailingvideo.engine.binstack.util.FinLog;
import com.bailingcloud.bailingvideo.engine.view.BlinkVideoView;
import com.bumptech.glide.Glide;

import java.util.Random;

/**
 * @Author DengXuDong.
 * @Time 2018/2/7.
 * @Description:
 */
public class CoverView extends RelativeLayout{

    private static final String TAG="CoverView";

    public RelativeLayout mRl_Container,rl_CoverBase;
    public TextView tv_userName;
    private GradientDrawable mGroupDrawable;
    public ImageView iv_Header,iv_Audiolevel;
    private Context mContext;
    //    private TextPaint textPaint;
    private String UserId="",UserName="BaiLing";
    public ProgressBar progressBar;
    public BlinkVideoView blinkVideoView=null;
    public VideoViewManager.RenderHolder mRenderHolder;

    public CoverView(Context context) {
        super(context);
        this.mContext=context;
        init();
    }

    public CoverView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CoverView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void init() {
        try {
            LayoutInflater.from(mContext).inflate(R.layout.layout_cover, this, true);
            mRl_Container= (RelativeLayout) findViewById(R.id.relative_cover);
            rl_CoverBase= (RelativeLayout) findViewById(R.id.rl_CoverBase);

            progressBar= (ProgressBar) findViewById(R.id.progressBar);
            tv_userName= (TextView) findViewById(R.id.tv_UserName);
            tv_userName.setTextColor(Color.WHITE);
            tv_userName.clearFocus();

//            textPaint=tv_userName.getPaint();
//            textPaint.setFakeBoldText(true);
            iv_Header= (ImageView) findViewById(R.id.iv_bg);
            iv_Audiolevel= (ImageView) findViewById(R.id.iv_audiolevel);
            Glide.with(Utils.getContext()).load(R.drawable.sound).into(iv_Audiolevel);

            mGroupDrawable= (GradientDrawable) iv_Header.getBackground();

            int Height=SessionManager.getInstance(Utils.getContext()).getInt(Utils.KEY_screeHeight);
            int width=SessionManager.getInstance(Utils.getContext()).getInt(Utils.KEY_screeWidth);
            ViewGroup.LayoutParams para;
            para = iv_Header.getLayoutParams();
            para.height = Height;
            para.width = width;

            iv_Header.setLayoutParams(para);
            iv_Header.setOnClickListener(clickListener);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showAudioLevel(){
        if(null==iv_Audiolevel){return;}
        if(iv_Audiolevel.getVisibility()!=VISIBLE){
            iv_Audiolevel.setVisibility(VISIBLE);
        }
    }

    public void closeAudioLevel(){
        if(null==iv_Audiolevel){return;}
        if(iv_Audiolevel.getVisibility()!=INVISIBLE){
            iv_Audiolevel.setVisibility(INVISIBLE);
        }
    }

    public void setUserInfo(String name,String id){
        if(null!=tv_userName && !TextUtils.isEmpty(name)){
            UserName=UserUtils.truncatameUserName(name);
            tv_userName.setText(UserName);
        }

        if(!TextUtils.isEmpty(id)){
            this.UserId=id;
        }
        setUserType();
    }

    /**
     *隱藏用戶名等
     */
    private void setCoverTransoarent(){
        iv_Header.setVisibility(INVISIBLE);
        tv_userName.setVisibility(INVISIBLE);
        closeLoading();
    }

    /**
     * 設置視頻
     * @param videoView
     */
    public void setBlinkVideoView(BlinkVideoView videoView){
        this.blinkVideoView=videoView;
        if(null==mRl_Container){return;}
        try {
            for (int i = 0; i < mRl_Container.getChildCount(); i++) {
                if(mRl_Container.getChildAt(i) instanceof BlinkVideoView){
                    mRl_Container.removeView(mRl_Container.getChildAt(i));
                }
            }
            LayoutParams p = new LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT);
            p.addRule(RelativeLayout.CENTER_IN_PARENT);
            mRl_Container.addView(blinkVideoView,p);

//            LayoutParams p1 = new LayoutParams(20, 20);
//            iv_Audiolevel=new ImageView(rl_CoverBase.getContext());
//            iv_Audiolevel.setBackgroundColor(Color.RED);
//            rl_CoverBase.addView(iv_Audiolevel,2,p1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showBlinkVideoView(){
        for (int i = 0; i < mRl_Container.getChildCount(); i++) {
            if(mRl_Container.getChildAt(i) instanceof  BlinkVideoView){
                mRl_Container.getChildAt(i).setVisibility(VISIBLE);
            }
        }
        setCoverTransoarent();
    }

    public void showUserHeader(){
        try {
            iv_Header.setVisibility(VISIBLE);
            tv_userName.setVisibility(VISIBLE);
            for (int i = 0; i < mRl_Container.getChildCount(); i++) {
                if(mRl_Container.getChildAt(i) instanceof  BlinkVideoView){
                    BlinkVideoView videoView= (BlinkVideoView) mRl_Container.getChildAt(i);
                    if(videoView.getVisibility()==VISIBLE){
                        closeLoading();
                    }
//                    FinLog.i(TAG,"显示头像之前 blinkVideo是可见？="+(mRl_Container.getChildAt(i).getVisibility()==VISIBLE));
                    videoView.setVisibility(INVISIBLE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            FinLog.i(TAG,"error:"+e.getMessage());
        }
    }

    public void showLoading(){
        if(null==progressBar){return;}
        progressBar.setVisibility(VISIBLE);
    }

    public void closeLoading(){
        if(null==progressBar){return;}
        progressBar.setVisibility(GONE);
    }

    /**
     * 设置用户类型 根据类型设置背景
     */
    private void setUserType(){
        String colorStr=" ";
        colorStr=SessionManager.getInstance(Utils.getContext()).getString("color"+UserId);
        if(!TextUtils.isEmpty(colorStr)){
            mGroupDrawable.setColor(Color.parseColor(colorStr));
            return;
        }
        if(mGroupDrawable!= null){
            switch (new Random().nextInt(6)){
                case 0:
                    colorStr="#0066CC";
                    break;
                case 1:
                    colorStr="#009900";
                    break;
                case 2:
                    colorStr="#CC3333";
                    break;
                case 3:
                    colorStr="#CC9966";
                    break;
                case 4:
                    colorStr="#FF9900";
                    break;
                case 5:
                    colorStr="#CC33CC";
                    break;
            }
            mGroupDrawable.setColor(Color.parseColor(colorStr));
        }
    }

    public BlinkVideoView getBlinkVideoView(){
        return blinkVideoView;
    }

    private OnClickListener clickListener=new OnClickListener() {
        @Override
        public void onClick(View v) {
            try {
                if(blinkVideoView!=null){
                    mGroupDrawable.setGradientType(GradientDrawable.RADIAL_GRADIENT);
                    blinkVideoView.performClick();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
}
