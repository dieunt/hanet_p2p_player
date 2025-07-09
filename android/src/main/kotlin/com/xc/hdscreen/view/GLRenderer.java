package com.xc.hdscreen.view;

import android.annotation.SuppressLint;
import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

@SuppressLint("ParserError")
public class GLRenderer implements GLSurfaceView.Renderer{

	private int x, y;
	private int width;
	private int height;
	private int winId;
	private String deviceId;
	//private Handler handler;


	private native int nativeInit(int winId);

	private native static int nativeSetup(int winId, int x, int y, int w, int h);

	private native void nativeDrawFrame(int playerId);

	@Override
	public void onDrawFrame(GL10 gl) {
		nativeDrawFrame(winId);
	}
	
	public void onSurfaceChanged(GL10 arg0, int w, int h) {
		nativeSetup(winId, 0, 0, w, h);
	}

	public void onSurfaceCreated(GL10 gl, EGLConfig arg1) {
		if(winId > 0){
		   nativeInit(winId);
		}
	}

	@SuppressWarnings("static-access")
	public GLRenderer(int winId, int weight, int height, String deviceId) {
		this.winId = winId;
		this.width = weight;
		this.height = height;
		this.deviceId = deviceId;
	}

	public void renderVideo(int win,int x,int y, int weight, int heigh) {
		width=weight;
		height=heigh;
		nativeSetup(win, x, y, weight, heigh);
	}
}
