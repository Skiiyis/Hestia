package io.github.sawameimei.playopengles20.glprogram;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

import io.github.sawameimei.opengleslib.glprogram.TextureGLProgram;
import io.github.sawameimei.playopengles20.R;
import io.github.sawameimei.opengleslib.common.GLUtil;
import io.github.sawameimei.opengleslib.common.GLVertex;
import io.github.sawameimei.opengleslib.common.RawResourceReader;
import io.github.sawameimei.opengleslib.common.ShaderHelper;
import io.github.sawameimei.opengleslib.common.TextureHelper;

/**
 * Created by huangmeng on 2017/12/20.
 */

public class FilterWriteSkinGLProgram implements TextureGLProgram {

    private final WeakReference<Context> mContext;

    private final GLVertex mFullRectangleCoords = new FullRectangleCoords();
    private final GLVertex mFullRectangleTextureCoords = new FullRectangleTextureCoords();

    private final int mTextureWidth;
    private final int mTextureHeight;

    private int[] mInputTextureId = new int[1];
    private int[] mFiltertTextureId = new int[1];

    private int mProgramHandle;
    private int mVertexShaderHandle;
    private int mFragmentShaderHandle;

    private int muMVPMatrixLoc;
    private int muTexMatrixLoc;
    private int maPositionLoc;
    private int maTextureCoordLoc;
    private int muInputTextureLoc;
    private int muCurveLoc;
    private int muTexelWidthOffsetLoc;
    private int muTexelHeightOffsetLoc;

    private float[] mTextureM = GLUtil.getIdentityM();
    private float[] muPositionM = GLUtil.getIdentityM();

    {
        //Matrix.scaleM(muPositionM, 0, -1, 1, 1);
        //Matrix.rotateM(muPositionM, 0, 180F, 0, 0, 1);
    }

    public FilterWriteSkinGLProgram(Context context, float[] textureM, int textureId, int textureWidth, int textureHeight) {
        this.mContext = new WeakReference<>(context);
        this.mTextureM = textureM;
        this.mTextureWidth = textureWidth;
        this.mTextureHeight = textureHeight;
        this.mInputTextureId[0] = textureId;
    }

