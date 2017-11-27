EGL的使用

## 简介
EGL是OpenGL ES和底层的native window system之间的接口，使用EGL可以脱离GLSurface来实现GL下的渲染

## 初始化EGL环境
1.初始化EGLDisplay
EGLDisplay mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1);

2.初始化EGLConfig
int[] attribList = {
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                //EGL14.EGL_DEPTH_SIZE, 16,
                //EGL14.EGL_STENCIL_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, renderableType,
                EGL14.EGL_NONE, 0,      // placeholder for recordable [@-3]
                EGL14.EGL_NONE
        }
EGLConfig[] configs = new EGLConfig[1];
EGL14.eglChooseConfig(mEGLDisplay, attribList, 0, configs, 0, configs.length, numConfigs, 0);

3.初始化EGLContext
int[] attrib2_list = {
                    EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                    EGL14.EGL_NONE
            };
EGLContext mEGLContext = EGL14.eglCreateContext(mEGLDisplay, mEGLConfig, mSharedEGLContext, attrib2_list, 0);

## EGL使用
1.创建EGLSurface
int[] surfaceAttribs = {
                EGL14.EGL_NONE
        };
EGLSurface eglSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, mEGLConfig, Surface/SurfaceTexture, surfaceAttribs, 0);

2.选择使用当前EGLSurface绘制或读取
EGL14.eglMakeCurrent(mEGLDisplay, mDrawEglSurface, mReadEglSurface, mEGLContext);

3.准备好GLProgram等其他GL要素
4.创建一个GLTexture并获取TextureHandle传递给SurfaceTexture绑定
5.SurfaceTexture设置setOnFrameAvailableListener回调
6.Camera设置并开始预览并将预览输出到SurfaceTexture

## 在SurfaceTexture的onFrameAvailable回调中刷新帧数据
DisplayEGLSurface.makeCurrent();
SurfaceTexture.updateTexImage();
GL绘制数据
EGL14.eglSwapBuffers(mEGLDisplay,mEGLSurface);


