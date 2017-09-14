package io.github.sawameimei.mediacodec.mediaCodec2;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by huangmeng on 2017/9/14.
 */

/**
 * 这里把所有的从摄像头获取的图像数据使用gl画出来，并读取到buffer里
 */
public class PreviewSurfaceView extends SurfaceView implements SurfaceHolder.Callback, SurfaceTexture.OnFrameAvailableListener {

    private Texture2dProgram program;

    public PreviewSurfaceView(Context context) {
        super(context);
        init();
    }

    public PreviewSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PreviewSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        this.getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        /*if (!EGL14.eglMakeCurrent(EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY), eglSurface, eglSurface, mEGLContext)) {
            throw new RuntimeException("eglMakeCurrent failed");
        }*/

        program = new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT);
        //生成一个材质并获取该材质的id
        int textureId = program.createTextureObject();
        SurfaceTexture surfaceTexture = new SurfaceTexture(textureId);
        surfaceTexture.setOnFrameAvailableListener(this);

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {

    }

    public void release() {
        if (program != null) {
            program.release();
        }
    }
}
