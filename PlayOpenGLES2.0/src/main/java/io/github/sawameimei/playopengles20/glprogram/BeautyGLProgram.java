package io.github.sawameimei.playopengles20.glprogram;

/**
 * Created by huangmeng on 2017/12/19.
 */

public class BeautyGLProgram implements TextureGLProgram {

    private int[] mTextureId = new int[1];

    public BeautyGLProgram() {

    }

    @Override
    public void compileAndLink() {

    }

    @Override
    public void drawFrame() {

    }

    @Override
    public void release() {

    }

    @Override
    public int[] texture() {
        return mTextureId;
    }
}
