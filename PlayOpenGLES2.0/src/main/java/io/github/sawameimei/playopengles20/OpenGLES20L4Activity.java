package io.github.sawameimei.playopengles20;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import io.github.sawameimei.playopengles20.common.CameraPrevGLProgram;
import io.github.sawameimei.playopengles20.common.CameraUtil;
import io.github.sawameimei.playopengles20.common.EGLCore;
import io.github.sawameimei.playopengles20.common.GLProgram;
import io.github.sawameimei.playopengles20.common.MP4Encoder;
import io.github.sawameimei.playopengles20.common.TextureHelper;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class OpenGLES20L4Activity extends AppCompatActivity implements SurfaceTexture.OnFrameAvailableListener {

    private static String TAG = "OpenGLES2.0";

    private int ENCODER_WIDTH = 1280;
    private int ENCODER_HEIGHT = 720;
    private int ENCODER_BIT_RATE = 6000000; // = ENCODER_WIDTH * ENCODER_HEIGHT * 7?
    private int PREV_FPS = 24;

    private SurfaceView mSurfaceView;
    private EGLSurface mPreviewSurface;
    private EGLSurface mPreviewSurface2;
    private Button mRecording;
    private EGLSurface mRecorderSurface;
    private File recordingFile;
    private SurfaceView mSurfaceView2;
    private MP4Encoder mEncoder;
    private Camera mCamera;

    private EGLCore mEGLCore = new EGLCore(null, EGLCore.FLAG_RECORDABLE);
    private GLProgram mPrevProgram;

    private SurfaceTexture mPrevSurfaceTexture;

    private float[] mTextureM = new float[16];

    private int mWidth;
    private int mHeight;

    private int mProgramHandle;
    private Boolean mIsRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_gles20_l4);

        mSurfaceView = findViewById(R.id.surfaceView);
        mSurfaceView2 = findViewById(R.id.surfaceView2);
        mRecording = findViewById(R.id.recording);
        mRecording.setText("Start Recording");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{RECORD_AUDIO, READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE, INTERNET, CAMERA}, 1);
        }

        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/audio/");
        file.mkdirs();
        try {
            recordingFile = File.createTempFile("recording", ".mp4", file);
            Log.e(TAG, "filePath:" + recordingFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsRecording != null) {
                    mRecording.setText(mIsRecording ? "Start Recording" : "Stop Recording");
                } else {
                    return;
                }
                if (mIsRecording) {
                    mIsRecording = false;
                    mEncoder.shutDown();
                } else {
                    mIsRecording = true;
                }
            }
        });

        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {

            @Override
            public void surfaceCreated(SurfaceHolder holder) {

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                mWidth = width;
                mHeight = height;

                mPreviewSurface = mEGLCore.createWindowSurface(holder.getSurface());
                mEGLCore.makeCurrent(mPreviewSurface);
                int textureId = TextureHelper.loadOESTexture();
                mPrevProgram = new CameraPrevGLProgram(getApplicationContext(), textureId, mTextureM);
                mProgramHandle = mPrevProgram.compileAndLink();
                mPrevSurfaceTexture = new SurfaceTexture(textureId);
                mPrevSurfaceTexture.setOnFrameAvailableListener(OpenGLES20L4Activity.this);

                try {
                    mCamera = CameraUtil.prevCamera(Camera.CameraInfo.CAMERA_FACING_BACK, mPrevSurfaceTexture, ENCODER_WIDTH, ENCODER_HEIGHT, PREV_FPS);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "could not prevCamera", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    mEncoder = new MP4Encoder(ENCODER_WIDTH, ENCODER_HEIGHT, ENCODER_BIT_RATE, CameraUtil.getActualPrevFPS(mCamera), recordingFile);
                    mEncoder.prepare();
                    Surface inputSurface = mEncoder.getInputSurface();
                    mRecorderSurface = mEGLCore.createWindowSurface(inputSurface);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });

        mSurfaceView2.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                mPreviewSurface2 = mEGLCore.createWindowSurface(holder.getSurface());
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        mainHandler.sendEmptyMessage(MSG_FRAME_AVAILABLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    private MainHandler mainHandler = new MainHandler();
    private long lastTime;
    private final int MSG_FRAME_AVAILABLE = 1;

    private class MainHandler extends Handler {

        @Override
        public void dispatchMessage(Message msg) {
            switch (msg.what) {
                case MSG_FRAME_AVAILABLE:
                    //Log.e(TAG, "currentTime:" + (System.currentTimeMillis() - lastTime));
                    lastTime = System.currentTimeMillis();

                    mEGLCore.makeCurrent(mPreviewSurface);
                    mPrevSurfaceTexture.updateTexImage();
                    mPrevSurfaceTexture.getTransformMatrix(mTextureM);
                    GLES20.glViewport(0, 0, mWidth, mHeight);
                    GLES20.glUseProgram(mProgramHandle);
                    mPrevProgram.drawFrame();
                    mEGLCore.swapBuffers(mPreviewSurface);

                    if (mIsRecording != null && mIsRecording) {
                        mEGLCore.makeCurrent(mRecorderSurface);
                        GLES20.glViewport(0, 0, ENCODER_WIDTH, ENCODER_HEIGHT);
                        GLES20.glUseProgram(mProgramHandle);
                        mPrevProgram.drawFrame();
                        mEGLCore.swapBuffers(mRecorderSurface);
                        mEGLCore.setPresentationTime(mRecorderSurface, mPrevSurfaceTexture.getTimestamp());
                    }
                    break;
            }
        }
    }
}
