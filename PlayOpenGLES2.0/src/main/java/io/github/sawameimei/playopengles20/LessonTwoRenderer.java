package io.github.sawameimei.playopengles20;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import io.github.sawameimei.playopengles20.common.RawResourceReader;
import io.github.sawameimei.playopengles20.common.ShaderHelper;
import io.github.sawameimei.playopengles20.common.TextureHelper;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1f;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;

/**
 * Created by huangmeng on 2017/11/20.
 */
class LessonTwoRenderer implements GLSurfaceView.Renderer {

    private final Context mContext;
    /**
     * 相机位置
     */
    private float[] mViewMatrix = new float[16];

    /**
     * 场景位置
     */
    private float[] mProjectionMatrix = new float[16];
    private float[] mMVPMatrix = new float[16];
    private float[] mModelMatrix = new float[16];

    private int mVertexShaderHandle;
    private int mFragmentShaderHandle;
    private int mProgramHandle;
    private int mTextureHandle;
    private int mTexUnitHandle;

    private int mPositionHandle;
    private int mTexCoordinateHandle;
    private int mMVPMatrixHandle;

    /**
     * 每个点的位置用数组的几个元素来表示 X,Y,Z
     */
    private static final int SIZE_POSITION = 3;
    /**
     * 每个颜色的值对应的映射坐标用几个元素来表示（2D材质只要x,y)
     */
    private static final int SIZE_TEXTURE = 2;
    private static final int SIZE_BYTE_OF_PER_FLOAT = 4;

    private FloatBuffer mTriangleBuffer;
    private FloatBuffer mTextureBuffer;
    private float[] mTriangle = new float[]{ //ModelMatrix 模型坐标 屏幕坐标为（-1.0F,-1.0F,0.0F)~(1.0F,1.0F,0.0F)
            0.5f, 0.5f, 0.0F,
            -0.5f, 0.5f, 0.0f,
            -0.5f, -0.5f, 0.0f,

            -0.5f, -0.5f, 0.0f,
            0.5f, -0.5f, 0.0f,
            0.5f, 0.5f, 0.0F
    };

    private float[] mTexture = new float[]{
            1f, 0f,
            0f, 0f,
            0f, 1f,

            0f, 1f,
            1f, 1f,
            1f, 0f
    };
    private float mNear;
    private float mFar;
    private float mRatio;
    private float mLeft;
    private float mRight;
    private float mBottom;
    private float mTop;

    public LessonTwoRenderer(Context context) {
        this.mContext = context;
        mTriangleBuffer = ByteBuffer.allocateDirect(mTriangle.length * SIZE_BYTE_OF_PER_FLOAT)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTextureBuffer = ByteBuffer.allocateDirect(mTexture.length * SIZE_BYTE_OF_PER_FLOAT)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTriangleBuffer.put(mTriangle).position(0);
        mTextureBuffer.put(mTexture).position(0);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        glClearColor(0.5F, 0.5F, 0.5F, 0.5F);

        //glEnable(GL_CULL_FACE);
        //glEnable(GL_DEPTH_TEST);
        //相机的绝对位置
        final float eyeX = 0.0F;
        final float eyeY = 0.0F;
        final float eyeZ = 3.0F;

        //视觉方向为三个方向的矢量和
        final float lookX = 0.0F;
        final float lookY = 0.0F;
        final float lookZ = 2.0F;

        //相机方向相机的顶部方向为三个方向的矢量和
        final float upX = 0.0F;
        final float upY = 1.0F;
        final float upZ = 0.0F;

        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);

        mVertexShaderHandle = ShaderHelper.compileShader(GL_VERTEX_SHADER, RawResourceReader.readTextFileFromRawResource(mContext, R.raw.lesson2_vertex_shader_source));
        mFragmentShaderHandle = ShaderHelper.compileShader(GL_FRAGMENT_SHADER, RawResourceReader.readTextFileFromRawResource(mContext, R.raw.lesson2_fragment_shader_source));
        mProgramHandle = ShaderHelper.createAndLinkProgram(mVertexShaderHandle, mFragmentShaderHandle, new String[]{"a_Position", "a_TexCoordinate"});
        mTextureHandle = TextureHelper.loadTexture(mContext, R.mipmap.usb_android);

        mMVPMatrixHandle = glGetUniformLocation(mProgramHandle, "u_MVPMatrix");
        mPositionHandle = glGetAttribLocation(mProgramHandle, "a_Position");
        mTexCoordinateHandle = glGetAttribLocation(mProgramHandle, "a_TexCoordinate");
        mTexUnitHandle = glGetUniformLocation(mProgramHandle, "u_TextureUnit");

        glUseProgram(mProgramHandle);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        glViewport(0, 0, width, height);
        //使用透视投影
        mRatio = (float) width / height;
        mLeft = -mRatio;
        mRight = mRatio;
        mBottom = -1.0f;
        mTop = 1.0f;
        mNear = 1.0f; //取景最近的距离，低于这个取景距离的将不可见,这个距离应该不大于模型到相机的距离
        mFar = 10.0f; //取景最远的距离，超出这个取景距离的将不可见
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);

        glVertexAttribPointer(mPositionHandle, SIZE_POSITION, GL_FLOAT, false, 0, mTriangleBuffer);
        glEnableVertexAttribArray(mPositionHandle);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, mTextureHandle);
        glUniform1f(mTexUnitHandle, 0);

        glVertexAttribPointer(mTexCoordinateHandle, SIZE_TEXTURE, GL_FLOAT, false, 0, mTextureBuffer);
        glEnableVertexAttribArray(mTexCoordinateHandle);

        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.frustumM(mProjectionMatrix, 0, mLeft, mRight, mBottom, mTop, mNear, mFar);
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
        glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        /*Matrix.setIdentityM(mMVPMatrix, 0);
        glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);*/
        glDrawArrays(GL_TRIANGLES, 0, 6);
    }

    public void setNear(float near) {
        if (near == mFar || near <= 0) {
            return;
        }
        this.mNear = near;
    }

    public void setFar(float far) {
        if (far == mNear || far <= 0) {
            return;
        }
        this.mFar = far;
    }
}
