package io.github.sawameimei.opengleslib.glprogram;

/**
 * Created by huangmeng on 2018/1/15.
 */

public interface FragmentShader {
    int compile();

    void findLocation(int programHandle);

    void passData();

    void disable();
}
