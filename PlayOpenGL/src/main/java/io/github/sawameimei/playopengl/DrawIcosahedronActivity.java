package io.github.sawameimei.playopengl;

import android.os.Bundle;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.microedition.khronos.opengles.GL10;

public class DrawIcosahedronActivity extends OpenGLESActivity {

    static final float X = .525731112119133606f;
    static final float Z = .850650808352039932f;

    //12个顶点？
    static float vertices[] = new float[]{
            -X, 0.0f, Z,
            X, 0.0f, Z,
            -X, 0.0f, -Z,
            X, 0.0f, -Z,
            0.0f, Z, X,
            0.0f, Z, -X,
            0.0f, -Z, X,
            0.0f, -Z, -X,
            Z, X, 0.0f,
            -Z, X, 0.0f,
            Z, -X, 0.0f,
            -Z, -X, 0.0f
    };

    //20个面？
    static short indices[] = new short[]{
            0, 4, 1,
            0, 9, 4,
            9, 5, 4,
            4, 5, 8,
            4, 8, 1,
            8, 10, 1,
            8, 3, 10,
            5, 3, 8,
            5, 2, 3,
            2, 7, 3,
            7, 10, 3,
            7, 6, 10,
            7, 11, 6,
            11, 0, 6,
            0, 1, 6,
            6, 1, 10,
            9, 0, 11,
            9, 11, 2,
            9, 2, 5,
            7, 2, 11
    };

    //顶点颜色
    static float[] colors = {
            0f, 0f, 0f, 1f,
            0f, 0f, 1f, 1f,
            0f, 1f, 0f, 1f,
            0f, 1f, 1f, 1f,
            1f, 0f, 0f, 1f,
            1f, 0f, 1f, 1f,
            1f, 1f, 0f, 1f,
            1f, 1f, 1f, 1f,
            1f, 0f, 0f, 1f,
            0f, 1f, 0f, 1f,
            0f, 0f, 1f, 1f,
            1f, 0f, 1f, 1f
    };
    private Buffer vbb;
    private Buffer cbb;
    private Buffer ibb;
    private float angle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vbb = ByteBuffer
                .allocateDirect(vertices.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertices)
                .position(0);
        cbb = ByteBuffer
                .allocateDirect(colors.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(colors)
                .position(0);
        ibb = ByteBuffer.allocateDirect(indices.length * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer()
                .put(indices)
                .position(0);
    }

    @Override
    public void drawFrame(GL10 gl) {
        super.drawFrame(gl);
        angle++;
        angle %= 360;

        gl.glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
        gl.glLoadIdentity();
        gl.glTranslatef(0, 0, -5);
        gl.glRotatef(angle, 0, 1, 0);
        gl.glFrontFace(GL10.GL_CCW);
        gl.glEnable(GL10.GL_CULL_FACE);
        gl.glCullFace(GL10.GL_BACK);

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vbb);
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
        gl.glColorPointer(4, GL10.GL_FLOAT, 0, cbb);
        gl.glDrawElements(GL10.GL_TRIANGLES, indices.length, GL10.GL_UNSIGNED_SHORT, ibb);
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glDisable(GL10.GL_CULL_FACE);
    }
}
