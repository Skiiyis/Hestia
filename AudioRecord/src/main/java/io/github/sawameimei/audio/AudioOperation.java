package io.github.sawameimei.audio;

/**
 * Created by huangmeng on 2017/9/1.
 */

public interface AudioOperation {

    String filePath();

    void startRecord();

    void stopRecord();

    void play();
}
