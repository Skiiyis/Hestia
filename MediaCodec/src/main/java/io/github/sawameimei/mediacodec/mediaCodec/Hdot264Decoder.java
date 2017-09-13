package io.github.sawameimei.mediacodec.mediaCodec;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by huangmeng on 2017/9/12.
 */

public class Hdot264Decoder {

    private final Surface playSurface;
    private MediaExtractor mediaExtractor;
    private File recordingFile;
    private MediaCodec mediaDecoder;
    private MediaCodec.BufferInfo bufferInfo;

    public Hdot264Decoder(File recordingFile, Surface playSurface) {
        this.recordingFile = recordingFile;
        this.playSurface = playSurface;
    }

    public void prepare() {
        mediaExtractor = new MediaExtractor();
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
        bufferInfo = new MediaCodec.BufferInfo();
    }

    public int decode() {
        int sampleSize = 0;
        int bufferIndex = mediaDecoder.dequeueInputBuffer(-1);
        if (bufferIndex >= 0) {
            ByteBuffer inputBuffer = mediaDecoder.getInputBuffer(bufferIndex);
            inputBuffer.clear();
            sampleSize = mediaExtractor.readSampleData(inputBuffer, 0);
        } else {
            return sampleSize;
        }
        if (sampleSize > 0) {
            mediaDecoder.queueInputBuffer(bufferIndex, 0, sampleSize, mediaExtractor.getSampleTime(), 0);
            mediaExtractor.advance();
        }
        int outIndex = mediaDecoder.dequeueOutputBuffer(bufferInfo, 1000L);
        if (outIndex >= 0) {
            mediaDecoder.releaseOutputBuffer(outIndex, true);
        }
        return sampleSize;
    }

    public void shutDown() {
        mediaDecoder.stop();
        mediaDecoder.release();
        mediaExtractor.release();
    }
}
