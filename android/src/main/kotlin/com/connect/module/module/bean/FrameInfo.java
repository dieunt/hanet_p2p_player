package com.connect.module.module.bean;

public class FrameInfo {

    public short codec_id;

    public char flags;

    public char cam_index;

    public char onlineNum;

    public char reserve1;

    public int reserve2;

    public int timestamp;

    public short getCodec_id() {
        return codec_id;
    }

    public void setCodec_id(short codec_id) {
        this.codec_id = codec_id;
    }

    public char getFlags() {
        return flags;
    }

    public void setFlags(char flags) {
        this.flags = flags;
    }

    public char getCam_index() {
        return cam_index;
    }

    public void setCam_index(char cam_index) {
        this.cam_index = cam_index;
    }

    public char getOnlineNum() {
        return onlineNum;
    }

    public void setOnlineNum(char onlineNum) {
        this.onlineNum = onlineNum;
    }

    public char getReserve1() {
        return reserve1;
    }

    public void setReserve1(char reserve1) {
        this.reserve1 = reserve1;
    }

    public int getReserve2() {
        return reserve2;
    }

    public void setReserve2(int reserve2) {
        this.reserve2 = reserve2;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }
}
