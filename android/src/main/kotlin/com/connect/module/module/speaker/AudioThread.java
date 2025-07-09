package com.connect.module.module.speaker;


import com.tutk.IOTC.AVAPIs;

public class AudioThread implements Runnable {
    private static final String TAG = "AudioThread";
    static final int AUDIO_BUF_SIZE = 1024;
    static final int FRAME_INFO_SIZE = 16;
    private final AVAPIs av;
    private final int avIndex;
    public boolean isPause = false;
    public boolean isMute = false;
    public boolean isStop = false;
    private boolean isRunning = true;
    public boolean isRecord = false;

    private final int cameraType;

    public AudioThread(AVAPIs av, int avIndex, int cameraType) {
        this.av = av;
        this.cameraType = cameraType;
        this.avIndex = avIndex;
    }

    @Override
    public void run() {

        AACDecoderUtil aacDecoderUtil = new AACDecoderUtil();
        aacDecoderUtil.start(cameraType);

        byte[] frameInfo = new byte[FRAME_INFO_SIZE];
        byte[] audioBuffer = new byte[AUDIO_BUF_SIZE];
        while (isRunning) {
            int ret = av.avCheckAudioBuf(avIndex);
            if (ret < 0) {
                // Same error codes as below
                System.out.printf("[%s] avCheckAudioBuf() failed: %d\n",
                        Thread.currentThread().getName(), ret);
                break;
            } else if (ret < 3) {
                try {
                    Thread.sleep(120);
                    continue;
                } catch (InterruptedException e) {
                    System.out.println(e.getMessage());
                    break;
                }
            }
            int[] frameNumber = new int[1];
            ret = av.avRecvAudioData(avIndex, audioBuffer, AUDIO_BUF_SIZE, frameInfo, FRAME_INFO_SIZE, frameNumber);
            // Log.e(TAG, "ret: - " + ret);
            if (ret > 0) {
                if (isPause || isMute)
                    continue;
                // System.out.printf("[TUTK]->[audio recv]recv size = %d \n",ret);
                if (!isStop && !isRecord) {
                    aacDecoderUtil.decode(audioBuffer, 0, ret);
                }

            } else if (ret < 0) {
                if (ret == AVAPIs.AV_ER_SESSION_CLOSE_BY_REMOTE) {
                    System.out.printf("[%s] AV_ER_SESSION_CLOSE_BY_REMOTE\n",
                            Thread.currentThread().getName());
                    break;
                } else if (ret == AVAPIs.AV_ER_REMOTE_TIMEOUT_DISCONNECT) {
                    System.out.printf("[%s] AV_ER_REMOTE_TIMEOUT_DISCONNECT\n",
                            Thread.currentThread().getName());
                    break;
                } else if (ret == AVAPIs.AV_ER_INVALID_SID) {
                    System.out.printf("[%s] Session cant be used anymore\n",
                            Thread.currentThread().getName());
                    break;
                } else if (ret == AVAPIs.AV_ER_LOSED_THIS_FRAME) {
                    // System.out.printf("[%s] Audio frame losed\n",
                    // Thread.currentThread().getName());
                    continue;
                }
            }
        }
        aacDecoderUtil.stop();

    }

    public void stop() {
        isRunning = false;
    }

    public void mute(boolean bol) {
        isRecord = bol;
    }
}
