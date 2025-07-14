package com.connect.module.native_view;


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;


import com.camera.camera_connect.R;
import com.connect.module.module.action.PlayerAction;
import com.connect.module.module.bean.DeviceBean;
import com.connect.module.module.bean.EventResult;
import com.connect.module.module.bean.PlayerViewBean;
import com.tutk.IOTC.AVAPIs;
import com.tutk.IOTC.IOTCAPIs;
import com.tutk.IOTC.TUTKGlobalAPIs;
import com.xc.hdscreen.view.GLPlayView;
import com.xc.p2pVideo.NativeMediaPlayer;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.platform.PlatformView;

public class CameraMultiWidget implements PlatformView, MethodChannel.MethodCallHandler {

    private static final String TAG = "CameraMultiWidget";
    private static final String LICENSE_KEY = "AQAAAMHY3vUDYAhbA/F5ekE+00jq1ACuTIznLJDK55p/jpI7riWN6bp7KYLTDrsQ3XJkzsVkJSBK3rmD3ZPAWF4JlZzn3J/qpmA3O31yfX7VxVNDXd1h3vJYFtgsjOcl9vn4c4k2oPKXHUGjtGxH3O+4Wc14AI/mkmvJIFVI2k3M2J9eanoTqXbEhLMRRpXa+tmbCzM4/L/q3NMZqc4sdErADNIb";

    private final View rootView;
    private  LinearLayout playerParent;
    private final Context context;

    private Activity activity;

    private NativeMediaPlayer nativeMediaPlayer;

    private  String UID = "";
    private  String PWD = "";



    private int screenWidth = 0,screenHeight = 0;

    private List<DeviceBean> players = new ArrayList<DeviceBean>();


    private List<PlayerViewBean> playerViewBeans = new ArrayList<PlayerViewBean>();




