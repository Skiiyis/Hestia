package io.github.sawameimei.playopengles20;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGLSurface;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import java.io.IOException;

import io.github.sawameimei.opengleslib.common.EGLCore;
import io.github.sawameimei.opengleslib.common.GLUtil;
import io.github.sawameimei.opengleslib.common.TextureHelper;
import io.github.sawameimei.opengleslib.glprogram.FBOGroupGLProgram;
import io.github.sawameimei.opengleslib.glprogram.GLProgram;
import io.github.sawameimei.opengleslib.glprogram.TextureGLProgram;
import io.github.sawameimei.playopengles20.glprogram.CameraPrevGLProgram;
import io.github.sawameimei.playopengles20.glprogram.CameraUtil;
import io.github.sawameimei.playopengles20.glprogram.FilterFaceDetectorGLProgram;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class OpenGLES20L9Activity extends AppCompatActivity implements SurfaceTexture.OnFrameAvailableListener, SurfaceHolder.Callback {

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

        //GLES20.glViewport(0, 0, mContentView.getMeasuredWidth(), mContentView.getMeasuredHeight());
        mGlProgram.drawFrame();
        mEGLCore.swapBuffers(mEglSurface);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mEglSurface = mEGLCore.createWindowSurface(holder.getSurface());
        mEGLCore.makeCurrent(mEglSurface);
        int mTextureId = TextureHelper.loadOESTexture();
        mPrevSurfaceTexture = new SurfaceTexture(mTextureId);
        mPrevSurfaceTexture.setOnFrameAvailableListener(OpenGLES20L9Activity.this);

        if (mCamera == null) {
            try {
                mCamera = CameraUtil.prevCamera(Camera.CameraInfo.CAMERA_FACING_BACK, mPrevSurfaceTexture, PREV_WIDTH, PREV_HEIGHT, PREV_FPS);
                Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
                PREV_HEIGHT = previewSize.height;
                PREV_WIDTH = previewSize.width;

                TextureGLProgram inputProgram = new CameraPrevGLProgram(getApplicationContext(), mTextureM, mTextureId);
                FilterFaceDetectorGLProgram middlewareProgram = new FilterFaceDetectorGLProgram(this, mTextureM, mTextureId, PREV_WIDTH, PREV_HEIGHT);
                //TextureGLProgram outputProgram = new FilterBeautyGLProgram(this, mTextureM, mTextureId, PREV_WIDTH, PREV_HEIGHT).setBeautyLevel(1);
                mGlProgram = new FBOGroupGLProgram(mContentView.getMeasuredWidth(), mContentView.getMeasuredHeight(), PREV_WIDTH, PREV_HEIGHT, inputProgram, /*outputProgram, */middlewareProgram);
                mGlProgram.compileAndLink();

                /**
                 * 使用系统自带的人脸检测，效率约25ms每帧，异步回调
                 */
                if (mCamera.getParameters().getMaxNumDetectedFaces() > 0) {
                    mCamera.setFaceDetectionListener(middlewareProgram);
                    mCamera.startFaceDetection();
                } else {
                    Log.e("FaceDetection", "unSupport FaceDetection!!");
                }
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
