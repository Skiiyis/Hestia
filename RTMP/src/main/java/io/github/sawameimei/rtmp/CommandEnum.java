package io.github.sawameimei.rtmp;

import com.github.faucamp.simplertmp.amf.AmfNull;
import com.github.faucamp.simplertmp.amf.AmfObject;
import com.github.faucamp.simplertmp.io.ChunkStreamInfo;
import com.github.faucamp.simplertmp.io.RtmpSessionInfo;
import com.github.faucamp.simplertmp.packets.Command;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by huangmeng on 2017/12/25.
 */

public class CommandEnum {

    private static final Pattern RTMP_URL_PATTERN = Pattern.compile("^rtmp://([^/:]+)(:(\\d+))*/([^/]+)(/(.*))*$");

    public static Command connect(int transactionId, RtmpSessionInfo rtmpSessionInfo, String url) {
        Matcher matcher = RTMP_URL_PATTERN.matcher(url);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid RTMP URL. Must be in format: rtmp://host[:port]/application/streamName");
        }
        String tcUrl = url.substring(0, url.lastIndexOf('/'));
        String swfUrl = "";
        String pageUrl = "";
        String appName = matcher.group(4);

        Command invoke = new Command("connect", transactionId, rtmpSessionInfo.getChunkStreamInfo(ChunkStreamInfo.RTMP_CID_OVER_CONNECTION));
        invoke.getHeader().setMessageStreamId(0);
        AmfObject args = new AmfObject();
        args.setProperty("app", appName);
        args.setProperty("flashVer", "LNX 11,2,202,233"); // Flash player OS: Linux, version: 11.2.202.233
        args.setProperty("swfUrl", swfUrl);
        args.setProperty("tcUrl", tcUrl);
        args.setProperty("fpad", false);
        args.setProperty("capabilities", 239);
        args.setProperty("audioCodecs", 3575);
        args.setProperty("videoCodecs", 252);
        args.setProperty("videoFunction", 1);
        args.setProperty("pageUrl", pageUrl);
        args.setProperty("objectEncoding", 0);
        invoke.addData(args);
        return invoke;
    }

    public static Command createStream(RtmpSessionInfo rtmpSessionInfo, int transactionId) {
        final ChunkStreamInfo chunkStreamInfo = rtmpSessionInfo.getChunkStreamInfo(ChunkStreamInfo.RTMP_CID_OVER_CONNECTION);
        // Send createStream() command
        Command createStream = new Command("createStream", transactionId, chunkStreamInfo);
        return createStream;
    }

    public static Command play(int transactionId, int streamId, String path, boolean isLive) {
        Command play = new Command("play", transactionId);
        play.getHeader().setChunkStreamId(ChunkStreamInfo.RTMP_STREAM_CHANNEL);
        play.getHeader().setMessageStreamId(streamId);
        play.addData(new AmfNull());
        play.addData(path); // what to play
        play.addData(isLive ? -1 : 0); // play start position
        play.addData(isLive ? -1 : -2); // play duration
        return play;
    }
}
