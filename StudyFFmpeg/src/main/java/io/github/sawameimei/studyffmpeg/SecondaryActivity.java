package io.github.sawameimei.studyffmpeg;

import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.List;

import io.github.sawameimei.ffmpeg.FFmpegBridge;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class SecondaryActivity extends AppCompatActivity {

    private static final String TAG = "studyFFmpeg";
    private File recordingFile;
    private Camera fontCamera;
    private int previewWidth = 1280;
    private int previewHeight = 760;
    private int previewFps;
    public Surface playSurface;

    public boolean capturing;
    private FFMpegEncoderDispatcher encoder = new FFMpegEncoderDispatcher();
    private DecoderDispatcher decoder = new DecoderDispatcher();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{RECORD_AUDIO, READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE, INTERNET, CAMERA}, 1);
        }
        final File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/audio/");
        file.mkdirs();
        try {
            recordingFile = File.createTempFile("recording", ".h264", file);
            Log.e("recordingFile", recordingFile.getAbsolutePath().toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        final TextView decode = (TextView) findViewById(R.id.decode);
        final TextView encode = (TextView) findViewById(R.id.encode);
        final SurfaceView previewSurface = (SurfaceView) findViewById(R.id.prevSurface);
        final SurfaceView playSurfaceView = (SurfaceView) findViewById(R.id.playSurface);


        boolean hasCamera = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
        if (!hasCamera) {
            Toast.makeText(this, "没有摄像头！！", Toast.LENGTH_SHORT).show();
            return;
        }
        fontCamera = Camera.open();
        Camera.Parameters parameters = fontCamera.getParameters();
        parameters.setRecordingHint(true);
        parameters.setPreviewFormat(ImageFormat.NV21);
        choosePreviewSize(parameters, previewWidth, previewHeight);
        previewFps = chooseFixedPreviewFps(parameters, 15 * 1000);
        fontCamera.setParameters(parameters);
        Camera.Size previewSize = fontCamera.getParameters().getPreviewSize();
        previewWidth = previewSize.width;
        previewHeight = previewSize.height;
        fontCamera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                if (capturing) {
                    encoder.encode(data);
                }
            }
        });

        playSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {

            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                playSurface = surfaceHolder.getSurface();
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

            }
        });

        previewSurface.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    fontCamera.addCallbackBuffer(new byte[1024]);
                    fontCamera.addCallbackBuffer(new byte[1024]);
                    fontCamera.addCallbackBuffer(new byte[1024]);
                    fontCamera.setPreviewDisplay(holder);
                    fontCamera.startPreview();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                fontCamera.stopPreview();
            }
        });

        encode.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (capturing) {
                    encoder.endEncode();
                    File ret = new File(file, "ret.mp4");
                    encoder.muxing(recordingFile.getAbsolutePath(), null, ret.getAbsolutePath());
                    recordingFile = ret;
                    encoder.shutDown();
                } else {
                    encoder.start(recordingFile, previewWidth, previewHeight, previewFps);
                }
                capturing = !capturing;
                encode.setText(capturing ? "停止录制" : "点我录制");
            }
        });

        decoder.start();
        decode.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                decoder.decode(recordingFile, playSurface);
                /*final MediaPlayer mediaPlayer = new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(recordingFile.getAbsolutePath());
                    mediaPlayer.setSurface(playSurface);
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mediaPlayer.start();
                        }
                    });
                    mediaPlayer.prepareAsync();
                } catch (IOException e) {
                    e.printStackTrace();
                }*/
            }
        });
    }

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
