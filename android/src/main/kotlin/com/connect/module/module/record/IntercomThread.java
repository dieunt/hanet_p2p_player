package com.connect.module.module.record;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.tutk.IOTC.AVAPIs;
import com.xc.p2pVideo.NativeMediaPlayer;


public class IntercomThread implements Runnable {
    private boolean isRunning = true;
    private final AVAPIs av;
    private final int avIndex;
    private final int cameraType;
    private boolean isPCM;

    private int speekChannel = 2;
    private int recordSampleRate = 48000;
    private int channelConfig = AudioFormat.CHANNEL_IN_STEREO;
    private static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

    public IntercomThread(AVAPIs av, int avIndex, int cameraType) {
        this.av = av;
        this.cameraType = cameraType;
        this.avIndex = avIndex;
    }

    @Override
    public void run() {
        try {

            if (cameraType == NativeMediaPlayer.CAM_GO) {
                speekChannel = 1;
                recordSampleRate = 16000;
                channelConfig = AudioFormat.CHANNEL_IN_MONO;
                isPCM = true;
            } else if (cameraType == NativeMediaPlayer.CAM_HOME_INDOOR
                    || cameraType == NativeMediaPlayer.CAM_HOME_OUTDOOR) {
                speekChannel = 1;
                recordSampleRate = 16000;
                channelConfig = AudioFormat.CHANNEL_IN_MONO;
                isPCM = true;
            } else {
                speekChannel = 2;
                recordSampleRate = 48000;
                channelConfig = AudioFormat.CHANNEL_IN_STEREO;
                isPCM = false;
            }

            int bufferSize = AudioRecord.getMinBufferSize(recordSampleRate, channelConfig, audioEncoding);

            AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.MIC, recordSampleRate, channelConfig,
                    audioEncoding, bufferSize);

            record.startRecording();
            AacEncode aacMediaEncode = new AacEncode(recordSampleRate, speekChannel);

            byte[] buffer = new byte[bufferSize];

            final int FRAME_INFO_SIZE = 16;
            byte[] frameInfo = new byte[FRAME_INFO_SIZE];

            while (isRunning) {
                int bufferReadResult = record.read(buffer, 0, bufferSize);
                if (AudioRecord.ERROR_INVALID_OPERATION != bufferReadResult) {
                    if (isPCM) {
                        av.avSendAudioData(avIndex, buffer, bufferSize, frameInfo, FRAME_INFO_SIZE);
                    } else {
                        byte[] ret = aacMediaEncode.offerEncoder(buffer);
                        if (ret.length > 0) {
                            if (ret.length > 1023) {
                                av.avSendAudioData(avIndex, ret, 1023, frameInfo, FRAME_INFO_SIZE);
                            } else {
                                av.avSendAudioData(avIndex, ret, ret.length, frameInfo, FRAME_INFO_SIZE);
                            }
                        }
                    }
                }
            }
            record.stop();
            aacMediaEncode.close();
            if (av != null) {
                av.avSendIOCtrl(avIndex, 0x0351/* IOTYPE_USER_IPCAM_SPEAKERSTOP */, new byte[8], 8);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        isRunning = false;
    }
}