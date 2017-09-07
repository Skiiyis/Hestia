package io.github.sawameimei.playopengl.solarSystem;

import android.opengl.GLU;
import android.os.Bundle;

import javax.microedition.khronos.opengles.GL10;

import io.github.sawameimei.playopengl.OpenGLESActivity;

public class DrawSolarSystemActivity extends OpenGLESActivity {


    private Star sun = new Star();
    private Star earth = new Star();
    private Star moon = new Star();
    private int angle = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void drawFrame(GL10 gl) {
        super.drawFrame(gl);
        angle++;
        angle %= 360;

        gl.glLoadIdentity();
        GLU.gluLookAt(gl,
                0.0f, 0.0f, 15.0f,
                0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f
        );

        gl.glPushMatrix();
        gl.glRotatef(angle, 0, 0, 1);
        gl.glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
        sun.draw(gl);
        gl.glPopMatrix();

        gl.glPushMatrix();
        gl.glRotatef(-angle, 0, 0, 1);
        gl.glTranslatef(3, 0, 0);
        gl.glScalef(0.5f, 0.5f, 0.5f);
        gl.glColor4f(0.0f, 0.0f, 1.0f, 1.0f);
        earth.draw(gl);

        gl.glPushMatrix();
        gl.glRotatef(-angle, 0, 0, 1);
        gl.glTranslatef(2, 0, 0);
        gl.glScalef(0.5f, 0.5f, 0.5f);
        gl.glRotatef(angle * 10, 0, 0, 1);
        gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        moon.draw(gl);

        gl.glPopMatrix();
        gl.glPopMatrix();
    }
}
