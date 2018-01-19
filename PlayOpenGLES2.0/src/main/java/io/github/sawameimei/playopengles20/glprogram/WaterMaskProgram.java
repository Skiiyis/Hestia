package io.github.sawameimei.playopengles20.glprogram;

import android.opengl.GLES20;

import io.github.sawameimei.opengleslib.common.GLUtil;
import io.github.sawameimei.opengleslib.common.GLVertex;
import io.github.sawameimei.opengleslib.common.ShaderHelper;
import io.github.sawameimei.opengleslib.glprogram.TextureGLProgram;

/**
 * Created by huangmeng on 2018/1/16.
 */

public class WaterMaskProgram implements TextureGLProgram {

    private int[] mTextureId = new int[1];

    private final FullRectangleCoords mFullRectangleCoords = new FullRectangleCoords();
    private final FullRectangleTextureCoords mFullRectangleTextureCoords = new FullRectangleTextureCoords();

    private int mVertexShaderHandle;
    private int mFragmentShaderHandle;
    private int mProgramHandle;

    private int muMVPMatrixLoc;
    private int maPositionLoc;
    private int maTextureCoordLoc;
    private int muTexMatrixLoc;
    private int muTexture;

    private float[] mTextureM = GLUtil.getIdentityM();
    private float[] muPositionM = GLUtil.getIdentityM();

    private static final String VERTEX_SHADER = "" +
            "uniform mat4 uMVPMatrix;\n" +
            "uniform mat4 uTexMatrix;\n" +
            "attribute vec4 aPosition;\n" +
            "attribute vec4 aTextureCoord;\n" +
            "varying vec2 vTextureCoord;\n" +
            "\n" +
            "void main() {\n" +
            "    gl_Position = uMVPMatrix * aPosition;\n" +
            "    vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n" +
            "}";

    private static final String FRAGMENT_SHADER = "" +
            "precision mediump float;\n" +
            "varying vec2 vTextureCoord;\n" +
            "uniform sampler2D sTexture;\n" +
            "\n" +
            "void main() {\n" +
            "    vec4 sampleColor = texture2D(sTexture, vTextureCoord);\n" +
            "   if(sampleColor.a == 0.0){" +
            "       gl_FragColor = vec4(0.0,0.0,0.0,0.0);" +
            "   } else {" +
            "       gl_FragColor = sampleColor;" +
            "   }" +
            "}\n";

    @Override
    public int[] texture() {
        return mTextureId;
    }

    @Override
    public void compile() {
        mVertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        mFragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        mProgramHandle = ShaderHelper.createAndLinkProgram(mVertexShaderHandle, mFragmentShaderHandle, new String[]{"aPosition", "aTextureCoord"});

        muMVPMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
        maPositionLoc = GLES20.glGetAttribLocation(mProgramHandle, "aPosition");
        maTextureCoordLoc = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord");
        muTexMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexMatrix");
        muTexture = GLES20.glGetUniformLocation(mProgramHandle, "sTexture");
    }

    @Override
    public void draw() {
        GLES20.glUseProgram(mProgramHandle);
        GLUtil.checkGlError("glUseProgram");

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId[0]);
        GLES20.glUniform1i(muTexture, 0);
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
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        GLES20.glUseProgram(0);
        GLUtil.checkGlError("disable");
    }

    @Override
    public void release() {
        GLES20.glDeleteProgram(mProgramHandle);
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
