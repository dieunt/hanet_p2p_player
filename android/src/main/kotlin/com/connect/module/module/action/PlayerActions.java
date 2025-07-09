package com.connect.module.module.action;



import static com.xc.p2pVideo.NativeMediaPlayer.CHANGE_DECODE;
import static com.xc.p2pVideo.NativeMediaPlayer.CONNECT;
import static com.xc.p2pVideo.NativeMediaPlayer.DECODE;

import android.media.MediaCodec;
import android.os.Build;
import android.util.Log;


import com.connect.module.module.audio.AACDecoderUtil;
import com.connect.module.module.bean.DeviceBean;
import com.connect.module.module.bean.EventResult;
import com.connect.module.module.record.IntercomThread;
import com.connect.module.module.speaker.AudioThread;
import com.tutk.IOTC.AVAPIs;
import com.tutk.IOTC.IOTCAPIs;
import com.tutk.IOTC.St_AVClientStartInConfig;
import com.tutk.IOTC.St_AVClientStartOutConfig;
import com.xc.p2pVideo.NativeMediaPlayer;

import org.greenrobot.eventbus.EventBus;

import java.nio.ByteBuffer;

public class PlayerActions {

    public boolean isStartRead = true;
    private static final int VIDEO_BUF_SIZE = 5 * 1024 * 1024;
    private static final int FRAME_INFO_SIZE = 16;
    private DeviceBean deviceBean;
    public int decodeType = NativeMediaPlayer.HARDDECODE;

    private AVAPIs av = null;
    private int avIndex = -1;

    // 音频解码器
    private AACDecoderUtil audioUtil;

    public int recordSampleRate = 48000;
    public int speekEncodeType = 1;// 1 aac 2pcm
    public int speekChannel = 2;// 单双通道
    public boolean isRecording = false; // 记录是否正在进行录制
    private int failureCounts = 0;// 连续解码失败的次数

    public static final int MAXCHANGE = 40; // frames
    public static final String RECORD_TAG = "RECORD_AUDIO";

    private final int qualityType;
    private final int cameraType;

    private Thread threadAudio;
    private Thread threadIntercom;
    private AudioThread audioThread;
    private IntercomThread intercomThread;

    public PlayerActions(DeviceBean deviceBean, int decodeType, int qualityType, int cameraType) {
        this.deviceBean = deviceBean;
        this.decodeType = decodeType;
        this.qualityType = qualityType;
        this.cameraType = cameraType;
        this.audioUtil = new AACDecoderUtil();
    }

    /**
     * 开始连接设备
     */
    public void startDeviceConnection() {
        int ret = -1;
        avIndex = 0;
        int sid = IOTCAPIs.IOTC_Get_SessionID();
        if (sid < 0) {
            // Toast.makeText(context, "IOTC_Get_SessionID error code is "+sid,
            // Toast.LENGTH_SHORT).show();
        }
        ret = IOTCAPIs.IOTC_Connect_ByUID_Parallel(deviceBean.getDeviceUid(), sid);
        St_AVClientStartInConfig av_client_in_config = new St_AVClientStartInConfig();
        St_AVClientStartOutConfig av_client_out_config = new St_AVClientStartOutConfig();

        av_client_in_config.iotc_session_id = sid;
        av_client_in_config.iotc_channel_id = qualityType;
        av_client_in_config.timeout_sec = 60;
        av_client_in_config.account_or_identity = deviceBean.getDeviceName();
        av_client_in_config.password_or_token = deviceBean.getDevicePwd();
        av_client_in_config.resend = 1;
        av_client_in_config.security_mode = 0; // enable DTLS
        av_client_in_config.auth_type = 0;

        avIndex = AVAPIs.avClientStartEx(av_client_in_config, av_client_out_config);

        if (startIpcamStream(avIndex)) {
            if (decodeType == NativeMediaPlayer.HARDDECODE) {
                Log.e("CAMERA_VIEW", "--hardware--");
                readFrameLoopForHard(avIndex, sid);
            } else {
                Log.e("CAMERA_VIEW", "--software--");
                readFrameLoopForSoft(avIndex, sid);
            }
        } else {
            AVAPIs.avClientStop(avIndex);
            IOTCAPIs.IOTC_Session_Close(sid);
            EventBus.getDefault().post(EventResult.getResult(CONNECT, 0, deviceBean.getPlayerId()));
        }
    }

