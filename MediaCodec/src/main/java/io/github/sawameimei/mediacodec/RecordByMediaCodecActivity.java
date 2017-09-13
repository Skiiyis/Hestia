package io.github.sawameimei.mediacodec;

import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.media.MediaCodec.CONFIGURE_FLAG_ENCODE;

/**
 * 使用mediacodec和mediaExtractor来录制视频
 */
public class RecordByMediaCodecActivity extends AppCompatActivity implements View.OnClickListener {

    private File recordingFile;
    private Camera fontCamera;
    private boolean capturing;
    private MediaCodec mediaDecoder;
    private Surface playSurface;
    private SurfaceView surfaceView;
    private SurfaceView previewSurface;
    private Button capture;
    private Button play;
    private MediaMuxer mediaMuxer;
    private MediaCodec encoder = null;
    private Encoder encoderThread;

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
        } catch (Exception e) {
            e.printStackTrace();
        }
        findViews();
    }
    //private TextureView textureView2;

    /**
     * Find the Views in the layout<br />
     * <br />
     * Auto-created on 2017-09-01 16:12:09 by Android Layout Finder
     * (http://www.buzzingandroid.com/tools/android-layout-finder)
     */
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

        /**
         * 获取nv21的原始视频数据??
         */
        Camera.Parameters parameters = fontCamera.getParameters();
        parameters.setPreviewFormat(ImageFormat.NV21);
        parameters.setPreviewSize(1280, 720);
        prepareEncoder(1280, 720);
        fontCamera.setParameters(parameters);
        fontCamera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                encoderThread.addData(data);
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
                fontCamera.release();
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
            if (!capturing) {
                encoderThread.shutdown();
            } else {
                new Thread(encoderThread).start();
            }
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

    private void prepareEncoder(int width, int height) {
        MediaFormat format = MediaFormat.createVideoFormat("video/avc", width, height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
        format.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 5);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        try {
            encoder = MediaCodec.createEncoderByType(format.getString(MediaFormat.KEY_MIME));
            mediaMuxer = new MediaMuxer(recordingFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        encoder.configure(format, null, null, CONFIGURE_FLAG_ENCODE);
        encoder.start();
        encoderThread = new Encoder(encoder, mediaMuxer, width, height);
    }

    private static class Encoder implements Runnable {

        private final MediaCodec encoder;
        private final MediaMuxer mediaMuxer;
        private final int width;
        private final int height;
        private boolean capturing = true;
        private BlockingQueue<byte[]> data;

        public Encoder(MediaCodec encoder, MediaMuxer mediaMuxer, int width, int height) {
            this.encoder = encoder;
            this.mediaMuxer = mediaMuxer;
            data = new ArrayBlockingQueue<>(10);
            this.width = width;
            this.height = height;
        }

        public void addData(byte[] b) {
            if (data.size() >= 10) {
                data.poll();
            }
            this.data.add(b);
        }

        private byte[] NV21ToNV12(byte[] nv21) {
            int framesize = width * height;
            byte[] nv12 = new byte[nv21.length];
            int i = 0, j = 0;
            System.arraycopy(nv21, 0, nv12, 0, framesize);
            for (i = 0; i < framesize; i++) {
                nv12[i] = nv21[i];
            }
            for (j = 0; j < framesize / 2; j += 2) {
                nv12[framesize + j - 1] = nv21[j + framesize];
            }
            for (j = 0; j < framesize / 2; j += 2) {
                nv12[framesize + j] = nv21[j + framesize - 1];
            }
            return nv12;
        }

        public void shutdown() {
            this.capturing = false;
        }

        @Override
        public void run() {
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int encoderVideoTrackIndex = -1;
            while (true) {
                byte[] data = this.data.poll();
                if (data == null) {
                    continue;
                }
                data = NV21ToNV12(data);
                int inputIndex = encoder.dequeueInputBuffer(-1);
                ByteBuffer inputBuffer = encoder.getInputBuffer(inputIndex);
                inputBuffer.clear();
                inputBuffer.put(data);
                encoder.queueInputBuffer(inputIndex, 0, data.length, 0, 0);

                int bufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000);
                if (bufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat outputFormat = encoder.getOutputFormat();
                    encoderVideoTrackIndex = mediaMuxer.addTrack(outputFormat);
                    mediaMuxer.start();
                } else if (bufferIndex > 0) {
                    ByteBuffer outputBuffer = encoder.getOutputBuffer(bufferIndex);
                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        bufferInfo.size = 0;
                    }
                    outputBuffer.position(bufferInfo.offset);
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                    mediaMuxer.writeSampleData(encoderVideoTrackIndex, outputBuffer, bufferInfo);
                    encoder.releaseOutputBuffer(bufferIndex, false);
                }

                if (!capturing) {
                    encoder.stop();
                    encoder.release();
                    mediaMuxer.stop();
                    mediaMuxer.release();
                    break;
                }

                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break;
                }
            }
        }
    }
}
