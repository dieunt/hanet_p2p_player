package com.connect.module.native_view;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.camera.camera_connect.R;
import com.connect.module.module.action.PlayerAction;
import com.connect.module.module.bean.DeviceBean;
import com.connect.module.module.bean.EventResult;
import com.connect.utils.SharedPreferencesUtils;
import com.tutk.IOTC.AVAPIs;
import com.tutk.IOTC.IOTCAPIs;
import com.tutk.IOTC.TUTKGlobalAPIs;
import com.xc.hdscreen.view.GLPlayView;
import com.xc.p2pVideo.NativeMediaPlayer;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.platform.PlatformView;

public class CameraPlayerWidget implements PlatformView, MethodChannel.MethodCallHandler, SurfaceHolder.Callback, View.OnClickListener {

    private static final String TAG = "CameraPlayerWidget";
    private static final String LICENSE_KEY = "AQAAAMHY3vUDYAhbA/F5ekE+00jq1ACuTIznLJDK55p/jpI7riWN6bp7KYLTDrsQ3XJkzsVkJSBK3rmD3ZPAWF4JlZzn3J/qpmA3O31yfX7VxVNDXd1h3vJYFtgsjOcl9vn4c4k2oPKXHUGjtGxH3O+4Wc14AI/mkmvJIFVI2k3M2J9eanoTqXbEhLMRRpXa+tmbCzM4/L/q3NMZqc4sdErADNIb";

    private final View rootView;
    private LinearLayout playerParent;
    private final Activity activity;

    private final List<DeviceBean> players = new ArrayList<>();
    private final List<LinearLayout> viewParents = new ArrayList<>();
    private final List<ProgressBar> progressBars = new ArrayList<>();

    private ImageView ic_microphone, ic_audio;

    private NativeMediaPlayer nativeMediaPlayer;
    private int currentPlayers = 0 ;//标记当前选哪个播放器 控制音频
    private int audioSample = 48000; //device audio samplerate
    private int audioEncodeType = 1;// 1:AAC 2:pcm

    private int intercomSample = 48000;//intercom sampleRate
    private int intercomEncode = 1;// 1:AAC 2:pcm

    private int audioChannel = 2; //1：单通道 2：双通道
    private int intercomChannel  = 2; //1:单通道 2：双通道

    private String UID = "";
    private String PWD = "";

    private int screenWidth = 0, screenHeight = 0;

    private boolean isMuted = false;
    private boolean isSpeak = false;

    public CameraPlayerWidget(Context context, Activity activity, int id, BinaryMessenger messenger, Object args) {
        this.activity = activity;

        rootView = LayoutInflater.from(context).inflate(R.layout.camera_view_player, null);
        SharedPreferencesUtils.getInstance().init(context);
        EventBus.getDefault().register(this);

        // Lấy tham số từ Flutter (uuid, pass, width, height)
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

        myRequetPermission();
        initDeviceSource();
        initView();
    }

    private void initTUTK() {
        int ret = TUTKGlobalAPIs.TUTK_SDK_Set_License_Key(LICENSE_KEY);
        if (ret != TUTKGlobalAPIs.TUTK_ER_NoERROR) return;

        ret = IOTCAPIs.IOTC_Initialize2(0);
        if (ret != IOTCAPIs.IOTC_ER_NoERROR) return;

        AVAPIs.avInitialize(32);
        NativeMediaPlayer.JniInitClassToJni();
    }

    private void initDeviceSource() {
        initTUTK();
        DeviceBean deviceBean = new DeviceBean();
        deviceBean.setPlayerId(1);
        deviceBean.setDeviceUid(UID);
        deviceBean.setDevicePwd(PWD);
        deviceBean.setDeviceName("admin");
        deviceBean.setPlayerAction(new PlayerAction(deviceBean, NativeMediaPlayer.SOFTDECODE,
                activity, null, PlayerAction.MAINSTREAM));
        players.add(deviceBean);
    }

