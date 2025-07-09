package com.connect.module.module.bean;

import android.media.MediaCodec;
import android.media.MediaFormat;

import com.connect.module.module.action.PlayerAction;
import com.connect.module.module.action.PlayerActions;
import com.xc.hdscreen.view.GLPlayView;

public class DeviceBean {

    public String deviceUid;

    public String deviceName;

    public String devicePwd;

    public int playerId;

    public GLPlayView glPlayView;

    public PlayerAction playerAction;

    public PlayerActions playerActions;

    public MediaCodec mediaCodec;

    public MediaFormat mediaFormat;

    public boolean isDecode = false;

    public PlayerViewBean playerViewBeans;

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

    public PlayerActions getPlayerActions() {
        return playerActions;
    }

    public void setPlayerActions(PlayerActions playerActions) {
        this.playerActions = playerActions;
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
}