    @Override
    public void compile() {
        mVertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, RawResourceReader.readTextFileFromRawResource(mContext.get(), R.raw.filter_write_skin_vertex_sharder));
        mFragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, RawResourceReader.readTextFileFromRawResource(mContext.get(), R.raw.filter_write_skin_fragment_sharder));
        mProgramHandle = ShaderHelper.createAndLinkProgram(mVertexShaderHandle, mFragmentShaderHandle, new String[]{"aPosition", "aTextureCoord"});

        muMVPMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
        muTexMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexMatrix");
        maPositionLoc = GLES20.glGetAttribLocation(mProgramHandle, "aPosition");
        maTextureCoordLoc = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord");

        muCurveLoc = GLES20.glGetUniformLocation(mProgramHandle, "uCurve");
        muTexelWidthOffsetLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexelWidthOffset");
        muTexelHeightOffsetLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexelHeightOffset");
        muInputTextureLoc = GLES20.glGetUniformLocation(mProgramHandle, "uInputTexture");
        genFilterTexture();
    }

    /**
     * 传递过滤的纹理到纹理堆上
     */
    private void genFilterTexture() {
        GLES20.glGenTextures(1, mFiltertTextureId, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFiltertTextureId[0]);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        byte[] arrayOfByte = new byte[1024];
        int[] arrayOfInt1 = {95, 95, 96, 97, 97, 98, 99, 99, 100, 101, 101, 102, 103, 104, 104, 105, 106, 106, 107, 108, 108, 109, 110, 111, 111, 112, 113, 113, 114, 115, 115, 116, 117, 117, 118, 119, 120, 120, 121, 122, 122, 123, 124, 124, 125, 126, 127, 127, 128, 129, 129, 130, 131, 131, 132, 133, 133, 134, 135, 136, 136, 137, 138, 138, 139, 140, 140, 141, 142, 143, 143, 144, 145, 145, 146, 147, 147, 148, 149, 149, 150, 151, 152, 152, 153, 154, 154, 155, 156, 156, 157, 158, 159, 159, 160, 161, 161, 162, 163, 163, 164, 165, 165, 166, 167, 168, 168, 169, 170, 170, 171, 172, 172, 173, 174, 175, 175, 176, 177, 177, 178, 179, 179, 180, 181, 181, 182, 183, 184, 184, 185, 186, 186, 187, 188, 188, 189, 190, 191, 191, 192, 193, 193, 194, 195, 195, 196, 197, 197, 198, 199, 200, 200, 201, 202, 202, 203, 204, 204, 205, 206, 207, 207, 208, 209, 209, 210, 211, 211, 212, 213, 213, 214, 215, 216, 216, 217, 218, 218, 219, 220, 220, 221, 222, 223, 223, 224, 225, 225, 226, 227, 227, 228, 229, 229, 230, 231, 232, 232, 233, 234, 234, 235, 236, 236, 237, 238, 239, 239, 240, 241, 241, 242, 243, 243, 244, 245, 245, 246, 247, 248, 248, 249, 250, 250, 251, 252, 252, 253, 254, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255};
        int[] arrayOfInt2 = {0, 0, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 5, 5, 5, 6, 6, 6, 7, 7, 8, 8, 8, 9, 9, 10, 10, 10, 11, 11, 11, 12, 12, 13, 13, 13, 14, 14, 14, 15, 15, 16, 16, 16, 17, 17, 17, 18, 18, 18, 19, 19, 20, 20, 20, 21, 21, 21, 22, 22, 23, 23, 23, 24, 24, 24, 25, 25, 25, 25, 26, 26, 27, 27, 28, 28, 28, 28, 29, 29, 30, 29, 31, 31, 31, 31, 32, 32, 33, 33, 34, 34, 34, 34, 35, 35, 36, 36, 37, 37, 37, 38, 38, 39, 39, 39, 40, 40, 40, 41, 42, 42, 43, 43, 44, 44, 45, 45, 45, 46, 47, 47, 48, 48, 49, 50, 51, 51, 52, 52, 53, 53, 54, 55, 55, 56, 57, 57, 58, 59, 60, 60, 61, 62, 63, 63, 64, 65, 66, 67, 68, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 88, 89, 90, 91, 93, 94, 95, 96, 97, 98, 100, 101, 103, 104, 105, 107, 108, 110, 111, 113, 115, 116, 118, 119, 120, 122, 123, 125, 127, 128, 130, 132, 134, 135, 137, 139, 141, 143, 144, 146, 148, 150, 152, 154, 156, 158, 160, 163, 165, 167, 169, 171, 173, 175, 178, 180, 182, 185, 187, 189, 192, 194, 197, 199, 201, 204, 206, 209, 211, 214, 216, 219, 221, 224, 226, 229, 232, 234, 236, 239, 241, 245, 247, 250, 252, 255};
        for (int i = 0; i < 256; i++) {
            arrayOfByte[(i * 4)] = ((byte) arrayOfInt1[i]);
            arrayOfByte[(1 + i * 4)] = ((byte) arrayOfInt1[i]);
            arrayOfByte[(2 + i * 4)] = ((byte) arrayOfInt2[i]);
            arrayOfByte[(3 + i * 4)] = -1;
        }
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 256, 1, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ByteBuffer.wrap(arrayOfByte));
    }

    @Override
    public void draw() {
        GLES20.glUseProgram(mProgramHandle);
        GLUtil.checkGlError("glUseProgram");

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mInputTextureId[0]);
        GLES20.glUniform1i(muInputTextureLoc, 0);
        GLUtil.checkGlError("glBindTexture");

        GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFiltertTextureId[0]);
        GLES20.glUniform1i(muCurveLoc, 3);
        GLUtil.checkGlError("glBindTexture");

        GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, muPositionM, 0);
        GLUtil.checkGlError("glUniformMatrix4fv:muMVPMatrixLoc");

        GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, mTextureM, 0);
        GLUtil.checkGlError("glUniformMatrix4fv:muTexMatrixLoc");

        GLES20.glUniform1f(muTexelWidthOffsetLoc, 2.0F / mTextureWidth);
        GLES20.glUniform1f(muTexelHeightOffsetLoc, 2.0F / mTextureHeight);

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

        GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
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
        return mInputTextureId;
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
