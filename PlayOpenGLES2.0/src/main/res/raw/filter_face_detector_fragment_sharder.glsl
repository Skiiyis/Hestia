precision mediump float;
varying vec2 vTextureCoord;
//外部纹理图像
uniform sampler2D sTexture;

uniform vec2 uLeftEyePointCoord;
uniform vec2 uRightEyePointCoord;
uniform vec2 uMouthPointCoord;

bool in_round(vec2 point1,vec2 point2,float radius,float width){
    if(point2.x == 0.0 && point2.y == 0.0){
        return false;
    }
    float pX2 = pow(point1.x - point2.x,2.0);
    float pY2 = pow(point1.y - point2.y,2.0);
    float pr2 = pow(radius,2.0);
    float prw2 = pow(radius+width,2.0);
    return pX2 + pY2 < prw2 && pX2 + pY2 > pr2;
}

void main() {
        if(in_round(uLeftEyePointCoord,vTextureCoord,0.05,0.01)){
            gl_FragColor = vec4(1.0,0.0,0.0,1.0);
            return;
        }
        if(in_round(uRightEyePointCoord,vTextureCoord,0.05,0.01)){
            gl_FragColor = vec4(1.0,0.0,0.0,1.0);
            return;
        }
        if(in_round(uMouthPointCoord,vTextureCoord,0.05,0.01)){
            gl_FragColor = vec4(1.0,0.0,0.0,1.0);
            return;
        }
        gl_FragColor = texture2D(sTexture, vTextureCoord);
}
