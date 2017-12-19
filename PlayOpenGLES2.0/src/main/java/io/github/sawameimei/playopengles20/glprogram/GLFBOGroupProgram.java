package io.github.sawameimei.playopengles20.glprogram;

import android.opengl.GLES20;
import android.support.annotation.NonNull;
import android.util.Size;

/**
 * Created by huangmeng on 2017/12/18.
 */

public class GLFBOGroupProgram implements GLProgram {

    private final GLProgram mInputProgram;
    private final TextureGLProgram mOutputProgram;
    private final TextureGLProgram mMiddleWareProgram[];
    private Size mTextureSize;

    private int[] mFrameBufferTextureId;
    private int[] mFrameBuffer;

    public GLFBOGroupProgram(@NonNull Size textureSize, @NonNull GLProgram inputProgram, @NonNull TextureGLProgram outputProgram, TextureGLProgram... middleWareProgram) {
        this.mTextureSize = textureSize;
        this.mInputProgram = inputProgram;
        this.mOutputProgram = outputProgram;
        this.mMiddleWareProgram = middleWareProgram;
    }

    @Override
    public void compileAndLink() {
        /**
         * 创建FBO,获取一个可用纹理ID
         */
        mFrameBuffer = new int[1];
        mFrameBufferTextureId = new int[1];
        GLES20.glGenFramebuffers(1, mFrameBuffer, 0);
        GLES20.glGenTextures(1, mFrameBufferTextureId, 0);

        /**
         * 创建一个空的2D纹理
         */
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mTextureSize.getWidth(), mTextureSize.getHeight(), 0,
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
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffer[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, mFrameBufferTextureId[0], 0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        /**
         * 挂载中间件程序以及输出程序的纹理为该FBO关联的纹理，即从FBO上读取纹理
         */
        for (TextureGLProgram glProgram : mMiddleWareProgram) {
            glProgram.texture()[0] = mFrameBufferTextureId[0];
        }
        mOutputProgram.texture()[0] = mFrameBufferTextureId[0];

        /**
         * 编译所有gl程序
         */
        mInputProgram.compileAndLink();
        for (TextureGLProgram glProgram : mMiddleWareProgram) {
            glProgram.compileAndLink();
        }
        mOutputProgram.compileAndLink();
    }

    /**
     * 绘制的时候，
     * 输入程序从外部读取纹理并渲染到FBO纹理上，
     * 中间件程序从FBO读取纹理并渲染到FBO纹理上，
     * 输出程序从FBO读取纹理并渲染到外部(Surface,FBO 等)
     */
    @Override
    public void drawFrame() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffer[0]);
        mInputProgram.drawFrame();
        for (GLProgram glProgram : mMiddleWareProgram) {
            glProgram.drawFrame();
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        mOutputProgram.drawFrame();
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
