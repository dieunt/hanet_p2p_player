package com.connect.module.module.action;

import static com.connect.module.module.base.Contacts.IOTYPE_USER_IPCAM_AUDIOSTOP;
import static com.connect.module.module.base.Contacts.IOTYPE_USER_IPCAM_SPEAKERSTART;
import static com.connect.module.module.base.Contacts.IOTYPE_USER_IPCAM_SPEAKERSTOP;
import static com.xc.p2pVideo.NativeMediaPlayer.CHANGE_DECODE;
import static com.xc.p2pVideo.NativeMediaPlayer.CONNECT;
import static com.xc.p2pVideo.NativeMediaPlayer.DECODE;
import static com.xc.p2pVideo.NativeMediaPlayer.RELEASE_DECODER;


import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.widget.TextView;


import com.connect.module.module.audio.AACDecoderUtil;
import com.connect.module.module.audio.AacEncode;
import com.connect.module.module.base.Contacts;
import com.connect.module.module.bean.AudioDataStruct;
import com.connect.module.module.bean.DeviceBean;
import com.connect.module.module.bean.EventResult;
import com.tutk.IOTC.AVAPIs;
import com.tutk.IOTC.IOTCAPIs;
import com.tutk.IOTC.St_AVClientStartInConfig;
import com.tutk.IOTC.St_AVClientStartOutConfig;
import com.xc.p2pVideo.NativeMediaPlayer;

import org.greenrobot.eventbus.EventBus;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

public class PlayerAction {

    public boolean isStartRead = false;
    private static final int VIDEO_BUF_SIZE = 5 * 1024 * 1024;
    private static final int FRAME_INFO_SIZE = 16;
    public static final int MAINSTREAM = 0 ;
    public static final int SUBSTREAM = 1;
    public static final int DEFAULTFRAMES = 50;
    private DeviceBean deviceBean ;
    public int decodeType = NativeMediaPlayer.HARDDECODE ;

    private AVAPIs av = null;
    private int avIndex = -1;
    private boolean audioOpen = false;

    // 音频解码器
    private AACDecoderUtil audioUtil;
    private int connectChannel = 0;
    private Activity context;

    public int recordSampleRate = 48000;
    public int speekEncodeType = 1;// 1 aac 2pcm
    public int speekChannel = 2;// 单双通道
    public boolean isRecording = false; // 记录是否正在进行录制
    private int sendFrameCounts = 0;// send frames
    private TextView sendFrames;
    private int failureCounts = 0;// 连续解码失败的次数
    private Queue<AudioDataStruct> audioQueue;

    public static final int MAXCHANGE = 40; // frames
    public static final String RECORD_TAG = "RECORD_AUDIO";
    public int writeFilePlayerId = 3;

    // public File audioFile ,orignFile;

    public PlayerAction(DeviceBean deviceBean, int decodeType, Activity context,TextView textView,int channel){
        this.deviceBean = deviceBean ;
        this.decodeType = decodeType ;
        this.audioUtil = new AACDecoderUtil();
        this.context = context;
        this.sendFrames = textView;
        audioQueue = new LinkedList<>();
        this.connectChannel = channel;
        //FileUtils.getSDCardPath();
//        FileUtils.getAndrod12SDCardPath();
//        audioFile = FileUtils.createFile(context,"reciveAudioQueue.pcm");
//        //orignFile = FileUtils.createFile(context,"originAudioFile.aac");
//        if(isWriteToFile&&deviceBean.getPlayerId()==writeFilePlayerId) {
//            vidioFile = FileUtils.createFile(context, "newReciveaudio.h265");
//        }
    }

