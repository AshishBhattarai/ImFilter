package com.test.tear.imfilter.Renderer.Shader

import android.opengl.GLES20

class ColorShader(vertexShader: Shader, fragmentShader: Shader):
    ShaderProgram(vertexShader, fragmentShader)
{
    private var colorLoc: Int = -1

    init {
        start()
        colorLoc = getUniformLoc("rgb")
        /* Defaults */
        loadColor(1.0f, 1.0f, 1.0f)
        stop()
    }

    fun loadColor(r: Float, g: Float, b: Float) {
        GLES20.glUniform3f(colorLoc, r, g, b)
    }
}