package io.github.sawameimei.playopengles20;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.github.sawameimei.opengleslib.common.EGLCore;
import io.github.sawameimei.opengleslib.common.GLUtil;
import io.github.sawameimei.opengleslib.common.TextureHelper;
import io.github.sawameimei.opengleslib.glprogram.FBOGroupGLProgram;
import io.github.sawameimei.opengleslib.glprogram.GLProgram;
import io.github.sawameimei.playopengles20.glprogram.CameraPrevGLProgram;
import io.github.sawameimei.playopengles20.glprogram.CameraUtil;
import io.github.sawameimei.playopengles20.glprogram.FilterBeautyGLProgram;
import io.github.sawameimei.playopengles20.glprogram.FilterWriteSkinGLProgram;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class OpenGLES20L8Activity extends AppCompatActivity implements SurfaceTexture.OnFrameAvailableListener, SurfaceHolder.Callback {

    private static String TAG = "OpenGLES2.0";

    private int PREV_WIDTH = 720;
    private int PREV_HEIGHT = 1280;
    private int ENCODER_BIT_RATE = 6000000; // = PREV_WIDTH * PREV_HEIGHT * 7?
    private int PREV_FPS = 24;

    private SurfaceView mSurfaceView1;
    private SurfaceView mSurfaceView2;
    private SurfaceView mSurfaceView3;
    private SurfaceView mSurfaceView4;

    private Camera mCamera;
    private EGLCore mEGLCore = new EGLCore(null, EGLCore.FLAG_RECORDABLE);
    private SurfaceTexture mPrevSurfaceTexture;
    private float[] mTextureM = new float[16];
    private int mTextureId = -1000;
    private List<GLValuePair> mGLValuePair = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_gles20_l8);

        mSurfaceView1 = findViewById(R.id.continuousCapture_surfaceView1);
        mSurfaceView2 = findViewById(R.id.continuousCapture_surfaceView2);
        mSurfaceView3 = findViewById(R.id.continuousCapture_surfaceView3);
        mSurfaceView4 = findViewById(R.id.continuousCapture_surfaceView4);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{RECORD_AUDIO, READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE, INTERNET, CAMERA}, 1);
        }

        mSurfaceView1.getHolder().addCallback(this);
        mSurfaceView2.getHolder().addCallback(this);
        mSurfaceView3.getHolder().addCallback(this);
        mSurfaceView4.getHolder().addCallback(this);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        if (mEGLCore == null) {
            return;
        }
        mPrevSurfaceTexture.updateTexImage();
        mPrevSurfaceTexture.getTransformMatrix(mTextureM);
        for (GLValuePair glValuePair : mGLValuePair) {
            mEGLCore.makeCurrent(glValuePair.getEglSurface());
            GLES20.glViewport(0, 0, mSurfaceView2.getMeasuredWidth(), mSurfaceView2.getMeasuredHeight());
            glValuePair.getGlProgram().draw();
            mEGLCore.swapBuffers(glValuePair.getEglSurface());
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        EGLSurface eglSurface = mEGLCore.createWindowSurface(holder.getSurface());
        mEGLCore.makeCurrent(eglSurface);
        if (mTextureId == -1000) {
            mTextureId = TextureHelper.loadOESTexture();
            mPrevSurfaceTexture = new SurfaceTexture(mTextureId);
            mPrevSurfaceTexture.setOnFrameAvailableListener(OpenGLES20L8Activity.this);
        }
        getGLValuePair(holder, eglSurface);

        if (mCamera == null) {
            try {
                mCamera = CameraUtil.prevCamera(Camera.CameraInfo.CAMERA_FACING_FRONT, mPrevSurfaceTexture, PREV_WIDTH, PREV_HEIGHT, PREV_FPS);
                Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
                PREV_HEIGHT = previewSize.height;
                PREV_WIDTH = previewSize.width;
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "could not prevCamera", Toast.LENGTH_SHORT).show();
                return;
            }
        }
    }

    private void getGLValuePair(SurfaceHolder holder, EGLSurface eglSurface) {
        if (holder == mSurfaceView1.getHolder()) {
            GLProgram glProgram = new CameraPrevGLProgram(getApplicationContext(), mTextureM, mTextureId);
            glProgram.compile();
            mGLValuePair.add(new GLValuePair(eglSurface, glProgram));
        } else if (holder == mSurfaceView2.getHolder()) {
            GLProgram inputGLProgram = new CameraPrevGLProgram(getApplicationContext(), mTextureM, mTextureId);
            FilterBeautyGLProgram outputProgram = new FilterBeautyGLProgram(getApplicationContext(), GLUtil.getIdentityM(), mTextureId, PREV_WIDTH, PREV_HEIGHT);
            outputProgram.setBeautyLevel(1);
            GLProgram glProgram = new FBOGroupGLProgram(new Size(mSurfaceView1.getMeasuredWidth(), mSurfaceView1.getMeasuredHeight()), inputGLProgram, outputProgram);
            glProgram.compile();
            mGLValuePair.add(new GLValuePair(eglSurface, glProgram));
        } else if (holder == mSurfaceView3.getHolder()) {
            GLProgram inputGLProgram = new CameraPrevGLProgram(getApplicationContext(), mTextureM, mTextureId);
            FilterWriteSkinGLProgram outputProgram = new FilterWriteSkinGLProgram(getApplicationContext(), GLUtil.getIdentityM(), mTextureId, PREV_WIDTH, PREV_HEIGHT);
            GLProgram glProgram = new FBOGroupGLProgram(new Size(mSurfaceView1.getMeasuredWidth(), mSurfaceView1.getMeasuredHeight()), inputGLProgram, outputProgram);
            glProgram.compile();
            mGLValuePair.add(new GLValuePair(eglSurface, glProgram));
        } else {
            GLProgram inputGLProgram = new CameraPrevGLProgram(getApplicationContext(), mTextureM, mTextureId);
            FilterBeautyGLProgram middleWareProgram = new FilterBeautyGLProgram(getApplicationContext(), GLUtil.getIdentityM(), mTextureId, PREV_WIDTH, PREV_HEIGHT);
            middleWareProgram.setBeautyLevel(1);
            FilterWriteSkinGLProgram outputProgram = new FilterWriteSkinGLProgram(getApplicationContext(), GLUtil.getIdentityM(), mTextureId, PREV_WIDTH, PREV_HEIGHT);
            GLProgram glProgram = new FBOGroupGLProgram(new Size(mSurfaceView1.getMeasuredWidth(), mSurfaceView1.getMeasuredHeight()), inputGLProgram, outputProgram, middleWareProgram);
            glProgram.compile();
            mGLValuePair.add(new GLValuePair(eglSurface, glProgram));
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            Log.d(TAG, "releaseCamera -- done");
        }
        if (mPrevSurfaceTexture != null) {
            mPrevSurfaceTexture.release();
            mPrevSurfaceTexture = null;
        }
        if (mEGLCore != null) {
            mEGLCore.release();
            mEGLCore = null;
        }
    }

    private static class GLValuePair {
        private EGLSurface eglSurface;
        private GLProgram glProgram;

        public GLValuePair(EGLSurface eglSurface, GLProgram glProgram) {
            this.eglSurface = eglSurface;
            this.glProgram = glProgram;
        }

        public EGLSurface getEglSurface() {
            return eglSurface;
        }

        public void setEglSurface(EGLSurface eglSurface) {
            this.eglSurface = eglSurface;
        }

        public GLProgram getGlProgram() {
            return glProgram;
        }

        public void setGlProgram(GLProgram glProgram) {
            this.glProgram = glProgram;
        }
    }
}
