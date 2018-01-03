package io.github.sawameimei.playopengles20;

import android.opengl.Matrix;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {

    public void matrixTest() throws Exception {
        float[] identityM = new float[16];
        Matrix.setIdentityM(identityM, 0);

    }

    private void printMatrix(float[] m, int line) {

    }
}