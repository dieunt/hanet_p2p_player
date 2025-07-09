package com.connect.module.module.utils;

public class Log {
    private static final String TAG = "Tutk";
    private static final boolean isLog = true;

    public static void e(String s) {
        if (isLog)
            android.util.Log.e(TAG, s);
    }
}
