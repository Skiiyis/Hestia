package io.github.sawameimei.playopengl.sphere;

import android.os.Bundle;

import javax.microedition.khronos.opengles.GL10;

import io.github.sawameimei.playopengl.OpenGLESActivity;

public class DrawSphereActivity extends OpenGLESActivity {

    Sphere sphere = new Sphere();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void drawFrame(GL10 gl) {
        super.drawFrame(gl);
        sphere.initScene(gl);
        sphere.drawScene(gl);
    }
}
