package com.bailingcloud.bailingvideo;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.blink.RendererCommon;
import com.bailingcloud.bailingvideo.engine.binstack.util.FinLog;
import com.bailingcloud.bailingvideo.engine.context.BlinkContext;
import com.bailingcloud.bailingvideo.engine.view.BlinkVideoView;
import com.bailingcloud.bailingvideo.util.BlinkTalkTypeUtil;
import com.bailingcloud.bailingvideo.util.CoverView;
import com.bailingcloud.bailingvideo.util.SessionManager;
import com.bailingcloud.bailingvideo.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.bailingcloud.bailingvideo.engine.context.BlinkContext.ConfigParameter.userID;

/**
 * Created by Huichao on 2016/8/26.
 */
public class VideoViewManager {

    private RelativeLayout remoteRenderLayout, remoteRenderLayout2, remoteRenderLayout3, remoteRenderLayout4, remoteRenderLayout5, remoteRenderLayout6, remoteRenderLayout7, remoteRenderLayout8, remoteRenderLayout9;

    private Context context;
    private LinearLayout holderContainer;
    private ContainerLayout holderBigContainer;
    private RenderHolder selectedRender;
    private LinearLayout debugInfoView;
    public boolean isObserver;
    private List<RenderHolder> unUsedRemoteRenders = new ArrayList<>();
    private int screenWidth;
    private int screenHeight;

    public HashMap<String, RenderHolder> connetedRemoteRenders = new HashMap<>();
    public HashMap<String, RenderHolder> connectedUsers = new HashMap<>();
    private ArrayList<RenderHolder> positionRenders = new ArrayList<>();

    LinearLayout.LayoutParams remoteLayoutParams;
//    RelativeLayout.LayoutParams localLayoutParams;
    /**
     * 存储当前显示在大屏幕上的用户id
     **/
    private List<String> selectedUserid = new ArrayList<>();

