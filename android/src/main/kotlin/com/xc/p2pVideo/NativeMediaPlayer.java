package com.xc.p2pVideo;

import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.util.Log;


import com.connect.module.module.bean.EventResult;

import org.greenrobot.eventbus.EventBus;

import java.nio.charset.StandardCharsets;

public class NativeMediaPlayer {
    private static final String TAG = "NativeMediaPlayer";
    private static Handler handler;

    public static final int  DECODESTATUS = 901 ;
    public static final String DECODE = "native_decode";
    public static final int CONNECTSTATUS = 900;
    public static final String CONNECT = "native_connect";
    public static final int DECODETIMEOUT = 911 ;
    public static final String TIMEOUT = "decode_timeout";
    public static final String SOFTDECODE_BACK = "soft_back";
    public static final String MUTIL_BACK = "multip_back";
    public static final String CHANGE_DECODE = "change_decode_type";
    public static final String RELEASE_DECODER = "release_decoder";


    public static final int QUALITY_HIGH = 0;
    public static final int QUALITY_LOW = 1;
    public static final int CAM_COMMON = -1;
    public static final int CAM_AI = 0;
    public static final int CAM_GO = 1;
    public static final int CAM_ACCESS = 2;
    public static final int CAM_SUPER = 3;
    public static final int CAM_ACCESS_TEMP = 4;
    public static final int CAM_HOME_INDOOR = 5;
    public static final int CAM_HOME_OUTDOOR = 6;
    public static final int CAM_ACCESS_F1 = 7;




    public static final int SOFTDECODE = 1; //软解
    public static final int HARDDECODE = 2 ;//硬解码


    static {
        try {
            System.loadLibrary("avutil-56");
            System.loadLibrary("swresample-3");
            System.loadLibrary("avcodec-58");
            System.loadLibrary("avformat-58");
            System.loadLibrary("swscale-5");
            System.loadLibrary("LsBitdogCloudAppSdk");
            System.out.println("Load Library suucess!!");
        } catch (Exception e) {
            System.out.println("Load Library error!!");
        }
    }

    public static native int JniInitClassToJni();

    public native static int JniCreateVideoPlayer(Object playerInstance_this, Object surfaceView, String rtspUrl, String id, int channelId, int windowId, int streamId);

    public native static int JniVideoPlay(int playId);

    public native static int JniOpenAudio(int playId);

    public native static int JniCloseAudio(int playId);

    public native static int JniCloseVideoPlay(int playId);

    public native static int JniVideoPlayerStartRecord(int playId, String fileName);

    public native static int JniVideoPlayerStopRecord(int playId);

    public native static int JniToMp4File(String inFilePath, String outFilePath, int playerid);

    public static native void JniAppClassExist();

    public static native int JniProcessFrameData(byte[] frameData,int len,int frameType,int encodeType,int defaultFrameRate,int playerId);



    private static NativeMediaPlayer instance;

    public static NativeMediaPlayer getInstance() {
        synchronized (NativeMediaPlayer.class) {
            if (instance == null) {
                instance = new NativeMediaPlayer();
            }
            return instance;
        }
    }

    public NativeMediaPlayer() {
    }

    public NativeMediaPlayer(Handler handler) {
        /*
         * Native setup requires a weak reference to our object. It's easier to
         * create it here than in C++.
         */
        NativeMediaPlayer.handler = handler;

        System.out.println("class is " + handler.getClass());

        //mGLSurfaceView = glView;
    }

    public int NativeCreateMediaPlayer(GLSurfaceView glView, String rtspUrl, String id, int channelId, int windowId, int streamId) {

        return JniCreateVideoPlayer(this, glView, rtspUrl, id,
                    channelId, windowId, streamId);
    }

    public static void UpdataFrameSize(int frameWidth, int frameHeight, int playerId) {

        System.out.println("--------------------------------------------");
    }

    public static int drawFrame(Object mGLSurfaceView) {
         //System.out.println("draw frame!!" + mGLSurfaceView);
        if (mGLSurfaceView == null) return -1;
        ((GLSurfaceView) mGLSurfaceView).requestRender();
        return 0;
    }


    public static void NativePostEventString(int what, String arg, int winId) {
        Log.d("NativePostEventString","NativePostEventString what = "+ what+" arg = "+arg +", winId =" + winId);
    }

    public static void NativePostEventString(int what, byte[] arg0, int winId) {
        String arg = null;
        arg = new String(arg0, StandardCharsets.UTF_8);
        System.out.println("what 409 = [" + what + "], arg = [" + arg + "], winId = [" + winId + "]");

    }

    public static void NativePostEvent(int what, int status, int winId) { // arg:
        switch (what) {
            case CONNECTSTATUS: // connect failured
                if (status == 0) {
                    EventBus.getDefault().post(EventResult.getResult(CONNECT,0,winId));
                }
                break;
            case DECODESTATUS:
                if (status == 1) {// decode success
                    EventBus.getDefault().post(EventResult.getResult(DECODE,1,winId));
                }
                break;
            case DECODETIMEOUT:
                if(status == 0){
                    EventBus.getDefault().post(EventResult.getResult(TIMEOUT,0,winId));
                }
                break;

        }

    }

}