    private void readFrameLoopForHard(final int avIndex, final int sid) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                AVAPIs av = new AVAPIs();
                byte[] frameInfo = new byte[FRAME_INFO_SIZE];
                byte[] videoBuffer = new byte[VIDEO_BUF_SIZE];
                int[] outBufSize = new int[1];
                int[] outFrameSize = new int[1];
                int[] outFrmInfoBufSize = new int[1];
                if (deviceBean.getMediaCodec() == null)
                    return;
                ByteBuffer byteBuffer = null;
                MediaCodec mediaCodec = deviceBean.getMediaCodec();
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

                boolean changeDecode = false;
                failureCounts = 0;
                while (isStartRead) {
                    int[] frameNumber = new int[1];
                    int length = av.avRecvFrameData2(avIndex, videoBuffer,
                            VIDEO_BUF_SIZE, outBufSize, outFrameSize,
                            frameInfo, FRAME_INFO_SIZE,
                            outFrmInfoBufSize, frameNumber);
                    if (length == AVAPIs.AV_ER_LOSED_THIS_FRAME) {
                        continue;
                    } else if (length == AVAPIs.AV_ER_INCOMPLETE_FRAME) {
                        continue;
                    } else if (length == AVAPIs.AV_ER_SESSION_CLOSE_BY_REMOTE) {
                        break;
                    } else if (length == AVAPIs.AV_ER_REMOTE_TIMEOUT_DISCONNECT) {
                        break;
                    } else if (length == AVAPIs.AV_ER_INVALID_SID) {
                        break;
                    }
                    if (length < 0) {
                        try {
                            Thread.sleep(50);
                            continue;
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                    if (failureCounts > MAXCHANGE) {
                        changeDecode = true;
                        isStartRead = false;
                        failureCounts = 0;
                    }
                    /**
                     * 调用硬解码
                     */
                    int inIndex = -1;
                    try {
                        inIndex = mediaCodec.dequeueInputBuffer(1000000);// dateReceive
                        if (inIndex >= 0) {
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                                byteBuffer = mediaCodec.getInputBuffers()[inIndex];
                                byteBuffer.clear();
                            } else {
                                byteBuffer = mediaCodec.getInputBuffer(inIndex);
                            }
                            if (byteBuffer == null) {
                                continue;
                            }
                            byteBuffer.put(videoBuffer, 0, length);
                            mediaCodec.queueInputBuffer(inIndex, 0, length, 0, 0);
                        } else {
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            failureCounts++;
                            continue;
                        }
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                        failureCounts++;
                        continue;
                    }

                    int outIndex;
                    try {
                        outIndex = mediaCodec.dequeueOutputBuffer(info, 0);
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                        failureCounts++;
                        continue;
                    }
                    if (outIndex >= 0) {
                        boolean doRender = (info.size != 0);
                        try {
                            mediaCodec.releaseOutputBuffer(outIndex, doRender);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (!deviceBean.isDecode()) {
                            EventBus.getDefault().post(EventResult.getResult(DECODE, 1, deviceBean.getPlayerId()));
                            deviceBean.setDecode(true);
                        }
                        failureCounts = 0;
                        System.gc();
                    } else {
                        failureCounts++;
                        switch (outIndex) {
                            case MediaCodec.INFO_TRY_AGAIN_LATER:
                                break;
                            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                                break;
                        }
                    }
                }

                AVAPIs.avClientStop(avIndex);
                IOTCAPIs.IOTC_Session_Close(sid);
                if (changeDecode) {
                    EventBus.getDefault().post(EventResult.getResult(CHANGE_DECODE, 0, deviceBean.getPlayerId()));
                } else {
                    EventBus.getDefault().post(EventResult.getResult("", 1, deviceBean.getPlayerId()));
                }
            }
        }).start();
    }

    private void readFrameLoopForSoft(final int avIndex, final int sid) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                AVAPIs av = new AVAPIs();
                byte[] frameInfo = new byte[FRAME_INFO_SIZE];
                byte[] videoBuffer = new byte[VIDEO_BUF_SIZE];
                int[] outBufSize = new int[1];
                int[] outFrameSize = new int[1];
                int[] outFrmInfoBufSize = new int[1];
                while (isStartRead) {
                    int[] frameNumber = new int[1];
                    int length = av.avRecvFrameData2(avIndex, videoBuffer,
                            VIDEO_BUF_SIZE, outBufSize, outFrameSize,
                            frameInfo, FRAME_INFO_SIZE,
                            outFrmInfoBufSize, frameNumber);
                    if (length == AVAPIs.AV_ER_LOSED_THIS_FRAME) {
                        continue;
                    } else if (length == AVAPIs.AV_ER_INCOMPLETE_FRAME) {
                        continue;
                    } else if (length == AVAPIs.AV_ER_SESSION_CLOSE_BY_REMOTE) {
                        break;
                    } else if (length == AVAPIs.AV_ER_REMOTE_TIMEOUT_DISCONNECT) {
                        break;
                    } else if (length == AVAPIs.AV_ER_INVALID_SID) {
                        break;
                    }
                    if (length < 0) {
                        try {
                            Thread.sleep(30);
                            continue;
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                    /**
                     * 此处调用jni接口传值下去 进行softdecode
                     */
                    NativeMediaPlayer.JniProcessFrameData(videoBuffer, length, 1, 2, 25, deviceBean.getPlayerId());
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        break;
                    }
                }

                AVAPIs.avClientStop(avIndex);
                IOTCAPIs.IOTC_Session_Close(sid);
                EventBus.getDefault().post(EventResult.getResult("", 1, deviceBean.getPlayerId()));
            }
        }).start();
    }

    private boolean startIpcamStream(int avIndex) {
        av = new AVAPIs();
        int IOTYPE_USER_IPCAM_START = 0x1FF;
        final int ret = av.avSendIOCtrl(avIndex, IOTYPE_USER_IPCAM_START, new byte[8], 8);
        if (ret < 0) {
            return false;
        }
        return true;
    }

    public void setStartRead(boolean startRead) {
        isStartRead = startRead;
        if (!startRead && threadAudio != null) {
            threadAudio.interrupt();
            destroy();
        }
    }

    /*
     *
     * AUDIO
     *
     */
    private static boolean isAudio;

    public void mute() {
        if (isAudio) {
            isAudio = false;
            stopAudio();
        } else {
            isAudio = true;
            startAudio();
        }
    }

    public void startIntercom() {
        startIntercoms();
    }

    public void stopIntercom() {
        stopIntercoms();
    }

    /*
     *
     * NEW AUDIO
     *
     */
    public void destroy() {
        this.stopIntercoms();
        this.stopAudio();
        isAudio = false;
    }

    public void startAudio() {
        if (!isStartRead)
            return;
        if (av == null)
            av = new AVAPIs();
        new Thread(new Runnable() {
            @Override
            public void run() {
                int ret = av.avSendIOCtrl(avIndex, 0x300/* IOTYPE_USER_IPCAM_AUDIOSTART */, new byte[8], 8);
            }
        }).start();

        if (audioThread == null)
            audioThread = new AudioThread(av, avIndex, cameraType);
        threadAudio = new Thread(audioThread);
        threadAudio.start();
    }

    public void stopAudio() {
        if (!isStartRead)
            return;
        if (av == null)
            av = new AVAPIs();
        new Thread(new Runnable() {
            @Override
            public void run() {
                int ret = av.avSendIOCtrl(avIndex, 0x301/* IOTYPE_USER_IPCAM_AUDIOSTOP */, new byte[8], 8);
            }
        }).start();

        if (audioThread != null) {
            audioThread.stop();
            threadAudio.interrupt();
            audioThread = null;
            threadAudio = null;
        }
    }

    public void startIntercoms() {
        if (!isStartRead)
            return;
        if (av == null)
            av = new AVAPIs();
        new Thread(new Runnable() {
            @Override
            public void run() {
                int ret = av.avSendIOCtrl(avIndex, 0x0350 /* IOTYPE_USER_IPCAM_SPEAKERSTART */, new byte[8], 8);
            }
        }).start();

        if (intercomThread == null)
            intercomThread = new IntercomThread(av, avIndex, cameraType);
        threadIntercom = new Thread(intercomThread);
        threadIntercom.start();

        if (audioThread != null) {
            audioThread.mute(true);
        }
    }

    public void stopIntercoms() {
        if (!isStartRead)
            return;
        if (av == null)
            av = new AVAPIs();
        new Thread(new Runnable() {
            @Override
            public void run() {
                int ret = av.avSendIOCtrl(avIndex, 0x0351 /* IOTYPE_USER_IPCAM_SPEAKERSTOP */, new byte[8], 8);
            }
        }).start();

        if (intercomThread != null) {
            intercomThread.stop();
            threadIntercom.interrupt();
            intercomThread = null;
            threadIntercom = null;
        }

        if (audioThread != null) {
            audioThread.mute(false);
        }
    }

    public void ptzControl(int direction) {
        char[] actionChar = String.valueOf(direction).toCharArray();
        byte[] cmdAction = new byte[actionChar.length];
        for (int i = 0; i < actionChar.length; i++) {
            byte temp = (byte) actionChar[i];
            cmdAction[i] = temp;
        }
        if (av != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    int ret = av.avSendIOCtrl(avIndex, 0x1001/* IOTYPE_USER_IPCAM_PTZ_COMMAND */, cmdAction,
                            actionChar.length);
                }
            }).start();
        }

    }
}