    public void initViews(Context context, boolean isObserver) {
        this.context = context;
        getSize();
        int base = screenHeight < screenWidth ? screenHeight : screenWidth;
        remoteLayoutParams = new LinearLayout.LayoutParams(base / 4, base / 3);

        SessionManager.getInstance(Utils.getContext()).put(Utils.KEY_screeHeight, base / 3);
        SessionManager.getInstance(Utils.getContext()).put(Utils.KEY_screeWidth, base / 4);

        holderContainer = (LinearLayout) ((Activity) context).findViewById(R.id.call_reder_container);
        holderBigContainer = (ContainerLayout) ((Activity) context).findViewById(R.id.call_render_big_container);
        debugInfoView = (LinearLayout) ((Activity) context).findViewById(R.id.debug_info);
        debugInfoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doubleClick(false);
            }
        });


        this.isObserver = isObserver;

        remoteRenderLayout = (RelativeLayout) ((Activity) context).findViewById(R.id.remote_video_layout);
        remoteRenderLayout2 = (RelativeLayout) ((Activity) context).findViewById(R.id.remote_video_layout2);
        remoteRenderLayout3 = (RelativeLayout) ((Activity) context).findViewById(R.id.remote_video_layout3);
        remoteRenderLayout4 = (RelativeLayout) ((Activity) context).findViewById(R.id.remote_video_layout4);
        remoteRenderLayout5 = (RelativeLayout) ((Activity) context).findViewById(R.id.remote_video_layout5);
        remoteRenderLayout6 = (RelativeLayout) ((Activity) context).findViewById(R.id.remote_video_layout6);
        remoteRenderLayout7 = (RelativeLayout) ((Activity) context).findViewById(R.id.remote_video_layout7);
        remoteRenderLayout8 = (RelativeLayout) ((Activity) context).findViewById(R.id.remote_video_layout8);
        remoteRenderLayout9 = (RelativeLayout) ((Activity) context).findViewById(R.id.remote_video_layout9);

        // Create video renderers.
        initRemoteRendersList();

        holderBigContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onLocalVideoViewClickedListener != null)
                    onLocalVideoViewClickedListener.onClick();
            }
        });

        holderContainer.removeAllViews();
        holderBigContainer.removeAllViews();
        toggleTips();
    }

    private void getSize() {
        WindowManager wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);

        screenWidth = wm.getDefaultDisplay().getWidth();
        screenHeight = wm.getDefaultDisplay().getHeight();
    }

    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    private class RemoteRenderClickListener implements View.OnClickListener {
        private RenderHolder renderHolder;

        public RemoteRenderClickListener(RenderHolder renderHolder) {
            this.renderHolder = renderHolder;
        }

        @Override
        public void onClick(View view) {
            touchRender(renderHolder);
            doubleClick(true);
        }
    }

    long lastClickTime = 0;
    int tapStep = 0;

    private void doubleClick(boolean isShow) {
        if (!BuildConfig.DEBUG)
            return;
        long timeDuration = System.currentTimeMillis() - lastClickTime;
        if (timeDuration > 300) {
            tapStep = 0;
            lastClickTime = 0;
        } else {
            tapStep++;
            if (tapStep == 1) {
                if (isShow)
                    debugInfoView.setVisibility(View.VISIBLE);
                else debugInfoView.setVisibility(View.GONE);
            }
        }
        lastClickTime = System.currentTimeMillis();
    }

    public void touchRender(RenderHolder render) {
        if (render == selectedRender) {//被点击的是大窗口，不做窗口切换
            if (onLocalVideoViewClickedListener != null)
                onLocalVideoViewClickedListener.onClick();
            return;
        }
        ArrayList<BlinkContext.MediaStreamTypeMode> mediaStreamTypeModeList = new ArrayList<BlinkContext.MediaStreamTypeMode>();
        int index = positionRenders.indexOf(render);
        RenderHolder lastSelectedRender = selectedRender;
        positionRenders.set(index, selectedRender);
        selectedRender = render;

        if (!lastSelectedRender.userId.equals(userID)) {
            //原来的大窗口变小流
            BlinkContext.MediaStreamTypeMode mediaStreamTypeModeTiny = new BlinkContext.MediaStreamTypeMode();
            mediaStreamTypeModeTiny.uid = lastSelectedRender.userId;
            mediaStreamTypeModeTiny.flowType = "2";
            mediaStreamTypeModeList.add(mediaStreamTypeModeTiny);
        }

        holderContainer.removeView(selectedRender.containerLayout);
        holderBigContainer.removeView(lastSelectedRender.containerLayout);

        //大窗口显示于宿主窗口下层
        selectedRender.coverView.getBlinkVideoView().setZOrderMediaOverlay(false);

        if (!selectedRender.userId.equals(userID)) {
            //原来的小窗口变大流
            BlinkContext.MediaStreamTypeMode mediaStreamTypeMode = new BlinkContext.MediaStreamTypeMode();
            mediaStreamTypeMode.uid = selectedRender.userId;
            mediaStreamTypeMode.flowType = "1";
            mediaStreamTypeModeList.add(mediaStreamTypeMode);
        }
        holderBigContainer.addView(selectedRender, screenWidth, screenHeight);

        //添加之后设置VIdeoView的缩放类型,防止旋转屏幕之后 pc共享 再切换大小 显示不全问题
        selectedRender.coverView.getBlinkVideoView().setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);

//        lastSelectedRender.coverView.getBlinkVideoView().setZOrderOnTop(true);
        lastSelectedRender.coverView.getBlinkVideoView().setZOrderMediaOverlay(true);

        //防止横竖切换 再 小大切换 导致的小屏尺寸没更新 host——304
        lastSelectedRender.coverView.getBlinkVideoView().setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        //pc分屏时 必须通知videoview listener 去刷新size//手动转屏不需要通知listerer
        holderContainer.addView(lastSelectedRender.containerLayout, index, remoteLayoutParams);

        saveSelectUserId(render);
        //2.0取消大小流 故注掉
//        BlinkEngine.getInstance().subscribeStream(mediaStreamTypeModeList);
    }

    private void initRemoteRendersList() {
        unUsedRemoteRenders.add(new RenderHolder(remoteRenderLayout, 0));
        unUsedRemoteRenders.add(new RenderHolder(remoteRenderLayout2, 1));
        unUsedRemoteRenders.add(new RenderHolder(remoteRenderLayout3, 2));
        unUsedRemoteRenders.add(new RenderHolder(remoteRenderLayout4, 3));
        unUsedRemoteRenders.add(new RenderHolder(remoteRenderLayout5, 4));
        unUsedRemoteRenders.add(new RenderHolder(remoteRenderLayout6, 5));
        unUsedRemoteRenders.add(new RenderHolder(remoteRenderLayout7, 6));
        unUsedRemoteRenders.add(new RenderHolder(remoteRenderLayout8, 7));
        unUsedRemoteRenders.add(new RenderHolder(remoteRenderLayout9, 8));
    }

