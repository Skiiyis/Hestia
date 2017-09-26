package io.github.sawameimei.ffmpeg;

/**
 * Created by huangmeng on 2017/9/15.
 */

public class FFmpegBridge {

    static {
        System.loadLibrary("ffmpeg-bridge");
        System.loadLibrary("avcodec");
        System.loadLibrary("avformat");
        System.loadLibrary("avutil");
        System.loadLibrary("swscale");
        System.loadLibrary("fdk-aac");
    }

    public static native int commandRun(String[] cmd);

    public static native String supportedProtocol();

    public static native String supportedAVFormat();

    public static native String supportedAVCodecInfo();

    public static native String supportedAVFilterInfo();

    public static native String ffmpegConfigInfo();

    public static native void decode(String filePath, onDecodeFrame callBack);

    public interface onDecodeFrame {
        void onDecode(byte[] yuv420p);
    }

    public static native int prepareEncoder(String outputUrl, int width, int height, String mineType, int bitRate, int frameRate);

    public static native int encode(byte[] yuv420p);

    public static native int release();

    public static void muxing(String inputFileVideo, String inputFileAudio, String outputFile) {
        muxing(inputFileVideo, outputFile);
    }

    public static native void muxing(String inputFileVideo, String outputFile);
}
