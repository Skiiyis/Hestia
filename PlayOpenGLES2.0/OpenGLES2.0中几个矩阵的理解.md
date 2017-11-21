OpenGLES2.0中几个矩阵的理解

##ModelMatrix
  模型矩阵，描述的是模型的移动等变换，一般通过Matrix.setIdentityM来设置不变换模型的位置。

##ViewMatrix
  视觉矩阵，通过Matrix.setLookAtM方法设置，九个关键参数
  eyeX,eyeY,eyeZ 决定摄像机的绝对坐标
  lookX,lookY,lookZ 决定摄像机视觉方向，这个方向为三个参数的矢量和
  upX,upY,upZ 决定摄像机的正上方的方向，这个方向为三个参数的矢量和

##ProjectionMatrix
  投射矩阵，通过Matrix.frustumM(投影矩阵)或Matrix.orthoM(正交矩阵)设置，六个关键参数
  left，right，bottom，top 确定near面的上下左右边距
  决定图像平面的拉伸压缩变化，一般为(-width / height，width / height，-1，0)
  这个设置能让x,y轴坐标单位长度一致
  near,far 决定取景的范围，模型必须在该取景范围内才可见。

##Loacl
  描述模型顶点位置，三个一组代表一个position;
  这个矩阵的坐标系是以屏幕中心点为原点z轴正向外，x轴正方向向右，y轴正方向向上的矩阵
  矩阵坐标必须在(-1.0F,-1.0F,-1.0F)~(1.0F,1.0F,1.0F)内??
  其中坐标系的单位长度可以参考 屏幕顶点坐标 左上（-1.0F,-1.0F,0.0F)~ 右下(1.0F,1.0F,0.0F)
  其实是一个x,y单位长度不一致的坐标系

##最终图像变化的矩阵
  MVPMatrix = ProjectionMatrix * ViewMatrix * ModelMatrix (注意不满足乘法交换律)
  OpenGLES使用的是列向量，故最后计算的时候要反过来乘

##最终图像的顶点位置
  V = MVPMatrix * V(local);

##不使用MVP矩阵时的情况（使用Matrix.setIdentityM来作为MVP矩阵）
  默认为使用正交矩阵，模型可视范围为z(-1.0F)~z(0.0F)

##参考
  https://blog.piasy.com/2016/06/07/Open-gl-es-android-2-part-1/
