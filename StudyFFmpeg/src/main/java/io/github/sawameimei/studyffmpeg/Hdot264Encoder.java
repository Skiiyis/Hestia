package io.github.sawameimei.studyffmpeg;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import static android.media.MediaCodec.CONFIGURE_FLAG_ENCODE;

/**
 * Created by huangmeng on 2017/9/12.
 */

public class Hdot264Encoder {

    private static final int FRAME_INTERVAL = 1;

    private final File recordingFile;
    private MediaCodec mediaEncoder;
    private MediaMuxer mediaMuxer;
    private MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
    private int encoderVideoTrackIndex = -1;
    private int width;
    private int height;
    private long frameIndex;
    private int frameRate;

    public Hdot264Encoder(File recordingFile, int width, int height, int frameRate) {
        this.width = width;
        this.height = height;
        this.recordingFile = recordingFile;
        this.frameRate = frameRate;
    }

    public void prepare() {
        MediaFormat format = MediaFormat.createVideoFormat("video/avc", width, height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
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
        mediaEncoder.start();
        frameIndex = 0;
    }

    public void shutDown() {
        mediaEncoder.stop();
        mediaEncoder.release();
        mediaMuxer.stop();
        mediaMuxer.release();
    }

    public void encode(byte[] yuv21) {
        byte[] yuv12 = NV21ToNV12(yuv21);
        int inputIndex = mediaEncoder.dequeueInputBuffer(-1);
        ByteBuffer inputBuffer = mediaEncoder.getInputBuffer(inputIndex);
        inputBuffer.clear();
        inputBuffer.put(yuv12);
        mediaEncoder.queueInputBuffer(inputIndex, 0, yuv12.length, System.nanoTime()/1000, 0);
        int bufferIndex = mediaEncoder.dequeueOutputBuffer(bufferInfo, 10000);
        if (bufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            MediaFormat outputFormat = mediaEncoder.getOutputFormat();
            encoderVideoTrackIndex = mediaMuxer.addTrack(outputFormat);
            mediaMuxer.start();
        } else if (bufferIndex > 0) {
            ByteBuffer outputBuffer = mediaEncoder.getOutputBuffer(bufferIndex);
            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                bufferInfo.size = 0;
            }
            outputBuffer.position(bufferInfo.offset);
            outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
            mediaMuxer.writeSampleData(encoderVideoTrackIndex, outputBuffer, bufferInfo);
            mediaEncoder.releaseOutputBuffer(bufferIndex, false);
        }
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

    private long computePresentationTime() {
        frameIndex++;
        return frameIndex * 1000000 / frameRate;
    }
}
