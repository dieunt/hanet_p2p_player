package com.connect.module.module.bean;

import android.media.MediaCodec;
import android.media.MediaFormat;

import com.connect.module.module.action.PlayerAction;
import com.xc.hdscreen.view.GLPlayView;

import java.io.Serializable;

public class DeviceBean implements Serializable {

    public String deviceUid ;

    public String deviceName ;

    public String devicePwd ;

    public int playerId ;

    public GLPlayView glPlayView ;

    public PlayerAction playerAction ;

    // public MuiltPlayAction muiltPlayAction;

    public MediaCodec mediaCodec ;

    public MediaFormat mediaFormat;

    public boolean isDecode = false;

    public int sid =-1;

    public int avIndex = -1;

    public PlayerViewBean playerViewBeans;

    public int channelId;

    public boolean isOverById=false; //是否执行完IOTC_Connect_ByUID_Parallel

    public int retryTimes = 2;

    public int getRetryTimes() {
        return retryTimes;
    }

    public void setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
    }

//    public MuiltPlayAction getMuiltPlayAction() {
//        return muiltPlayAction;
//    }
//
//    public void setMuiltPlayAction(MuiltPlayAction muiltPlayAction) {
//        this.muiltPlayAction = muiltPlayAction;
//    }

    public boolean isOverById() {
        return isOverById;
    }

    public void setOverById(boolean overById) {
        isOverById = overById;
    }

    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    public PlayerViewBean getPlayerViewBeans() {
        return playerViewBeans;
    }

    public void setPlayerViewBeans(PlayerViewBean playerViewBeans) {
        this.playerViewBeans = playerViewBeans;
    }

    public boolean isDecode() {
        return isDecode;
    }

    public void setDecode(boolean decode) {
        isDecode = decode;
    }

    public MediaFormat getMediaFormat() {
        return mediaFormat;
    }

    public void setMediaFormat(MediaFormat mediaFormat) {
        this.mediaFormat = mediaFormat;
    }

    public MediaCodec getMediaCodec() {
        return mediaCodec;
    }

    public void setMediaCodec(MediaCodec mediaCodec) {
        this.mediaCodec = mediaCodec;
    }

    public PlayerAction getPlayerAction() {
        return playerAction;
    }

    public void setPlayerAction(PlayerAction playerAction) {
        this.playerAction = playerAction;
    }

    public GLPlayView getGlPlayView() {
        return glPlayView;
    }

    public void setGlPlayView(GLPlayView glPlayView) {
        this.glPlayView = glPlayView;
    }

    public int getPlayerId() {
        return playerId;
    }

    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }

    public String getDeviceUid() {
        return deviceUid;
    }

    public void setDeviceUid(String deviceUid) {
        this.deviceUid = deviceUid;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDevicePwd() {
        return devicePwd;
    }

    public void setDevicePwd(String devicePwd) {
        this.devicePwd = devicePwd;
    }

    public int getSid() {
        return sid;
    }

    public void setSid(int sid) {
        this.sid = sid;
    }

    public int getAvIndex() {
        return avIndex;
    }

    public void setAvIndex(int avIndex) {
        this.avIndex = avIndex;
    }
}
