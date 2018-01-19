package io.github.sawameimei.playopengles20;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import java.io.IOException;

import io.github.sawameimei.opengleslib.common.EGLCore;
import io.github.sawameimei.opengleslib.common.TextureHelper;
import io.github.sawameimei.opengleslib.glprogram.GLProgram;
import io.github.sawameimei.playopengles20.glprogram.CameraPrevGLProgram;
import io.github.sawameimei.playopengles20.glprogram.CameraUtil;
import io.github.sawameimei.playopengles20.glprogram.WaterMaskProgram;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class OpenGLES20L10Activity extends AppCompatActivity implements SurfaceTexture.OnFrameAvailableListener, SurfaceHolder.Callback {

    private static String TAG = "OpenGLES2.0";

    private int PREV_WIDTH = 720;
    private int PREV_HEIGHT = 1280;
    private int PREV_FPS = 24;

    private Camera mCamera;
    private EGLCore mEGLCore = new EGLCore(null, EGLCore.FLAG_RECORDABLE);
    private SurfaceTexture mPrevSurfaceTexture;
    private float[] mTextureM = new float[16];
    private SurfaceView mContentView;
    private EGLSurface mEglSurface;

    private GLProgram mGlProgram;
    private WaterMaskProgram mGlProgram2;
    private int mWaterMaskHeight;
    private int mWaterMaskWidth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_open_gles20_l8);
        mContentView = new SurfaceView(this);
        setContentView(mContentView);

        mContentView.getHolder().addCallback(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{RECORD_AUDIO, READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE, INTERNET, CAMERA}, 1);
        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        if (mEGLCore == null || mEglSurface == null || mGlProgram == null) {
            return;
        }
        mEGLCore.makeCurrent(mEglSurface);
        mPrevSurfaceTexture.updateTexImage();
        mPrevSurfaceTexture.getTransformMatrix(mTextureM);

        GLES20.glViewport(0, 0, mContentView.getMeasuredWidth(), mContentView.getMeasuredHeight());
        mGlProgram.draw();
        //mEGLCore.swapBuffers(mEglSurface);

        GLES20.glViewport(mContentView.getMeasuredWidth() / 8, mContentView.getMeasuredHeight() / 8, mContentView.getMeasuredWidth() * 6 / 8, mContentView.getMeasuredHeight() * 6 / 8);
        mGlProgram.draw();

        GLES20.glViewport(mContentView.getMeasuredWidth() / 4, mContentView.getMeasuredHeight() / 4, mContentView.getMeasuredWidth() / 2, mContentView.getMeasuredHeight() / 2);
        mGlProgram.draw();

        /**
         * 使用颜色混合，水印为透明的部分使用目标颜色，否则使用水印颜色（源颜色）
         * src.alpha = 0 ? origin.color : src.color;
         * ret.color = src.color * src.alpha + origin.color * (1-src.alpha);
         *                            ^      ^                      ^
         *                   GL_SRC_ALPHA  GL_FUNC_ADD    GL_ONE_MINUS_SRC_ALPHA
         */
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glBlendEquation(GLES20.GL_FUNC_ADD);

        GLES20.glViewport(mContentView.getMeasuredWidth() / 4, mContentView.getMeasuredHeight() / 4, mWaterMaskWidth, mWaterMaskHeight);
        mGlProgram2.draw();
        mEGLCore.swapBuffers(mEglSurface);

        GLES20.glDisable(GLES20.GL_BLEND);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mEglSurface = mEGLCore.createWindowSurface(holder.getSurface());
        mEGLCore.makeCurrent(mEglSurface);
        int mTextureId = TextureHelper.loadOESTexture();
        mPrevSurfaceTexture = new SurfaceTexture(mTextureId);
        mPrevSurfaceTexture.setOnFrameAvailableListener(OpenGLES20L10Activity.this);

        if (mCamera == null) {
            try {
                mCamera = CameraUtil.prevCamera(Camera.CameraInfo.CAMERA_FACING_BACK, mPrevSurfaceTexture, PREV_WIDTH, PREV_HEIGHT, PREV_FPS);
                Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
                PREV_HEIGHT = previewSize.height;
                PREV_WIDTH = previewSize.width;

                mGlProgram = new CameraPrevGLProgram(this, mTextureM, mTextureId);
                mGlProgram.compile();

                mGlProgram2 = new WaterMaskProgram();
                Bitmap textureBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher, null);
                mWaterMaskHeight = textureBitmap.getHeight();
                mWaterMaskWidth = textureBitmap.getWidth();
                //new GLSurfaceView().setEGLConfigChooser();
                mGlProgram2.texture()[0] = TextureHelper.loadTexture(getApplicationContext(), textureBitmap);
                textureBitmap.recycle();
                mGlProgram2.compile();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "could not prevCamera", Toast.LENGTH_SHORT).show();
                return;
            }
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
}
