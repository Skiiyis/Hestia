package io.github.sawameimei.opengleslib.glprogram;

/**
 * Created by huangmeng on 2018/1/15.
 */

public interface VertexShader {
    int compile();

    void findLocation(int programHandle);

    void passData();

    void draw();

    void disable();
}
