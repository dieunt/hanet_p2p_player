package com.connect.module.module.action;



import static com.xc.p2pVideo.NativeMediaPlayer.CHANGE_DECODE;
import static com.xc.p2pVideo.NativeMediaPlayer.DECODE;
import static com.xc.p2pVideo.NativeMediaPlayer.RELEASE_DECODER;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;


import com.connect.module.module.audio.AACDecoderUtil;
import com.connect.module.module.bean.DeviceBean;
import com.connect.module.module.bean.EventResult;
import com.tutk.IOTC.AVAPIs;
import com.tutk.IOTC.IOTCAPIs;
import com.tutk.IOTC.St_AVClientStartInConfig;
import com.tutk.IOTC.St_AVClientStartOutConfig;
import com.xc.p2pVideo.NativeMediaPlayer;

import org.greenrobot.eventbus.EventBus;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class PlayBackAction {
    private boolean status = false;// is playing
    private static final int VIDEO_BUF_SIZE = 5 * 1024 * 1024;
    private static final int FRAME_INFO_SIZE = 16;
    private DeviceBean deviceBean;
    private AVAPIs av = null;
    private int avIndex = -1, playBackIndex = -1;
    private int sid = -1;
    private Context context;
    private boolean isWait = true; // searching
    private EventResult eventResult, eventPostTimes;
    public int decodeType = NativeMediaPlayer.SOFTDECODE;
    private int failureCounts = 0;// 连续解码失败的次数
    public static final int MAXCHANGE = 40; // frames
    private boolean audioOpen = false;
    private int playBackStreamChannel = -1; // 回放流通道
    // 音频解码器
    private AACDecoderUtil audioUtil;
    private int fileLength = 10;// file times
    private int currentPosition = 0;

    private int IOTYPE_USER_SD_TIME_REQ = 0x07ff;
    private int IOTYPE_USER_SD_TIME_RESP = 2303; // 0x08ff
    private int IOTYPE_USER_SD_PLAY_REQ = 0x09ff;
    private int IOTYPE_USER_SD_PLAY_RESP = 2815;// 0x0aff;
    private int IOTYPE_USER_IPCAM_LISTEVENT_REQ2 = 0x0d01;
    private int IOTYPE_USER_IPCAM_LISTEVENT_RESP2 = 3330; // 0x0d02
    private int IOTYPE_USER_IPCAM_RECORD_START = 0x0d11;// 0x031A;
    private int IOTYPE_USER_IPCAM_RECORD_PLAYCONTROL_RESP = 795;// 0x031B;
    private int IOTYPE_USER_IPCAM_RECORD_STOP = 0x0d14;
    private int IOTTYPE_USER_IPCAM_RECORD_PLAY_PAUSE = 0x0d12;
    private int IOTYPE_USER_IPCAM_RECORD_PLAY_NEXT = 0x0d13;
    private int IOTYPE_USER_VIDEO_PLAY_TIME_REQ = 0x0bff;

    public static final String RECORD_SEARCH = "record_search";
    public static final String RECORD_EVENTLIST = "record_list";
    public static final String RECORD_FILE_RESEARCH = "file_research";
    public static final String RECORD_CREATE_CHANNEL = "create_channel";
    public static final String RECORD_VIDEO_PLAY = "record_video_play";
    public static final String CLEAR_VIDEO_PLAY = "clear_video_play";
    public static final String DEVICE_CONNECT = "device_connect";
    public static final String DEVICE_SHOW_TIMEBAR = "show_player_time";
    public static final String REFRESH_TIMES = "refresh_player_times";
    public static final int SEARCH_DATA_FAILURE = 1002;
    public static final int SEARCH_DATA_SUCCESS = 1003;

    public PlayBackAction(DeviceBean deviceBean) {
        this.deviceBean = deviceBean;
        this.audioUtil = new AACDecoderUtil();
        eventPostTimes = new EventResult();
        eventPostTimes.setRequestUrl(REFRESH_TIMES);
    }

    /**
     * 开始连接设备
     */
    public void startDeviceConnection() {
        eventResult = new EventResult();
        eventResult.setRequestUrl(DEVICE_CONNECT);
        int ret = -1;
        avIndex = 0;
        sid = IOTCAPIs.IOTC_Get_SessionID();
        if (sid < 0) {
        }
        ret = IOTCAPIs.IOTC_Connect_ByUID_Parallel(deviceBean.getDeviceUid(), sid);
        St_AVClientStartInConfig av_client_in_config = new St_AVClientStartInConfig();
        St_AVClientStartOutConfig av_client_out_config = new St_AVClientStartOutConfig();
        av_client_in_config.iotc_session_id = sid;
        av_client_in_config.iotc_channel_id = 0;
        av_client_in_config.timeout_sec = 60;
        av_client_in_config.account_or_identity = deviceBean.getDeviceName();
        av_client_in_config.password_or_token = deviceBean.getDevicePwd();
        av_client_in_config.resend = 1;
        av_client_in_config.security_mode = 0; // enable DTLS
        av_client_in_config.auth_type = 0;
        avIndex = AVAPIs.avClientStartEx(av_client_in_config, av_client_out_config);
        av = new AVAPIs();
        if (avIndex < 0) {
            eventResult.setResponseCode(SEARCH_DATA_FAILURE);
            eventResult.setObject("MSG channel :" + avIndex);
        } else {
            eventResult.setResponseCode(SEARCH_DATA_SUCCESS);
        }
        if (avIndex > -1) {
            int rets = av.avSendIOCtrl(avIndex, IOTYPE_USER_IPCAM_RECORD_START, new byte[8], 8);
            if (rets < 0) {
                eventResult.setResponseCode(SEARCH_DATA_FAILURE);
                eventResult.setObject("IOTYPE_USER_IPCAM_RECORD_START  :" + rets);
            } else {
                isWait = true;
                int count = 0;
                int ioctrl[] = { IOTYPE_USER_IPCAM_RECORD_PLAYCONTROL_RESP };
                while (isWait) {
                    byte buffer[] = new byte[64 * 2048];// 这个大小很重要
                    byte temp[] = null;
                    ret = av.avRecvIOCtrl(avIndex, ioctrl, buffer, 64 * 2048, 500);
                    if (ret > 0 && ioctrl[0] == IOTYPE_USER_IPCAM_RECORD_PLAYCONTROL_RESP && buffer.length > 0) {
                        temp = new byte[ret];
                        System.arraycopy(buffer, 0, temp, 0, ret);
                        isWait = false;
                        playBackStreamChannel = Integer.parseInt(temp[0] + "");
                        St_AVClientStartInConfig av_client_in_config2 = new St_AVClientStartInConfig();
                        St_AVClientStartOutConfig av_client_out_config2 = new St_AVClientStartOutConfig();
                        av_client_in_config2.iotc_session_id = sid;
                        av_client_in_config2.iotc_channel_id = playBackStreamChannel;
                        av_client_in_config2.timeout_sec = 60;
                        av_client_in_config2.account_or_identity = deviceBean.getDeviceName();
                        av_client_in_config2.password_or_token = deviceBean.getDevicePwd();
                        av_client_in_config2.resend = 1;
                        av_client_in_config2.security_mode = 0; // enable DTLS
                        av_client_in_config2.auth_type = 0;
                        playBackIndex = AVAPIs.avClientStartEx(av_client_in_config2, av_client_out_config2);
                        if (playBackIndex < 0) {
                            stopVideoClient();
                            eventResult.setResponseCode(SEARCH_DATA_FAILURE);
                            eventResult.setObject("playBackIndex  :" + playBackIndex);
                        }
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    count++;
                    if (count == 10) {
                        isWait = false;
                        eventResult.setResponseCode(SEARCH_DATA_FAILURE);
                        eventResult.setObject("read playbackChannel timeout!!!!");
                    }
                }
            }
        }
        EventBus.getDefault().post(eventResult);
    }

    /**
     * search date
     */
    public void searchRecordDataTime() {
        eventResult = new EventResult();
        eventResult.setRequestUrl(RECORD_SEARCH);
        if (avIndex < 0) {
            startDeviceConnection();
        }
        if (avIndex < 0) {
            AVAPIs.avClientStop(avIndex);
            avIndex = -1;
            IOTCAPIs.IOTC_Session_Close(sid);
            eventResult.setResponseCode(SEARCH_DATA_FAILURE);
            EventBus.getDefault().post(eventResult);
            return;
        }
        if (startIpcamSearchData(avIndex)) {
            // Log.d(BaseApplication.TAG,"startIpcamSearchData success!!!");
        } else {
            eventResult.setResponseCode(SEARCH_DATA_FAILURE);
            EventBus.getDefault().post(eventResult);
            return;
        }
        isWait = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                int count = 0;
                while (isWait) {
                    int ioctrl[] = { IOTYPE_USER_SD_TIME_REQ };
                    byte buffer[] = new byte[64 * 2048];// 这个大小很重要
                    byte temp[] = null;
                    String searchData = "";
                    int ret = av.avRecvIOCtrl(avIndex, ioctrl, buffer, 64 * 2048, 500);
                    if (ret > 0 && IOTYPE_USER_SD_TIME_RESP == ioctrl[0] && buffer.length > 0) { // 录像日期
                        temp = new byte[ret];
                        System.arraycopy(buffer, 0, temp, 0, ret);
                        try {
                            searchData = new String(temp, "utf-8");
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        if (searchData.contains("{")) {
                            eventResult.setResponseCode(SEARCH_DATA_SUCCESS);
                            eventResult.setObject(searchData);
                            isWait = false;
                        } else {
                            isWait = false;
                            eventResult.setResponseCode(SEARCH_DATA_FAILURE);
                            EventBus.getDefault().post(eventResult);
                        }
                        EventBus.getDefault().post(eventResult);
                    }
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    count++;
                    if (count == 20) {
                        isWait = false;
                        eventResult.setResponseCode(SEARCH_DATA_FAILURE);
                        EventBus.getDefault().post(eventResult);
                    }
                }
            }
        }).start();
    }

    public void getTimeListForDate(final String date) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean isReSearch = false; // 切换日期搜索，处理正在等待结果问题
                if (isWait) {
                    isWait = false;
                    isReSearch = true;
                }
                eventResult = new EventResult();
                eventResult.setRequestUrl(RECORD_EVENTLIST);
                if (avIndex < 0) {
                    startDeviceConnection();
                }
                if (avIndex < 0) {
                    AVAPIs.avClientStop(avIndex);
                    avIndex = -1;
                    IOTCAPIs.IOTC_Session_Close(sid);
                    eventResult.setResponseCode(SEARCH_DATA_FAILURE);
                    EventBus.getDefault().post(eventResult);
                    return;
                }
                if (sentGetListEvent(date)) {
                    // Log.d(BaseApplication.TAG,"sentGetListEvent success!!!");
                } else {
                    eventResult.setResponseCode(SEARCH_DATA_FAILURE);
                    EventBus.getDefault().post(eventResult);
                    return;
                }
                isWait = true;

                int count = 0;
                int ioctrl[] = { IOTYPE_USER_IPCAM_LISTEVENT_RESP2 };
                StringBuilder sb = new StringBuilder();
                while (isWait) {
                    String searchData = "";
                    byte buffer[] = new byte[64 * 2048];// 这个大小很重要
                    int ret = av.avRecvIOCtrl(avIndex, ioctrl, buffer, 64 * 2048, 500);
                    if (ret > 0 && ioctrl[0] == IOTYPE_USER_IPCAM_LISTEVENT_RESP2 && buffer.length > 1) { // 录像列表
                        try {
                            searchData = new String(buffer, "utf-8");
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        if (searchData.contains("}")) {
                            sb.append(searchData);
                            eventResult.setResponseCode(SEARCH_DATA_SUCCESS);
                            eventResult.setObject(sb.toString());
                            isWait = false;
                            EventBus.getDefault().post(eventResult);
                        } else {
                            sb.append(searchData);
                            continue;
                        }
                    }
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    count++;
                    if (count == 20) {
                        isWait = false;
                        eventResult.setResponseCode(SEARCH_DATA_FAILURE);
                        EventBus.getDefault().post(eventResult);
                    }
                }
                if (isReSearch) {
                    eventResult = new EventResult();
                    eventResult.setObject(date);
                    eventResult.setRequestUrl(RECORD_FILE_RESEARCH);
                    EventBus.getDefault().post(eventResult);
                }

            }
        }).start();

    }

    private boolean sentGetListEvent(String date) {
        int ret = av.avSendIOCtrl(avIndex, IOTYPE_USER_IPCAM_LISTEVENT_REQ2,
                date.getBytes(), date.getBytes().length);
        if (ret < 0) {
            // Log.d(BaseApplication.TAG,"sentGetListEvent failed result is "+ret);
            return false;
        }
        // Log.d(BaseApplication.TAG,"sentGetListEvent true result is "+ret);
        return true;
    }

    private boolean startIpcamSearchData(int avIndex) {
        int ret = av.avSendIOCtrl(avIndex, IOTYPE_USER_SD_TIME_REQ,
                new byte[8], 8);
        if (ret < 0) {
            // Log.d(BaseApplication.TAG,"start_ipcam_stream failed result is "+ret);
            return false;
        }
        return true;
    }

    /**
     * @param fileName
     * @return
     */
    private boolean sendPlayFile(String fileName) {
        int ret = -1;
        EventResult event = new EventResult();
        event.setRequestUrl(DEVICE_SHOW_TIMEBAR);
        // Log.d(BaseApplication.TAG,"play fileName is "+fileName);
        ret = av.avSendIOCtrl(avIndex, IOTYPE_USER_IPCAM_RECORD_PLAY_NEXT,
                new byte[8], 8);
        // Log.d(BaseApplication.TAG,"IOTYPE_USER_IPCAM_RECORD_PLAY_NEXT ret = "+ret);
        ret = av.avSendIOCtrl(avIndex, IOTYPE_USER_SD_PLAY_REQ,
                fileName.getBytes(), fileName.getBytes().length);
        if (ret < 0) {
            // Log.d(BaseApplication.TAG,"sendPlayFile failed result is "+ret);
            return false;
        }
        isWait = true;
        int count = 0;
        String searchData = "";
        while (isWait) {
            int ioctrl[] = { IOTYPE_USER_SD_PLAY_REQ };
            byte buffer[] = new byte[64 * 2048];// 这个大小很重要
            ret = av.avRecvIOCtrl(avIndex, ioctrl, buffer, 64 * 2048, 500);
            if (ret > 0 && IOTYPE_USER_SD_PLAY_RESP == ioctrl[0] && buffer.length > 0) { // 文件时长
                try {
                    searchData = new String(buffer, "utf-8");
                    fileLength = Integer.parseInt(searchData.substring(0, (ret - 1)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                isWait = false;
                if (fileLength > 0) {
                    event.setObject(fileLength);
                    EventBus.getDefault().post(event);
                }
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            count++;
            if (count == 20) {
                isWait = false;
            }
        }
        return true;
    }

    /**
     * create playback video
     * 
     * @param channelId
     */
    public void createVideoPlay(int channelId) {
        if (playBackIndex < 0) {
            eventResult = new EventResult();
            eventResult.setRequestUrl(RECORD_VIDEO_PLAY);
            St_AVClientStartInConfig av_client_in_config = new St_AVClientStartInConfig();
            St_AVClientStartOutConfig av_client_out_config = new St_AVClientStartOutConfig();
            av_client_in_config.iotc_session_id = sid;
            // Log.d(BaseApplication.TAG, "create video play channel id is " + channelId);
            av_client_in_config.iotc_channel_id = channelId;
            av_client_in_config.timeout_sec = 60;
            av_client_in_config.account_or_identity = deviceBean.getDeviceName();
            av_client_in_config.password_or_token = deviceBean.getDevicePwd();
            av_client_in_config.resend = 1;
            av_client_in_config.security_mode = 0; // enable DTLS
            av_client_in_config.auth_type = 0;
            playBackIndex = AVAPIs.avClientStartEx(av_client_in_config, av_client_out_config);
            // Log.d(BaseApplication.TAG, "create video play index is " + playBackIndex);
            if (playBackIndex < 0) {
                stopVideoClient();
                eventResult.setResponseCode(SEARCH_DATA_FAILURE);
                EventBus.getDefault().post(eventResult);
                return;
            }
        }
        /*
         * if(decodeType == NativeMediaPlayer.HARDDECODE){ //硬解码
         * readFrameLoopForHard(playBackIndex);
         * }else{
         * readFrameLoopForSoft(playBackIndex);
         * }
         */
        readFrameLoopForSoft(playBackIndex);
        audioOpen = true;
        readAudioLoop();
    }

    private void readFrameLoopForSoft(final int playVideoIndex) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                AVAPIs av = new AVAPIs();
                byte[] frameInfo = new byte[FRAME_INFO_SIZE];
                byte[] videoBuffer = new byte[VIDEO_BUF_SIZE];
                int[] outBufSize = new int[1];
                int[] outFrameSize = new int[1];
                int[] outFrmInfoBufSize = new int[1];
                byte[] temp = null;
                status = true;
                // Log.d(BaseApplication.TAG," start read frame "+ status);
                while (status) {
                    int[] frameNumber = new int[1];
                    int length = av.avRecvFrameData2(playVideoIndex, videoBuffer,
                            VIDEO_BUF_SIZE, outBufSize, outFrameSize,
                            frameInfo, FRAME_INFO_SIZE,
                            outFrmInfoBufSize, frameNumber);

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
                    temp = new byte[3];
                    System.arraycopy(frameInfo, 0, temp, 0, 3);
                    if (currentPosition == Integer.parseInt(new String(temp))) {

                    } else {
                        currentPosition = Integer.parseInt(new String(temp));
                        // Log.d(BaseApplication.TAG," frameInfo time is "+currentPosition);
                        eventPostTimes.setObject(currentPosition);
                        EventBus.getDefault().post(eventPostTimes);
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
                failureCounts = 0;
                stopVideoClient();
                EventBus.getDefault().post(EventResult.getResult(RELEASE_DECODER, 1, deviceBean.getPlayerId()));
            }
        }).start();
    }

    private void readFrameLoopForHard(final int index) {
        new Thread(new Runnable() {
            @Override
            public void run() {
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
                status = true;
                boolean changeDecode = false;
                failureCounts = 0;
                while (status) {
                    int[] frameNumber = new int[1];
                    int length = av.avRecvFrameData2(index, videoBuffer,
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
                        status = false;
                        failureCounts = 0;
                    }
                    /**
                     * 调用硬解码
                     */
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
                failureCounts = 0;
                stopVideoClient();
                if (changeDecode) {
                    EventBus.getDefault().post(EventResult.getResult(CHANGE_DECODE, 0, deviceBean.getPlayerId()));
                } else {
                    EventBus.getDefault().post(EventResult.getResult(RELEASE_DECODER, 1, deviceBean.getPlayerId()));
                }
            }
        }).start();
    }

    /**
     * 循环读取音频
     */
    private void readAudioLoop() {
        final int AUDIO_BUF_SIZE = 1024 * 2 * 1024;
        final int FRAME_INFO_SIZE = 16;
        byte[] frameInfo = new byte[FRAME_INFO_SIZE];
        byte[] audioBuffer = new byte[AUDIO_BUF_SIZE];
        int sampleRate = 16000;
        int audioEncodeType = 1;// 1:AAC 2:pcm
        int audioChannel = 2; // 1：单通道 2：双通道
        audioUtil.start(sampleRate, audioEncodeType, audioChannel);
        while (audioOpen) {
            int ret = -1;
            int[] frameNumber = new int[1];
            ret = av.avRecvAudioData(playBackIndex, audioBuffer,
                    AUDIO_BUF_SIZE, frameInfo, FRAME_INFO_SIZE,
                    frameNumber);
            // System.out.printf("[TUTK]start_ipcam_audio stream decode[%d]\n", ret);
            if (ret > 0) {
                // 此处解码播放
                // System.out.printf("[TUTK]start_ipcam_audio stream decode[%d]\n", ret);
                audioUtil.decode(audioBuffer, 0, ret);
            } else if (ret < 0) {
                // System.out.printf("[TUTK]start_ipcam_audio stream decode[%d]\n", ret);
                if (ret == AVAPIs.AV_ER_SESSION_CLOSE_BY_REMOTE) {
                    System.out.printf("[TUTK][%s] AV_ER_SESSION_CLOSE_BY_REMOTE\n",
                            Thread.currentThread().getName());
                    break;
                } else if (ret == AVAPIs.AV_ER_DATA_NOREADY) {
                    try {
                        Thread.sleep(30);
                        continue;
                    } catch (InterruptedException e) {
                        System.out.println(e.getMessage());
                        break;
                    }
                } else if (ret == AVAPIs.AV_ER_REMOTE_TIMEOUT_DISCONNECT) {
                    System.out.printf("[TUTK][%s] AV_ER_REMOTE_TIMEOUT_DISCONNECT\n",
                            Thread.currentThread().getName());
                    break;
                } else if (ret == AVAPIs.AV_ER_INVALID_SID) {
                    System.out.printf("[TUTK][%s] Session cant be used anymore\n",
                            Thread.currentThread().getName());
                    break;
                } else if (ret == AVAPIs.AV_ER_LOSED_THIS_FRAME) {
                    System.out.printf("[TUTK][%s] Audio frame losed\n",
                            Thread.currentThread().getName());
                    continue;
                } else {
                    break;
                }
            }
            try {
                Thread.sleep(30);
                continue;
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
                break;
            }
        }
        audioUtil.stop();
    }

    /**
     * create play
     * 
     * @param fileName
     */
    public void createPlaybackFile(final String fileName) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                eventResult = new EventResult();
                eventResult.setRequestUrl(RECORD_CREATE_CHANNEL);
                isWait = true;
                if (playBackStreamChannel < 0) {
                    eventResult.setResponseCode(SEARCH_DATA_FAILURE);
                    EventBus.getDefault().post(eventResult);
                } else {
                    if (sendPlayFile(fileName)) {
                        if (status) {

                        } else {
                            eventResult.setResponseCode(SEARCH_DATA_SUCCESS);
                            eventResult.setObject(playBackStreamChannel + "");
                            EventBus.getDefault().post(eventResult);
                        }
                    } else {
                        eventResult.setResponseCode(SEARCH_DATA_FAILURE);
                        EventBus.getDefault().post(eventResult);
                    }
                }
            }
        }).start();
    }

    /**
     * play pause
     */
    public void playPause() {
        if (!status)
            return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                int ret = av.avSendIOCtrl(avIndex, IOTTYPE_USER_IPCAM_RECORD_PLAY_PAUSE,
                        new byte[8], 8);
                // Log.d(BaseApplication.TAG,"IOTTYPE_USER_IPCAM_RECORD_PLAY_PAUSE ret = "+ret);
            }
        }).start();
    }

    /**
     * play resume
     */
    public void playResume() {
        if (!status)
            return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                int ret = av.avSendIOCtrl(avIndex, IOTYPE_USER_IPCAM_RECORD_PLAY_NEXT,
                        new byte[8], 8);
                // Log.d(BaseApplication.TAG,"IOTYPE_USER_IPCAM_RECORD_PLAY_NEXT ret = "+ret);
            }
        }).start();
    }

    /**
     * seekTo
     * 
     * @param time
     */
    public void seekTo(final String time) {
        // Log.d(BaseApplication.TAG,"search date is "+time);
        new Thread(new Runnable() {
            @Override
            public void run() {
                int ret = av.avSendIOCtrl(avIndex, IOTYPE_USER_VIDEO_PLAY_TIME_REQ,
                        time.getBytes(), time.getBytes().length);
                // Log.d(BaseApplication.TAG," IOTYPE_USER_VIDEO_PLAY_TIME_REQ result is "+ret);
            }
        }).start();
    }

    private void stopVideoClient() {
        status = false;
        audioOpen = false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                int ret = av.avSendIOCtrl(avIndex, IOTTYPE_USER_IPCAM_RECORD_PLAY_PAUSE,
                        new byte[8], 8);
                // Log.d(BaseApplication.TAG,"IOTTYPE_USER_IPCAM_RECORD_PLAY_PAUSE ret = "+ret);
            }
        }).start();
        if (decodeType == NativeMediaPlayer.SOFTDECODE) {
            NativeMediaPlayer.JniCloseVideoPlay(deviceBean.playerId);
        }
    }

    public void stopPlayback(boolean isDestory) {
        eventResult = new EventResult();
        eventResult.setRequestUrl(CLEAR_VIDEO_PLAY);
        isWait = false;
        status = false;
        audioOpen = false;
        if (isDestory) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    AVAPIs.avClientStop(playBackIndex);
                    int ret = av.avSendIOCtrl(avIndex, IOTYPE_USER_IPCAM_RECORD_STOP,
                            new byte[8], 8);
                    // Log.d(BaseApplication.TAG,"IOTYPE_USER_IPCAM_RECORD_STOP ret = "+ret);
                    AVAPIs.avClientStop(avIndex);
                    IOTCAPIs.IOTC_Session_Close(sid);
                }
            }).start();
        }
        EventBus.getDefault().post(eventResult);
    }

    public int getDecodeType() {
        return decodeType;
    }

    public void setDecodeType(int decodeType) {
        this.decodeType = decodeType;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }
}