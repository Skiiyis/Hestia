package io.github.sawameimei.playopengles20.common;

/**
 * Created by huangmeng on 2017/12/8.
 */
public interface GLProgram {

    /**
     * @return programHandle
     */
    void compileAndLink();

    void drawFrame();

    void release();
}
