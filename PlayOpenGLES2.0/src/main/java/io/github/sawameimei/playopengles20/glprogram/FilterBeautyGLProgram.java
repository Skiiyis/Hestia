package io.github.sawameimei.playopengles20.glprogram;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.support.annotation.FloatRange;

import java.lang.ref.WeakReference;
import java.nio.FloatBuffer;

import io.github.sawameimei.playopengles20.R;
import io.github.sawameimei.playopengles20.common.GLUtil;
import io.github.sawameimei.playopengles20.common.GLVertex;
import io.github.sawameimei.playopengles20.common.RawResourceReader;
import io.github.sawameimei.playopengles20.common.ShaderHelper;

/**
 * 美颜滤镜程序
 * Created by huangmeng on 2017/12/21.
 */
public class FilterBeautyGLProgram implements TextureGLProgram {

    private final WeakReference<Context> mContext;

    private final FullRectangleTextureCoords mFullRectangleTextureCoords = new FullRectangleTextureCoords();
    private final FullRectangleCoords mFullRectangleCoords = new FullRectangleCoords();

    private int mVertexShaderHandle;
    private int mFragmentShaderHandle;
    private int mProgramHandle;

    private int muMVPMatrixLoc;
    private int maPositionLoc;
    private int maTextureCoordLoc;
    private int muTexMatrixLoc;
    private int mvStepOffsetLoc;
    private int mfLevelLoc;

    private int[] mTextureId = new int[1];

    private float[] mTextureM = GLUtil.getIdentityM();
    private float[] muPositionM = GLUtil.getIdentityM();

    private float[] mStepOffset = new float[2];

    private float mBeautyLevel = 1.0F;
    private int msTextureLoc;

    {
        //Matrix.scaleM(muPositionM, 0, -1, 1, 1);
        Matrix.rotateM(muPositionM, 0, 180F, 0, 0, 1);
    }

    public FilterBeautyGLProgram(Context context, float[] textureM, int textureId, int textureWidth, int textureHeight) {
        this.mContext = new WeakReference<>(context);
        this.mTextureM = textureM;
        mStepOffset[0] = 2.0F / textureWidth;
        mStepOffset[1] = 2.0F / textureHeight;
        mTextureId[0] = textureId;
    }

    public void setBeautyLevel(@FloatRange(from = 0, to = 1) float level) {
        mBeautyLevel = 1.0F - (1.0F - 0.33F) * level;
    }

    public void setPreviewSize(int textureWidth, int textureHeight) {
        mStepOffset[0] = 2.0F / textureWidth;
        mStepOffset[1] = 2.0F / textureHeight;
    }

    @Override
    public void compileAndLink() {
        mVertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, RawResourceReader.readTextFileFromRawResource(mContext.get(), R.raw.filter_beauty_vertex_sharder));
        mFragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, RawResourceReader.readTextFileFromRawResource(mContext.get(), R.raw.filter_beauty_fragment_sharder));
        mProgramHandle = ShaderHelper.createAndLinkProgram(mVertexShaderHandle, mFragmentShaderHandle, new String[]{"aPosition", "aTextureCoord"});

        muMVPMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
        muTexMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexMatrix");
        maPositionLoc = GLES20.glGetAttribLocation(mProgramHandle, "aPosition");
        maTextureCoordLoc = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord");

        mvStepOffsetLoc = GLES20.glGetUniformLocation(mProgramHandle, "vStepOffset");
        mfLevelLoc = GLES20.glGetUniformLocation(mProgramHandle, "fLevel");
        msTextureLoc = GLES20.glGetUniformLocation(mProgramHandle, "sTexture");
    }

    @Override
    public void drawFrame() {
        GLES20.glUseProgram(mProgramHandle);
        GLUtil.checkGlError("glUseProgram");

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId[0]);
        GLES20.glUniform1i(msTextureLoc, 0);
        GLUtil.checkGlError("glBindTexture:mTextureHandle");

        GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, muPositionM, 0);
        GLUtil.checkGlError("glUniformMatrix4fv:muMVPMatrixLoc");

        GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, mTextureM, 0);
        GLUtil.checkGlError("glUniformMatrix4fv:muTexMatrixLoc");

        GLES20.glUniform2fv(mvStepOffsetLoc, 1, FloatBuffer.wrap(mStepOffset));
        GLUtil.checkGlError("glUniform2fv:mvStepOffsetLoc");

        GLES20.glUniform1f(mfLevelLoc, mBeautyLevel);
        GLUtil.checkGlError("glUniform1f:mfLevelLoc");

        GLES20.glVertexAttribPointer(maPositionLoc, mFullRectangleCoords.getSize(), GLES20.GL_FLOAT, false, mFullRectangleCoords.getStride(), mFullRectangleCoords.toByteBuffer().position(0));
        GLES20.glEnableVertexAttribArray(maPositionLoc);
        GLUtil.checkGlError("glEnableVertexAttribArray:maPositionLoc");

        GLES20.glVertexAttribPointer(maTextureCoordLoc, mFullRectangleTextureCoords.getSize(), GLES20.GL_FLOAT, false, mFullRectangleTextureCoords.getStride(), mFullRectangleTextureCoords.toByteBuffer().position(0));
        GLES20.glEnableVertexAttribArray(maTextureCoordLoc);
        GLUtil.checkGlError("glEnableVertexAttribArray:maTextureCoordLoc");

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, mFullRectangleCoords.getCount());
        GLUtil.checkGlError("glDrawArrays");

        GLES20.glDisableVertexAttribArray(maPositionLoc);
        GLES20.glDisableVertexAttribArray(maTextureCoordLoc);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glUseProgram(0);
        GLUtil.checkGlError("disable");
    }

    @Override
    public void release() {
        GLES20.glDeleteProgram(mProgramHandle);
    }

    @Override
    public int[] texture() {
        return mTextureId;
    }

    private static class FullRectangleTextureCoords extends GLVertex.FloatGLVertex {

        public FullRectangleTextureCoords() {
            super(new float[]{
                    0.0f, 0.0f,     // 0 bottom left
                    1.0f, 0.0f,     // 1 bottom right
                    0.0f, 1.0f,     // 2 top left
                    1.0f, 1.0f      // 3 top right
            });
        }

        @Override
        public int getSize() {
            return 2;
        }
    }

    private static class FullRectangleCoords extends GLVertex.FloatGLVertex {

        public FullRectangleCoords() {
            super(new float[]{
                    -1.0f, -1.0f,   // 0 bottom left
                    1.0f, -1.0f,   // 1 bottom right
                    -1.0f, 1.0f,   // 2 top left
                    1.0f, 1.0f,   // 3 top right
            });
        }

        @Override
        public int getSize() {
            return 2;
        }
    }
}
