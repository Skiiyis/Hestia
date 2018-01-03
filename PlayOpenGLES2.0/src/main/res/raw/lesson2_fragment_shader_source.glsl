precision mediump float;

varying vec2 v_TexCoordinate;

uniform sampler2D u_TextureUnit;

void main() {

    if(v_TexCoordinate.x - 0.005 < 0.5 && v_TexCoordinate.x + 0.005 > 0.5){
            gl_FragColor = vec4(v_TexCoordinate.x,v_TexCoordinate.y,0.0,1.0);
    }else if (v_TexCoordinate.y - 0.005 < 0.5 && v_TexCoordinate.y + 0.005 > 0.5){
            gl_FragColor = vec4(v_TexCoordinate.x,v_TexCoordinate.y,0.0,1.0);
    }else{
            gl_FragColor = texture2D(u_TextureUnit, v_TexCoordinate);
    }
}
