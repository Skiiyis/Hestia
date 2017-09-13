package io.github.sawameimei.mediacodec.mediaCodec;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.File;

import static io.github.sawameimei.mediacodec.mediaCodec.EncoderDispatcher.DispatchHandler.ACTION_ENCODE;
import static io.github.sawameimei.mediacodec.mediaCodec.EncoderDispatcher.DispatchHandler.ACTION_SHUTDOWN;

/**
 * Created by huangmeng on 2017/9/12.
 */

public class EncoderDispatcher implements Runnable {

    private Handler handler;
    private Hdot264Encoder encoder;
    private boolean isShutDown = true;

    @Override
    public void run() {
        Looper.prepare();
        handler = new DispatchHandler(Looper.myLooper(), this);
        isShutDown = false;
        Looper.loop();
    }

    public void start(File recordingFile, int width, int height) {
        encoder = new Hdot264Encoder(recordingFile, width, height);
        encoder.prepare();
        new Thread(this).start();
    }

    public void shutDown() {
        if (handler != null) {
            handler.sendEmptyMessage(ACTION_SHUTDOWN);
        }
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
        private final EncoderDispatcher dispatcher;

        public DispatchHandler(Looper looper, EncoderDispatcher dispatcher) {
            super(looper);
            this.dispatcher = dispatcher;
        }

        public void shutDown() {
            if (dispatcher.encoder != null) {
                dispatcher.encoder.shutDown();
            }
            Looper.myLooper().quit();
            if (dispatcher.handler != null) {
                dispatcher.handler = null;
            }
        }

        @Override
        public void dispatchMessage(Message msg) {
            switch (msg.what) {
                case ACTION_SHUTDOWN:
                    shutDown();
                    dispatcher.isShutDown = true;
                    break;
                case ACTION_ENCODE:
                    dispatcher.encoder.encode((byte[]) msg.obj);
                    break;
                default:
                    break;
            }
        }
    }
}
