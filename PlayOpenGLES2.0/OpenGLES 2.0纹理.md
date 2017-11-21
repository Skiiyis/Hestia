OpenGLES 2.0纹理

##在Render类中
	###初始化时（onSurfaceCreated）
		####GL生成空纹理并将句柄放置到数组中
			GLES20.glGenTextures(textureNum, textureHandleArray, offSet);
		####GL绑定指定纹理，切换到指定纹理的操作状态
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);
		####GL设置纹理颜色取样参数
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
		####GL设置生成纹理图案
			GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
	###绘制时（onDrawFrame）
		####GL激活纹理单元
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		####GL绑定指定纹理，切换到指定纹理的操作状态
		    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle)
		####GL传递纹理到program
			// Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
			GLES20.glUniform1i(mTextureUniformHandle, 0);
		####GL传递纹理坐标数据給program，并开启顶点着色，注意纹理的坐标系（Android中等同于屏幕坐标系）
			GLES20.glVertexAttribPointer(mTextureCoordinateHandle, mTextureCoordinateDataSize, GLES20.GL_FLOAT, false, 0, mCubeTextureCoordinates);
			GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);
		####GL绘制

			
		
		






