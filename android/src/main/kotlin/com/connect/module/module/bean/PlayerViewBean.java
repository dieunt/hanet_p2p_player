package com.connect.module.module.bean;

import android.view.SurfaceView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.xc.hdscreen.view.GLPlayView;

public class PlayerViewBean {

    private GLPlayView playView;

    public SurfaceView surfaceView;

    public ProgressBar progressBar;

    public LinearLayout linearLayout;

    public GLPlayView getPlayView() {
        return playView;
    }

    public void setPlayView(GLPlayView playView) {
        this.playView = playView;
    }

    public SurfaceView getSurfaceView() {
        return surfaceView;
    }

    public void setSurfaceView(SurfaceView surfaceView) {
        this.surfaceView = surfaceView;
    }

    public ProgressBar getProgressBar() {
        return progressBar;
    }

    public void setProgressBar(ProgressBar progressBar) {
        this.progressBar = progressBar;
    }

    public LinearLayout getLinearLayout() {
        return linearLayout;
    }

    public void setLinearLayout(LinearLayout linearLayout) {
        this.linearLayout = linearLayout;
    }
}
