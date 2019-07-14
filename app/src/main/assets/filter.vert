#version 100
attribute vec2 vPosition;
attribute vec2 vTexCoords;

varying vec2 fTexCoords;

void main() {
    gl_Position = vec4(vPosition, 0.0, 1.0);
    fTexCoords = vTexCoords;
}