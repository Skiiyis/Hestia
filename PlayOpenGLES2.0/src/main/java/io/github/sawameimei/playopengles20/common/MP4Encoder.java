package io.github.sawameimei.playopengles20.common;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import static android.media.MediaCodec.CONFIGURE_FLAG_ENCODE;
import static io.github.sawameimei.playopengles20.common.MP4Encoder.EncoderThread.ENCODER;
import static io.github.sawameimei.playopengles20.common.MP4Encoder.EncoderThread.END;

/**
 * Created by huangmeng on 2017/11/28.
 */

public class MP4Encoder {

    public static final String TAG = "OpenGLES2.0";
    private static final int FRAME_INTERVAL = 1;
    private final int width;
    private final int height;
    private final File recordingFile;
    private final int frameRate;
    private final int bitRate;
    private MediaCodec mediaEncoder;
    private MediaMuxer mediaMuxer;
    private MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
    private int encoderVideoTrackIndex;
    private Surface inputSurface;
    private EncoderThread encoderThread;

    public MP4Encoder(int width, int height, int bitRate, int frameRate, File recordingFile) {
        this.width = width;
        this.height = height;
        this.recordingFile = recordingFile;
        this.bitRate = bitRate;
        this.frameRate = frameRate;
        this.encoderThread = new EncoderThread();
    }

    public void prepare() {
        MediaFormat format = MediaFormat.createVideoFormat("video/avc", width, height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 5);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, FRAME_INTERVAL);
        try {
            mediaEncoder = MediaCodec.createEncoderByType(format.getString(MediaFormat.KEY_MIME));
            mediaMuxer = new MediaMuxer(recordingFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        mediaEncoder.configure(format, null, null, CONFIGURE_FLAG_ENCODE);
        inputSurface = mediaEncoder.createInputSurface();
        mediaEncoder.start();
        encoderThread.start();
    }

    public Surface getInputSurface() {
        return inputSurface;
    }

    public void shutDown() {
        mediaEncoder.stop();
        mediaEncoder.release();
        mediaMuxer.stop();
        mediaMuxer.release();
        encoderThread.getEncoderHandler().sendEmptyMessage(END);
    }

    public void drainEncoder() {
        encoderThread.getEncoderHandler().sendEmptyMessage(ENCODER);
    }

    class EncoderThread implements Runnable {

        public static final int ENCODER = 1;
        public static final int END = 2;
        private EncoderHandler encoderHandler;

        class EncoderHandler extends Handler {

            public EncoderHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void dispatchMessage(Message msg) {
                switch (msg.what) {
                    case ENCODER:
                        drainEncoder();
                        break;
                    case END:
                        Looper.myLooper().quit();
                        break;
                    default:
                        break;
                }
            }

            public void drainEncoder() {
                int bufferIndex = mediaEncoder.dequeueOutputBuffer(bufferInfo, 0);
                if (bufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat outputFormat = mediaEncoder.getOutputFormat();
                    encoderVideoTrackIndex = mediaMuxer.addTrack(outputFormat);
                    mediaMuxer.start();
                } else if (bufferIndex > 0) {
                    ByteBuffer outputBuffer = mediaEncoder.getOutputBuffer(bufferIndex);
                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        bufferInfo.size = 0;
                    }
                    if (bufferInfo.size != 0) {
                        outputBuffer.position(bufferInfo.offset);
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                        mediaMuxer.writeSampleData(encoderVideoTrackIndex, outputBuffer, bufferInfo);
                        mediaEncoder.releaseOutputBuffer(bufferIndex, false);
                    } else {
                        return;
                    }
                } else if (bufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    return;
                }

                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.w(TAG, "reached end of stream unexpectedly");
                    return;
                }
            }
        }


        public void start() {
            new Thread(this).start();
        }

        @Override
        public void run() {
            Looper.prepare();
            encoderHandler = new EncoderHandler(Looper.myLooper());
            Looper.loop();
        }

        public EncoderHandler getEncoderHandler() {
            return encoderHandler;
        }
    }
}
