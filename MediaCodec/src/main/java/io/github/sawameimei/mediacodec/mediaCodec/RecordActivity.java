package io.github.sawameimei.mediacodec.mediaCodec;

import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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
import java.nio.ByteBuffer;

import io.github.sawameimei.mediacodec.R;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class RecordActivity extends AppCompatActivity implements View.OnClickListener {

    private Camera fontCamera;
    private SurfaceView surfaceView;
    private SurfaceView previewSurface;
    private Button capture;
    private Button play;
    private boolean capturing;
    private Surface playSurface;

    private EncoderDispatcher encoderDispatcher = new EncoderDispatcher();
    private DecoderDispatcher decoderDispatcher = new DecoderDispatcher();
    private File recordingFile;

    public static final int PREVIEW_WIDTH = 1280;
    public static final int PREVIEW_HEIGHT = 720;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_2);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{RECORD_AUDIO, READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE, INTERNET, CAMERA}, 1);
        }
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/audio/");
        file.mkdirs();
        try {
            recordingFile = File.createTempFile("recording", ".mp4", file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        findViews();
    }

    private void findViews() {
        previewSurface = (SurfaceView) findViewById(R.id.previewSurface);
        capture = (Button) findViewById(R.id.capture);
        play = (Button) findViewById(R.id.play);
        //textureView2 = (TextureView) findViewById(R.id.textureView2);
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);

        capture.setOnClickListener(this);
        play.setOnClickListener(this);

        boolean hasCamera = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
        if (!hasCamera) {
            Toast.makeText(this, "没有摄像头！！", Toast.LENGTH_SHORT).show();
            return;
        }
        fontCamera = Camera.open();
        fontCamera.setDisplayOrientation(90);
        /**
         * 获取nv21的原始视频数据??
         */
        Camera.Parameters parameters = fontCamera.getParameters();
        parameters.setPreviewFormat(ImageFormat.NV21);
        parameters.setPreviewSize(PREVIEW_WIDTH, PREVIEW_HEIGHT);

        fontCamera.setParameters(parameters);
        fontCamera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                encoderDispatcher.encode(data);
            }
        });

        previewSurface.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
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

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                playSurface = holder.getSurface();
                decoderDispatcher.start();
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
    public void onClick(View v) {
        if (v == capture) {
            capturing = !capturing;
            if (!capturing) {
                encoderDispatcher.shutDown();
            } else {
                encoderDispatcher.start(recordingFile, PREVIEW_WIDTH, PREVIEW_HEIGHT);
            }
            capture.setText(capturing ? "停止录制" : "点我录制");
        } else if (v == play) {
            if (playSurface != null) {
                decoderDispatcher.decode(recordingFile, playSurface);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        encoderDispatcher.shutDown();
        decoderDispatcher.shutDown();
        fontCamera.release();
    }
}
