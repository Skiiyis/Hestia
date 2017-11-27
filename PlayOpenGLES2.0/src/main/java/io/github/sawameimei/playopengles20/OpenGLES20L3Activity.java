package io.github.sawameimei.playopengles20;

import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGL14;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Random;

import io.github.sawameimei.playopengles20.common.EGLCore;
import io.github.sawameimei.playopengles20.common.GLUtil;
import io.github.sawameimei.playopengles20.common.RawResourceReader;
import io.github.sawameimei.playopengles20.common.ShaderHelper;
import io.github.sawameimei.playopengles20.common.TextureHelper;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class OpenGLES20L3Activity extends AppCompatActivity implements SurfaceTexture.OnFrameAvailableListener, TextureView.SurfaceTextureListener {

    private static String TAG = "OpenGLES2.0";
    private EGLCore mEGLCore;
    private int mProgramHandle;
    private int mFragmentShaderHandle;
    private int mVertexShaderHandle;
    private int mTextureHandle;
    private Handler mMainHandler = new Handler() {
        @Override
        public void dispatchMessage(Message msg) {
            drawFrame();
        }
    };
    private EGLSurface mWindowSurfaceHandle;
    private SurfaceTexture mCameraSurfaceTexture;
    private float[] MATRIX_IDENTITY = new float[16];
    private int muMVPMatrixLoc;
    private int muTexMatrixLoc;
    private int maPositionLoc;
    private int maTextureCoordLoc;

    private static final float FULL_RECTANGLE_COORDS[] = {
            -1.0f, -1.0f,   // 0 bottom left
            1.0f, -1.0f,   // 1 bottom right
            -1.0f, 1.0f,   // 2 top left
            1.0f, 1.0f,   // 3 top right
    };

    private static final float FULL_RECTANGLE_TEX_COORDS[] = {
            0.0f, 0.0f,     // 0 bottom left
            1.0f, 0.0f,     // 1 bottom right
            0.0f, 1.0f,     // 2 top left
            1.0f, 1.0f      // 3 top right
    };

    private ByteBuffer rectangleVertexBuffer;
    private ByteBuffer rectangleTextureBuffer;
    private int mWidth;
    private int mHeight;
    private TextureView mTextureView;

    {
        Matrix.setIdentityM(MATRIX_IDENTITY, 0);
        //Matrix.scaleM(MATRIX_IDENTITY, 0, -1, 1, 1);
        //Matrix.rotateM(MATRIX_IDENTITY, 0, 270, 0, 0, 1);

        rectangleVertexBuffer = ByteBuffer.allocateDirect(FULL_RECTANGLE_COORDS.length * 4);
        rectangleVertexBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer().put(FULL_RECTANGLE_COORDS);

        rectangleTextureBuffer = ByteBuffer.allocateDirect(FULL_RECTANGLE_COORDS.length * 4);
        rectangleTextureBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer().put(FULL_RECTANGLE_TEX_COORDS);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{RECORD_AUDIO, READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE, INTERNET, CAMERA}, 1);
        }

        mTextureView = new TextureView(this);
        mTextureView.setSurfaceTextureListener(this);
        Display defaultDisplay = getWindowManager().getDefaultDisplay();
        int width = defaultDisplay.getWidth();
        //int height = width * 1280 / 720;
        int height = width * 720 / 1280;
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(width, height);
        layoutParams.gravity = Gravity.CENTER;
        setContentView(mTextureView, layoutParams);
    }

    private void drawFrame() {
        mEGLCore.makeCurrent(mWindowSurfaceHandle, mWindowSurfaceHandle);
        mCameraSurfaceTexture.updateTexImage();
        GLUtil.checkGlError("draw start");

        GLES20.glViewport(0, 0, mWidth, mHeight);
        GLES20.glUseProgram(mProgramHandle);
        GLUtil.checkGlError("glUseProgram");

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureHandle);
        GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, MATRIX_IDENTITY, 0);
        GLUtil.checkGlError("glUniformMatrix4fv:muMVPMatrixLoc");

        float[] textMatrix = new float[16];
        mCameraSurfaceTexture.getTransformMatrix(textMatrix);
        GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, textMatrix, 0);
        GLUtil.checkGlError("glUniformMatrix4fv:muTexMatrixLoc");

        GLES20.glVertexAttribPointer(maPositionLoc, 2, GLES20.GL_FLOAT, false, 8, rectangleVertexBuffer.position(0));
        GLES20.glEnableVertexAttribArray(maPositionLoc);
        GLUtil.checkGlError("glEnableVertexAttribArray:maPositionLoc");

        GLES20.glVertexAttribPointer(maTextureCoordLoc, 2, GLES20.GL_FLOAT, false, 8, rectangleTextureBuffer.position(0));
        GLES20.glEnableVertexAttribArray(maTextureCoordLoc);
        GLUtil.checkGlError("glEnableVertexAttribArray:muTexMatrixLoc");

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(maPositionLoc);
        GLES20.glDisableVertexAttribArray(maTextureCoordLoc);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        GLES20.glUseProgram(0);

        //drawExtra(new Random().nextInt(2), mWidth, mHeight);
        mEGLCore.swapBuffers(mWindowSurfaceHandle);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        mMainHandler.sendEmptyMessage(1);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        try {
            mWidth = width;
            mHeight = height;

            mEGLCore = new EGLCore(null, EGLCore.FLAG_RECORDABLE);
            mWindowSurfaceHandle = mEGLCore.createWindowSurface(surface);
            mEGLCore.makeCurrent(mWindowSurfaceHandle, mWindowSurfaceHandle);

            mVertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, RawResourceReader.readTextFileFromRawResource(this, R.raw.lesson3_vertex_sharder_source));
            mFragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, RawResourceReader.readTextFileFromRawResource(this, R.raw.lesson3_fragment_sharder_source));
            mProgramHandle = ShaderHelper.createAndLinkProgram(mVertexShaderHandle, mFragmentShaderHandle, new String[]{"aPosition", "aTextureCoord"});

            muMVPMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
            maPositionLoc = GLES20.glGetAttribLocation(mProgramHandle, "aPosition");
            maTextureCoordLoc = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord");
            muTexMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexMatrix");

            mTextureHandle = TextureHelper.loadOESTexture();

            mCameraSurfaceTexture = new SurfaceTexture(mTextureHandle);
            mCameraSurfaceTexture.setOnFrameAvailableListener(this);

            boolean hasCamera = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
            if (!hasCamera) {
                Toast.makeText(this, "没有摄像头！！", Toast.LENGTH_SHORT).show();
                return;
            }
            Camera camera = null;
            Camera.CameraInfo info = new Camera.CameraInfo();
            // Try to find a front-facing camera (e.g. for videoconferencing).
            int numCameras = Camera.getNumberOfCameras();
            for (int i = 0; i < numCameras; i++) {
                Camera.getCameraInfo(i, info);
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    camera = Camera.open(i);
                    break;
                }
            }
            if (camera == null) {
                Log.d(TAG, "No front-facing camera found; opening default");
                camera = Camera.open();    // opens first back-facing camera
            }
            Camera.Parameters parms = camera.getParameters();
            //choosePreviewSize(parms, 720, 1280);
            choosePreviewSize(parms, 1280, 720);
            chooseFixedPreviewFps(parms, 15);
            camera.setParameters(parms);
            parms.setRecordingHint(true);
            camera.setPreviewTexture(mCameraSurfaceTexture);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    /**
     * Attempts to find a preview size that matches the provided width and height (which
     * specify the dimensions of the encoded video).  If it fails to find a match it just
     * uses the default preview size for video.
     * <p>
     * TODO: should do a best-fit match, e.g.
     * https://github.com/commonsguy/cwac-camera/blob/master/camera/src/com/commonsware/cwac/camera/CameraUtils.java
     */
    public static void choosePreviewSize(Camera.Parameters parms, int width, int height) {
        // We should make sure that the requested MPEG size is less than the preferred
        // size, and has the same aspect ratio.
        Camera.Size ppsfv = parms.getPreferredPreviewSizeForVideo();
        if (ppsfv != null) {
            Log.d(TAG, "Camera preferred preview size for video is " +
                    ppsfv.width + "x" + ppsfv.height);
        }

        //for (Camera.Size size : parms.getSupportedPreviewSizes()) {
        //    Log.d(TAG, "supported: " + size.width + "x" + size.height);
        //}

        for (Camera.Size size : parms.getSupportedPreviewSizes()) {
            if (size.width == width && size.height == height) {
                parms.setPreviewSize(width, height);
                return;
            }
        }

        Log.w(TAG, "Unable to set preview size to " + width + "x" + height);
        if (ppsfv != null) {
            parms.setPreviewSize(ppsfv.width, ppsfv.height);
        }
        // else use whatever the default size is
    }

    /**
     * Attempts to find a fixed preview frame rate that matches the desired frame rate.
     * <p>
     * It doesn't seem like there's a great deal of flexibility here.
     * <p>
     * TODO: follow the recipe from http://stackoverflow.com/questions/22639336/#22645327
     *
     * @return The expected frame rate, in thousands of frames per second.
     */
    public static int chooseFixedPreviewFps(Camera.Parameters parms, int desiredThousandFps) {
        List<int[]> supported = parms.getSupportedPreviewFpsRange();

        for (int[] entry : supported) {
            //Log.d(TAG, "entry: " + entry[0] + " - " + entry[1]);
            if ((entry[0] == entry[1]) && (entry[0] == desiredThousandFps)) {
                parms.setPreviewFpsRange(entry[0], entry[1]);
                return entry[0];
            }
        }

        int[] tmp = new int[2];
        parms.getPreviewFpsRange(tmp);
        int guess;
        if (tmp[0] == tmp[1]) {
            guess = tmp[0];
        } else {
            guess = tmp[1] / 2;     // shrug
        }

        Log.d(TAG, "Couldn't find match for " + desiredThousandFps + ", using " + guess);
        return guess;
    }

    /**
     * Adds a bit of extra stuff to the display just to give it flavor.
     */
    private static void drawExtra(int frameNum, int width, int height) {
        // We "draw" with the scissor rect and clear calls.  Note this uses window coordinates.
        int val = frameNum % 3;
        switch (val) {
            case 0:
                GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
                break;
            case 1:
                GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
                break;
            case 2:
                GLES20.glClearColor(0.0f, 0.0f, 1.0f, 1.0f);
                break;
        }

        int xpos = (int) (width * ((frameNum % 100) / 100.0f));
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
        GLES20.glScissor(xpos, 0, width / 32, height / 32);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
    }
}
