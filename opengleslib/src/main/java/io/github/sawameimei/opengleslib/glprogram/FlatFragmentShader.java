package io.github.sawameimei.opengleslib.glprogram;

import android.opengl.GLES20;

import io.github.sawameimei.opengleslib.common.GLUtil;
import io.github.sawameimei.opengleslib.common.ShaderHelper;

/**
 * Created by huangmeng on 2018/1/15.
 */

public class FlatFragmentShader implements FragmentShader {

    private static final String FRAGMENT_SHADER = "" +
            "precision mediump float;\n" +
            "varying vec2 vTextureCoord;\n" +
            "uniform sampler2D sTexture;\n" +
            "\n" +
            "void main() {\n" +
            "    gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
            "}\n";

    private final int mTextureId;
    private int msInputTextureLoc;

    public FlatFragmentShader(int textureId) {
        this.mTextureId = textureId;
    }

    @Override
    public int compile() {
        return ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, FRAGMENT_SHADER);
    }

    @Override
    public void findLocation(int programHandle) {
        msInputTextureLoc = GLES20.glGetUniformLocation(programHandle, "sTexture");
    }

    @Override
    public void passData() {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
        GLES20.glUniform1i(msInputTextureLoc, 0);
        GLUtil.checkGlError("glBindTexture:mTextureHandle");
    }

    @Override
    public void disable() {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }
}
