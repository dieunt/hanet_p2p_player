package com.connect.module.module.bean.audio;

import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

/**
 * Created by dll
 * 播放pcm数据
 */
public class MyAudioTrack {
    private int mFrequency;// 采样率
    private int mChannel;// 声道
    private int mSampBit;// 采样精度
    private AudioTrack mAudioTrack;

    public MyAudioTrack(int frequency, int channel, int sampbit) {
        this.mFrequency = frequency;
        this.mChannel = channel;
        this.mSampBit = sampbit;
    }

    /**
     * 初始化
     */
    public void init() {
        if (mAudioTrack != null) {
            release();
        }
        // 获得构建对象的最小缓冲区大小
        int minBufSize = getMinBufferSize();
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                mFrequency, mChannel, mSampBit, minBufSize, AudioTrack.MODE_STREAM);
        mAudioTrack.play();
        Log.d("RECORD_AUDIO", "start AudioTrack....over");
    }

    /**
     * 释放资源
     */
    public void release() {
        if (mAudioTrack != null) {
            mAudioTrack.stop();
            mAudioTrack.release();
        }
    }

    /**
     * 将解码后的pcm数据写入audioTrack播放
     *
     * @param data   数据
     * @param offset 偏移
     * @param length 需要播放的长度
     */
    public void playAudioTrack(byte[] data, int offset, int length) {
        if (data == null || data.length == 0) {
            return;
        }
        try {
            // System.out.printf("[TUTK]start_ipcam_audio playAudioTrack
            // info.size[%d]\n",11111);
            int ret = mAudioTrack.write(data, offset, length);
            System.out.printf("[TUTK]start_ipcam_audio playAudioTrack playResult[%d]\n", ret);
        } catch (Exception e) {
            Log.e("MyAudioTrack", "AudioTrack Exception : " + e.toString());
        }
    }

    public int getMinBufferSize() {
        return AudioTrack.getMinBufferSize(mFrequency,
                mChannel, mSampBit);
    }
}
