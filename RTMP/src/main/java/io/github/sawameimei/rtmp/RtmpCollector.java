package io.github.sawameimei.rtmp;

import com.github.faucamp.simplertmp.Logger;
import com.github.faucamp.simplertmp.amf.AmfNumber;
import com.github.faucamp.simplertmp.amf.AmfObject;
import com.github.faucamp.simplertmp.amf.AmfString;
import com.github.faucamp.simplertmp.io.ChunkStreamInfo;
import com.github.faucamp.simplertmp.packets.Abort;
import com.github.faucamp.simplertmp.packets.Command;
import com.github.faucamp.simplertmp.packets.ContentData;
import com.github.faucamp.simplertmp.packets.Data;
import com.github.faucamp.simplertmp.packets.RtmpHeader;
import com.github.faucamp.simplertmp.packets.RtmpPacket;
import com.github.faucamp.simplertmp.packets.UserControl;
import com.github.faucamp.simplertmp.packets.WindowAckSize;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by huangmeng on 2017/12/26.
 */
public class RtmpCollector extends RtmpConnection {

    private static final String TAG = "RtmpCollector";
    private static final int CREATE_STREAM_STEP_TIME = 5_000;

    private boolean connected;
    private List<Integer> streamIdList = new ArrayList<>();
    private int currentSteamId = -1;

    public RtmpCollector(String url) throws IllegalArgumentException {
        super(url);
    }

    public RtmpCollector connect() throws IOException, IllegalStateException {
        super.connect();
        connected = true;
        return this;
    }

    public RtmpCollector createStream(CreateStreamCallback callback) throws IOException {
        checkConnected();
        Logger.d(TAG, "createStream(): Sending createStream command...");
        sendRtmpPacket(CommandEnum.createStream(rtmpSessionInfo, ++transactionCounter));

        long startTime = System.currentTimeMillis();
        boolean createStream = false;
        while (System.currentTimeMillis() - startTime < CREATE_STREAM_STEP_TIME) {
            RtmpPacket packet = readPacket();
            if (packet == null) {
                continue;
            }
            if (!packet.getHeader().getMessageType().equals(RtmpHeader.MessageType.COMMAND_AMF0)) {
                //throw new IllegalStateException("Error RTMP message type");
                continue;
            }
            Command command = (Command) packet;
            if (!"_result".equals(command.getCommandName())) {
                //throw new IllegalStateException("Error RTMP message type");
                continue;
            }
            if (!"createStream".equals(rtmpSessionInfo.takeInvokedCommand(command.getTransactionId()))) {
                //throw new IllegalStateException("Error RTMP message type");
                continue;
            }
            int streamId = (int) ((AmfNumber) command.getData().get(1)).getValue();
            Logger.d(TAG, "createStream(): createStream success，streamId=" + streamId);
            streamIdList.add(streamId);
            if (callback != null) {
                callback.created(streamId);
            }
            createStream = true;
            break;
        }
        if (!createStream) {
            throw new SocketTimeoutException("createStream timeout");
        }
        return this;
    }

    public RtmpCollector playFile() {
        checkConnected();
        checkStream();
        try {
            sendRtmpPacket(CommandEnum.play(transactionCounter, currentSteamId, streamName, false));
            Logger.d(TAG, "playFile(): send play packet，streamName=" + streamName + "，streamId = " + currentSteamId + "，transactionId=" + transactionCounter);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public boolean handlePacket = false;

    public RtmpCollector playLive() throws IOException {
        checkConnected();
        checkStream();
        handlePacket = true;
        try {
            sendRtmpPacket(CommandEnum.play(transactionCounter, currentSteamId, streamName, true));
            Logger.d(TAG, "playLive(): send play packet，streamName=" + streamName + "，streamId = " + currentSteamId + "，transactionId=" + transactionCounter);
        } catch (IOException e) {
            e.printStackTrace();
        }

        handlePacket = true;
        while (handlePacket) {
            handlePacket();
        }
        return this;
    }

    public RtmpCollector makeCurrent() {
        if (!streamIdList.isEmpty()) {
            currentSteamId = streamIdList.get(0);
            return this;
        }
        throw new IllegalArgumentException("could not find steamId");
    }

    public RtmpCollector makeCurrent(int streamId) {
        if (streamIdList.contains(streamId)) {
            currentSteamId = streamId;
            return this;
        }
        throw new IllegalArgumentException("could not find steamId");
    }

    private void checkConnected() {
        if (!connected) {
            throw new IllegalStateException("must connect first");
        }
    }

    private void checkStream() {
        if (currentSteamId == -1) {
            throw new IllegalStateException("must createStream and makeCurrent first");
        }
    }

    public RtmpCollector release() throws IOException {
        super.release();
        connected = false;
        return this;
    }

    public interface CreateStreamCallback {
        void created(int streamId);
    }

    private void handlePacket() throws IOException {
        RtmpPacket rtmpPacket = readPacket();
        switch (rtmpPacket.getHeader().getMessageType()) {
            case ABORT:
                rtmpSessionInfo.getChunkStreamInfo(((Abort) rtmpPacket).getChunkStreamId()).clearStoredChunks();
                break;
            case USER_CONTROL_MESSAGE: {
                UserControl ping = (UserControl) rtmpPacket;
                switch (ping.getType()) {
                    case PING_REQUEST: {
                        ChunkStreamInfo channelInfo = rtmpSessionInfo.getChunkStreamInfo(ChunkStreamInfo.RTMP_CID_PROTOCOL_CONTROL);
                        Logger.d(TAG, "handlePacket(): Sending PONG reply..");
                        UserControl pong = new UserControl(ping, channelInfo);
                        sendRtmpPacket(pong);
                        break;
                    }
                    case STREAM_EOF:
                        Logger.d(TAG, "handlePacket(): Stream EOF reached, closing RTMP writer...");
                        //rtmpStreamWriter.close();
                        break;
                }
                break;
            }
            case WINDOW_ACKNOWLEDGEMENT_SIZE: {
                WindowAckSize windowAckSize = (WindowAckSize) rtmpPacket;
                Logger.d(TAG, "handlePacket(): Setting acknowledgement window size to: " + windowAckSize.getAcknowledgementWindowSize());
                rtmpSessionInfo.setAcknowledgmentWindowSize(windowAckSize.getAcknowledgementWindowSize());
                break;
            }
            case COMMAND_AMF0:
                Command command = (Command) rtmpPacket;
                if ("onStatus".equals(command.getCommandName())) {
                    String code = ((AmfString) ((AmfObject) command.getData().get(1)).getProperty("code")).getValue();
                    Logger.d(TAG, "handlePacket(): command code: " + code);
                }
                break;
            case DATA_AMF0: {
                Data data = (Data) rtmpPacket;
                if ("onMetaData".equals(data.getType())) {
                    //rtmpStreamWriter.write(data);
                    Logger.d(TAG, data.toString());
                }
                break;
            }
            case AUDIO:
            case VIDEO:
                Logger.d(TAG, "handlePacket(): timeStamp:" + rtmpPacket.getHeader().getAbsoluteTimestamp() + "，contentDataLength:" + ((ContentData) rtmpPacket).size());
                break;
            case SET_CHUNK_SIZE:
                break;
            default:
                Logger.d(TAG, "handlePacket(): Not handling unimplemented/unknown packet of type: " + rtmpPacket.getHeader().getMessageType());
                break;
        }
    }
}
