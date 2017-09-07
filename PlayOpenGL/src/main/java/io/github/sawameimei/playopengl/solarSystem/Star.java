package io.github.sawameimei.playopengl.solarSystem;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by huangmeng on 2017/9/7.
 */

public class Star {

    private final Buffer vbb;
    protected float vertices[];

    public Star() {
        float a = (float) (1.0f / (2.0f - 2f * Math.cos(72f * Math.PI / 180.f)));
        float bx = (float) (a * Math.cos(18 * Math.PI / 180.0f));
        float by = (float) (a * Math.sin(18 * Math.PI / 180f));
        float cy = (float) (-a * Math.cos(18 * Math.PI / 180f));
        vertices = new float[]{
                0, a,
                0.5f, cy,
                -bx, by,
                bx, by,
                -0.5f, cy
        };
        vbb = ByteBuffer
                .allocateDirect(vertices.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertices)
                .position(0);
    }

    public void draw(GL10 gl) {
        gl.glFrontFace(GL10.GL_CCW);
        gl.glEnable(GL10.GL_CULL_FACE);
        gl.glCullFace(GL10.GL_BACK);

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, vbb);
        gl.glDrawArrays(GL10.GL_LINE_LOOP, 0, 5);
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glDisable(GL10.GL_CULL_FACE);
    }
}
