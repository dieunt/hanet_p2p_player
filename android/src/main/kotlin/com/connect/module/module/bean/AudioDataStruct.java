package com.connect.module.module.bean;

public class AudioDataStruct {
    public byte[] audioBuffer;

    public int length;

    public AudioDataStruct(byte[] data, int length) {
        this.audioBuffer = data;
        this.length = length;
    }

}