    /**
     * 开始连接设备
     */
    public void startDeviceConnection() {
        int ret = -1;
        avIndex = 0;
        int sid = IOTCAPIs.IOTC_Get_SessionID();
        if (sid < 0) {
            // Log.d(BaseApplication.TAG,"IOTC_Get_SessionID error code is "+sid);
            // Toast.makeText(context, "IOTC_Get_SessionID error code is "+sid,
            // Toast.LENGTH_SHORT).show();
        }
        ret = IOTCAPIs.IOTC_Connect_ByUID_Parallel(deviceBean.getDeviceUid(), sid);
        // Log.d(BaseApplication.TAG,"Step 2: call IOTC_Connect_ByUID_Parallel ret is
        // "+ret);
        St_AVClientStartInConfig av_client_in_config = new St_AVClientStartInConfig();
        St_AVClientStartOutConfig av_client_out_config = new St_AVClientStartOutConfig();

        av_client_in_config.iotc_session_id = sid;
        av_client_in_config.iotc_channel_id = 1;
        av_client_in_config.timeout_sec = 60;
        av_client_in_config.account_or_identity = deviceBean.getDeviceName();
        av_client_in_config.password_or_token = deviceBean.getDevicePwd();
        av_client_in_config.resend = 1;
        av_client_in_config.security_mode = 0; // enable DTLS
        av_client_in_config.auth_type = 0;

        avIndex = AVAPIs.avClientStartEx(av_client_in_config, av_client_out_config);

        // Log.d(BaseApplication.TAG,"Step 3: avClientStartExl avIndex is "+ret);
        if (startIpcamStream(avIndex)) {
            if (decodeType == NativeMediaPlayer.HARDDECODE) { // 硬解码
                readFrameLoopForHard(avIndex, sid);
            } else {
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
                // 存放目标文件的数据
                ByteBuffer byteBuffer = null;
                MediaCodec mediaCodec = deviceBean.getMediaCodec();
                // 解码后的数据，包含每一个buffer的元数据信息，例如偏差，在相关解码器中有效的数据大小
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

                // Log.d(BaseApplication.TAG," start read frame "+ isStartRead);
                boolean changeDecode = false;
                failureCounts = 0;
                while (isStartRead) {
                    int[] frameNumber = new int[1];
                    int length = av.avRecvFrameData2(avIndex, videoBuffer,
                            VIDEO_BUF_SIZE, outBufSize, outFrameSize,
                            frameInfo, FRAME_INFO_SIZE,
                            outFrmInfoBufSize, frameNumber);
                    // Log.d(BaseApplication.TAG," videoBuffer size is "+length+" playerId is
                    // "+deviceBean.getPlayerId());
                    if (length == AVAPIs.AV_ER_LOSED_THIS_FRAME) {
                        // Log.d(BaseApplication.TAG,Thread.currentThread().getName()+" Lost video frame
                        // number "+ frameNumber[0]);
                        continue;
                    } else if (length == AVAPIs.AV_ER_INCOMPLETE_FRAME) {
                        // Log.d(BaseApplication.TAG,Thread.currentThread().getName()+" Incomplete video
                        // frame number "+frameNumber[0]);
                        continue;
                    } else if (length == AVAPIs.AV_ER_SESSION_CLOSE_BY_REMOTE) {
                        // Log.d(BaseApplication.TAG,Thread.currentThread().getName()+"AV_ER_SESSION_CLOSE_BY_REMOTE");
                        break;
                    } else if (length == AVAPIs.AV_ER_REMOTE_TIMEOUT_DISCONNECT) {
                        // Log.d(BaseApplication.TAG,Thread.currentThread().getName()+"
                        // AV_ER_REMOTE_TIMEOUT_DISCONNECT");
                        break;
                    } else if (length == AVAPIs.AV_ER_INVALID_SID) {
                        // Log.d(BaseApplication.TAG,Thread.currentThread().getName()+" Session cant be
                        // used anymore");
                        break;
                    }
                    if (length < 0) {
                        try {
                            Thread.sleep(50);
                            continue;
                        } catch (InterruptedException e) {
                            // Log.d(BaseApplication.TAG,"read error message is "+e.getMessage());
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
                    // Log.d(BaseApplication.TAG," failureCounts is "+failureCounts + "playerId is
                    // "+deviceBean.getPlayerId());
                    // 1 准备填充器
                    int inIndex = -1;
                    try {
                        inIndex = mediaCodec.dequeueInputBuffer(1000000);// dateReceive
                        if (inIndex >= 0) {
                            // 2 准备填充数据
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
                            // 3 把数据传给解码器
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
                        // Log.e(BaseApplication.TAG, "IllegalStateException dequeueInputBuffer ");
                        continue;
                    }

                    int outIndex = MediaCodec.INFO_TRY_AGAIN_LATER;
                    // 4 开始解码
                    try {
                        outIndex = mediaCodec.dequeueOutputBuffer(info, 0);
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                        // Log.e(BaseApplication.TAG, "IllegalStateException dequeueOutputBuffer " +
                        // e.getMessage());
                        failureCounts++;
                        continue;
                    }
                    if (outIndex >= 0) {
                        // 帧控制
                        boolean doRender = (info.size != 0);
                        // 对outputbuffer的处理完后，调用这个函数把buffer重新返回给codec类。
                        // 调用这个api之后，SurfaceView才有图像
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
                        // Log.d(BaseApplication.TAG,"support 11 INFO_OUTPUT_FORMAT_CHANGED outIndex is
                        // "+outIndex);
                        switch (outIndex) {
                            case MediaCodec.INFO_TRY_AGAIN_LATER:
                                break;
                            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                                MediaFormat newFormat = mediaCodec.getOutputFormat();
                                int videoWidth = newFormat.getInteger("width");
                                int videoHeight = newFormat.getInteger("height");
                                // Log.d(BaseApplication.TAG,"support 22 INFO_OUTPUT_FORMAT_CHANGED type is
                                // "+mediaCodec.getOutputFormat()+ "videoWidth is "+videoWidth+" videoHeight is
                                // "+videoHeight);
                                break;
                        }
                    }
                }
                audioOpen = false;
                setSendFrames(0);
                // Log.d(BaseApplication.TAG," avClientStop OK11 avIndex is "+avIndex);
                AVAPIs.avClientStop(avIndex);
                IOTCAPIs.IOTC_Session_Close(sid);
                if (changeDecode) {
                    EventBus.getDefault().post(EventResult.getResult(CHANGE_DECODE, 0, deviceBean.getPlayerId()));
                } else {
                    EventBus.getDefault().post(EventResult.getResult(RELEASE_DECODER, 1, deviceBean.getPlayerId()));
                }
            }
        }).start();
    }

    public int onFrame(byte[] buf, int length, MediaCodec mediaCodec, MediaCodec.BufferInfo mediaCodecBufferInfo) {
        try {
            ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
            int inputBufferIndex = mediaCodec.dequeueInputBuffer(50);
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                inputBuffer.put(buf, 0, length);
                mediaCodec.queueInputBuffer(inputBufferIndex, 0, length, System.currentTimeMillis(), 0);
            }
            int outputBufferIndex = mediaCodec.dequeueOutputBuffer(mediaCodecBufferInfo, 0);
            // Log.d(BaseApplication.TAG," onFrame outputBufferIndex is
            // "+outputBufferIndex);
            if (outputBufferIndex >= 0) {
                if (!deviceBean.isDecode()) {
                    EventBus.getDefault().post(EventResult.getResult(DECODE, 1, deviceBean.getPlayerId()));
                    deviceBean.setDecode(true);
                }
                mediaCodec.releaseOutputBuffer(outputBufferIndex, true);
                mediaCodec.dequeueOutputBuffer(mediaCodecBufferInfo, 0);
                System.gc();
                return 1;
            } else {
                System.gc();
                return -1;
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return -1;
        }
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
                // Log.d(BaseApplication.TAG," start read frame "+ isStartRead);
                while (isStartRead) {
                    int[] frameNumber = new int[1];
                    int length = av.avRecvFrameData2(avIndex, videoBuffer,
                            VIDEO_BUF_SIZE, outBufSize, outFrameSize,
                            frameInfo, FRAME_INFO_SIZE,
                            outFrmInfoBufSize, frameNumber);
                    // Log.d(BaseApplication.TAG," videoBuffer size is "+length);
                    if (length == AVAPIs.AV_ER_LOSED_THIS_FRAME) {
                        // Log.d(BaseApplication.TAG,Thread.currentThread().getName()+" Lost video frame
                        // number "+ frameNumber[0]);
                        continue;
                    } else if (length == AVAPIs.AV_ER_INCOMPLETE_FRAME) {
                        // Log.d(BaseApplication.TAG,Thread.currentThread().getName()+" Incomplete video
                        // frame number "+frameNumber[0]);
                        continue;
                    } else if (length == AVAPIs.AV_ER_SESSION_CLOSE_BY_REMOTE) {
                        // Log.d(BaseApplication.TAG,Thread.currentThread().getName()+"AV_ER_SESSION_CLOSE_BY_REMOTE");
                        break;
                    } else if (length == AVAPIs.AV_ER_REMOTE_TIMEOUT_DISCONNECT) {
                        // Log.d(BaseApplication.TAG,Thread.currentThread().getName()+"
                        // AV_ER_REMOTE_TIMEOUT_DISCONNECT");
                        break;
                    } else if (length == AVAPIs.AV_ER_INVALID_SID) {
                        // Log.d(BaseApplication.TAG,Thread.currentThread().getName()+" Session cant be
                        // used anymore");
                        break;
                    }
                    if (length < 0) {
                        try {
                            Thread.sleep(30);
                            continue;
                        } catch (InterruptedException e) {
                            // Log.d(BaseApplication.TAG,"read error message is "+e.getMessage());
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
                        // Log.d(BaseApplication.TAG,"sleep error message is "+e.getMessage());
                        break;
                    }
                }
                audioOpen = false;
                setSendFrames(0);
                // Log.d(BaseApplication.TAG," avClientStop OK11 avIndex is "+avIndex);
                AVAPIs.avClientStop(avIndex);
                IOTCAPIs.IOTC_Session_Close(sid);
                EventBus.getDefault().post(EventResult.getResult(RELEASE_DECODER, 1, deviceBean.getPlayerId()));
            }
        }).start();
    }

