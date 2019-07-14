#version 100
precision mediump float;
varying vec2 fTexCoords;

uniform sampler2D sTexture;

uniform vec3 rgb;

void main() {
    vec4 color = texture2D(sTexture, fTexCoords);
    color.rgb *= rgb;
    gl_FragColor = color;
}