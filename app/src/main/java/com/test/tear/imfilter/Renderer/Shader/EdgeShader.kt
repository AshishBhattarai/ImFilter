package com.test.tear.imfilter.Renderer.Shader

import android.opengl.GLES20

class EdgeShader(vertexShader: Shader, fragmentShader: Shader) :
    ShaderProgram(vertexShader, fragmentShader)
{
    private var pixelSizeLoc: Int
    private var thresholdLoc: Int

    init {
        start()
        pixelSizeLoc = getUniformLoc("pixelSize")
        thresholdLoc = getUniformLoc("gradientThreshold")

        /* Defaults */
        loadPxielSize(0.0f, 0.0f)
        loadThreshold(0.1f)
        stop()
    }

    fun loadPxielSize(x: Float, y: Float) {
        GLES20.glUniform2f(pixelSizeLoc, x, y)
    }

    fun loadThreshold(threshold: Float) {
        GLES20.glUniform1f(thresholdLoc, threshold)
    }
}