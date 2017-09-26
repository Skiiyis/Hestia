package io.github.sawameimei.studyffmpeg;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Surface;

import java.io.File;

import static io.github.sawameimei.studyffmpeg.DecoderDispatcher.DecoderHandler.ACTION_SHUTDOWN;
import static io.github.sawameimei.studyffmpeg.EncoderDispatcher.DispatchHandler.ACTION_ENCODE;

/**
 * Created by huangmeng on 2017/9/13.
 */

public class DecoderDispatcher implements Runnable {

    private DecoderHandler handler;
    private boolean isShutDown;
    private Hdot264Decoder decoder;

    @Override
    public void run() {
        Looper.prepare();
        handler = new DecoderHandler(Looper.myLooper(), this);
        isShutDown = false;
        Looper.loop();
    }

    public void start() {
        new Thread(this).start();
    }

    public void shutDown() {
        if (handler != null) {
            handler.sendEmptyMessage(ACTION_SHUTDOWN);
        }
    }

    public void decode(File recordingFile, Surface surface) {
        if (isShutDown) {
            return;
        }
        if (handler == null) {
            throw new IllegalStateException("must use start() method first");
        }
        if (decoder != null) {
            decoder.shutDown();
        }
        decoder = new Hdot264Decoder(recordingFile, surface);
        decoder.prepare();
        handler.sendEmptyMessage(ACTION_ENCODE);
    }

    public static class DecoderHandler extends Handler {

        private final DecoderDispatcher dispatcher;
        public static final int ACTION_SHUTDOWN = -1;
        public static final int ACTION_DECODER = 1;

        public DecoderHandler(Looper looper, DecoderDispatcher dispatcher) {
            super(looper);
            this.dispatcher = dispatcher;
        }

        private void shutDown() {
            if (dispatcher.decoder != null) {
                dispatcher.decoder.shutDown();
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
                case ACTION_DECODER:
                    int sampleSize = dispatcher.decoder.decode();
                    if (sampleSize > 0) {
                        sendEmptyMessage(ACTION_DECODER);
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
