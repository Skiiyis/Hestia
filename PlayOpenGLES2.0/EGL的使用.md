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


//EGL中几个关键对象的对应关系
EGLContext 为一个GL程序执行的上下文对象。可以包含一个或多个GLProgram,一个或多个Texture的数据。
           EGLContext默认和当前线程绑定，一个EGLContext不可以绑定到多个线程？
           可以通过createContext方法传入一个原有EGLContext来线程共享同一个EGLContext
GLProgram  通过GLES20.glCreateProgram创建，和当前EGLContext绑定。需要共享GLProgram的时候必须通过共享EGLContext;
EGLSurface 为一个GL程序执行的目的地（draw/read) 一个EGLSurface的创建和EGLContext无关，只和EGLDisplay,EGLConfig有关
           一个EGLSurface可以通过makeCurrent方法  绑定到一个EGLContext，并且同时只能绑定到一个EGLContext
GLTexture  通过GLES20.glGenTextures生成的texure和EGLContext无关，但更新到这个texture上的内容和EGLContext有关
           线程间共享texture内容必须通过共享EGLContext;推测bindTexture方法和当前EGLContext有关。

//makeCurrent方法中的参数意义
public static native boolean eglMakeCurrent(
        EGLDisplay dpy,  //绑定到的Display
        EGLSurface draw, //绑定的写渲染缓冲区，该缓冲区可以同时为读渲染缓冲区
        EGLSurface read, //绑定的读渲染缓冲区，该缓冲区可以同时为写渲染缓冲区
        EGLContext ctx   //绑定到的EGLContext
    );
draw 为GLES的draw指令的缓冲区，即drawArrays等命令会将数据填充到该缓冲区
read 为GLES的read指令的缓冲区，即readPixes等会将会从该缓冲区读取数据出来

//https://katatunix.wordpress.com/2014/09/17/lets-talk-about-eglmakecurrent-eglswapbuffers-glflush-glfinish/
GLES30.glBlitFramebuffer 这个命令可以将read中的数据拷贝到draw中。

//某些手机上的EGLContext共享问题
比较代码
1.
                mTextureId = TextureHelper.loadOESTexture();
                mPrevProgram = new CameraPrevGLProgram(getApplicationContext(), mTextureM, mTextureId);
                mPrevSurfaceTexture = new SurfaceTexture(mTextureId);
                mPrevSurfaceTexture.setOnFrameAvailableListener(OpenGLES20L5Activity.this);

                mEGLCore = new EGLCore(null, EGLCore.FLAG_RECORDABLE);
                mPreviewSurface = mEGLCore.createWindowSurface(holder.getSurface());
                mEGLCore.makeCurrent(mPreviewSurface);
和
2.
                mEGLCore = new EGLCore(null, EGLCore.FLAG_RECORDABLE);
                mPreviewSurface = mEGLCore.createWindowSurface(holder.getSurface());
                mEGLCore.makeCurrent(mPreviewSurface);

                mTextureId = TextureHelper.loadOESTexture();
                mPrevProgram = new CameraPrevGLProgram(getApplicationContext(), mTextureM, mTextureId);
                mPrevSurfaceTexture = new SurfaceTexture(mTextureId);
                mPrevSurfaceTexture.setOnFrameAvailableListener(OpenGLES20L5Activity.this);

在真机上没有makeCurrent就生成Texture会导致Texture不可跨线程共享。。
模拟器可以跨线程共享？模拟器上的线程不是操作系统级别的线程?

//滤镜的制作，贴图的制作
http://blog.csdn.net/oshunz/article/details/50214449



