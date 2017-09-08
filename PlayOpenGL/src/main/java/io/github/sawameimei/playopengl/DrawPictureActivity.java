package io.github.sawameimei.playopengl;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES10;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by huangmeng on 2017/9/8.
 */

public class DrawPictureActivity extends AppCompatActivity {

    private FloatBuffer vertexBuffer;
    private FloatBuffer txtCoodsBuffer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GLSurfaceView glSurfaceView = new GLSurfaceView(this);
        setContentView(glSurfaceView);
        glSurfaceView.setRenderer(new GLSurfaceView.Renderer() {

            @Override
            public void onSurfaceCreated(GL10 gl, EGLConfig config) {
                float[] vertexs = new float[]{
                        -5.0f, 5.0f, -30.0f,
                        5.0f, -5.0f, -30.0f,
                        5.0f, 5.0f, -30.0f,
                };
                vertexBuffer = ByteBuffer.allocateDirect(vertexs.length * 4)
                        .order(ByteOrder.nativeOrder()).asFloatBuffer();
                vertexBuffer.put(vertexs);
                vertexBuffer.position(0);

                int[] tempArray = new int[1];
                GLES10.glEnable(GLES10.GL_TEXTURE_2D);
                GLES10.glGenTextures(1, tempArray, 0);
                GLES10.glBindTexture(GLES10.GL_TEXTURE_2D, tempArray[0]);
                GLES10.glTexParameterx(GLES10.GL_TEXTURE_2D, GLES10.GL_TEXTURE_MAG_FILTER, GLES10.GL_NEAREST);

                Bitmap image = BitmapFactory.decodeResource(getResources(), R.mipmap.texture);
                GLUtils.texImage2D(GLES10.GL_TEXTURE_2D, 0, image, 0);
                image.recycle();

                float[] texcoods = new float[]{
                        0, 0,
                        1, 1,
                        1, 0,
                };
                txtCoodsBuffer = ByteBuffer.allocateDirect(texcoods.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
                txtCoodsBuffer.put(texcoods);
                txtCoodsBuffer.position(0);
            }

            @Override
            public void onSurfaceChanged(GL10 gl, int width, int height) {
            }

            @Override
            public void onDrawFrame(GL10 gl) {
                GLES10.glClearColor(0.0f, 0.0f, 0.0f, 0.0f); // 清空场景为黑色。
                GLES10.glClear(GLES10.GL_COLOR_BUFFER_BIT | GLES10.GL_DEPTH_BUFFER_BIT);// 清空相关缓存。

                GLES10.glEnableClientState(GLES10.GL_VERTEX_ARRAY);
                GLES10.glVertexPointer(3, GLES10.GL_FLOAT, 0, vertexBuffer);

                GLES10.glEnableClientState(GLES10.GL_TEXTURE_COORD_ARRAY);
                GLES10.glTexCoordPointer(2, GLES10.GL_FLOAT, 0, txtCoodsBuffer);

                GLES10.glDrawArrays(GLES10.GL_TRIANGLES, 0, 3);

                GLES10.glFlush();
            }
        });
    }
}
