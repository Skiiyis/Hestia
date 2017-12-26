package io.github.sawameimei.rtmp;

import com.github.faucamp.simplertmp.Logger;
import com.github.faucamp.simplertmp.io.ChunkStreamInfo;
import com.github.faucamp.simplertmp.io.RtmpDecoder;
import com.github.faucamp.simplertmp.io.RtmpSessionInfo;
import com.github.faucamp.simplertmp.packets.Abort;
import com.github.faucamp.simplertmp.packets.Acknowledgement;
import com.github.faucamp.simplertmp.packets.Audio;
import com.github.faucamp.simplertmp.packets.Command;
import com.github.faucamp.simplertmp.packets.Data;
import com.github.faucamp.simplertmp.packets.Handshake;
import com.github.faucamp.simplertmp.packets.RtmpHeader;
import com.github.faucamp.simplertmp.packets.RtmpPacket;
import com.github.faucamp.simplertmp.packets.SetChunkSize;
import com.github.faucamp.simplertmp.packets.SetPeerBandwidth;
import com.github.faucamp.simplertmp.packets.UserControl;
import com.github.faucamp.simplertmp.packets.Video;
import com.github.faucamp.simplertmp.packets.WindowAckSize;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by huangmeng on 2017/12/25.
 */

public class RtmpConnection {

    private static final String TAG = "RtmpConnection";
    private static final Pattern RTMP_URL_PATTERN = Pattern.compile("^rtmp://([^/:]+)(:(\\d+))*/([^/]+)(/(.*))*$");

    private final String tcUrl;
    private final String swfUrl;
    private final String pageUrl;
    private final String host;
    private final int port;
    private final String appName;

    protected final String streamName;
    protected final String connectUrl;

    protected RtmpSessionInfo rtmpSessionInfo;
    protected Socket socket;
    protected BufferedInputStream inputStream;
    protected BufferedOutputStream outputStream;
    protected int transactionCounter;

    private static final int CONNECT_STEP_TIME = 5_000;

    public RtmpConnection(String url) throws IllegalArgumentException {
        this.connectUrl = url;
        Matcher matcher = RTMP_URL_PATTERN.matcher(url);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid RTMP URL. Must be in format: rtmp://host[:port]/application/streamName");
        }
        tcUrl = url.substring(0, url.lastIndexOf('/'));
        swfUrl = "";
        pageUrl = "";
        host = matcher.group(1);
        String portStr = matcher.group(3);
        port = portStr != null ? Integer.parseInt(portStr) : 1935;
        appName = matcher.group(4);
        streamName = matcher.group(6);

