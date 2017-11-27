package io.github.sawameimei.playopengles20.common;

import android.opengl.GLES20;
import android.util.Log;

/**
 * Created by huangmeng on 2017/11/24.
 */

public class GLUtil {

    public static void checkGlError(String op) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            String msg = op + ": glError 0x" + Integer.toHexString(error);
            Log.e("OpenGLES2.0", msg);
            throw new RuntimeException(msg);
        }
    }
}
