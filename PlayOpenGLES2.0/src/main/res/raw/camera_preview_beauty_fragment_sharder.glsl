#extension GL_OES_EGL_image_external : require
precision mediump float;

//纹理坐标
varying vec2 vTextureCoord;
//外部纹理图像
uniform samplerExternalOES sTexture;
//取样步长
uniform vec2 vStepOffset;
//美颜等级
uniform mediump float fLevel;

const highp vec3 W = vec3(0.299,0.587,0.114);
vec2 blurCoordinates[20];
float hardLight(float color) {
	if(color <= 0.5){
        color = color * color * 2.0;
	}else{
        color = 1.0 - ((1.0 - color)*(1.0 - color) * 2.0);
	}
	return color;
}

void main() {
    vec3 centralColor = texture2D(sTexture, vTextureCoord).rgb;
        if(fLevel != 1.0){
            //模糊取样
            blurCoordinates[0] = vTextureCoord.xy + vStepOffset * vec2(0.0, -10.0);
            blurCoordinates[1] = vTextureCoord.xy + vStepOffset * vec2(0.0, 10.0);
            blurCoordinates[2] = vTextureCoord.xy + vStepOffset * vec2(-10.0, 0.0);
            blurCoordinates[3] = vTextureCoord.xy + vStepOffset * vec2(10.0, 0.0);
            blurCoordinates[4] = vTextureCoord.xy + vStepOffset * vec2(5.0, -8.0);
            blurCoordinates[5] = vTextureCoord.xy + vStepOffset * vec2(5.0, 8.0);
            blurCoordinates[6] = vTextureCoord.xy + vStepOffset * vec2(-5.0, 8.0);
            blurCoordinates[7] = vTextureCoord.xy + vStepOffset * vec2(-5.0, -8.0);
            blurCoordinates[8] = vTextureCoord.xy + vStepOffset * vec2(8.0, -5.0);
            blurCoordinates[9] = vTextureCoord.xy + vStepOffset * vec2(8.0, 5.0);
            blurCoordinates[10] = vTextureCoord.xy + vStepOffset * vec2(-8.0, 5.0);
            blurCoordinates[11] = vTextureCoord.xy + vStepOffset * vec2(-8.0, -5.0);
            blurCoordinates[12] = vTextureCoord.xy + vStepOffset * vec2(0.0, -6.0);
            blurCoordinates[13] = vTextureCoord.xy + vStepOffset * vec2(0.0, 6.0);
            blurCoordinates[14] = vTextureCoord.xy + vStepOffset * vec2(6.0, 0.0);
            blurCoordinates[15] = vTextureCoord.xy + vStepOffset * vec2(-6.0, 0.0);
            blurCoordinates[16] = vTextureCoord.xy + vStepOffset * vec2(-4.0, -4.0);
            blurCoordinates[17] = vTextureCoord.xy + vStepOffset * vec2(-4.0, 4.0);
            blurCoordinates[18] = vTextureCoord.xy + vStepOffset * vec2(4.0, -4.0);
            blurCoordinates[19] = vTextureCoord.xy + vStepOffset * vec2(4.0, 4.0);

            //在片段着色器依次取出这些点的绿色通道值，乘以权重，最后除以总权重，得到模糊后的绿色通道值
            float sampleColor = centralColor.g * 20.0;
            sampleColor += texture2D(sTexture, blurCoordinates[0]).g;
            sampleColor += texture2D(sTexture, blurCoordinates[1]).g;
            sampleColor += texture2D(sTexture, blurCoordinates[2]).g;
            sampleColor += texture2D(sTexture, blurCoordinates[3]).g;
            sampleColor += texture2D(sTexture, blurCoordinates[4]).g;
            sampleColor += texture2D(sTexture, blurCoordinates[5]).g;
            sampleColor += texture2D(sTexture, blurCoordinates[6]).g;
            sampleColor += texture2D(sTexture, blurCoordinates[7]).g;
            sampleColor += texture2D(sTexture, blurCoordinates[8]).g;
            sampleColor += texture2D(sTexture, blurCoordinates[9]).g;
            sampleColor += texture2D(sTexture, blurCoordinates[10]).g;
            sampleColor += texture2D(sTexture, blurCoordinates[11]).g;
            sampleColor += texture2D(sTexture, blurCoordinates[12]).g * 2.0;
            sampleColor += texture2D(sTexture, blurCoordinates[13]).g * 2.0;
            sampleColor += texture2D(sTexture, blurCoordinates[14]).g * 2.0;
            sampleColor += texture2D(sTexture, blurCoordinates[15]).g * 2.0;
            sampleColor += texture2D(sTexture, blurCoordinates[16]).g * 2.0;
            sampleColor += texture2D(sTexture, blurCoordinates[17]).g * 2.0;
            sampleColor += texture2D(sTexture, blurCoordinates[18]).g * 2.0;
            sampleColor += texture2D(sTexture, blurCoordinates[19]).g * 2.0;
            //除以总权重，得到模糊后的绿色通道值
            sampleColor = sampleColor / 48.0;
            //高反差保留
            float highPass = centralColor.g - sampleColor + 0.5;
            //对上述结果值进行3-5次强光处理（见第七章的“叠加”混合模式），此步骤可以使得噪声更加突出
            for(int i = 0; i < 5;i++){
                highPass = hardLight(highPass);
            }
            //计算原图灰度值
            float luminance = dot(centralColor, W);

            //将灰度值作为阈值，用来排除非皮肤部分，根据灰度值计算，将原图与强光叠加后的结果图合成
            //pow函数中第二个参数可调（1/3~1)，值越小，alpha越大，磨皮效果越明显，修改该值可作为美颜程度
            float alpha = pow(luminance, fLevel);
            vec3 smoothColor = centralColor + (centralColor-vec3(highPass))*alpha*0.1;

            //以灰度值作为透明度将原图与混合后结果进行滤色、柔光等混合，并调节饱和度
            gl_FragColor = vec4(mix(smoothColor.rgb, max(smoothColor, centralColor), alpha), 1.0);
        }else{
            gl_FragColor = vec4(centralColor.rgb,1.0);
        }
}
