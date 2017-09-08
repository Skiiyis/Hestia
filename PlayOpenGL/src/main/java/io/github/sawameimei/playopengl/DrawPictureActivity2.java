package io.github.sawameimei.playopengl;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES10;
import android.opengl.GLUtils;
import android.os.Bundle;
import android.support.annotation.Nullable;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by huangmeng on 2017/9/8.
 */

public class DrawPictureActivity2 extends OpenGLESActivity {

    float[] vertexs = new float[]{
            -5.0f, 5.0f, -30.0f,
            5.0f, -5.0f, -30.0f,
            5.0f, 5.0f, -30.0f,

            -5.0f, 5.0f, -30.0f,
            5.0f, -5.0f, -30.0f,
            -5.0f, -5.0f, -30.0f,
    };

    float[] textureCoords = new float[]{
            0, 0,
            0, 1,
            1, 1,

            0, 1,
            1, 1,
            1, 0
    };

    private Buffer vbb;
    private Buffer tbb;
    private int[] texture = new int[1];

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        vbb = ByteBuffer.allocateDirect(vertexs.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexs)
                .position(0);

        tbb = ByteBuffer.allocateDirect(textureCoords.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(textureCoords)
                .position(0);

        GLES10.glEnable(GLES10.GL_TEXTURE_2D);
        GLES10.glGenTextures(1, texture, 0);
        GLES10.glBindTexture(GLES10.GL_TEXTURE_2D, texture[0]);
        GLES10.glTexParameterx(GLES10.GL_TEXTURE_2D, GLES10.GL_TEXTURE_MAG_FILTER, GLES10.GL_NEAREST);

        Bitmap image = BitmapFactory.decodeResource(getResources(), R.mipmap.texture);
        //GLUtils.texImage2D(GLES10.GL_TEXTURE_2D, 0, image, 0);
        Buffer pixels = ByteBuffer.allocateDirect(image.getWidth() * image.getHeight() * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        GLES10.glTexImage2D(GLES10.GL_TEXTURE_2D, 0, -1, image.getWidth(), image.getHeight(), 0, GLES10.GL_RGBA, GLES10.GL_FLOAT, pixels);
        image.recycle();
    }

    @Override
    public void drawFrame(GL10 gl) {
        super.drawFrame(gl);

        gl.glLoadIdentity();
        gl.glTranslatef(0, 0, -4);

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vbb);

        gl.glActiveTexture(texture[0]);
        gl.glEnableClientState(GLES10.GL_TEXTURE_COORD_ARRAY);
        gl.glTexCoordPointer(2, GLES10.GL_FLOAT, 0, tbb);

        gl.glDrawArrays(GL10.GL_TRIANGLES, 0, 6);
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }
}
