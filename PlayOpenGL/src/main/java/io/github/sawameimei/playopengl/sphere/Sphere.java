package io.github.sawameimei.playopengl.sphere;

import android.opengl.GLU;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by huangmeng on 2017/9/7.
 */

public class Sphere {

    /*public void draw(GL10 gl) {
        float theta, pai;
        float co, si;
        float r1, r2;
        float h1, h2;
        float step = 2.0f;
        float[][] v = new float[32][3];
        ByteBuffer vbb;
        FloatBuffer vBuf;
        vbb = ByteBuffer.allocateDirect(v.length * v[0].length * 4);
        vbb.order(ByteOrder.nativeOrder());

        vBuf = vbb.asFloatBuffer();
        vBuf.position(0);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);

        for (pai = -90.0f; pai < 90.0f; pai += step) {
            int n = 0;
            r1 = (float) Math.cos(pai * Math.PI / 180.0);
            r2 = (float) Math.cos((pai + step) * Math.PI / 180.0);
            h1 = (float) Math.sin(pai * Math.PI / 180.0);
            h2 = (float) Math.sin((pai + step) * Math.PI / 180.0);

            loop:
            for (theta = 0.0f; theta <= 360.0f; theta += step) {
                co = (float) Math.cos(theta * Math.PI / 180.0);
                si = -(float) Math.sin(theta * Math.PI / 180.0);
                v[n][0] = (r2 * co);
                v[n][1] = (h2);
                v[n][2] = (r2 * si);
                v[n + 1][0] = (r1 * co);
                v[n + 1][1] = (h1);
                v[n + 1][2] = (r1 * si);
                vBuf.put(v[n]);
                vBuf.put(v[n + 1]);
                n += 2;
                if (n > 31) {
                    vBuf.position(0);
                    gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vBuf);
                    gl.glNormalPointer(GL10.GL_FLOAT, 0, vBuf);
                    gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, n);
                    n = 0;
                    theta -= step;
                }
            }

            vBuf.position(0);
            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vBuf);
            gl.glNormalPointer(GL10.GL_FLOAT, 0, vBuf);
            gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, n);
        }
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);
    }*/

