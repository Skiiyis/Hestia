package io.github.sawameimei.playopengles20;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import io.github.sawameimei.opengleslib.glprogram.GLProgram;
import io.github.sawameimei.opengleslib.common.EGLCore;
import io.github.sawameimei.playopengles20.glprogram.CameraPrevGLProgram;
import io.github.sawameimei.playopengles20.glprogram.CameraUtil;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class OpenGLES20L3Activity extends AppCompatActivity implements SurfaceTexture.OnFrameAvailableListener {

    private static String TAG = "OpenGLES2.0";

    private int ENCODER_WIDTH = 1280;
    private int ENCODER_HEIGHT = 720;
    private int ENCODER_BIT_RATE = 6000000; // = ENCODER_WIDTH * ENCODER_HEIGHT * 7?
    private int PREV_FPS = 24;

    private SurfaceView mSurfaceView;
    private EGLSurface mPreviewSurface;
    private Button mRecording;
    private EGLSurface mRecorderSurface;
    private File recordingFile;
    private MP4Encoder mEncoder;
    private Camera mCamera;

    private EGLCore mEGLCore;
    private GLProgram mPrevProgram;

    private SurfaceTexture mPrevSurfaceTexture;

    private float[] mTextureM = new float[16];

    private boolean mIsRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_gles20_l4);

        mSurfaceView = findViewById(R.id.continuousCapture_surfaceView);
        mRecording = findViewById(R.id.capture_button);
        mRecording.setText("Start Recording");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{RECORD_AUDIO, READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE, INTERNET, CAMERA}, 1);
        }

        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/audio/");
        file.mkdirs();
        try {
            recordingFile = File.createTempFile(getClass().getSimpleName(), ".mp4", file);
            Log.e(TAG, "filePath:" + recordingFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsRecording = !mIsRecording;
                mRecording.setText(mIsRecording ? "Stop Recording" : "Start Recording");
            }
        });

        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                CameraPrevGLProgram glProgram = new CameraPrevGLProgram(getApplicationContext(), mTextureM);
                mPrevSurfaceTexture = new SurfaceTexture(glProgram.texture()[0]);
                mPrevProgram = glProgram;
                mPrevSurfaceTexture.setOnFrameAvailableListener(OpenGLES20L3Activity.this);
                mEGLCore = new EGLCore(null, EGLCore.FLAG_RECORDABLE);

                mPreviewSurface = mEGLCore.createWindowSurface(holder.getSurface());
                mEGLCore.makeCurrent(mPreviewSurface);
                mPrevProgram.compile();

                try {
                    mCamera = CameraUtil.prevCamera(Camera.CameraInfo.CAMERA_FACING_BACK, mPrevSurfaceTexture, ENCODER_WIDTH, ENCODER_HEIGHT, PREV_FPS);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "could not prevCamera", Toast.LENGTH_SHORT).show();
                    return;
                }
                mEncoder = new MP4Encoder(ENCODER_WIDTH, ENCODER_HEIGHT, ENCODER_BIT_RATE, CameraUtil.getActualPrevFPS(mCamera), recordingFile);
                mEncoder.prepare();
                mRecorderSurface = mEGLCore.createWindowSurface(mEncoder.getInputSurface());
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.d(TAG, "surfaceChanged fmt=" + format + " size=" + width + "x" + height +
                        " holder=" + holder);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.d(TAG, "surfaceDestroyed holder=" + holder);
            }
        });
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        if (mEGLCore == null) {
            return;
        }
        mEGLCore.makeCurrent(mPreviewSurface);
        mPrevSurfaceTexture.updateTexImage();
        mPrevSurfaceTexture.getTransformMatrix(mTextureM);

        GLES20.glViewport(0, 0, mSurfaceView.getMeasuredWidth(), mSurfaceView.getMeasuredHeight());
        mPrevProgram.draw();
        mEGLCore.swapBuffers(mPreviewSurface);

        if (mIsRecording) {
            mEGLCore.makeCurrent(mRecorderSurface);
            GLES20.glViewport(0, 0, ENCODER_WIDTH, ENCODER_HEIGHT);
            mPrevProgram.draw();
            mEncoder.drainEncoder();
            mEGLCore.setPresentationTime(mRecorderSurface, mPrevSurfaceTexture.getTimestamp());
            mEGLCore.swapBuffers(mRecorderSurface);
        }
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
        if (mEncoder != null) {
            mEncoder.shutDown();
            mEncoder = null;
        }
        if (mEGLCore != null) {
            mEGLCore.release();
            mEGLCore = null;
        }
    }
}
