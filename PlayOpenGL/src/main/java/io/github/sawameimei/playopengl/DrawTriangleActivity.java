package io.github.sawameimei.playopengl;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public class DrawTriangleActivity extends OpenGLESActivity {

    float vertexArray[] = {
            -0.8f, -0.4f * 1.732f, 0.0f,
            0.0f, -0.4f * 1.732f, 0.0f,
            -0.4f, 0.4f * 1.732f, 0.0f,
            0.0f, -0.0f * 1.732f, 0.0f,
            0.8f, -0.0f * 1.732f, 0.0f,
            0.4f, 0.4f * 1.732f, 0.0f
    };

    private int index = 0;
    private Handler handler = new Handler() {
        @Override
        public void dispatchMessage(Message msg) {
            index++;
            index %= 3;
            handler.sendMessageDelayed(Message.obtain(), 1000);
        }
    };
    private FloatBuffer vertex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler.sendEmptyMessage(1);

        ByteBuffer vbb = ByteBuffer.allocateDirect(vertexArray.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        vertex = vbb.asFloatBuffer();
        vertex.put(vertexArray);
        vertex.position(0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void drawFrame(GL10 gl) {
        super.drawFrame(gl);

        gl.glLoadIdentity();
        gl.glTranslatef(0, 0, -4);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertex);

        switch (index) {
            case 0:
                gl.glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
                gl.glDrawArrays(GL10.GL_TRIANGLES, 0, 6);
                break;
            case 1:
                gl.glColor4f(0.0f, 1.0f, 0.0f, 1.0f);
                gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 6);
                break;
            case 2:
                gl.glColor4f(0.0f, 0.0f, 1.0f, 1.0f);
                gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, 6);
                break;
        }
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }
}