    public CameraMultiWidget(Context context,Activity activity, int id, BinaryMessenger messenger, Object args) {
        this.context = context;
        this.activity = activity;
        rootView = LayoutInflater.from(context).inflate(R.layout.camera_view_multi, null);
        EventBus.getDefault().register(this);

        // Parse creationParams
        if (args instanceof Map) {
            Map<String, Object> creationParams = (Map<String, Object>) args;
            if (creationParams.containsKey("uuid")) {
                UID = (String) creationParams.get("uuid");
            }
            if (creationParams.containsKey("pass")) {
                PWD = (String) creationParams.get("pass");
            }
            if (creationParams.containsKey("width")) {
                screenWidth = ((Number) creationParams.get("width")).intValue();
            }
            if (creationParams.containsKey("height")) {
                screenHeight = ((Number) creationParams.get("height")).intValue();
            }
        }


        initTUTK();
        initView();

        // Optional: If width/height is not passed, fallback to layout listener
        if (screenWidth == 0 || screenHeight == 0) {
            ViewTreeObserver obse = rootView.getViewTreeObserver();
            obse.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    playerParent.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    screenWidth = playerParent.getWidth();
                    screenHeight = playerParent.getHeight();
                }
            });
            start();
        }
    }

    private void initTUTK() {
        int ret = TUTKGlobalAPIs.TUTK_SDK_Set_License_Key(LICENSE_KEY);
        if (ret != TUTKGlobalAPIs.TUTK_ER_NoERROR) return;

        ret = IOTCAPIs.IOTC_Initialize2(0);
        if (ret != IOTCAPIs.IOTC_ER_NoERROR) return;

        AVAPIs.avInitialize(32);
        NativeMediaPlayer.JniInitClassToJni();
    }


    private  void initView() {

        playerParent = rootView.findViewById(R.id.playerParent);
        nativeMediaPlayer = new NativeMediaPlayer();
        PlayerViewBean playerViewBean = new PlayerViewBean();
        playerViewBean.setLinearLayout(rootView.findViewById(R.id.playerLin1));
        playerViewBean.setProgressBar(rootView.findViewById(R.id.progress1));
        playerViewBeans.add(playerViewBean);
        if (screenHeight > 0 || screenWidth > 0) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    screenWidth,
                    screenHeight
            );
            playerParent.setLayoutParams(params);
        }

        start();
    }


    private void start() {
        players.clear();
        startPlay(false,false);

    }



    private void startPlay( boolean isChange, boolean isSoftDecode) {
        final DeviceBean deviceBean = new DeviceBean();
        deviceBean.setPlayerId(1);
        deviceBean.setDeviceUid(UID);
        deviceBean.setDevicePwd(PWD);
        deviceBean.setDeviceName("admin");
        deviceBean.setPlayerViewBeans(playerViewBeans.get(0));
        setMediaCode(deviceBean,screenWidth,screenHeight);
        if(isSoftDecode) {
            deviceBean.setPlayerAction(new PlayerAction(deviceBean, NativeMediaPlayer.SOFTDECODE, activity, null,PlayerAction.SUBSTREAM));
        }else{
            deviceBean.setPlayerAction(new PlayerAction(deviceBean, NativeMediaPlayer.HARDDECODE, activity, null,PlayerAction.SUBSTREAM));
        }

        GLPlayView surfaceView = new GLPlayView(activity,deviceBean.getPlayerId(),screenWidth,screenHeight,deviceBean.getDeviceUid());
        deviceBean.getPlayerViewBeans().setSurfaceView(surfaceView);
        deviceBean.getPlayerViewBeans().getLinearLayout().addView(surfaceView);
        deviceBean.getPlayerViewBeans().getSurfaceView().setVisibility(View.VISIBLE);
        deviceBean.setGlPlayView(surfaceView);
        deviceBean.getPlayerAction().setStartRead(true);

        if(deviceBean.getPlayerAction().decodeType == NativeMediaPlayer.HARDDECODE){
            deviceBean.getPlayerViewBeans().getSurfaceView().getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(@NonNull SurfaceHolder holder) {
                    if(deviceBean.getPlayerAction().decodeType == NativeMediaPlayer.HARDDECODE) {
                        try {
                            deviceBean.getMediaCodec().configure(deviceBean.getMediaFormat(), holder.getSurface(), null, 0);
                            deviceBean.getMediaCodec().start();
                            //Toast.makeText(MediaDecodeActivity.this, "surfaceCreated  success..", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            e.printStackTrace();
                            //Toast.makeText(MediaDecodeActivity.this, "surfaceCreated  error..", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                @Override
                public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

                }
                @Override
                public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                }
            });
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    deviceBean.getPlayerAction().startDeviceConnection();
                }
            }).start();
        }else{
            surfaceView.getHolder().addCallback((SurfaceHolder.Callback) context);
            nativeMediaPlayer.NativeCreateMediaPlayer(surfaceView,"",deviceBean.getDeviceUid(),0,deviceBean.getPlayerId(),1);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    NativeMediaPlayer.JniVideoPlay(deviceBean.getPlayerId());
                    deviceBean.getPlayerAction().startDeviceConnection();
                }
            }).start();
        }
        deviceBean.getPlayerViewBeans().getProgressBar().setVisibility(View.VISIBLE);
        if(isChange){
            players.set(0,deviceBean);
        }else {
            players.add(deviceBean);
        }
    }

    private void setMediaCode(DeviceBean deviceBean,int width,int height){   //根据视频编码创建解码器，这里是解码HEVC编码的视频
        MediaCodec  mediaCodec= null;
        MediaFormat mediaFormat = null;
        try {
            mediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "mediaCodec create error..", Toast.LENGTH_SHORT).show();
        }
        if(mediaCodec==null){
            Toast.makeText(context, "mediaCodec is null..", Toast.LENGTH_SHORT).show();
        }
        if ((width & 1) == 1) {
            width--;
        }
        if ((height & 1) == 1) {
            height--;
        }
        //创建视频格式信息
        mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, width, height);
        deviceBean.setMediaCodec(mediaCodec);
        deviceBean.setMediaFormat(mediaFormat);
    }

    private void stopPlayer() {

    }

    @Override
    public View getView() {
        return rootView;
    }




    @Override
    public void dispose() {
        stopPlayer();
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(EventResult event) {
        Log.d(TAG,"[CameraMultiWidget] onEventMainThread url: " + event.getRequestUrl());
        if (event.getRequestUrl().equals(NativeMediaPlayer.DECODE)) {
            int playerId = (int) event.getObject();
            if (event.getResponseCode() == 1) {
                players.get(playerId - 1).getPlayerViewBeans().getProgressBar().setVisibility(View.GONE);
            }
        } else if (event.getRequestUrl().equals(NativeMediaPlayer.CONNECT)) {
            int playerId = (int) event.getObject();
            Toast.makeText(context, "Player " + (playerId - 1) + " connect failure!!!", Toast.LENGTH_SHORT).show();
        }
        // Bạn có thể thêm xử lý khác tương ứng với `SOFTDECODE_BACK`, `MUTIL_BACK`, `CHANGE_DECODE`
    }

    @Override
    public void onMethodCall(MethodCall call, MethodChannel.Result result) {
        switch (call.method) {
            case "startStream":
                //  initPlayer();
                result.success("Started");
                break;
            case "stopStream":
                stopPlayer();
                result.success("Stopped");
                break;
            default:
                result.notImplemented();
        }
    }
}
