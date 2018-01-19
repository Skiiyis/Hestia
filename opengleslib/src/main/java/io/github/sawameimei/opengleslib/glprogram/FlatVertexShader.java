package io.github.sawameimei.opengleslib.glprogram;

import android.opengl.GLES20;

import io.github.sawameimei.opengleslib.common.GLUtil;
import io.github.sawameimei.opengleslib.common.GLVertex;
import io.github.sawameimei.opengleslib.common.ShaderHelper;

/**
 * Created by huangmeng on 2018/1/15.
 */

public class FlatVertexShader implements VertexShader {

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

    private final FullRectangleCoords mFullRectangleCoords = new FullRectangleCoords();
    private final FullRectangleTextureCoords mFullRectangleTextureCoords = new FullRectangleTextureCoords();

    private int muMVPMatrixLoc;
    private int maPositionLoc;
    private int maTextureCoordLoc;
    private int muTexMatrixLoc;

    private float[] muPositionM = GLUtil.getIdentityM();
    private float[] mTextureM = GLUtil.getIdentityM();

    @Override
    public int compile() {
        return ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
    }

    @Override
    public void findLocation(int programHandle) {
        muMVPMatrixLoc = GLES20.glGetUniformLocation(programHandle, "uMVPMatrix");
        maPositionLoc = GLES20.glGetAttribLocation(programHandle, "aPosition");
        maTextureCoordLoc = GLES20.glGetAttribLocation(programHandle, "aTextureCoord");
        muTexMatrixLoc = GLES20.glGetUniformLocation(programHandle, "uTexMatrix");
    }

    @Override
    public void passData() {
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
    }

    @Override
    public void disable() {
        GLES20.glDisableVertexAttribArray(maPositionLoc);
        GLES20.glDisableVertexAttribArray(maTextureCoordLoc);
    }

    @Override
    public void draw() {
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, mFullRectangleCoords.getCount());
        GLUtil.checkGlError("glDrawArrays");
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
