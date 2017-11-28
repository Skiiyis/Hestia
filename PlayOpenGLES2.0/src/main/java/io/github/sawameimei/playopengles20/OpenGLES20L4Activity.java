package io.github.sawameimei.playopengles20;

import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGLSurface;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.List;

import io.github.sawameimei.playopengles20.common.MP4Encoder;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class OpenGLES20L4Activity extends AppCompatActivity implements SurfaceTexture.OnFrameAvailableListener {

    private static String TAG = "OpenGLES2.0";
    private SurfaceView mSurfaceView;
    private EGLSurface mPreviewSurface;
    private EGLSurface mPreviewSurface2;
    private CameraPreviewHelper mCameraPreviewHelper;
    private Button mRecording;
    public Boolean mIsRecording = false;
    private EGLSurface mRecorderSurface;
    private File recordingFile;
    private SurfaceView mSurfaceView2;
    public MP4Encoder mEncoder;


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
                    //mEncoder.shutDown();
                } else {
                    mIsRecording = true;
                }
            }
        });

        mCameraPreviewHelper = new CameraPreviewHelper(this);
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {

            @Override
            public void surfaceCreated(SurfaceHolder holder) {

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                mPreviewSurface = mCameraPreviewHelper.createWindowSurface(holder.getSurface());
                SurfaceTexture prevSurfaceTexture = mCameraPreviewHelper.initGLContext(mPreviewSurface, width, height, OpenGLES20L4Activity.this);
                boolean hasCamera = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
                if (!hasCamera) {
                    Toast.makeText(getApplicationContext(), "没有摄像头！！", Toast.LENGTH_SHORT).show();
                    return;
                }
                Camera camera = null;
                Camera.CameraInfo info = new Camera.CameraInfo();
                // Try to find a front-facing camera (e.g. for videoconferencing).
                int numCameras = Camera.getNumberOfCameras();
                for (int i = 0; i < numCameras; i++) {
                    Camera.getCameraInfo(i, info);
                    if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                        camera = Camera.open(i);
                        break;
                    }
                }
                if (camera == null) {
                    camera = Camera.open();    // opens first back-facing camera
                }
                Camera.Parameters parms = camera.getParameters();
                //choosePreviewSize(parms, 720, 1280);
                choosePreviewSize(parms, 1280, 720);
                int factFps = chooseFixedPreviewFps(parms, 15);
                camera.setParameters(parms);
                parms.setRecordingHint(true);
                try {
                    camera.setPreviewTexture(prevSurfaceTexture);
                    camera.startPreview();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                /*try {
                    mEncoder = new MP4Encoder(width, height, 6000000, factFps / 1000, recordingFile);
                    mEncoder.prepare();
                    Surface inputSurface = mEncoder.getInputSurface();
                    mRecorderSurface = mCameraPreviewHelper.createWindowSurface(inputSurface);
                } catch (Exception e) {
                    e.printStackTrace();
                }*/
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });

        mSurfaceView2.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                mPreviewSurface2 = mCameraPreviewHelper.createWindowSurface(holder.getSurface());
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
        mainHandler.sendEmptyMessage(1);
    }

    private MainHandler mainHandler = new MainHandler();

    private class MainHandler extends Handler {
        @Override
        public void dispatchMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    mCameraPreviewHelper.drawFrame(mPreviewSurface);
                    if (mIsRecording != null && mIsRecording) {
                        mCameraPreviewHelper.drawFrame(mPreviewSurface2);
                    }
                    break;
            }
        }
    }

    /**
     * Attempts to find a preview size that matches the provided width and height (which
     * specify the dimensions of the encoded video).  If it fails to find a match it just
     * uses the default preview size for video.
     * <p>
     * TODO: should do a best-fit match, e.g.
     * https://github.com/commonsguy/cwac-camera/blob/master/camera/src/com/commonsware/cwac/camera/CameraUtils.java
     */
    public static void choosePreviewSize(Camera.Parameters parms, int width, int height) {
        // We should make sure that the requested MPEG size is less than the preferred
        // size, and has the same aspect ratio.
        Camera.Size ppsfv = parms.getPreferredPreviewSizeForVideo();
        if (ppsfv != null) {
            Log.d(TAG, "Camera preferred preview size for video is " +
                    ppsfv.width + "x" + ppsfv.height);
        }

        //for (Camera.Size size : parms.getSupportedPreviewSizes()) {
        //    Log.d(TAG, "supported: " + size.width + "x" + size.height);
        //}

        for (Camera.Size size : parms.getSupportedPreviewSizes()) {
            if (size.width == width && size.height == height) {
                parms.setPreviewSize(width, height);
                return;
            }
        }

        Log.w(TAG, "Unable to set preview size to " + width + "x" + height);
        if (ppsfv != null) {
            parms.setPreviewSize(ppsfv.width, ppsfv.height);
        }
        // else use whatever the default size is
    }

    /**
     * Attempts to find a fixed preview frame rate that matches the desired frame rate.
     * <p>
     * It doesn't seem like there's a great deal of flexibility here.
     * <p>
     * TODO: follow the recipe from http://stackoverflow.com/questions/22639336/#22645327
     *
     * @return The expected frame rate, in thousands of frames per second.
     */
    public static int chooseFixedPreviewFps(Camera.Parameters parms, int desiredThousandFps) {
        List<int[]> supported = parms.getSupportedPreviewFpsRange();

        for (int[] entry : supported) {
            //Log.d(TAG, "entry: " + entry[0] + " - " + entry[1]);
            if ((entry[0] == entry[1]) && (entry[0] == desiredThousandFps)) {
                parms.setPreviewFpsRange(entry[0], entry[1]);
                return entry[0];
            }
        }

        int[] tmp = new int[2];
        parms.getPreviewFpsRange(tmp);
        int guess;
        if (tmp[0] == tmp[1]) {
            guess = tmp[0];
        } else {
            guess = tmp[1] / 2;     // shrug
        }

        Log.d(TAG, "Couldn't find match for " + desiredThousandFps + ", using " + guess);
        return guess;
    }
}