    private boolean startIpcamStream(int avIndex) {
        av = new AVAPIs();
        int IOTYPE_USER_IPCAM_START = 0x1FF;
        final int ret = av.avSendIOCtrl(avIndex, IOTYPE_USER_IPCAM_START,
                new byte[8], 8);
        if (ret < 0) {
            // Log.d(BaseApplication.TAG,"start_ipcam_stream failed result is "+ret);
            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Toast.makeText(context, "start_ipcam_stream failed result is "+ret,
                    // Toast.LENGTH_SHORT).show();
                }
            });
            return false;
        }
        return true;
    }

    /**
     * ptz control
     * 
     * @param action
     * @return
     */
    public boolean ptzControl(int action) {

        char[] actionChar = String.valueOf(action).toCharArray();
        byte[] cmdAction = new byte[actionChar.length];
        for (int i = 0; i < actionChar.length; i++) {
            Log.d("ptzControl", "char = " + actionChar[i]);
            byte temp = (byte) actionChar[i]; // 将每一个char转换成byte
            cmdAction[i] = temp; // 保存到数组中
        }
        if (av != null && isStartRead) {
            int ret = av.avSendIOCtrl(avIndex, Contacts.IOTYPE_USER_IPCAM_PTZ_COMMAND,
                    cmdAction, actionChar.length);
            Log.d("ptzControl", "ret = " + ret + " action = " + cmdAction[0]);
            if (ret < 0) {
                return false;
            }
            return true;
        } else {
            Log.d("ptzControl", "device not create  play channel");
            return false;
        }
    }

    public void setStartRead(boolean startRead) {
        isStartRead = startRead;
        if (!isStartRead) {
            isRecording = false;
        }
        if (audioOpen) {
            audioOpen = false;
        }
    }

    /**
     * 提示
     * 
     * @param message
     */
    public void showMessage(final String message) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 发送音频帧数
     * 
     * @param frames
     */
    public void setSendFrames(final int frames) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (sendFrames != null) {
                    sendFrames.setText("Send Frames：" + frames);
                }
                if (frames == 0) {
                    sendFrameCounts = 0;
                }
            }
        });
    }

    /*********************** 音频相关 **************************/
    /**
     * 关闭音频
     */
    public void stopAudioStream() {
        if (audioOpen) {
            audioOpen = false;
        } else {
            showMessage("音频未开启,无需关闭.");
            return;
        }
        if (!isStartRead) {
            showMessage("连接通道未建立");
            return;
        }
        if (av != null) {
            System.out.printf("[TUTK]start_ipcam_audio stop audio ");
            int ret = av.avSendIOCtrl(avIndex, IOTYPE_USER_IPCAM_AUDIOSTOP,
                    new byte[8], 8);
            if (ret < 0) {
                System.out.printf("[TUTK]start_ipcam_audio stream failed[%d]\n", ret);
            } else {
                System.out.printf("[TUTK]start_ipcam_audio stop audio success");
            }
        }
        audioUtil.stop();
        showMessage("关闭音频");
    }

    /**
     * 开启音频
     */
    public void startAudioStream(int sampleRate, int audioEncodeType, int audioChannel) {
        if (audioOpen) {
            // Log.d(BaseApplication.TAG,"start_ipcam_audio audioOpen is open");
            showMessage("音频已经开启");
            return;
        }
        if (!isStartRead) {
            showMessage("连接通道未建立");
            return;
        }
        if (av == null) {
            // Log.d(BaseApplication.TAG,"ipcam init null");
            av = new AVAPIs();
        }
        int IOTYPE_USER_IPCAM_AUDIOSTART = 0x300;
        int ret = av.avSendIOCtrl(avIndex, IOTYPE_USER_IPCAM_AUDIOSTART,
                new byte[8], 8);
        if (ret < 0) {
            System.out.printf("[TUTK]start_ipcam_audio stream failed[%d]\n", ret);
        }
        System.out.printf("[TUTK]start_ipcam_audio stream start[%d]\n", ret);
        audioOpen = true;
        audioUtil.start(sampleRate, audioEncodeType, audioChannel);
        String type = "AAC";
        if (audioEncodeType == 1) {
            type = "AAC";
        } else {
            type = "PCM";
        }
        showMessage("开启音频" + type + sampleRate + "：" + audioChannel + "Channel");
        new Thread(new Runnable() {
            @Override
            public void run() {
                readAudioLoop();
            }
        }).start();
        readAudioToPlay();
    }

    private void readAudioToPlay() {
        AudioDataStruct audioDataStruct;
        /*
         * FileOutputStream fileOutputStream = null;
         * try {
         * fileOutputStream = new FileOutputStream(audioFile);
         * } catch (FileNotFoundException e) {
         * e.printStackTrace();
         * }
         */
        while (audioOpen) {
            // Log.d(BaseApplication.TAG,"audioQueue size is "+audioQueue.size());
            if (audioQueue.size() == 0) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }
            audioDataStruct = audioQueue.poll();
            /*
             * try {
             * fileOutputStream.write(audioDataStruct.audioBuffer,0,audioDataStruct.length);
             * } catch (IOException e) {
             * e.printStackTrace();
             * }
             */
            if (audioDataStruct != null) {
                audioUtil.decode(audioDataStruct.audioBuffer, 0, audioDataStruct.length);
            }
            try {
                Thread.sleep(10);
                continue;
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
        }
        /*
         * try {
         * fileOutputStream.flush();
         * fileOutputStream.close();
         * } catch (IOException e) {
         * e.printStackTrace();
         * }
         */
        audioQueue.clear();
    }

    /**
     * audio input audio queue
     */
    private void readAudioLoop() {
        final int AUDIO_BUF_SIZE = 1024 * 2 * 1024;
        final int FRAME_INFO_SIZE = 16;
        byte[] frameInfo = new byte[FRAME_INFO_SIZE];
        byte[] audioBuffer = new byte[AUDIO_BUF_SIZE];
        byte[] realAudio = null;
        long timemills = System.currentTimeMillis();
        while (audioOpen) {
            int ret = -1;
            int[] frameNumber = new int[1];
            ret = av.avRecvAudioData(avIndex, audioBuffer,
                    AUDIO_BUF_SIZE, frameInfo, FRAME_INFO_SIZE,
                    frameNumber);
            if (ret > 0) {
                // 添加到队列
                realAudio = new byte[ret];
                timemills = System.currentTimeMillis();
                System.arraycopy(audioBuffer, 0, realAudio, 0, ret);
                // Log.d(BaseApplication.TAG,"times ==
                // "+(System.currentTimeMillis()-timemills));
                audioQueue.offer(new AudioDataStruct(realAudio, ret));
                // audioUtil.decode(audioBuffer,0,ret);
            } else if (ret <= 0) {
                System.out.printf("[TUTK][%s] AV_ER_SESSION_CLOSE_BY_REMOTE\n",
                        Thread.currentThread().getName() + " ret == " + ret);
                try {
                    Thread.sleep(5);
                    continue;
                } catch (InterruptedException e) {
                    System.out.println(e.getMessage());
                    break;
                }
            }

        }
    }

    /************************ 音频采集 ***************************/
    /**
     * 开始录音
     * 
     * @param recordSampleRate
     * @return
     */
    public boolean startRecord(int recordSampleRate, int encodeType, int channel) {
        if (isRecording) {
            showMessage("已经在录音");
            return false;
        }
        if (!isStartRead) {
            showMessage("连接通道未建立");
            return false;
        }
        this.recordSampleRate = recordSampleRate;
        this.speekEncodeType = encodeType;
        this.speekChannel = channel;
        isRecording = true;
        String type = "AAC";
        if (speekEncodeType == 1) {
            type = "AAC";
        } else {
            type = "PCM";
        }
        showMessage("建立语音对讲" + type + recordSampleRate + ":" + speekChannel + "Channel");
        new RecordTask().execute();

        return true;
    }

    /**
     * 停止录音
     * 
     * @return
     */
    public boolean stopRecord() {
        if (!isRecording) {
            showMessage("未开始录音");
            return false;
        }
        setSendFrames(0);
        isRecording = false;
        return true;
    }

    // 录制音频参数
    // private int frequence = 48000;
    // //录制频率，单位hz.这里的值注意了，写的不好，可能实例化AudioRecord对象的时候，会出错。我开始写成11025就不行。这取决于硬件设备
    private int channelConfig = AudioFormat.CHANNEL_IN_STEREO;
    private int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

    // 录音线程
    class RecordTask extends AsyncTask<Void, Integer, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                // 先发开始对讲指令
                if (av != null) {
                    Log.d(RECORD_TAG, "send speek commond to device");
                    int ret = av.avSendIOCtrl(avIndex, IOTYPE_USER_IPCAM_SPEAKERSTART,
                            new byte[8], 8);
                    if (ret < 0) {
                        Log.d(RECORD_TAG, "send speek commond to failure");
                    } else {
                        Log.d(RECORD_TAG, "send speek commond to success");
                    }
                }

                // 开通输出流到指定的文件
                // DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new
                // FileOutputStream(audioFile)));
                // 根据定义好的几个配置，来获取合适的缓冲大小
                if (speekChannel == 1) {
                    channelConfig = AudioFormat.CHANNEL_IN_MONO; // AudioFormat.CHANNEL_IN_MONO
                } else {
                    channelConfig = AudioFormat.CHANNEL_IN_STEREO;
                }
                int bufferSize = AudioRecord.getMinBufferSize(recordSampleRate, channelConfig, audioEncoding);
                // 实例化AudioRecord
                AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.MIC, recordSampleRate, channelConfig,
                        audioEncoding, bufferSize);
                // 开始录制
                record.startRecording();
                AacEncode aacMediaEncode = new AacEncode(recordSampleRate, speekChannel);
                // 定义缓冲
                byte[] buffer = new byte[bufferSize];
                // 定义循环，根据isRecording的值来判断是否继续录制

                final int FRAME_INFO_SIZE = 16;
                byte[] frameInfo = new byte[FRAME_INFO_SIZE];
                while (isRecording) {
                    // 从bufferSize中读取字节。
                    int bufferReadResult = record.read(buffer, 0, bufferSize);
                    // 获取字节流
                    if (AudioRecord.ERROR_INVALID_OPERATION != bufferReadResult) {
                        if (speekEncodeType == 2) {
                            Log.d(RECORD_TAG, "pcm data send length is " + buffer.length);
                            int result = av.avSendAudioData(avIndex, buffer, bufferSize, frameInfo, FRAME_INFO_SIZE);
                            Log.d(RECORD_TAG, "pcm data send result is " + result);
                            sendFrameCounts++;
                            setSendFrames(sendFrameCounts);
                        } else {
                            // 转成AAC编码
                            byte[] ret = aacMediaEncode.offerEncoder(buffer);
                            if (ret.length > 0) {
                                /***********************/
                                // String temp = Integer.toHexString(0xFF&ret[13]);
                                if (ret.length > 14) {
                                    // Log.d(BaseApplication.TAG, "byt[5] == " + Integer.toHexString(0xFF&ret[13]));
                                }
                                // Log.d(BaseApplication.TAG,"byte[3] = "+ ret[3] +" byte[4] = "+ret[4]+"
                                // byte[5] ="+ret[5]);

                                if (ret.length > 1023) {
                                    // 发送数据到设备
                                    Log.d(RECORD_TAG, "AAC data 1023  length is " + ret.length);
                                    int result = av.avSendAudioData(avIndex, ret, 1023, frameInfo, FRAME_INFO_SIZE);
                                    Log.d(RECORD_TAG, "AAC data send result is " + result);
                                } else {
                                    // 发送数据到设备
                                    Log.d(RECORD_TAG, "AAC data length is " + ret.length);
                                    int result = av.avSendAudioData(avIndex, ret, ret.length, frameInfo,
                                            FRAME_INFO_SIZE);
                                    Log.d(RECORD_TAG, "AAC data send result is " + result);
                                }
                                // 发送数据到设备
                                /*
                                 * Log.d(RECORD_TAG, "AAC data length is " + ret.length);
                                 * int result = av.avSendAudioData(avIndex, ret, ret.length, frameInfo,
                                 * FRAME_INFO_SIZE);
                                 * Log.d(RECORD_TAG, "AAC data send result is " + result);
                                 */
                                sendFrameCounts++;
                                setSendFrames(sendFrameCounts);
                            }
                        }
                    }
                }
                // 录制结束
                record.stop();
                // 释放编码器
                aacMediaEncode.close();
                // 发停止对讲指令
                if (av != null) {
                    Log.d(RECORD_TAG, "send ");
                    int ret = av.avSendIOCtrl(avIndex, IOTYPE_USER_IPCAM_SPEAKERSTOP,
                            new byte[8], 8);
                    if (ret < 0) {
                        Log.d(RECORD_TAG, "send stopspeek commond to failure");
                    } else {
                        Log.d(RECORD_TAG, "send stopspeek commond to success");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
