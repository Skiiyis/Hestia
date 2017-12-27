package io.github.sawameimei.rtmp;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void testConnect() throws Exception {
        new RtmpCollector("rtmp://192.168.1.149/live/live")
                .connect()
                .createStream(null)
                .makeCurrent()
                .playLive();
    }
}