package com.connect.module.module.speaker;

import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class MyAudioTrack {
    private int mFrequency;// 采样率
    private int mChannel;// 声道
    private int mSampBit;// 采样精度
    private AudioTrack mAudioTrack;

    public MyAudioTrack(int frequency, int channel, int sampbit) {
        mFrequency = frequency;
        mChannel = channel;
        mSampBit = sampbit;
    }

    public void init() {
        if (mAudioTrack != null) {
            release();
        }
        // 获得构建对象的最小缓冲区大小
        int minBufSize = getMinBufferSize();
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                mFrequency, mChannel, mSampBit, minBufSize, AudioTrack.MODE_STREAM);
        mAudioTrack.play();
    }

    public void release() {
        if (mAudioTrack != null) {
            mAudioTrack.stop();
            mAudioTrack.release();
        }
    }

    public void playAudioTrack(byte[] data, int offset, int length) {
        if (data == null || data.length == 0) {
            return;
        }
        try {
            mAudioTrack.write(data, offset, length);
        } catch (Exception e) {
            Log.e("MyAudioTrack", "AudioTrack Exception : " + e.toString());
            e.printStackTrace();
        }
    }

    public int getMinBufferSize() {
        return AudioTrack.getMinBufferSize(mFrequency,
                mChannel, mSampBit);
    }
}