//    public void setLocalRender(BlinkVideoView render, int talkType) {
//        localHolder.blinkVideoView = render;
//        localHolder.init(talkType);
//        localHolder.blinkVideoView.setOnClickListener(new RemoteRenderClickListener(localHolder));
//    }

    public void userJoin(String userID, String userName, String talkType) {
        try {
            if (connectedUsers.size() == 0) {
                RenderHolder renderHolder = unUsedRemoteRenders.get(0);
                renderHolder.userName = userName;
                renderHolder.userId = userID;
                renderHolder.initCover(talkType);
                connectedUsers.put(userID, renderHolder);

                ((CallActivity) context).setWaitingTipsVisiable(false);

                unUsedRemoteRenders.remove(0);
            }
            if (connectedUsers.size() != 0 && connectedUsers != null && !connectedUsers.containsKey(userID)) {
                RenderHolder renderHolder = unUsedRemoteRenders.get(0);
                renderHolder.userName = userName;
                renderHolder.userId = userID;
                renderHolder.initCover(talkType);
                connectedUsers.put(userID, renderHolder);
                holderContainer.addView(renderHolder.containerLayout, remoteLayoutParams);
                ((CallActivity) context).setWaitingTipsVisiable(false);

                unUsedRemoteRenders.remove(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param talkType BlinkTalkTypeUtil
     */
    public void setVideoView(boolean isSelf, String userID, String userName, BlinkVideoView render, String talkType) {
        //第一个加进来的人，放入大窗口
        if (connetedRemoteRenders.size() == 0) {
            RenderHolder renderHolder = null;
            if (isSelf) {
                renderHolder = unUsedRemoteRenders.get(0);
                unUsedRemoteRenders.remove(0);
            } else {
                renderHolder = connectedUsers.get(userID);
            }
            renderHolder.userName = userName;
            renderHolder.userId = userID;

            render.setOnClickListener(new RemoteRenderClickListener(renderHolder));
            //添加缩放解决 观察者 横屏 进入pc的共享屏幕 导致的 显示不全问题
            render.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_BALANCED);
            connetedRemoteRenders.put(userID, renderHolder);
            connectedUsers.put(userID, renderHolder);
            renderHolder.init(talkType, isSelf);
//            positionRenders.add(renderHolder);
//            unUsedRemoteRenders.remove(0);

            selectedRender = renderHolder;

//            render.setZOrderOnTop(false);
            render.setZOrderMediaOverlay(false);

            renderHolder.coverView.setBlinkVideoView(render);

            //本地与会人加入时未打开摄像头
            if (talkType == BlinkTalkTypeUtil.C_CAMERA) {
                renderHolder.coverView.showUserHeader();
            }
            holderBigContainer.addView(renderHolder, screenWidth, screenHeight);

            toggleTips();
            saveSelectUserId(renderHolder);
            return;
        }
        if (unUsedRemoteRenders.size() != 0 && connetedRemoteRenders != null && !connetedRemoteRenders.containsKey(userID)) {
            RenderHolder renderHolder = null;
            //存在就是正常加入的用户，不存在是被降级 后升级的用户
            if (connectedUsers.containsKey(userID)) {
                renderHolder = connectedUsers.get(userID);
                renderHolder.userName = userName;
            } else {
                renderHolder = unUsedRemoteRenders.get(0);
                renderHolder.userName = userName;
                renderHolder.initCover(talkType);
                connectedUsers.put(userID, renderHolder);

                holderContainer.addView(renderHolder.containerLayout, remoteLayoutParams);
                ((CallActivity) context).setWaitingTipsVisiable(false);

                unUsedRemoteRenders.remove(0);
            }

            renderHolder.userId = userID;

            render.setOnClickListener(new RemoteRenderClickListener(renderHolder));
            connetedRemoteRenders.put(userID, renderHolder);
            positionRenders.add(renderHolder);
//            unUsedRemoteRenders.remove(0);

            render.setZOrderOnTop(true);
            render.setZOrderMediaOverlay(true);
            render.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);

            renderHolder.coverView.setBlinkVideoView(render);

            renderHolder.init(talkType, isSelf);

        } else if (unUsedRemoteRenders.size() != 0 && connetedRemoteRenders != null && connetedRemoteRenders.containsKey(userID)) {
            //host_545
            RenderHolder renderHolder = connetedRemoteRenders.get(userID);
            renderHolder.userName = userName;
            renderHolder.userId = userID;

            holderContainer.removeView(renderHolder.containerLayout);

            render.setOnClickListener(new RemoteRenderClickListener(renderHolder));
            render.setZOrderOnTop(true);
            render.setZOrderMediaOverlay(true);
            render.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);

//            renderHolder.release();
            renderHolder.coverView.setBlinkVideoView(render);

            holderContainer.addView(renderHolder.containerLayout, remoteLayoutParams);

            renderHolder.init(isSelf);
        }
    }

    public void rotateView() {
        getSize();//重新获取屏幕的宽高 w:1920 h:1080
        holderBigContainer.refreshView(this.screenWidth, this.screenHeight);
    }

    /**
     * 退出聊天室时用到
     *
     * @param userID
     */
    public void removeVideoView(String userID) {
        SessionManager.getInstance(Utils.getContext()).remove("color" + userID);
        if (connectedUsers.containsKey(userID)) {
            if (null != connectedUsers.get(userID).containerLayout) {
                holderContainer.removeView(connectedUsers.get(userID).containerLayout);
            }
            connectedUsers.get(userID).coverView.removeAllViews();
            connectedUsers.remove(userID);
        }

        if (connetedRemoteRenders.containsKey(userID)) { //

            connetedRemoteRenders.get(userID).release();
            RenderHolder releaseTaget = connetedRemoteRenders.remove(userID);
            int index = 0;
            for (int i = 0; i < unUsedRemoteRenders.size(); i++) {
                RenderHolder compare = unUsedRemoteRenders.get(i);
                if (releaseTaget.targetZindex < compare.targetZindex) {
                    index = i;
                    break;
                }
            }
            try {
                if (releaseTaget.coverView != null) {
                    if (releaseTaget.coverView.blinkVideoView != null) {
                        if (releaseTaget.coverView.mRl_Container != null) {
                            releaseTaget.coverView.mRl_Container.removeView(releaseTaget.coverView.blinkVideoView);
                        }
                    }
                    releaseTaget.coverView = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            unUsedRemoteRenders.add(index, releaseTaget);

            if (selectedRender == releaseTaget) {
                if (connetedRemoteRenders.size() != 0) {
                    Set set = connetedRemoteRenders.entrySet();
                    Iterator iterator = set.iterator();
                    while (iterator.hasNext()) {
                        Map.Entry mapentry = (Map.Entry) iterator.next();
                        RenderHolder newRender = (RenderHolder) mapentry.getValue();// 从远程连接中获取到新的 渲染器
                        String id = (String) mapentry.getKey();
                        FinLog.e("render:", "删除小窗口：" + id);
                        holderContainer.removeView(newRender.containerLayout);// 小容器删除 layout
                        holderBigContainer.addView(newRender, screenWidth, screenHeight);//将新的渲染器添加到大容器中
                        selectedRender = newRender;// 新渲染器辅给大窗口渲染器
                        positionRenders.remove(newRender);//
                        break;
                    }
                }
                FinLog.e("render:", "selectedRender == releaseTaget  remove:" + userID);
                holderBigContainer.removeView(releaseTaget.containerLayout);
            } else {
                FinLog.e("render:", " remove:" + userID);
                holderContainer.removeView(releaseTaget.containerLayout);
                positionRenders.remove(releaseTaget);
            }
            if (null != selectedRender && null != selectedRender.coverView && null != selectedRender.coverView.getBlinkVideoView()) {
                selectedRender.coverView.getBlinkVideoView().setZOrderMediaOverlay(false);
            }
            refreshViews();
        } else {
            toggleTips();
        }
    }

    public void refreshViews() {

        for (int i = 0; i < positionRenders.size(); i++) {
            RenderHolder holder = positionRenders.get(i);
            holder.coverView.getBlinkVideoView().setZOrderMediaOverlay(true);
            holder.coverView.getBlinkVideoView().requestLayout();

        }
        toggleTips();

    }

    /**
     * 控制屏幕中間的提示
     */
    private void toggleTips() {
        int size = isObserver ? 0 : 1;
        if (connetedRemoteRenders.size() == size) {
            ((CallActivity) context).setWaitingTipsVisiable(true);
        } else {
            ((CallActivity) context).setWaitingTipsVisiable(false);
        }
    }

    public void toggleLocalView(boolean visible) {
        if (visible == (holderBigContainer.getVisibility() == View.VISIBLE))
            return;
        if (visible)
            holderBigContainer.setVisibility(View.VISIBLE);
        else holderBigContainer.setVisibility(View.INVISIBLE);
    }

    /**
     * Method to judge whether has conneted users
     */
    public boolean hasConnectedUser() {
        int size = isObserver ? 0 : 1;
        //connectedRemoteRenders only contains local render by default. when its size is large than 1, means new user joined
        return connetedRemoteRenders.size() > size;
    }

    /**
     * @param userId
     * @param talkType
     */
    public void updateTalkType(String userId, String talkType) {
        if (connetedRemoteRenders.containsKey(userId)) {
            connetedRemoteRenders.get(userId).CameraSwitch(talkType);
        }
    }

    public class RenderHolder {
        RelativeLayout containerLayout;
        int targetZindex;
        CoverView coverView;
        public String talkType = "";
        private String userName, userId;

        RenderHolder(RelativeLayout containerLayout, int index) {
            this.containerLayout = containerLayout;
            targetZindex = index;
        }

        /**
         * 设置摄像头被关闭后的封面视图
         */
        public void setCoverView() {
            if (null == coverView) {
                coverView = new CoverView(context);
                coverView.mRenderHolder = this;
            }
            coverView.setUserInfo(userName, userId);
            coverView.showUserHeader();
            RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT);
            removeCoverView();
            this.containerLayout.addView(coverView, p);
        }

        public void removeCoverView() {
            this.containerLayout.removeView(coverView);
        }


        public void initCover(String talkType) {
            this.talkType = talkType;
            FinLog.i("blinkTalkType", "initCover  blinkTalkType==" + talkType + ",name=" + userName);
            setCoverView();//0-音频；1-视频；2-音频+视频；3-无
            coverView.showLoading();
            switch (talkType) {
                case BlinkTalkTypeUtil.O_CAMERA:
                    break;
                case BlinkTalkTypeUtil.O_MICROPHONE:
                    break;
                case BlinkTalkTypeUtil.O_CM:
                    break;

                case BlinkTalkTypeUtil.C_CAMERA:
                    coverView.closeLoading();
                    break;
                case BlinkTalkTypeUtil.C_MICROPHONE:
                    break;
                case BlinkTalkTypeUtil.C_CM:
                    coverView.closeLoading();
                    break;
            }
        }

        public void init(String talktype, boolean isSelf) {
            this.talkType = talktype;
            if (isSelf) {
                setCoverView();
            }
            blinkTalkType();
        }

        public void CameraSwitch(String talkType) {
            this.talkType = talkType;
            blinkTalkType();
        }

        public void init(boolean isSelf) {
            init(talkType, isSelf);
        }

        private void blinkTalkType() {
            switch (talkType) {
                case BlinkTalkTypeUtil.O_CAMERA:
                    coverView.showBlinkVideoView();
                    break;
                case BlinkTalkTypeUtil.O_MICROPHONE:
                    break;
                case BlinkTalkTypeUtil.O_CM:
                    coverView.showBlinkVideoView();
                    break;
                case BlinkTalkTypeUtil.C_CAMERA:
                    coverView.showUserHeader();
                    break;
                case BlinkTalkTypeUtil.C_MICROPHONE:
                    break;
                case BlinkTalkTypeUtil.C_CM:
                    coverView.showUserHeader();
                    break;
            }
        }

        public void release() {
            if (containerLayout != null) {
                containerLayout.requestLayout();
                containerLayout.removeAllViews();
            }
        }
    }

    private OnLocalVideoViewClickedListener onLocalVideoViewClickedListener;

    public void setOnLocalVideoViewClickedListener(OnLocalVideoViewClickedListener onLocalVideoViewClickedListener) {
        this.onLocalVideoViewClickedListener = onLocalVideoViewClickedListener;
    }

    /**
     * 内部接口：用于本地视频图像的点击事件监听
     */
    public interface OnLocalVideoViewClickedListener {
        void onClick();
    }

    private void saveSelectUserId(RenderHolder render) {
        if (null == selectedUserid) return;
        for (String userid : connetedRemoteRenders.keySet()) {
            if (connetedRemoteRenders.get(userid).equals(render)) {
                try {
                    selectedUserid.clear();
                    selectedUserid.add(userid);
                    if (mActivity.isSharing(userid)) {
                        Toast.makeText(context, context.getResources().getString(R.string.meeting_control_OpenWiteBoard), Toast.LENGTH_SHORT).show();
                    }
                } catch (Resources.NotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Boolean isBig(String userid) {
        if (null == selectedUserid) return false;
        else return selectedUserid.contains(userid);
    }

    private CallActivity mActivity = null;

    public void setActivity(CallActivity activity) {
        mActivity = activity;
    }

    public void delSelect(String userid) {
        selectedUserid.remove(userid);
    }
}