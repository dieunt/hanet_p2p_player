package com.connect.module.module.utils;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

public class MyScaleGestures implements View.OnTouchListener, ScaleGestureDetector.OnScaleGestureListener {
    private final ScaleGestureDetector gestureScale;

    private static final float MIN_ZOOM = 1.0f;
    private static final float MAX_ZOOM = 3.0f;

    private Mode mode = Mode.NONE;

    private enum Mode {
        NONE,
        DRAG,
        ZOOM
    }

    private float _scale = 0f;
    private float scale = 1.0f;
    private float lastScaleFactor = 0f;

    private float startX = 0f;
    private float startY = 0f;
    private float dx = 0f;
    private float dy = 0f;
    private float prevDx = 0f;
    private float prevDy = 0f;
    private float oldDistance;
    private boolean isZoomIn;

    public MyScaleGestures(Context c) {
        gestureScale = new ScaleGestureDetector(c, this);
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                if (scale > MIN_ZOOM) {
                    mode = Mode.DRAG;
                    startX = event.getX() - prevDx;
                    startY = event.getY() - prevDy;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == Mode.DRAG) {
                    dx = event.getX() - startX;
                    dy = event.getY() - startY;
                } else if (mode == Mode.ZOOM) {
                    float newDistance = spacing(event);
                    if (newDistance > 5f) {
                        float s = newDistance / oldDistance;
                        if (s > 1) {
                            isZoomIn = true;
                        } else {
                            isZoomIn = false;
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                oldDistance = spacing(event);
                if (oldDistance > 5f)
                    mode = Mode.ZOOM;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                mode = Mode.DRAG;
                break;
            case MotionEvent.ACTION_UP:
                mode = Mode.NONE;
                prevDx = dx;
                prevDy = dy;
                break;
        }
        gestureScale.onTouchEvent(event);

        if ((mode == Mode.DRAG && scale >= MIN_ZOOM)) {
            float maxDx = (view.getWidth() - (view.getWidth() / scale)) / 2 * scale;
            float maxDy = (view.getHeight() - (view.getHeight() / scale)) / 2 * scale;
            dx = Math.min(Math.max(dx, -maxDx), maxDx);
            dy = Math.min(Math.max(dy, -maxDy), maxDy);
            view.setTranslationX(dx);
            view.setTranslationY(dy);
        } else if (mode == Mode.ZOOM && scale >= MIN_ZOOM) {
            if (isZoomIn) {
                if (scale > _scale) {
                    _scale = scale;
                    view.setScaleX(scale);
                    view.setScaleY(scale);
                }
            } else {
                _scale = 0;
                view.setScaleX(scale);
                view.setScaleY(scale);
            }
        }
        return true;
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float scaleFactor = detector.getScaleFactor();
        if (lastScaleFactor == 0 || (Math.signum(scaleFactor) == Math.signum(lastScaleFactor))) {
            scale *= scaleFactor;
            scale = Math.max(MIN_ZOOM, Math.min(scale, MAX_ZOOM));
            lastScaleFactor = scaleFactor;
        } else {
            lastScaleFactor = 0;
        }
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
    }
}