package io.github.sawameimei.playopengles20;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import io.github.sawameimei.playopengles20.common.EGLCore;
import io.github.sawameimei.playopengles20.common.GLUtil;
import io.github.sawameimei.playopengles20.common.RawResourceReader;
import io.github.sawameimei.playopengles20.common.ShaderHelper;
import io.github.sawameimei.playopengles20.common.TextureHelper;

/**
 * Created by huangmeng on 2017/11/28.
 */

public class CameraPreviewHelper {

    private final Context mContext;
    private EGLCore mEglCore = new EGLCore(null, EGLCore.FLAG_RECORDABLE);
    private int mVertexShaderHandle;
    private int mFragmentShaderHandle;
    private int mProgramHandle;
    private int muMVPMatrixLoc;
    private int maPositionLoc;
    private int maTextureCoordLoc;
    private int muTexMatrixLoc;
    private int mTextureHandle;
    private SurfaceTexture mCameraSurfaceTexture;

    private int mWidth;
    private int mHeight;

    private float[] MATRIX_IDENTITY = new float[16];
    private static final float FULL_RECTANGLE_COORDS[] = {
            -1.0f, -1.0f,   // 0 bottom left
            1.0f, -1.0f,   // 1 bottom right
            -1.0f, 1.0f,   // 2 top left
            1.0f, 1.0f,   // 3 top right
    };

    private static final float FULL_RECTANGLE_TEX_COORDS[] = {
            0.0f, 0.0f,     // 0 bottom left
            1.0f, 0.0f,     // 1 bottom right
            0.0f, 1.0f,     // 2 top left
            1.0f, 1.0f      // 3 top right
    };
    private ByteBuffer rectangleVertexBuffer;
    private ByteBuffer rectangleTextureBuffer;

    {
        Matrix.setIdentityM(MATRIX_IDENTITY, 0);
        Matrix.scaleM(MATRIX_IDENTITY, 0, -1, 1, 1);
        //Matrix.rotateM(MATRIX_IDENTITY, 0, 270, 0, 0, 1);

        rectangleVertexBuffer = ByteBuffer.allocateDirect(FULL_RECTANGLE_COORDS.length * 4);
        rectangleVertexBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer().put(FULL_RECTANGLE_COORDS);

        rectangleTextureBuffer = ByteBuffer.allocateDirect(FULL_RECTANGLE_COORDS.length * 4);
        rectangleTextureBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer().put(FULL_RECTANGLE_TEX_COORDS);
    }

    public CameraPreviewHelper(Context context) {
        this.mContext = context;
    }

    public SurfaceTexture initGLContext(EGLSurface eglSurface, int surfaceWidth, int surfaceHeight, SurfaceTexture.OnFrameAvailableListener cb) {
        mEglCore.makeCurrent(eglSurface);
        mVertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, RawResourceReader.readTextFileFromRawResource(mContext, R.raw.lesson3_vertex_sharder_source));
        mFragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, RawResourceReader.readTextFileFromRawResource(mContext, R.raw.lesson3_fragment_sharder_source));
        mProgramHandle = ShaderHelper.createAndLinkProgram(mVertexShaderHandle, mFragmentShaderHandle, new String[]{"aPosition", "aTextureCoord"});

        muMVPMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
        maPositionLoc = GLES20.glGetAttribLocation(mProgramHandle, "aPosition");
        maTextureCoordLoc = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord");
        muTexMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexMatrix");

        mTextureHandle = TextureHelper.loadOESTexture();

        mCameraSurfaceTexture = new SurfaceTexture(mTextureHandle);
        mCameraSurfaceTexture.setOnFrameAvailableListener(cb);

        this.mWidth = surfaceWidth;
        this.mHeight = surfaceHeight;
        return mCameraSurfaceTexture;
    }

    public EGLSurface createWindowSurface(Surface surface) {
        return mEglCore.createWindowSurface(surface);
    }

    public EGLSurface createWindowSurface(SurfaceTexture surfaceTexture) {
        return mEglCore.createWindowSurface(surfaceTexture);
    }

    public void drawFrame(EGLSurface windowSurface) {
        float[] textMatrix = new float[16];
        mCameraSurfaceTexture.getTransformMatrix(textMatrix);

        mEglCore.makeCurrent(windowSurface);
        mCameraSurfaceTexture.updateTexImage();
        GLUtil.checkGlError("draw start");

        GLES20.glViewport(0, 0, mWidth, mHeight);
        GLES20.glUseProgram(mProgramHandle);
        GLUtil.checkGlError("glUseProgram");

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureHandle);
        GLUtil.checkGlError("glBindTexture:mTextureHandle");

        GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, MATRIX_IDENTITY, 0);
        GLUtil.checkGlError("glUniformMatrix4fv:muMVPMatrixLoc");

        GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, textMatrix, 0);
        GLUtil.checkGlError("glUniformMatrix4fv:muTexMatrixLoc");

        GLES20.glVertexAttribPointer(maPositionLoc, 2, GLES20.GL_FLOAT, false, 8, rectangleVertexBuffer.position(0));
        GLES20.glEnableVertexAttribArray(maPositionLoc);
        GLUtil.checkGlError("glEnableVertexAttribArray:maPositionLoc");

        GLES20.glVertexAttribPointer(maTextureCoordLoc, 2, GLES20.GL_FLOAT, false, 8, rectangleTextureBuffer.position(0));
        GLES20.glEnableVertexAttribArray(maTextureCoordLoc);
        GLUtil.checkGlError("glEnableVertexAttribArray:maTextureCoordLoc");

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLUtil.checkGlError("glDrawArrays");

        GLES20.glDisableVertexAttribArray(maPositionLoc);
        GLES20.glDisableVertexAttribArray(maTextureCoordLoc);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        GLES20.glUseProgram(0);
        GLUtil.checkGlError("disable");

        mEglCore.swapBuffers(windowSurface);
        GLUtil.checkGlError("swapBuffers");

        mEglCore.setPresentationTime(windowSurface, mCameraSurfaceTexture.getTimestamp());
    }
}