        if (streamName == null || appName == null) {
            throw new IllegalArgumentException(
                    "Invalid RTMP URL. Must be in format: rtmp://host[:port]/application/streamName");
        }
    }

    /**
     * connect
     *
     * @return
     * @throws IOException
     */
    protected RtmpConnection connect() throws IOException, IllegalStateException {
        rtmpSessionInfo = new RtmpSessionInfo();
        socket = new Socket();
        SocketAddress socketAddress = new InetSocketAddress(host, port);
        socket.connect(socketAddress, CONNECT_STEP_TIME);
        inputStream = new BufferedInputStream(socket.getInputStream());
        outputStream = new BufferedOutputStream(socket.getOutputStream());

        Logger.d(TAG, "connect(): socket connection established, doing handhake...");
        handshake(inputStream, outputStream);

        Logger.d(TAG, "connect(): socket connected，handshake done, send connect command...");
        sendRtmpPacket(CommandEnum.connect(++transactionCounter, rtmpSessionInfo, connectUrl));

        boolean setPeerBandWidth = false;
        boolean setAcknowledgementSize = false;

        boolean connectFailed = true;
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < CONNECT_STEP_TIME) {
            RtmpPacket packet = readPacket();
            if (packet == null) {
                continue;
            }
            if (packet.getHeader().getMessageType().equals(RtmpHeader.MessageType.SET_PEER_BANDWIDTH)) {
                SetPeerBandwidth bw = (SetPeerBandwidth) packet;
                int acknowledgementWindowSize = bw.getAcknowledgementWindowSize();
                rtmpSessionInfo.setAcknowledgmentWindowSize(acknowledgementWindowSize);
                socket.setSendBufferSize(acknowledgementWindowSize);
                Logger.d(TAG, "connect(): set bandwidth : " + acknowledgementWindowSize);

                ChunkStreamInfo chunkStreamInfo = rtmpSessionInfo.getChunkStreamInfo(ChunkStreamInfo.RTMP_CID_PROTOCOL_CONTROL);
                Logger.d(TAG, "connect(): send acknowledgement window size: " + acknowledgementWindowSize);
                sendRtmpPacket(new WindowAckSize(acknowledgementWindowSize, chunkStreamInfo));
                setPeerBandWidth = true;
            } else if (packet.getHeader().getMessageType().equals(RtmpHeader.MessageType.WINDOW_ACKNOWLEDGEMENT_SIZE)) {
                WindowAckSize windowAckSize = (WindowAckSize) packet;
                int size = windowAckSize.getAcknowledgementWindowSize();
                Logger.d(TAG, "connect(): setting acknowledgement window size: " + size);
                rtmpSessionInfo.setAcknowledgmentWindowSize(size);
                setAcknowledgementSize = true;
            } else {
                release();
                throw new IllegalStateException("Error RTMP message type");
            }
            if (setPeerBandWidth && setAcknowledgementSize) {
                connectFailed = false;
                break;
            }
        }
        if (connectFailed) {
            release();
            throw new SocketTimeoutException("connect(): socket timeout");
        }

        /*connectFailed = true;
        startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < CONNECT_STEP_TIME) {
            RtmpPacket packet = rtmpDecoder.readPacket(inputStream);
            if (packet == null) {
                continue;
            }
            if (!packet.getHeader().getMessageType().equals(RtmpHeader.MessageType.USER_CONTROL_MESSAGE)) {
                release();
                throw new IllegalStateException("Error RTMP message type");
            }
            UserControl userControlPacket = (UserControl) packet;
            if (!userControlPacket.getType().equals(UserControl.Type.STREAM_BEGIN)) {
                release();
                throw new IllegalStateException("Error RTMP message type");
            }
            int streamId = userControlPacket.getFirstEventData();
            Logger.d(TAG, "connect(): stream begin : " + streamId + ", the streamId must be 0");
            connectFailed = false;
        }
        if (connectFailed) {
            release();
            throw new SocketTimeoutException("connect(): socket timeout");
        }*/

        connectFailed = true;
        startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < CONNECT_STEP_TIME) {
            RtmpPacket packet = readPacket();
            if (packet == null) {
                continue;
            }
            if (!packet.getHeader().getMessageType().equals(RtmpHeader.MessageType.COMMAND_AMF0)) {
                /*release();
                throw new IllegalStateException("Error RTMP message type");*/
                continue;
            }
            Command command = (Command) packet;
            if (!"_result".equals(command.getCommandName())) {
                /*release();
                throw new IllegalStateException("Error RTMP message type");*/
                continue;
            }
            if (!"connect".equals(rtmpSessionInfo.takeInvokedCommand(command.getTransactionId()))) {
                /*release();
                throw new IllegalStateException("Error RTMP message type");*/
                continue;
            }
            connectFailed = false;
            Logger.d(TAG, "connect(): command _result connect");
            break;
        }
        if (connectFailed) {
            release();
            throw new SocketTimeoutException("connect(): socket timeout");
        }
        return this;
    }

    protected RtmpConnection release() throws IOException {
        if (inputStream != null) {
            inputStream.close();
        }

        if (outputStream != null) {
            outputStream.close();
        }
        if (socket != null) {
            socket.close();
        }
        return this;
    }

    private void handshake(BufferedInputStream in, BufferedOutputStream out) throws IOException {
        Handshake handshake = new Handshake();
        handshake.writeC0(out);
        handshake.writeC1(out); // Write C1 without waiting for S0
        out.flush();
        handshake.readS0(in);
        handshake.readS1(in);
        handshake.writeC2(out);
        handshake.readS2(in);
    }

    protected void sendRtmpPacket(RtmpPacket rtmpPacket) throws IOException {
        ChunkStreamInfo chunkStreamInfo = rtmpSessionInfo.getChunkStreamInfo(rtmpPacket.getHeader().getChunkStreamId());
        chunkStreamInfo.setPrevHeaderTx(rtmpPacket.getHeader());
        if (!(rtmpPacket instanceof Video || rtmpPacket instanceof Audio)) {
            rtmpPacket.getHeader().setAbsoluteTimestamp((int) chunkStreamInfo.markAbsoluteTimestampTx());
        }
        rtmpPacket.writeTo(outputStream, rtmpSessionInfo.getTxChunkSize(), chunkStreamInfo);
        //Logger.d(TAG, "sendPacket(): " + rtmpPacket + ", size: " + rtmpPacket.getHeader().getPacketLength());
        if (rtmpPacket instanceof Command) {
            rtmpSessionInfo.addInvokedCommand(((Command) rtmpPacket).getTransactionId(), ((Command) rtmpPacket).getCommandName());
        }
        outputStream.flush();
    }

    protected RtmpPacket readPacket() throws IOException {
        InputStream in = inputStream;
        RtmpHeader header = RtmpHeader.readHeader(in, rtmpSessionInfo);
        // Logger.d(TAG, "readPacket(): header.messageType: " + header.getMessageType());

        ChunkStreamInfo chunkStreamInfo = rtmpSessionInfo.getChunkStreamInfo(header.getChunkStreamId());
        chunkStreamInfo.setPrevHeaderRx(header);

        if (header.getPacketLength() > rtmpSessionInfo.getRxChunkSize()) {
            // If the packet consists of more than one chunk,
            // store the chunks in the chunk stream until everything is read
            if (!chunkStreamInfo.storePacketChunk(in, rtmpSessionInfo.getRxChunkSize())) {
                // return null because of incomplete packet
                return null;
            } else {
                // stored chunks complete packet, get the input stream of the chunk stream
                in = chunkStreamInfo.getStoredPacketInputStream();
            }
        }

        RtmpPacket rtmpPacket;
        switch (header.getMessageType()) {
            case SET_CHUNK_SIZE:
                /*setChunkSize.readBody(in);
                Logger.d(TAG, "readPacket(): Setting chunk size to: " + setChunkSize.getChunkSize());
                rtmpSessionInfo.setRxChunkSize(setChunkSize.getChunkSize());*/
                SetChunkSize setChunkSize = new SetChunkSize(header);
                setChunkSize.readBody(in);
                rtmpSessionInfo.setRxChunkSize(setChunkSize.getChunkSize());
                return setChunkSize;
            case ABORT:
                rtmpPacket = new Abort(header);
                break;
            case USER_CONTROL_MESSAGE:
                rtmpPacket = new UserControl(header);
                break;
            case WINDOW_ACKNOWLEDGEMENT_SIZE:
                rtmpPacket = new WindowAckSize(header);
                break;
            case SET_PEER_BANDWIDTH:
                rtmpPacket = new SetPeerBandwidth(header);
                break;
            case AUDIO:
                rtmpPacket = new Audio(header);
                break;
            case VIDEO:
                rtmpPacket = new Video(header);
                break;
            case COMMAND_AMF0:
                rtmpPacket = new Command(header);
                break;
            case DATA_AMF0:
                rtmpPacket = new Data(header);
                break;
            case ACKNOWLEDGEMENT:
                rtmpPacket = new Acknowledgement(header);
                break;
            default:
                throw new IOException("No packet body implementation for message type: " + header.getMessageType());
        }
        rtmpPacket.readBody(in);
        return rtmpPacket;
    }
}
