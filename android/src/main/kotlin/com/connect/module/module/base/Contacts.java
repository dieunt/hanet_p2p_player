package com.connect.module.module.base;

public class Contacts {

    public static int IOTYPE_USER_IPCAM_START = 0x01FF;
    public static int IOTYPE_USER_IPCAM_STOP = 0x02FF;
    public static int IOTYPE_USER_IPCAM_AUDIOSTART = 0x0300;
    public static int IOTYPE_USER_IPCAM_AUDIOSTOP = 0x0301;
    public static int IOTYPE_USER_IPCAM_SPEAKERSTART = 0x0350;
    public static int IOTYPE_USER_IPCAM_SPEAKERSTOP = 0x0351;
    public static int IOTYPE_USER_IPCAM_PUSH_STREAMING_START = 0x600;
    public static int IOTYPE_USER_IPCAM_PUSH_STREAMING_STOP = 0x601;

    public static int IOTYPE_USER_IPCAM_PTZ_COMMAND = 0x1001; // P2P PTZ Command Msg

    public static int PTZ_STOP = 0;
    public static int PTZ_ACTION_UP = 1;
    public static int PTZ_ACTION_DOWN = 2;
    public static int PTZ_ACTION_LEFT = 3;
    public static int PTZ_ACTION_LEFT_UP = 4;
    public static int PTZ_ACTION_LEFT_DOWN = 5;
    public static int PTZ_ACTION_RIGHT = 6;
    public static int PTZ_ACTION_RIGHT_UP = 7;
    public static int PTZ_ACTION_RIGHT_DOWN = 8;

}