    private void initView() {

        // khai báo view
        playerParent = rootView.findViewById(R.id.playerParent);
        viewParents.add(rootView.findViewById(R.id.playerLin1));
        progressBars.add(rootView.findViewById(R.id.progress1));
        ic_microphone = rootView.findViewById(R.id.microphone);
        ic_audio = rootView.findViewById(R.id.audio);
        nativeMediaPlayer = new NativeMediaPlayer();

        // set layout cho camera
        if (screenHeight > 0 || screenWidth > 0) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(screenWidth, screenHeight);
            playerParent.setLayoutParams(params);
        }  else  if (screenWidth == 0 || screenHeight == 0) {
            ViewTreeObserver obse = playerParent.getViewTreeObserver();
            obse.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                @Override
                public void onGlobalLayout() {
                    playerParent.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    screenWidth = playerParent.getWidth();
                    screenHeight = playerParent.getHeight();
                    Log.d(TAG, "screenWidth=" + screenWidth + " screenHeight=" + screenHeight);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(screenWidth, (screenWidth * 9) / 16);
                    playerParent.setLayoutParams(params);
                }
            });
        }

        // lắng nghe sự kiện click icon audio && speak
        ic_audio.setOnClickListener(this);
        ic_microphone.setOnClickListener(this);

        // sau khi init view và camera xong thì start camera
        startPlay(0);
    }


    private void startPlay(final int index) {
        if (players.isEmpty()) return;

        // Hiển thị ProgressBar khi đang kết nối
        progressBars.get(index).setVisibility(View.VISIBLE);

        players.get(index).setDeviceUid(UID);
        players.get(index).setDevicePwd(PWD);

        int width = (players.size() == 1) ? screenWidth : screenWidth / 2;

        GLPlayView playView = new GLPlayView(activity,players.get(index).getPlayerId(),width,screenHeight,players.get(index).getDeviceUid());
        playView.getHolder().addCallback(this);
        viewParents.get(index).addView(playView);
        players.get(index).setGlPlayView(playView);
        players.get(index).getPlayerAction().setStartRead(true);
        nativeMediaPlayer.NativeCreateMediaPlayer(playView,"",players.get(index).getDeviceUid(),0,players.get(index).getPlayerId(),1);
        SharedPreferencesUtils.getInstance().saveDeviceId(UID,PWD);
        new Thread(new Runnable() {
            @Override
            public void run() {
                NativeMediaPlayer.JniVideoPlay(players.get(index).getPlayerId());

                players.get(index).getPlayerAction().startDeviceConnection();
            }
        }).start();
        progressBars.get(index).setVisibility(View.VISIBLE);


    }

    private void stopPlayer(int index) {
        if (players.get(index).getAvIndex() > -1) {
            AVAPIs.avClientStop(players.get(index).getAvIndex());
        }
        if (players.get(index).getSid() > -1) {
            IOTCAPIs.IOTC_Session_Close(players.get(index).getSid());
        }

        players.get(index).getPlayerAction().setStartRead(false);
        NativeMediaPlayer.JniCloseVideoPlay(players.get(index).getPlayerId());
        viewParents.get(index).removeView(players.get(index).getGlPlayView());
        progressBars.get(index).setVisibility(View.GONE);

    }


    private void openAudio() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //用子线程调用
                players.get(currentPlayers).getPlayerAction().startAudioStream(audioSample,audioEncodeType,audioChannel);
            }
        }).start();
    }

    private void closeAudio() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //用子线程调用
                players.get(currentPlayers).getPlayerAction().stopAudioStream();
            }
        }).start();
    }

    private void openSpeak() {
        boolean resul = players.get(currentPlayers).getPlayerAction().startRecord(intercomSample,intercomEncode,intercomChannel);
        if(resul){
            new Thread(new Runnable() {
                @Override
                public void run() {

                }
            }).start();
        }
    }

    private void closeSpeak() {
        players.get(currentPlayers).getPlayerAction().stopRecord();
    }

    @Override
    public void onClick(View view) {
        if(view == ic_audio){
            isMuted = !isMuted;
            if (isMuted) {
                openAudio();
                ic_audio.setImageResource(R.drawable.ic_volume);
            } else {
                closeAudio();
                ic_audio.setImageResource(R.drawable.ic_volume_off);
            }

        } else if(view == ic_microphone) {
            isSpeak = !isSpeak;
            if (isSpeak) {
                openSpeak();
                ic_microphone.setImageResource(R.drawable.ic_microphone);
            } else {
                closeSpeak();
                ic_microphone.setImageResource(R.drawable.ic_microphone_mute);
            }
        }
    }

    @Override
    public void onMethodCall(MethodCall call, MethodChannel.Result result) {

    }

    @Nullable
    @Override
    public View getView() {
        return rootView;
    }

    @Override
    public void dispose() {
        EventBus.getDefault().unregister(this);
        for (int i = 0; i < players.size(); i++) {
            stopPlayer(i);
        }
    }



    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(EventResult event) {
        if (event.getRequestUrl().equals(NativeMediaPlayer.DECODE)) {
            int playerId = (int) event.getObject();
            Log.d(TAG, "Decode success for playerId=" + playerId);
            if (event.getResponseCode() == 1) {
                progressBars.get(playerId - 1).setVisibility(View.GONE);
            }
        } else if (event.getRequestUrl().equals(NativeMediaPlayer.CONNECT)) {
            int playerId = (int) event.getObject();
            Log.d(TAG, "Connect failed for playerId=" + playerId);
            stopPlayer(playerId - 1);
            Toast.makeText(activity, "Kết nối player " + playerId + " thất bại!", Toast.LENGTH_SHORT).show();
        } else if (event.getRequestUrl().equals(NativeMediaPlayer.TIMEOUT)) {
            int playerId = (int) event.getObject();
            Log.d(TAG, "Timeout for playerId=" + playerId);
            stopPlayer(playerId);
            Toast.makeText(activity, "Player chờ dữ liệu quá lâu, kết nối thất bại!", Toast.LENGTH_SHORT).show();
        }
    }

    private void myRequetPermission() {
        // RECORD_AUDIO
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        } else {
            Toast.makeText(activity, "Bạn đã cấp quyền ghi âm!", Toast.LENGTH_SHORT).show();
        }

        // WRITE_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        // READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "Surface created");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "Surface changed: " + width + "x" + height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "Surface destroyed");
    }

}
