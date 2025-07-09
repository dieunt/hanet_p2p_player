package com.connect.module.module.speaker;

import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;


import com.xc.p2pVideo.NativeMediaPlayer;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AACDecoderUtil {
    private static final String TAG = "AACDecoderUtil";
    private static int KEY_CHANNEL_COUNT = 2;
    private static int KEY_SAMPLE_RATE = 48000;
    private static int KEY_ENCODE_TYPE = 1; // ACC:1, PCM:2
    private MyAudioTrack mPlayer;
    private MediaCodec mDecoder;
    private int count = 0;

    public void start(int cameraType) {
        prepare(cameraType);
    }

    public boolean prepare(int cameraType) {

        byte[] data = new byte[] { (byte) 0x11, (byte) 0x88 }; // 48k
        int channel_out = AudioFormat.CHANNEL_OUT_STEREO;

        if (cameraType == NativeMediaPlayer.CAM_GO) {
            KEY_CHANNEL_COUNT = 1;
            KEY_SAMPLE_RATE = 16000;
            channel_out = AudioFormat.CHANNEL_OUT_MONO;
            data = new byte[] { (byte) 0x14, (byte) 0x08 }; // 16k
        } else if (cameraType == NativeMediaPlayer.CAM_HOME_INDOOR
                || cameraType == NativeMediaPlayer.CAM_HOME_OUTDOOR) {
            KEY_CHANNEL_COUNT = 2;
            KEY_SAMPLE_RATE = 16000;
            channel_out = AudioFormat.CHANNEL_OUT_STEREO;
            data = new byte[] { (byte) 0x14, (byte) 0x10 }; // 16k
        } else {
            KEY_CHANNEL_COUNT = 2;
            KEY_SAMPLE_RATE = 48000;
            channel_out = AudioFormat.CHANNEL_OUT_STEREO;
            data = new byte[] { (byte) 0x11, (byte) 0x90 }; // 48k
        }

        mPlayer = new MyAudioTrack(KEY_SAMPLE_RATE, channel_out, AudioFormat.ENCODING_PCM_16BIT);
        mPlayer.init();
        try {
            String mine = "audio/mp4a-latm";
            mDecoder = MediaCodec.createDecoderByType(mine);
            MediaFormat mediaFormat = new MediaFormat();
            mediaFormat.setString(MediaFormat.KEY_MIME, mine);
            mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, KEY_CHANNEL_COUNT);
            mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, KEY_SAMPLE_RATE);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000);
            mediaFormat.setInteger(MediaFormat.KEY_IS_ADTS, 1);
            mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            // byte[] data = new byte[]{(byte) 0x11, (byte) 0x90};
            ByteBuffer csd_0 = ByteBuffer.wrap(data);
            mediaFormat.setByteBuffer("csd-0", csd_0);
            mDecoder.configure(mediaFormat, null, null, 0);
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

    public void decode(byte[] buf, int offset, int length) {
        if (mDecoder == null || mPlayer == null)
            return;

        if (KEY_ENCODE_TYPE == 2) {// PCM
            mPlayer.playAudioTrack(buf, 0, length);
            return;
        }

        ByteBuffer[] codecInputBuffers = mDecoder.getInputBuffers();
        ByteBuffer[] codecOutputBuffers = mDecoder.getOutputBuffers();
        long kTimeOutUs = 0;
        try {
            int inputBufIndex = mDecoder.dequeueInputBuffer(kTimeOutUs);
            if (inputBufIndex >= 0) {
                ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
                dstBuf.clear();
                dstBuf.put(buf, offset, length);
                mDecoder.queueInputBuffer(inputBufIndex, 0, length, 0, 0);
            }
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            int outputBufferIndex = mDecoder.dequeueOutputBuffer(info, kTimeOutUs);

            ByteBuffer outputBuffer;
            while (outputBufferIndex >= 0) {
                outputBuffer = codecOutputBuffers[outputBufferIndex];
                byte[] outData = new byte[info.size];
                outputBuffer.get(outData);
                outputBuffer.clear();
                mPlayer.playAudioTrack(outData, 0, info.size);
                mDecoder.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = mDecoder.dequeueOutputBuffer(info, kTimeOutUs);
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    public int getCount() {
        return count;
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
                mDecoder = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
