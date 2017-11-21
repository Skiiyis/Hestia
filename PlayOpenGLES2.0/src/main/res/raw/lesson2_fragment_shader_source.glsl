precision mediump float;

varying vec2 v_TexCoordinate;

uniform sampler2D u_TextureUnit;

void main() {
    gl_FragColor = texture2D(u_TextureUnit, v_TexCoordinate);
}
