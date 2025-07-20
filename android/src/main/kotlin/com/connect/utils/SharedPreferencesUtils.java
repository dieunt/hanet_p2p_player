package com.connect.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesUtils {
    static SharedPreferencesUtils sharedPreferencesUtils;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor ;
    public static  SharedPreferencesUtils getInstance(){
        if(sharedPreferencesUtils==null){
            sharedPreferencesUtils = new SharedPreferencesUtils();
        }
        return  sharedPreferencesUtils;
    }

    public void init(Context context){
        //步骤1：创建一个SharedPreferences对象
        sharedPreferences= context.getSharedPreferences("data", Context.MODE_PRIVATE);
        //步骤2： 实例化SharedPreferences.Editor对象
        editor = sharedPreferences.edit();
    }

    public void saveDeviceId(String deviceId,String devicePwd){
        editor.putString("deviceId", deviceId);
        editor.putString("devicePwd", devicePwd);
        editor.commit();
    }
    public String getDeviceId(){
        String deviceId=sharedPreferences.getString("deviceId","AMTA9TFH6BZF5V8Z111A");//L7W9RSVGGV9LAWTJ111A VLAHVXAUCH9SHPER111A
        return deviceId;
    }
    public String getDevicePwd(){
        String devicePwd=sharedPreferences.getString("devicePwd","xoXA6b");//888888  a7293607
        return devicePwd;
    }
}
