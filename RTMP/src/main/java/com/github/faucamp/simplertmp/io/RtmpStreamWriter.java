package com.github.faucamp.simplertmp.io;

import com.github.faucamp.simplertmp.packets.ContentData;
import com.github.faucamp.simplertmp.packets.Data;

import java.io.IOException;

/**
 * Interface for writing RTMP content streams (audio/video)
 * 
 * @author francois
 */
public abstract class RtmpStreamWriter {

    public abstract void write(Data dataPacket) throws IOException;

    public abstract void write(ContentData packet) throws IOException;

    public void close() {
        synchronized (this) {
            this.notifyAll();
        }
    }
}
