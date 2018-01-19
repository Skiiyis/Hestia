package io.github.sawameimei.opengleslib.glprogram;

import android.opengl.GLES20;
import android.util.Size;

/**
 * Created by huangmeng on 2017/12/18.
 */

public class FBOGroupGLProgram implements GLProgram {

    private final GLProgram mInputProgram;
    private final TextureGLProgram mOutputProgram;
    private final TextureGLProgram mMiddleWareProgram[];
    private final int mViewportWidth;
    private final int mViewportHeight;
    private final int mInputTextureWidth;
    private final int mInputTextureHeight;

    private int[] mFrameBufferTextureId;
    private int[] mFrameBuffer;

    public FBOGroupGLProgram(Size textureSize, GLProgram inputProgram, TextureGLProgram outputProgram, TextureGLProgram... middleWareProgram) {
        this.mInputTextureWidth = textureSize.getWidth();
        this.mInputTextureHeight = textureSize.getHeight();
        this.mViewportWidth = mInputTextureWidth;
        this.mViewportHeight = mInputTextureHeight;
        this.mInputProgram = inputProgram;
        this.mOutputProgram = outputProgram;
        this.mMiddleWareProgram = middleWareProgram;
    }

    public FBOGroupGLProgram(int viewportWidth, int viewportHeight, int inputTextureWidth, int inputTextureHeight, GLProgram inputProgram, TextureGLProgram outputProgram, TextureGLProgram... middleWareProgram) {
        this.mViewportWidth = viewportWidth;
        this.mViewportHeight = viewportHeight;
        this.mInputTextureWidth = inputTextureWidth;
        this.mInputTextureHeight = inputTextureHeight;
        this.mInputProgram = inputProgram;
        this.mOutputProgram = outputProgram;
        this.mMiddleWareProgram = middleWareProgram;
    }

    @Override
    public void compile() {
        /**
         * 创建FBO,获取一个可用纹理ID
         */
        mFrameBuffer = new int[mMiddleWareProgram.length + 1];
        mFrameBufferTextureId = new int[mMiddleWareProgram.length + 1];
        for (int i = 0; i < mMiddleWareProgram.length + 1; i++) {
            GLES20.glGenFramebuffers(1, mFrameBuffer, i);
            GLES20.glGenTextures(1, mFrameBufferTextureId, i);

            /**
             * 创建一个空的2D纹理
             */
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameBufferTextureId[i]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mInputTextureWidth, mInputTextureHeight, 0,
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
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffer[i]);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                    GLES20.GL_TEXTURE_2D, mFrameBufferTextureId[i], 0);

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        }

        /**
         * 挂载中间件程序以及输出程序的纹理为该FBO关联的纹理，即从FBO上读取纹理
         */
        for (int i = 0; i < mMiddleWareProgram.length; i++) {
            mMiddleWareProgram[i].texture()[0] = mFrameBufferTextureId[i];
        }
        mOutputProgram.texture()[0] = mFrameBufferTextureId[mFrameBufferTextureId.length - 1];

        /**
         * 编译所有gl程序
         */
        mInputProgram.compile();
        for (TextureGLProgram glProgram : mMiddleWareProgram) {
            glProgram.compile();
        }
        mOutputProgram.compile();
    }

    /**
     * 绘制的时候，
     * 输入程序从外部读取纹理并渲染到FBO纹理上，
     * 中间件程序从FBO读取纹理并渲染到FBO纹理上，
     * 输出程序从FBO读取纹理并渲染到外部(Surface,FBO 等)
     */
    @Override
    public void draw() {
        GLES20.glViewport(0, 0, mInputTextureWidth, mInputTextureHeight);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffer[0]);
        mInputProgram.draw();
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        for (int i = 0; i < mMiddleWareProgram.length; i++) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffer[i + 1]);
            mMiddleWareProgram[i].draw();
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        }
        GLES20.glViewport(0, 0, mViewportWidth, mViewportHeight);
        mOutputProgram.draw();
    }

    @Override
    public void release() {
        mInputProgram.release();
        for (GLProgram glProgram : mMiddleWareProgram) {
            glProgram.release();
        }
        mOutputProgram.release();
        GLES20.glDeleteTextures(1, mFrameBufferTextureId, 0);
        GLES20.glDeleteFramebuffers(1, mFrameBuffer, 0);
    }
}
