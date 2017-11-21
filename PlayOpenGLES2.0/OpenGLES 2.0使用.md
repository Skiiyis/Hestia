OpenGLES 2.0使用

##在使用前确定是否支持OpenGLES2.0

manifest里
<uses-feature
        android:glEsVersion="0x00020000"
        android:required="true" />

final ActivityManager activityManasger = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;

if (supportsEs2) 
{
	// Request an OpenGL ES 2.0 compatible context.
	mGLSurfaceView.setEGLContextClientVersion(2);

	// Set the renderer to our demo renderer, defined below.
	mGLSurfaceView.setRenderer(new Renderer());
} 


##在Render类中
	###需要初始化的要素（onSurfaceCreated）
		####背景
			GLES20.glClearColor
		####开启深度测试和不绘制背面（3D效果需要）
			// Use culling to remove back faces.
			GLES20.glEnable(GLES20.GL_CULL_FACE);

			// Enable depth testing
			GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		####视点
			Matrix.setLookAtM
		####顶点着色器的代码
		####片段着色器的代码
		####GL创建顶点着色器并获取顶点着色器的句柄
			int vertexShaderHandle = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
		####GL创建片段着色器并获取片段着色器的句柄
			int fragmentShaderHandle = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
		####GL编译并加载着色器（顶点着色器和片段着色器
    			GLES20.glShaderSource(shaderHandle, shaderSource);
   			GLES20.glCompileShader(shaderHandle);
			
			// Get the compilation status.
    			GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
    			if (compileStatus[0] == 0) {
        				GLES20.glDeleteShader(shaderHandle);
        				shaderHandle = 0;
    			}
		####GL创建程序并获取程序句柄
			int programHandle = GLES20.glCreateProgram();
		####GL绑定程序和着色器（通过程序句柄和着色器句柄
			GLES20.glAttachShader(programHandle, shaderHandle);
		####GL把顶点属性名对应于一个顶点属性索引
			//a_Vertex来源于vertexShaderSource
			GLES20.glBindAttribLocation(programHandle, aIndex, “a_Vertex”);
		####GL链接程序
			// Link the two shaders together into a program.
			GLES20.glLinkProgram(programHandle);

			// Get the link status.
			final int[] linkStatus = new int[1];
			GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);

			// If the link failed, delete the program.
			if (linkStatus[0] == 0) {
  				 GLES20.glDeleteProgram(programHandle);
    				programHandle = 0;
			}
		####GL获取程序中的属性（attr）的句柄
			int aHandler = GLES20.glGetUniformLocation(programHandle, “a_Name”);
		####GL使用程序开始渲染
			GLES20.glUseProgram(programHandle);
	
	###开始可见的初始化（onSurfaceChanged）
		####GL设置视口
			// Set the OpenGL viewport to the same size as the surface.
			GLES20.glViewport(0, 0, width, height);
		####GL设置场景
			// Create a new perspective projection matrix. The height will stay the same
			// while the width will vary as per aspect ratio.
			final float ratio = (float) width / height;
			final float left = -ratio;
			final float right = ratio;
			final float bottom = -1.0f;
			final float top = 1.0f;
			final float near = 1.0f;
			final float far = 10.0f;

			Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);

	###绘制时的操作（onDrawFrame）
		####GL清理深度测试和颜色缓存
			GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
		####定位最终视觉变化的矩阵
			//mMVPMatrix = mModelMatrix * mViewMatrix * mProjectionMatrix
			Matrix.multiplyMM
		####GL传递数据給Program中的属性，并开启着色点着色
		    //https://baike.baidu.com/item/glVertexAttribPointer/6483823?fr=aladdin
		    //属性句柄，每个顶点属性的组件数量，每个组件的数据类型，？？，连续顶点的偏移量 基本单位为byte，属性的值的buffer
			GLES20.glVertexAttribPointer(attrHandle, attrDateElementSize, GLES20.GL_FLOAT, false, stride, attrDataBuffer);
			GLES20.glEnableVertexAttribArray(attrHandle);
		####GL传递MVP矩阵給Program
			mMVPMatrixHandle = GLES20.glGetUniformLocation(programHandle, uniformAttrName);
			GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
		####GL绘制
			//绘制模式，开始位置，绘制几个顶点
			GLES20.glDrawArrays(GLES20.GL_TRIANGLES,offset,count);



		

	



			


