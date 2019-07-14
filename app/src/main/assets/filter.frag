#version 100
precision mediump float;
varying vec2 fTexCoords;

uniform sampler2D sTexture;

void main() {
    gl_FragColor = texture2D(sTexture, fTexCoords);
}