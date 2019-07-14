// Soble edge detection

#version 100
precision mediump float;
varying vec2 fTexCoords;

uniform sampler2D sTexture;
uniform vec2 pixelSize;
uniform float gradientThreshold;

// Note: two pass will be faster
void main() {
    vec3 lum = vec3(0.2126, 0.7152, 0.0722); // Relative luminance
    float x = pixelSize.x;
    float y = pixelSize.y;

    // 3x3 Matrix of surround pixels & decreases luminace
    float m00 = dot(texture2D(sTexture, fTexCoords + vec2(-x, y)).rgb, lum);
    float m10 = dot(texture2D(sTexture, fTexCoords + vec2(-x, 0.0)).rgb, lum);
    float m20 = dot(texture2D(sTexture, fTexCoords + vec2(-x, -y)).rgb, lum);
    float m01 = dot(texture2D(sTexture, fTexCoords + vec2(0.0, y)).rgb, lum);
    float m21 = dot(texture2D(sTexture, fTexCoords + vec2(0.0, -y)).rgb, lum);
    float m02 = dot(texture2D(sTexture, fTexCoords + vec2(x, y)).rgb, lum);
    float m12 = dot(texture2D(sTexture, fTexCoords + vec2(x, 0.0)).rgb, lum);
    float m22 = dot(texture2D(sTexture, fTexCoords + vec2(x, -y)).rgb, lum);

    // apply sobel convolution kernel
    float px = m00 + 1.0*m10 + m20 - (m02 + 1.0*m12 + m22);
    float py = m00 + 1.0*m01 + m02 - (m20 + 1.0*m21 + m22);

    vec2 p = vec2(px, py);
    float distance = length(p);
    if(distance > gradientThreshold) {
        gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);
    } else {
        gl_FragColor = vec4(1.0);
    }
}
