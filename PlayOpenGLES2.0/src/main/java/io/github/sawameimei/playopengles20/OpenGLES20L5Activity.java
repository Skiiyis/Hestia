package io.github.sawameimei.playopengles20;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import io.github.sawameimei.opengleslib.common.TextureHelper;
import io.github.sawameimei.opengleslib.common.EGLCore;
import io.github.sawameimei.opengleslib.glprogram.GLProgram;
import io.github.sawameimei.playopengles20.glprogram.CameraPrevGLProgram;
import io.github.sawameimei.playopengles20.glprogram.CameraUtil;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class OpenGLES20L5Activity extends AppCompatActivity implements SurfaceTexture.OnFrameAvailableListener {

    private static String TAG = "OpenGLES2.0";

    private int PREV_WIDTH = 1280;
    private int PREV_HEIGHT = 720;
    private int ENCODER_BIT_RATE = 6000000; // = PREV_WIDTH * PREV_HEIGHT * 7?
    private int PREV_FPS = 24;

    private SurfaceView mSurfaceView;
    private EGLSurface mPreviewSurface;
    private Button mRecording;
    private File recordingFile;
    private Camera mCamera;

    private EGLCore mEGLCore;
    private GLProgram mPrevProgram;

    private SurfaceTexture mPrevSurfaceTexture;

    private float[] mTextureM = new float[16];

    private boolean mIsRecording = false;
    private RecordThread mRecordThread;

    private int mTextureId;

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

                mTextureId = TextureHelper.loadOESTexture();
                mPrevProgram = new CameraPrevGLProgram(getApplicationContext(), mTextureM, mTextureId);
                mPrevSurfaceTexture = new SurfaceTexture(mTextureId);
                mPrevSurfaceTexture.setOnFrameAvailableListener(OpenGLES20L5Activity.this);

                mEGLCore = new EGLCore(null, EGLCore.FLAG_RECORDABLE);
                mPreviewSurface = mEGLCore.createWindowSurface(holder.getSurface());
                mEGLCore.makeCurrent(mPreviewSurface);

                //在真机上没有makeCurrent就生成Texture会导致Texture不可跨线程共享。。
                //模拟器可以跨线程共享？模拟器上的线程不是操作系统级别的线程?

                /*mEGLCore = new EGLCore(null, EGLCore.FLAG_RECORDABLE);
                mPreviewSurface = mEGLCore.createWindowSurface(holder.getSurface());
                mEGLCore.makeCurrent(mPreviewSurface);

                mTextureId = TextureHelper.loadOESTexture();
                mPrevProgram = new CameraPrevGLProgram(getApplicationContext(), mTextureM, mTextureId);
                mPrevSurfaceTexture = new SurfaceTexture(mTextureId);
                mPrevSurfaceTexture.setOnFrameAvailableListener(OpenGLES20L5Activity.this);*/

                mPrevProgram.compile();
                try {
                    mCamera = CameraUtil.prevCamera(Camera.CameraInfo.CAMERA_FACING_BACK, mPrevSurfaceTexture, PREV_WIDTH, PREV_HEIGHT, PREV_FPS);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "could not prevCamera", Toast.LENGTH_SHORT).show();
                    return;
                }
                mRecordThread = RecordThread.start(CameraUtil.getActualPrevFPS(mCamera), recordingFile, EGL14.eglGetCurrentContext(), mPrevProgram);
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
            mRecordThread.record(mPrevSurfaceTexture.getTimestamp());
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
        if (mEGLCore != null) {
            mEGLCore.release();
            mEGLCore = null;
        }
        if (mRecordThread != null) {
            mRecordThread.release();
            mRecordThread = null;
        }
    }

    private static class RecordThread implements Runnable {

        public static final int PREPARE = 1;
        public static final int RECORD = 2;
        public static final int RELEASE = 3;

        private int ENCODER_WIDTH = 1280;
        private int ENCODER_HEIGHT = 720;
        private int ENCODER_BIT_RATE = 6000000; // = ENCODER_WIDTH * ENCODER_HEIGHT * 7?
        private int PREV_FPS = 24;
        private File RECORD_FILE;

        private Handler mH;
        private MP4Encoder mEncoder;
        private EGLSurface mRecorderSurface;
        private EGLCore mEGLCore;
        private EGLContext mEglContext;
        private GLProgram mPrevProgram;

        public RecordThread(int prevFPS, File file, EGLContext eglContext, GLProgram prevProgram) {
            this.PREV_FPS = prevFPS;
            this.RECORD_FILE = file;
            this.mEglContext = eglContext;
            this.mPrevProgram = prevProgram;
        }

        @Override
        public void run() {
            Looper.prepare();
            mH = new RecordHandler(Looper.myLooper());
            prepare();
            Looper.loop();
        }

        public static RecordThread start(int prevFPS, File file, EGLContext eglContext, GLProgram prevProgram) {
            RecordThread target = new RecordThread(prevFPS, file, eglContext, prevProgram);
            new Thread(target).start();
            return target;
        }

        private void handlePrepare() {
            mEGLCore = new EGLCore(mEglContext, EGLCore.FLAG_RECORDABLE);
            mEncoder = new MP4Encoder(ENCODER_WIDTH, ENCODER_HEIGHT, ENCODER_BIT_RATE, PREV_FPS, RECORD_FILE);
            mEncoder.prepare();
            mRecorderSurface = mEGLCore.createWindowSurface(mEncoder.getInputSurface());
            mEGLCore.makeCurrent(mRecorderSurface);
            mPrevProgram.compile();
        }

        private void handleRelease() {
            if (mEncoder != null) {
                mEncoder.shutDown();
                mEncoder = null;
            }
            if (mEGLCore != null) {
                mEGLCore.release();
                mEGLCore = null;
            }
            Looper looper = Looper.myLooper();
            if (looper != null) {
                looper.quit();
            }
        }

        private void handleRecord(long nsecs) {
            GLES20.glViewport(0, 0, ENCODER_WIDTH, ENCODER_HEIGHT);
            mPrevProgram.draw();
            mEncoder.drainEncoder();
            mEGLCore.setPresentationTime(mRecorderSurface, nsecs);
            mEGLCore.swapBuffers(mRecorderSurface);
        }

        private void record(long nsecs) {
            Message msg = Message.obtain();
            msg.what = RECORD;
            msg.obj = nsecs;
            mH.sendMessage(msg);
        }

        private void prepare() {
            mH.sendEmptyMessage(PREPARE);
        }

        private void release() {
            mH.sendEmptyMessage(RELEASE);
        }

        private class RecordHandler extends Handler {

            public RecordHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case PREPARE:
                        handlePrepare();
                        break;
                    case RECORD:
                        handleRecord((Long) msg.obj);
                        break;
                    case RELEASE:
                        handleRelease();
                        break;
                }
            }
        }
    }
}
