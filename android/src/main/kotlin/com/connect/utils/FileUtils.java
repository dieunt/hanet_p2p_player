package com.connect.utils;

import android.content.Context;
import android.os.Environment;
import java.io.File;

public class FileUtils {
    private static String SDPATH = "";

    /**
     * 获取到sd卡的根目录，并以String形式返回
     *
     * @return
     */
    public static String getSDCardPath() {
        SDPATH = Environment.getExternalStorageDirectory() + "/video";
        return SDPATH;
    }

    public static String getAndrod12SDCardPath() {
//        SDPATH = BaseApplication.getInstance().getExternalFilesDir(null).getAbsolutePath()+ File.separator+"log.txt";
//        return SDPATH;
        return  "";
    }

    /**
     * 创建文件或文件夹
     *
     * @param fileName
     *            文件名或问文件夹名
     */
    public static  File createFile(Context context, String fileName) {
        //String local_file = Environment.getExternalStorageDirectory().getAbsolutePath()+"/video/";
        //File f = new File(local_file);
        String local_file = context.getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+File.separator;
        File f = new File(local_file);
        if(!f.exists()){
            f.mkdirs();
        }
        String localFile = f.getAbsolutePath()+File.separator+fileName;
        File file = new File(localFile);
        try {
            if(file.exists()) {
                file.delete();
                file.createNewFile();
                System.out.println("File create already exists");
            }else{
                file.createNewFile();
            }
        } catch (Exception ex) {
            System.out.println("File create error");
            System.out.println(ex);
        }
        System.out.println("File create success file=》"+file.getAbsolutePath());
        return file;
    }
}