    public void draw(GL10 gl) {
        float theta, pai;
        float co, si;
        float r1, r2;
        float h1, h2;
        float step = 2.0f;
        float[][] v = new float[32][3];
        ByteBuffer vbb;
        FloatBuffer vBuf;
        vbb = ByteBuffer.allocateDirect(v.length * v[0].length * 4);
        vbb.order(ByteOrder.nativeOrder());
        vBuf = vbb.asFloatBuffer();
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
        for (pai = -90.0f; pai < 90.0f; pai += step) {
            int n = 0;
            r1 = (float) Math.cos(pai * Math.PI / 180.0);
            r2 = (float) Math.cos((pai + step) * Math.PI / 180.0);
            h1 = (float) Math.sin(pai * Math.PI / 180.0);
            h2 = (float) Math.sin((pai + step) * Math.PI / 180.0);
            for (theta = 0.0f; theta <= 360.0f; theta += step) {
                co = (float) Math.cos(theta * Math.PI / 180.0);
                si = -(float) Math.sin(theta * Math.PI / 180.0);
                v[n][0] = (r2 * co);
                v[n][1] = (h2);
                v[n][2] = (r2 * si);
                v[n + 1][0] = (r1 * co);
                v[n + 1][1] = (h1);
                v[n + 1][2] = (r1 * si);
                vBuf.put(v[n]);
                vBuf.put(v[n + 1]);
                n += 2;
                if (n > 31) {
                    vBuf.position(0);
                    gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vBuf);
                    gl.glNormalPointer(GL10.GL_FLOAT, 0, vBuf);
                    gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, n);
                    n = 0;
                    theta -= step;
                }
            }
            vBuf.position(0);
            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vBuf);
            gl.glNormalPointer(GL10.GL_FLOAT, 0, vBuf);
            gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, n);
        }
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);
    }

    public void drawScene(GL10 gl) {
        float[] mat_amb = {0.2f * 0.4f, 0.2f * 0.4f,
                0.2f * 1.0f, 1.0f,};
        float[] mat_diff = {0.4f, 0.4f, 1.0f, 1.0f,};
        float[] mat_spec = {1.0f, 1.0f, 1.0f, 1.0f,};
        ByteBuffer mabb = ByteBuffer.allocateDirect(mat_amb.length * 4);
        mabb.order(ByteOrder.nativeOrder());
        FloatBuffer mat_ambBuf = mabb.asFloatBuffer();
        mat_ambBuf.put(mat_amb);
        mat_ambBuf.position(0);
        ByteBuffer mdbb = ByteBuffer.allocateDirect(mat_diff.length * 4);
        mdbb.order(ByteOrder.nativeOrder());
        FloatBuffer mat_diffBuf = mdbb.asFloatBuffer();
        mat_diffBuf.put(mat_diff);
        mat_diffBuf.position(0);
        ByteBuffer msbb = ByteBuffer.allocateDirect(mat_spec.length * 4);
        msbb.order(ByteOrder.nativeOrder());
        FloatBuffer mat_specBuf = msbb.asFloatBuffer();
        mat_specBuf.put(mat_spec);
        mat_specBuf.position(0);
        gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_AMBIENT, mat_ambBuf);
        gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_DIFFUSE, mat_diffBuf);
        gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_SPECULAR, mat_specBuf);
        gl.glMaterialf(GL10.GL_FRONT_AND_BACK, GL10.GL_SHININESS, 64.0f);
        draw(gl);
    }

    public void initScene(GL10 gl) {
        float[] amb = {1.0f, 1.0f, 1.0f, 1.0f,};
        float[] diff = {1.0f, 1.0f, 1.0f, 1.0f,};
        float[] spec = {1.0f, 1.0f, 1.0f, 1.0f,};
        float[] pos = {0.0f, 5.0f, 5.0f, 1.0f,};
        float[] spot_dir = {0.0f, -1.0f, 0.0f,};
        gl.glEnable(GL10.GL_DEPTH_TEST);
        gl.glEnable(GL10.GL_CULL_FACE);
        gl.glEnable(GL10.GL_LIGHTING);
        gl.glEnable(GL10.GL_LIGHT0);
        ByteBuffer abb = ByteBuffer.allocateDirect(amb.length * 4);
        abb.order(ByteOrder.nativeOrder());
        FloatBuffer ambBuf = abb.asFloatBuffer();
        ambBuf.put(amb);
        ambBuf.position(0);

        ByteBuffer dbb = ByteBuffer.allocateDirect(diff.length * 4);
        dbb.order(ByteOrder.nativeOrder());
        FloatBuffer diffBuf = dbb.asFloatBuffer();
        diffBuf.put(diff);
        diffBuf.position(0);

        ByteBuffer sbb = ByteBuffer.allocateDirect(spec.length * 4);
        sbb.order(ByteOrder.nativeOrder());
        FloatBuffer specBuf = sbb.asFloatBuffer();
        specBuf.put(spec);
        specBuf.position(0);

        ByteBuffer pbb = ByteBuffer.allocateDirect(pos.length * 4);
        pbb.order(ByteOrder.nativeOrder());
        FloatBuffer posBuf = pbb.asFloatBuffer();
        posBuf.put(pos);
        posBuf.position(0);

        ByteBuffer spbb = ByteBuffer.allocateDirect(spot_dir.length * 4);
        spbb.order(ByteOrder.nativeOrder());
        FloatBuffer spot_dirBuf = spbb.asFloatBuffer();
        spot_dirBuf.put(spot_dir);
        spot_dirBuf.position(0);

        gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_AMBIENT, ambBuf);
        gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_DIFFUSE, diffBuf);
        gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_SPECULAR, specBuf);
        gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, posBuf);
        gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_SPOT_DIRECTION, spot_dirBuf);
        gl.glLightf(GL10.GL_LIGHT0, GL10.GL_SPOT_EXPONENT, 0.0f);
        gl.glLightf(GL10.GL_LIGHT0, GL10.GL_SPOT_CUTOFF, 45.0f);
        gl.glLoadIdentity();
        GLU.gluLookAt(gl,
                0.0f, 4.0f, 4.0f,
                0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f
        );
    }
}
