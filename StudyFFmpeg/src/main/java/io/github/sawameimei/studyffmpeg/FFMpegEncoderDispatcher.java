package io.github.sawameimei.studyffmpeg;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import io.github.sawameimei.ffmpeg.FFmpegBridge;

import static io.github.sawameimei.studyffmpeg.EncoderDispatcher.DispatchHandler.ACTION_ENCODE;
import static io.github.sawameimei.studyffmpeg.EncoderDispatcher.DispatchHandler.ACTION_SHUTDOWN;
import static io.github.sawameimei.studyffmpeg.FFMpegEncoderDispatcher.DispatchHandler.ACTION_END_ENCODE;
import static io.github.sawameimei.studyffmpeg.FFMpegEncoderDispatcher.DispatchHandler.ACTION_MUXING;

/**
 * Created by huangmeng on 2017/9/12.
 */

public class FFMpegEncoderDispatcher implements Runnable {

    private Handler handler;
    private boolean isShutDown = true;

    @Override
    public void run() {
        Looper.prepare();
        handler = new DispatchHandler(Looper.myLooper(), this);
        isShutDown = false;
        Looper.loop();
    }

    public void start(File recordingFile, int width, int height, int frameRate) {
        FFmpegBridge.prepareEncoder(recordingFile.getAbsolutePath(), width, height, "mp4", width * height * 5, frameRate / 1000);
        new Thread(this).start();
    }

    public void shutDown() {
        if (handler != null) {
            handler.sendEmptyMessage(ACTION_SHUTDOWN);
        }
    }

    public void endEncode() {
        if (handler != null) {
            handler.sendEmptyMessage(ACTION_END_ENCODE);
        }
    }

    public void muxing(String recordingVideoFilePath, String recordingAudioFilePath, String outputFilePath) {
        if (isShutDown) {
            return;
        }
        if (handler == null) {
            throw new IllegalStateException("must use start() method first");
        }
        Map<String, String> map = new HashMap<>();
        map.put("ifv", recordingVideoFilePath);
        map.put("ifa", recordingAudioFilePath);
        map.put("of", outputFilePath);
        Message message = Message.obtain();
        message.obj = map;
        message.what = ACTION_MUXING;
        handler.sendMessage(message);
    }

    public void encode(byte[] yuv21) {
        if (isShutDown) {
            return;
        }
        if (handler == null) {
            throw new IllegalStateException("must use start() method first");
        }
        Message message = Message.obtain();
        message.what = ACTION_ENCODE;
        message.obj = yuv21;
        handler.sendMessage(message);
    }

    public static class DispatchHandler extends Handler {

        public static final int ACTION_SHUTDOWN = -1;
        public static final int ACTION_ENCODE = 1;
        public static final int ACTION_END_ENCODE = 2;
        public static final int ACTION_MUXING = 3;

        private final FFMpegEncoderDispatcher dispatcher;

        public DispatchHandler(Looper looper, FFMpegEncoderDispatcher dispatcher) {
            super(looper);
            this.dispatcher = dispatcher;
        }

        public void shutDown() {
            Looper.myLooper().quit();
            if (dispatcher.handler != null) {
                dispatcher.handler = null;
            }
        }

        public void muxing(String recordingVideoFilePath, String recordingAudioFilePath, String outputFilePath) {
            FFmpegBridge.muxing(recordingVideoFilePath, recordingAudioFilePath, outputFilePath);
        }

        @Override
        public void dispatchMessage(Message msg) {
            switch (msg.what) {
                case ACTION_SHUTDOWN:
                    shutDown();
                    dispatcher.isShutDown = true;
                    break;
                case ACTION_ENCODE:
                    FFmpegBridge.encode((byte[]) msg.obj);
                    break;
                case ACTION_END_ENCODE:
                    FFmpegBridge.release();
                    break;
                case ACTION_MUXING:
                    Map<String, String> map = (Map<String, String>) msg.obj;
                    muxing(map.get("ifv"), map.get("ifa"), map.get("of"));
                    break;
                default:
                    break;
            }
        }
    }
}
