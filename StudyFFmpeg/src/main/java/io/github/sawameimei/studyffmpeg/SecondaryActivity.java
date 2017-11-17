package io.github.sawameimei.studyffmpeg;

import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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
        final File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/audio/");
        dir.mkdirs();
        try {
            recordingFile = File.createTempFile("recording", ".h264", dir);
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
        final Camera.Parameters parameters = fontCamera.getParameters();
        parameters.setRecordingHint(true);
        List<Integer> supportedPreviewFormats = parameters.getSupportedPreviewFormats();
        Log.e("supportedPreviewFormats", supportedPreviewFormats.toString());
        parameters.setPreviewFormat(ImageFormat.YV12); //yuv420sp --> yuv420p
        List<Integer> supportedPictureFormats = parameters.getSupportedPictureFormats();
        parameters.setPictureFormat(supportedPictureFormats.get(0));
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
                    //encoder.encode(data);
                    try {
                        data = YV12toNV12(data, previewWidth, previewHeight);
                        File of = new File(dir, "width_" + previewWidth + "_height_" + previewHeight + ".yuv");
                        FileOutputStream os = new FileOutputStream(of);
                        os.write(data);
                        os.flush();
                        os.close();
                        capturing = false;
                        Toast.makeText(getApplicationContext(), "ffmpeg:Success to save at" + of.getAbsolutePath(), Toast.LENGTH_LONG).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
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
                /*if (capturing) {
                    encoder.endEncode();
                    File ret = new File(dir, "ret.mp4");
                    encoder.muxing(recordingFile.getAbsolutePath(), null, ret.getAbsolutePath());
                    recordingFile = ret;
                    encoder.shutDown();
                } else {
                    encoder.start(recordingFile, previewWidth, previewHeight, previewFps);
                }*/
                capturing = !capturing;
                encode.setText(capturing ? "停止录制" : "点我录制");
            }
        });

        //decoder.start();
        decode.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                //decoder.decode(recordingFile, playSurface);
                final MediaPlayer mediaPlayer = new MediaPlayer();
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
                }
            }
        });
    }

    private byte[] YV12toNV12(byte[] yv12, int width, int height) {
        int ySize = width * height;
        int pixSize = yv12.length;
        byte[] uTemp = new byte[pixSize / 2];
        System.arraycopy(yv12, ySize, uTemp, 0, ySize / 4);
        System.arraycopy(yv12, ySize, yv12, ySize * 5 / 4, ySize / 4);
        System.arraycopy(uTemp, 0, yv12, ySize, ySize / 4);
        return yv12;
    }

    private byte[] NV21toNV12(byte[] yuv420sp, int width, int height) {
        int ySize = width * height;
        int pixSize = yuv420sp.length;
        if (pixSize != ySize * 3 / 2) {
            throw new RuntimeException("unkonw pixel data");
        }
        byte[] yuv420p = new byte[pixSize];
        System.arraycopy(yuv420sp, 0, yuv420p, 0, ySize); //copy y

        int uPoint = ySize;
        int vPoint = ySize * 5 / 4;
        for (int i = ySize; i < pixSize; i++) {
            if (i % 2 == 0) { //copy u
                yuv420p[uPoint] = yuv420sp[i];
                uPoint++;
            } else { //copy y
                yuv420p[vPoint] = yuv420sp[i];
                vPoint++;
            }
        }
        return yuv420p;
    }

    public static void choosePreviewSize(Camera.Parameters parms, int width, int height) {
        // We should make sure that the requested MPEG size is less than the preferred
        // size, and has the same aspect ratio.
        Camera.Size ppsfv = parms.getPreferredPreviewSizeForVideo();
        if (ppsfv != null) {
            Log.e(TAG, "Camera preferred preview size for video is " +
                    ppsfv.width + "x" + ppsfv.height);
        }

        for (Camera.Size size : parms.getSupportedPreviewSizes()) {
            if (size.width == width && size.height == height) {
                parms.setPreviewSize(width, height);
                return;
            }
        }

        width = 0;
        height = 0;
        for (Camera.Size size : parms.getSupportedPreviewSizes()) {
            width = Math.max(width, size.width);
            height = Math.max(height, size.height);
        }
        parms.setPictureSize(width, height);
        Log.e(TAG, "set preview size to " + width + "x" + height);

        /*Log.e(TAG, "Unable to set preview size to " + width + "x" + height);
        if (ppsfv != null) {
            parms.setPreviewSize(ppsfv.width, ppsfv.height);
        }*/
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fontCamera.release();
    }
}
