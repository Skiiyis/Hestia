package io.github.sawameimei.mediacodec;

import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private File recordingFile;
    private Camera fontCamera;
    private boolean capturing;
    private MediaRecorder mediaRecorder;
    private MediaCodec mediaDecoder;
    public Surface playSurface;
    private SurfaceView surfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{RECORD_AUDIO, READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE, INTERNET, CAMERA}, 1);
        }

        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/audio/");
        file.mkdirs();
        try {
            recordingFile = File.createTempFile("recording", ".mp4", file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        findViews();
    }

    private TextureView textureView;
    private Button capture;
    private Button play;
    //private TextureView textureView2;

    /**
     * Find the Views in the layout<br />
     * <br />
     * Auto-created on 2017-09-01 16:12:09 by Android Layout Finder
     * (http://www.buzzingandroid.com/tools/android-layout-finder)
     */
    private void findViews() {
        textureView = (TextureView) findViewById(R.id.textureView);
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

        /**
         * 获取nv21的原始视频数据??
         */
        Camera.Parameters parameters = fontCamera.getParameters();
        parameters.setPreviewFormat(ImageFormat.NV21);
        fontCamera.setParameters(parameters);
        fontCamera.setOneShotPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                //data为原始视频数据 NV21
            }
        });

        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                try {
                    fontCamera.setPreviewTexture(surface);
                    fontCamera.startPreview();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                fontCamera.stopPreview();
                fontCamera.release();
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                playSurface = holder.getSurface();
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
            capture(capturing);
            capture.setText(capturing ? "停止录制" : "点我录制");
        } else if (v == play) {
            new Thread() {
                @Override
                public void run() {
                    play();
                    //play(new MediaPlayer(),playSurface);
                }
            }.start();
        }
    }

    private void play(MediaPlayer mediaPlayer, Surface surface) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(recordingFile.getAbsolutePath());
            mediaPlayer.setSurface(surface);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            final MediaPlayer finalMediaPlayer = mediaPlayer;
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    finalMediaPlayer.start();
                }
            });
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void play() {
        MediaExtractor mediaExtractor = new MediaExtractor();
        MediaFormat trackFormat;
        try {
            mediaExtractor.setDataSource(recordingFile.getAbsolutePath());
            for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
                trackFormat = mediaExtractor.getTrackFormat(i);
                String mimeType = trackFormat.getString(MediaFormat.KEY_MIME);
                if (mimeType.startsWith("video/")) {
                    mediaExtractor.selectTrack(i);
                    mediaDecoder = MediaCodec.createDecoderByType(mimeType);
                    mediaDecoder.configure(trackFormat, playSurface, null, 0);
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (mediaDecoder == null) {
            Log.e("mediaDecoder", "create mediaDecoder failed");
            return;
        }

        //mediaDecoder.setOutputSurface(surface);
        mediaDecoder.start();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        int sampleSize = 0;
        do {
            int bufferIndex = mediaDecoder.dequeueInputBuffer(-1);
            if (bufferIndex >= 0) {
                ByteBuffer inputBuffer = mediaDecoder.getInputBuffer(bufferIndex);
                inputBuffer.clear();
                sampleSize = mediaExtractor.readSampleData(inputBuffer, 0);
            } else {
                continue;
            }

            if (sampleSize > 0) {
                mediaDecoder.queueInputBuffer(bufferIndex, 0, sampleSize, mediaExtractor.getSampleTime(), 0);
                mediaExtractor.advance();
            }

            int outIndex = mediaDecoder.dequeueOutputBuffer(info, 1000L);
            if (outIndex >= 0) {
                mediaDecoder.releaseOutputBuffer(outIndex, true);
            }
        } while (sampleSize > 0);
        mediaDecoder.stop();
        mediaDecoder.release();
        mediaDecoder = null;
        mediaExtractor.release();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaDecoder != null) {
            mediaDecoder.stop();
            mediaDecoder.release();
            mediaDecoder = null;
        }
    }

    private void capture(boolean capturing) {
        if (capturing) {
            mediaRecorder = new MediaRecorder();
            fontCamera.unlock();
            mediaRecorder.setCamera(fontCamera);
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
            mediaRecorder.setOutputFile(recordingFile.getAbsolutePath());
            mediaRecorder.setPreviewDisplay(new Surface(textureView.getSurfaceTexture()));

            new Thread() {
                @Override
                public void run() {
                    try {
                        mediaRecorder.prepare();
                        mediaRecorder.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }.start();

        } else {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                fontCamera.lock();
            }
        }
    }
}
