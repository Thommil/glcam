package com.thommil.animalsgo.gl.libgl;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.util.Log;
import android.view.Surface;


public final class EglCore {


    private static final String TAG = "A_GO/EglCore";

    public static final int GLES_VERSION_2 = 2;
    public static final int GLES_VERSION_3 = 3;

    private EGLDisplay mEGLDisplay = EGL14.EGL_NO_DISPLAY;
    private EGLContext mEGLContext = EGL14.EGL_NO_CONTEXT;
    private EGLConfig mEGLConfig = null;
    private int mGlVersion = -1;


    public EglCore() {
        this(null, null);
    }

    public EglCore(EGLContext sharedContext, EGLConfigChooser eglConfigChooser) {
        if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("EGL already set up");
        }

        if (sharedContext == null) {
            sharedContext = EGL14.EGL_NO_CONTEXT;
        }

        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("unable to get EGL14 display");
        }
        int[] version = new int[2];
        if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            mEGLDisplay = null;
            throw new RuntimeException("unable to allocate EGL14");
        }

        if(eglConfigChooser == null){
            eglConfigChooser = new RGBA888EGLConfigChooser(true);
        }

        eglConfigChooser.setVersion(GLES_VERSION_3);
        EGLConfig config = eglConfigChooser.chooseConfig(mEGLDisplay);
        if (config != null) {
            int[] attrib3_list = {
                    EGL14.EGL_CONTEXT_CLIENT_VERSION, GLES_VERSION_3,
                    EGL14.EGL_NONE
            };
            EGLContext context = EGL14.eglCreateContext(mEGLDisplay, config, sharedContext,
                    attrib3_list, 0);

            if (EGL14.eglGetError() == EGL14.EGL_SUCCESS) {
                mEGLConfig = config;
                mEGLContext = context;
                mGlVersion = GLES_VERSION_3;
            }
        }

        if (mEGLContext == EGL14.EGL_NO_CONTEXT) {
            eglConfigChooser.setVersion(GLES_VERSION_2);
            config = eglConfigChooser.chooseConfig(mEGLDisplay);
            if (config == null) {
                throw new RuntimeException("Unable to find a suitable EGLConfig");
            }
            int[] attrib2_list = {
                    EGL14.EGL_CONTEXT_CLIENT_VERSION, GLES_VERSION_2,
                    EGL14.EGL_NONE
            };
            EGLContext context = EGL14.eglCreateContext(mEGLDisplay, config, sharedContext,
                    attrib2_list, 0);
            GlOperation.checkGlError(TAG, "eglCreateContext");
            mEGLConfig = config;
            mEGLContext = context;
            mGlVersion = GLES_VERSION_2;
        }

        int[] values = new int[1];
        EGL14.eglQueryContext(mEGLDisplay, mEGLContext, EGL14.EGL_CONTEXT_CLIENT_VERSION,
                values, 0);
    }

    public void release() {
        if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_CONTEXT);
            EGL14.eglDestroyContext(mEGLDisplay, mEGLContext);
            EGL14.eglReleaseThread();
            EGL14.eglTerminate(mEGLDisplay);
        }

        mEGLDisplay = EGL14.EGL_NO_DISPLAY;
        mEGLContext = EGL14.EGL_NO_CONTEXT;
        mEGLConfig = null;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
                release();
            }
        } finally {
            super.finalize();
        }
    }

    public void releaseSurface(EGLSurface eglSurface) {
        EGL14.eglDestroySurface(mEGLDisplay, eglSurface);
    }

    public EGLSurface createWindowSurface(Object surface) {
        if (!(surface instanceof Surface) && !(surface instanceof SurfaceTexture)) {
            throw new RuntimeException("invalid surface: " + surface);
        }

        int[] surfaceAttribs = {
                EGL14.EGL_NONE
        };
        EGLSurface eglSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, mEGLConfig, surface,
                surfaceAttribs, 0);
        GlOperation.checkGlError(TAG, "eglCreateWindowSurface");
        if (eglSurface == null) {
            throw new RuntimeException("surface was null");
        }
        return eglSurface;
    }

    public EGLSurface createOffscreenSurface(int width, int height) {
        int[] surfaceAttribs = {
                EGL14.EGL_WIDTH, width,
                EGL14.EGL_HEIGHT, height,
                EGL14.EGL_NONE
        };
        EGLSurface eglSurface = EGL14.eglCreatePbufferSurface(mEGLDisplay, mEGLConfig,
                surfaceAttribs, 0);
        GlOperation.checkGlError(TAG, "eglCreatePbufferSurface");
        if (eglSurface == null) {
            throw new RuntimeException("surface was null");
        }
        return eglSurface;
    }

    public void makeCurrent(EGLSurface eglSurface) {
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            // called makeCurrent() before allocate?
            //Log.d(TAG, "NOTE: makeCurrent w/o display");
        }
        if (!EGL14.eglMakeCurrent(mEGLDisplay, eglSurface, eglSurface, mEGLContext)) {
            throw new RuntimeException("eglMakeCurrent failed");
        }
    }

    public void makeCurrent(EGLSurface drawSurface, EGLSurface readSurface) {
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            Log.e(TAG, "NOTE: makeCurrent w/o display");
        }
        if (!EGL14.eglMakeCurrent(mEGLDisplay, drawSurface, readSurface, mEGLContext)) {
            throw new RuntimeException("eglMakeCurrent(draw,read) failed");
        }
    }

    public void makeNothingCurrent() {
        if (!EGL14.eglMakeCurrent(mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT)) {
            throw new RuntimeException("eglMakeCurrent failed");
        }
    }

    public boolean swapBuffers(EGLSurface eglSurface) {
        return EGL14.eglSwapBuffers(mEGLDisplay, eglSurface);
    }

    public boolean isCurrent(EGLSurface eglSurface) {
        return mEGLContext.equals(EGL14.eglGetCurrentContext()) &&
            eglSurface.equals(EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW));
    }

    public int querySurface(EGLSurface eglSurface, int what) {
        int[] value = new int[1];
        EGL14.eglQuerySurface(mEGLDisplay, eglSurface, what, value, 0);
        return value[0];
    }

    public interface EGLConfigChooser {
        EGLConfig chooseConfig(final EGLDisplay display);
        void setVersion(final int version);
    }

    public static abstract class BaseConfigChooser implements EGLConfigChooser {

        private int mVersion = GLES_VERSION_3;
        protected int[] mConfigSpec;

        public BaseConfigChooser(int[] configSpec) {
            mConfigSpec = filterConfigSpec(configSpec);
        }

        @Override
        public EGLConfig chooseConfig(EGLDisplay display) {
            int[] num_config = new int[1];
            if (EGL14.eglChooseConfig(display, mConfigSpec, 0,  null, 0, 0, num_config, 0)) {
                int numConfigs = num_config[0];
                if (numConfigs > 0) {
                    EGLConfig[] configs = new EGLConfig[numConfigs];
                    if (EGL14.eglChooseConfig(display, mConfigSpec, 0,  configs, 0, numConfigs, num_config, 0)) {
                        return chooseConfig(display, configs);
                    }
                }
            }
            return null;
        }

        abstract EGLConfig chooseConfig(EGLDisplay display, EGLConfig[] configs);

        @Override
        public void setVersion(final int version){
            mVersion = version;

            int renderableType = EGL14.EGL_OPENGL_ES2_BIT;
            if (mVersion >= GLES_VERSION_3) {
                renderableType |= EGLExt.EGL_OPENGL_ES3_BIT_KHR;
            }
            mConfigSpec[mConfigSpec.length - 2] = renderableType;
        }

        private int[] filterConfigSpec(int[] configSpec) {
            int renderableType = EGL14.EGL_OPENGL_ES2_BIT;
            if (mVersion >= GLES_VERSION_3) {
                renderableType |= EGLExt.EGL_OPENGL_ES3_BIT_KHR;
            }

            int len = configSpec.length;
            int[] newConfigSpec = new int[len + 2];
            System.arraycopy(configSpec, 0, newConfigSpec, 0, len-1);
            newConfigSpec[len-1] = EGL14.EGL_RENDERABLE_TYPE;
            newConfigSpec[len] = renderableType;
            newConfigSpec[len+1] = EGL14.EGL_NONE;
            return newConfigSpec;
        }
    }

    public static class ComponentSizeChooser extends BaseConfigChooser {
        public ComponentSizeChooser(int redSize, int greenSize, int blueSize,
                                    int alphaSize, int depthSize, int stencilSize) {
            super(new int[] {
                    EGL14.EGL_RED_SIZE, redSize,
                    EGL14.EGL_GREEN_SIZE, greenSize,
                    EGL14.EGL_BLUE_SIZE, blueSize,
                    EGL14.EGL_ALPHA_SIZE, alphaSize,
                    EGL14.EGL_DEPTH_SIZE, depthSize,
                    EGL14.EGL_STENCIL_SIZE, stencilSize,
                    EGL14.EGL_NONE});
            mValue = new int[1];
            mRedSize = redSize;
            mGreenSize = greenSize;
            mBlueSize = blueSize;
            mAlphaSize = alphaSize;
            mDepthSize = depthSize;
            mStencilSize = stencilSize;
        }

        @Override
        public EGLConfig chooseConfig(EGLDisplay display, EGLConfig[] configs) {
            for (EGLConfig config : configs) {
                int d = findConfigAttrib(display, config,
                        EGL14.EGL_DEPTH_SIZE, 0);
                int s = findConfigAttrib(display, config,
                        EGL14.EGL_STENCIL_SIZE, 0);
                if ((d >= mDepthSize) && (s >= mStencilSize)) {
                    int r = findConfigAttrib(display, config,
                            EGL14.EGL_RED_SIZE, 0);
                    int g = findConfigAttrib(display, config,
                            EGL14.EGL_GREEN_SIZE, 0);
                    int b = findConfigAttrib(display, config,
                            EGL14.EGL_BLUE_SIZE, 0);
                    int a = findConfigAttrib(display, config,
                            EGL14.EGL_ALPHA_SIZE, 0);
                    if ((r == mRedSize) && (g == mGreenSize)
                            && (b == mBlueSize) && (a == mAlphaSize)) {
                        return config;
                    }
                }
            }
            return null;
        }

        private int findConfigAttrib(EGLDisplay display,
                                     EGLConfig config, int attribute, int defaultValue) {


            if (EGL14.eglGetConfigAttrib(display, config, attribute, mValue, 0)) {
                return mValue[0];
            }
            return defaultValue;
        }

        private int[] mValue;
        // Subclasses can adjust these values:
        protected int mRedSize;
        protected int mGreenSize;
        protected int mBlueSize;
        protected int mAlphaSize;
        protected int mDepthSize;
        protected int mStencilSize;
    }

    public static class RGB565EGLConfigChooser extends ComponentSizeChooser {
        public RGB565EGLConfigChooser(boolean withDepthBuffer) {
            super(5, 6, 5, 0, withDepthBuffer ? 16 : 0, 0);
        }
    }

    public static class RGB888EGLConfigChooser extends ComponentSizeChooser {
        public RGB888EGLConfigChooser(boolean withDepthBuffer) {
            super(8, 8, 8, 0, withDepthBuffer ? 16 : 0, 0);
        }
    }

    public static class RGBA888EGLConfigChooser extends ComponentSizeChooser {
        public RGBA888EGLConfigChooser(boolean withDepthBuffer) {
            super(8, 8, 8, 8, withDepthBuffer ? 16 : 0, 0);
        }
    }
}
