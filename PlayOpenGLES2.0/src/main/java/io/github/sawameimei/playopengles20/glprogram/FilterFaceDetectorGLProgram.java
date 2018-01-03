package io.github.sawameimei.playopengles20.glprogram;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.FaceDetector;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import io.github.sawameimei.opengleslib.common.GLUtil;
import io.github.sawameimei.opengleslib.common.GLVertex;
import io.github.sawameimei.opengleslib.common.RawResourceReader;
import io.github.sawameimei.opengleslib.common.ShaderHelper;
import io.github.sawameimei.opengleslib.glprogram.TextureGLProgram;
import io.github.sawameimei.playopengles20.R;

/**
 * Created by huangmeng on 2018/1/2.
 */
public class FilterFaceDetectorGLProgram implements TextureGLProgram, Camera.FaceDetectionListener {

    private final WeakReference<Context> mContext;
    private final float[] mTextureM;
    private final int mPrevWidth;
    private final int mPrevHeight;
    private int[] inputTextureId = new int[1];
    private ByteBuffer mPixelBuf;

    private final FullRectangleTextureCoords mFullRectangleTextureCoords = new FullRectangleTextureCoords();
    private final FullRectangleCoords mFullRectangleCoords = new FullRectangleCoords();

    private int mVertexShaderHandle;
    private int mFragmentShaderHandle;
    private int mProgramHandle;
    private int muMVPMatrixLoc;
    private int maPositionLoc;
    private int maTextureCoordLoc;
    private int muTexMatrixLoc;
    private int msInputTextureLoc;

    private float[] muPositionM = GLUtil.getIdentityM();
    private int[] mFrameBufferHandle = new int[1];
    private int[] mFrameBufferTextureId = new int[1];

    private int muLeftEyePointCoordLoc;
    private int muRightEyePointCoordLoc;
    private int muMouthPointCoordLoc;

    {
        Matrix.scaleM(muPositionM, 0, -1, 1, 1);
        Matrix.rotateM(muPositionM, 0, 180F, 0, 0, 1);
    }

    public FilterFaceDetectorGLProgram(Context context, float[] textureM, int textureId, int prevWidth, int prevHeight) {
        this.mContext = new WeakReference<>(context);
        this.mTextureM = textureM;

        this.inputTextureId[0] = textureId;
        this.mPrevWidth = prevWidth;
        this.mPrevHeight = prevHeight;
        //mFullRectangleTextureCoords = new CameraPrevGLProgram.FullRectangleTextureCoords();
        //mFullRectangleCoords = new CameraPrevGLProgram.FullRectangleCoords();
    }

    @Override
    public int[] texture() {
        return inputTextureId;
    }

