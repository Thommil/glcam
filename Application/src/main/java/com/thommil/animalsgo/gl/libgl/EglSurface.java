package com.thommil.animalsgo.gl.libgl;

import android.opengl.EGL14;
import android.opengl.EGLSurface;
import android.view.Surface;

public class EglSurface {
    private static final String TAG = "A_GO/EglSurface";

    protected EglCore mEglCore;

    private EGLSurface mEGLSurface = EGL14.EGL_NO_SURFACE;
    private int mWidth = -1;
    private int mHeight = -1;

    private Surface mSurface;
    private boolean mReleaseSurface;

    public EglSurface(EglCore eglCore, Surface surface, boolean releaseSurface) {
        mEglCore = eglCore;
        createWindowSurface(surface);
        mSurface = surface;
        mReleaseSurface = releaseSurface;
    }

    public void createWindowSurface(Object surface) {
        if (mEGLSurface != EGL14.EGL_NO_SURFACE) {
            throw new IllegalStateException("surface already created");
        }
        mEGLSurface = mEglCore.createWindowSurface(surface);
    }

    public void createOffscreenSurface(int width, int height) {
        if (mEGLSurface != EGL14.EGL_NO_SURFACE) {
            throw new IllegalStateException("surface already created");
        }
        mEGLSurface = mEglCore.createOffscreenSurface(width, height);
        mWidth = width;
        mHeight = height;
    }

    public int getWidth() {
        if (mWidth < 0) {
            return mEglCore.querySurface(mEGLSurface, EGL14.EGL_WIDTH);
        } else {
            return mWidth;
        }
    }

    public int getHeight() {
        if (mHeight < 0) {
            return mEglCore.querySurface(mEGLSurface, EGL14.EGL_HEIGHT);
        } else {
            return mHeight;
        }
    }

    public void releaseEglSurface() {
        mEglCore.releaseSurface(mEGLSurface);
        mEGLSurface = EGL14.EGL_NO_SURFACE;
        mWidth = mHeight = -1;
    }

    public void makeCurrent() {
        mEglCore.makeCurrent(mEGLSurface);
    }

    public boolean swapBuffers() {
        return mEglCore.swapBuffers(mEGLSurface);
    }

    public void release() {
        releaseEglSurface();
        if (mSurface != null) {
            if (mReleaseSurface) {
                mSurface.release();
            }
            mSurface = null;
        }
    }
}
