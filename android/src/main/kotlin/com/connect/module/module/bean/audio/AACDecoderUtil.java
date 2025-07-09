package com.connect.module.module.bean.audio;

import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by dll on 2017/5/17.
 * 用于aac音频解码
 */

public class AACDecoderUtil {
    private static final String TAG = "AACDecoderUtil";
    // 声道数
    // private static final int KEY_CHANNEL_COUNT = 2;
    // 采样率
    // private static final int KEY_SAMPLE_RATE = 16000;
    // 用于播放解码后的pcm
    private MyAudioTrack mPlayer;
    // 解码器
    private MediaCodec mDecoder;

    private int encodeType = 1;
    private int audioChannel = 2; // 1 单通道 2 双通道

    /**
     * 初始化所有变量
     */
    public void start(int sampleRate, int encodeType, int audioChannel) {
        this.encodeType = encodeType;
        this.audioChannel = audioChannel;
        prepare(sampleRate);
    }

    /**
     * 初始化解码器
     * 
     * @return 初始化失败返回false，成功返回true
     */
    public boolean prepare(int sampleRate) {
        // 初始化AudioTrack CHANNEL_OUT_MONO(单)
        if (audioChannel == 2) {
            mPlayer = new MyAudioTrack(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        } else {
            mPlayer = new MyAudioTrack(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        }
        mPlayer.init();
        try {
            // 需要解码数据的类型
            String mine = "audio/mp4a-latm";// mp4a-latm
            // 初始化解码器
            mDecoder = MediaCodec.createDecoderByType(mine);
            // MediaFormat用于描述音视频数据的相关参数
            MediaFormat mediaFormat = new MediaFormat();
            // 数据类型
            mediaFormat.setString(MediaFormat.KEY_MIME, mine);
            // 声道个数
            mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, audioChannel);
            // 采样率
            mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate);
            // 比特率
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000);
            // 用来标记AAC是否有adts头，1->有
            mediaFormat.setInteger(MediaFormat.KEY_IS_ADTS, 1);
            // 用来标记aac的类型
            mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            // ByteBuffer key（但必须设置）
            byte[] data = null;
            if (sampleRate == 48000) {
                if (audioChannel == 2) {
                    data = new byte[] { (byte) 0x11, (byte) 0x90 }; // 48k 双声道
                } else {
                    data = new byte[] { (byte) 0x11, (byte) 0x88 }; // 48k 单声道
                }
            } else {
                if (audioChannel == 2) {
                    data = new byte[] { (byte) 0x14, (byte) 0x10 }; // 16k 双声道
                } else {
                    data = new byte[] { (byte) 0x14, (byte) 0x08 }; // 16k 单声道
                }
            }
            ByteBuffer csd_0 = ByteBuffer.wrap(data);
            mediaFormat.setByteBuffer("csd-0", csd_0);
            // 解码器配置
            mDecoder.configure(mediaFormat, null, null, 0);
            Log.d("RECORD_AUDIO", "MediaCodec preper....success");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        if (mDecoder == null) {
            return false;
        }
        mDecoder.start();
        return true;
    }

    /**
     * aac解码+播放
     */
    public void decode(byte[] buf, int offset, int length) {

        if (encodeType == 2) { // PCM
            mPlayer.playAudioTrack(buf, 0, length);
            return;
        }
        // 输入ByteBuffer
        ByteBuffer[] codecInputBuffers = mDecoder.getInputBuffers();
        // 输出ByteBuffer
        ByteBuffer[] codecOutputBuffers = mDecoder.getOutputBuffers();
        // 等待时间，0->不等待，-1->一直等待
        long kTimeOutUs = 0;
        try {
            // 返回一个包含有效数据的input buffer的index,-1->不存在
            int inputBufIndex = mDecoder.dequeueInputBuffer(kTimeOutUs);
            if (inputBufIndex >= 0) {
                // 获取当前的ByteBuffer
                ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
                // 清空ByteBuffer
                dstBuf.clear();
                // 填充数据
                dstBuf.put(buf, offset, length);
                // 将指定index的input buffer提交给解码器
                mDecoder.queueInputBuffer(inputBufIndex, 0, length, 0, 0);
            }
            // 编解码器缓冲区
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            // 返回一个output buffer的index，-1->不存在
            int outputBufferIndex = mDecoder.dequeueOutputBuffer(info, kTimeOutUs);

            ByteBuffer outputBuffer;
            // System.out.printf("[TUTK]start_ipcam_audio decode stream
            // outputBufferIndex[%d]\n",outputBufferIndex);

            while (outputBufferIndex >= 0) {
                // 获取解码后的ByteBuffer
                outputBuffer = codecOutputBuffers[outputBufferIndex];
                // 用来保存解码后的数据
                byte[] outData = new byte[info.size];
                outputBuffer.get(outData);
                // 清空缓存
                outputBuffer.clear();
                // 播放解码后的数据
                // System.out.printf("[TUTK]start_ipcam_audio playAudioTrack stream
                // info.size[%d]\n",info.size);
                mPlayer.playAudioTrack(outData, 0, info.size);
                // 释放已经解码的buffer
                mDecoder.releaseOutputBuffer(outputBufferIndex, false);
                // 解码未解完的数据
                outputBufferIndex = mDecoder.dequeueOutputBuffer(info, kTimeOutUs);
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    /**
     * 释放资源
     */
    public void stop() {
        try {
            if (mPlayer != null) {
                mPlayer.release();
                mPlayer = null;
            }
            if (mDecoder != null) {
                mDecoder.stop();
                mDecoder.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