    @Override
    public void compileAndLink() {
        mPixelBuf = ByteBuffer.allocate(mPrevWidth * mPrevHeight * 4);

        mVertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, RawResourceReader.readTextFileFromRawResource(mContext.get(), R.raw.filter_face_detector_vertex_sharder));
        mFragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, RawResourceReader.readTextFileFromRawResource(mContext.get(), R.raw.filter_face_detector_fragment_sharder));
        mProgramHandle = ShaderHelper.createAndLinkProgram(mVertexShaderHandle, mFragmentShaderHandle, new String[]{"aPosition", "aTextureCoord"});

        muMVPMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
        maPositionLoc = GLES20.glGetAttribLocation(mProgramHandle, "aPosition");
        maTextureCoordLoc = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord");
        muTexMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexMatrix");
        msInputTextureLoc = GLES20.glGetUniformLocation(mProgramHandle, "sTexture");

        muLeftEyePointCoordLoc = GLES20.glGetUniformLocation(mProgramHandle, "uLeftEyePointCoord");
        muRightEyePointCoordLoc = GLES20.glGetUniformLocation(mProgramHandle, "uRightEyePointCoord");
        muMouthPointCoordLoc = GLES20.glGetUniformLocation(mProgramHandle, "uMouthPointCoord");


        GLES20.glGenFramebuffers(1, mFrameBufferHandle, 0);
        GLES20.glGenTextures(1, mFrameBufferTextureId, 0);

        /**
         * 创建一个空的2D纹理
         */
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameBufferTextureId[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mPrevWidth, mPrevHeight, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        /**
         * 挂载该纹理到FBO上
         */
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferHandle[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, mFrameBufferTextureId[0], 0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

    }

    @Override
    public void drawFrame() {
        GLES20.glUseProgram(mProgramHandle);
        GLUtil.checkGlError("glUseProgram");

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTextureId[0]);
        GLES20.glUniform1i(msInputTextureLoc, 0);
        GLUtil.checkGlError("glBindTexture:mTextureHandle");

        GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, muPositionM, 0);
        GLUtil.checkGlError("glUniformMatrix4fv:muMVPMatrixLoc");

        /*StringBuilder sb = new StringBuilder();
        for (float f : mTextureM) {
            sb.append(f);
            sb.append(",");
        }
        Log.e("textureM", sb.toString());*/
        GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, mTextureM, 0);
        GLUtil.checkGlError("glUniformMatrix4fv:muTexMatrixLoc");

        GLES20.glVertexAttribPointer(maPositionLoc, mFullRectangleCoords.getSize(), GLES20.GL_FLOAT, false, mFullRectangleCoords.getStride(), mFullRectangleCoords.toByteBuffer().position(0));
        GLES20.glEnableVertexAttribArray(maPositionLoc);
        GLUtil.checkGlError("glEnableVertexAttribArray:maPositionLoc");

        GLES20.glVertexAttribPointer(maTextureCoordLoc, mFullRectangleTextureCoords.getSize(), GLES20.GL_FLOAT, false, mFullRectangleTextureCoords.getStride(), mFullRectangleTextureCoords.toByteBuffer().position(0));
        GLES20.glEnableVertexAttribArray(maTextureCoordLoc);
        GLUtil.checkGlError("glEnableVertexAttribArray:maTextureCoordLoc");

        /**
         * 传入人脸特征点
         */
        GLES20.glUniform2fv(muLeftEyePointCoordLoc, 1, FloatBuffer.wrap(mLeftEye));
        GLUtil.checkGlError("glUniformMatrix4fv:muPointCoordLoc");
        GLES20.glUniform2fv(muRightEyePointCoordLoc, 1, FloatBuffer.wrap(mRightEye));
        GLUtil.checkGlError("glUniformMatrix4fv:muPointCoordLoc");
        GLES20.glUniform2fv(muMouthPointCoordLoc, 1, FloatBuffer.wrap(mMouth));
        GLUtil.checkGlError("glUniformMatrix4fv:muPointCoordLoc");

        /**
         * 使用FBO加速readPixels,readPixels的速度从200ms提升到8ms (wocao
         */
        //long startTime = System.currentTimeMillis();
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferHandle[0]);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, mFullRectangleCoords.getCount());
        GLES20.glReadPixels(0, 0, mPrevWidth, mPrevHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mPixelBuf);
        GLUtil.checkGlError("glDrawArrays");
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        //long usedTime = System.currentTimeMillis() - startTime;
        //Log.e("readPixel", "time:" + usedTime + ",width:" + mPrevWidth + ",height:" + mPrevHeight);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, mFullRectangleCoords.getCount());
        GLUtil.checkGlError("glDrawArrays");

        /*startTime = System.currentTimeMillis();
        faceDetect();
        usedTime = System.currentTimeMillis() - startTime;
        Log.e("faceDetectTime", ":" + usedTime);*/

        GLES20.glDisableVertexAttribArray(maPositionLoc);
        GLES20.glDisableVertexAttribArray(maTextureCoordLoc);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glUseProgram(0);
        GLUtil.checkGlError("disable");
    }

    @Override
    public void release() {
        GLES20.glDeleteProgram(mProgramHandle);
    }

    /**
     * android 自带的人脸检测没有特征点检测？？速度也很慢，在200～300ms上下
     */
    public void faceDetect() {
        Bitmap bitmap = Bitmap.createBitmap(mPrevWidth, mPrevHeight, Bitmap.Config.ARGB_8888);
        ByteBuffer b = ByteBuffer.wrap(mPixelBuf.array());
        bitmap.copyPixelsFromBuffer(b);
        FaceDetector faceDetector = new FaceDetector(mPrevWidth, mPrevHeight, 5);
        FaceDetector.Face[] faces = new FaceDetector.Face[5];
        int facesNum = faceDetector.findFaces(bitmap, faces);
        for (int i = 0; i < facesNum; i++) {
            PointF point = new PointF();
            faces[0].getMidPoint(point);
            Log.e("faceDetect", "midPoint:" + point);
        }
        bitmap.recycle();
    }

    //private long mStartTime;
    private float[] mLeftEye = new float[]{0F, 0F};
    private float[] mRightEye = new float[]{0F, 0F};
    private float[] mMouth = new float[]{0F, 0F};

    @Override
    public void onFaceDetection(Camera.Face[] faces, Camera camera) {
        /*if (mStartTime == 0) {
            mStartTime = System.currentTimeMillis();
            return;
        }
        long usedTime = System.currentTimeMillis() - mStartTime;
        Log.e("onFaceDetection", "usedTime:" + usedTime);
        mStartTime = System.currentTimeMillis();*/
        for (int i = 0; i < faces.length; i++) {
            Camera.Face face = faces[i];
            float leftX = 1.0F - (face.leftEye.x + 1000F) / 2000F;
            float leftY = 1.0F - (face.leftEye.y + 1000F) / 2000F;
            float rightX = 1.0F - (face.rightEye.x + 1000F) / 2000F;
            float rightY = 1.0F - (face.rightEye.y + 1000F) / 2000F;
            float mouthX = 1.0F - (face.mouth.x + 1000F) / 2000F;
            float mouthY = 1.0F - (face.mouth.y + 1000F) / 2000F;
            mLeftEye[0] = leftY;
            mLeftEye[1] = leftX;
            mRightEye[0] = rightY;
            mRightEye[1] = rightX;
            mMouth[0] = mouthY;
            mMouth[1] = mouthX;
            //mPointCoords.add(rectCalculate(face.rect));
            Log.e("faceDetect", "leftEyes:" + leftX + "," + leftY +
                    ",rightEyes:" + rightX + "," + rightY +
                    ",mouth:" + mouthX + "," + mouthY);
        }
    }

    private Rect rectCalculate(Rect rect) {
        rect.left = mPrevWidth * (rect.left + 1000) / 2000;
        rect.right = mPrevWidth * (rect.right + 1000) / 2000;
        rect.top = mPrevHeight * (rect.top + 1000) / 2000;
        rect.bottom = mPrevHeight * (rect.bottom + 1000) / 2000;
        return rect;
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
