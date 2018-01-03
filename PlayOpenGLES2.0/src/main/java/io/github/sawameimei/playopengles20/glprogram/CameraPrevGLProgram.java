package io.github.sawameimei.playopengles20.glprogram;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;

import java.lang.ref.WeakReference;

import io.github.sawameimei.opengleslib.glprogram.TextureGLProgram;
import io.github.sawameimei.opengleslib.common.GLUtil;
import io.github.sawameimei.opengleslib.common.GLVertex;
import io.github.sawameimei.opengleslib.common.RawResourceReader;
import io.github.sawameimei.opengleslib.common.ShaderHelper;
import io.github.sawameimei.opengleslib.common.TextureHelper;
import io.github.sawameimei.playopengles20.R;

/**
 * Created by huangmeng on 2017/12/11.
 */

public class CameraPrevGLProgram implements TextureGLProgram {

    private final WeakReference<Context> mContext;

    private final FullRectangleCoords mFullRectangleCoords;
    private final FullRectangleTextureCoords mFullRectangleTextureCoords;

    private int mVertexShaderHandle;
    private int mFragmentShaderHandle;
    private int mProgramHandle;

    private int muMVPMatrixLoc;
    private int maPositionLoc;
    private int maTextureCoordLoc;
    private int muTexMatrixLoc;

    private int[] mTextureId = new int[1];

    private float[] mTextureM = GLUtil.getIdentityM();
    private float[] muPositionM = GLUtil.getIdentityM();

    {
        Matrix.scaleM(muPositionM, 0, -1, 1, 1);
    }

    public CameraPrevGLProgram(Context context, float[] textureM) {
        this.mContext = new WeakReference<>(context);
        this.mTextureM = textureM;
        mFullRectangleTextureCoords = new FullRectangleTextureCoords();
        mFullRectangleCoords = new FullRectangleCoords();
        mTextureId[0] = TextureHelper.loadOESTexture();
    }

    public CameraPrevGLProgram(Context context, float[] textureM, int textureId) {
        this.mContext = new WeakReference<>(context);
        this.mTextureM = textureM;
        mFullRectangleTextureCoords = new FullRectangleTextureCoords();
        mFullRectangleCoords = new FullRectangleCoords();
        mTextureId[0] = textureId;
    }

    @Override
    public void compileAndLink() {
        mVertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, RawResourceReader.readTextFileFromRawResource(mContext.get(), R.raw.camera_preview_vertex_sharder));
        mFragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, RawResourceReader.readTextFileFromRawResource(mContext.get(), R.raw.camera_preview_fragment_sharder));
        mProgramHandle = ShaderHelper.createAndLinkProgram(mVertexShaderHandle, mFragmentShaderHandle, new String[]{"aPosition", "aTextureCoord"});

        muMVPMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
        maPositionLoc = GLES20.glGetAttribLocation(mProgramHandle, "aPosition");
        maTextureCoordLoc = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord");
        muTexMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexMatrix");
    }

    @Override
    public void drawFrame() {
        GLES20.glUseProgram(mProgramHandle);
        GLUtil.checkGlError("glUseProgram");

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureId[0]);
        GLUtil.checkGlError("glBindTexture:mTextureHandle");

        GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, muPositionM, 0);
        GLUtil.checkGlError("glUniformMatrix4fv:muMVPMatrixLoc");

        GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, mTextureM, 0);
        GLUtil.checkGlError("glUniformMatrix4fv:muTexMatrixLoc");

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

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

        GLES20.glUseProgram(0);
        GLUtil.checkGlError("disable");
    }

    public TextureGLProgram rotate(float angle) {
        muPositionM = GLUtil.getIdentityM();
        Matrix.rotateM(muPositionM, 0, angle, 0, 0, 1);
        return this;
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
