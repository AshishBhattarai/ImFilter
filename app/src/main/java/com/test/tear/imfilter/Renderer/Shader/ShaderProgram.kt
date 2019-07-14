package com.test.tear.imfilter.Renderer.Shader

import android.opengl.GLES20
import android.util.Log
import com.test.tear.imfilter.BuildConfig

open class ShaderProgram(vertexShader: Shader, fragmentShader: Shader, usesTexture: Boolean = true, usesColor: Boolean = false) {
    private var program: Int = 0

    private var TAG = "ShaderProgram"

    init {
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader.getID())
        GLES20.glAttachShader(program, fragmentShader.getID())

        /*set attribute location */
        GLES20.glBindAttribLocation(program, ShaderConfig.POSITION_ATTRIBUTE_LOC, ShaderConfig.POSITION_ATTRIBUTE)
        if(usesColor)
            GLES20.glBindAttribLocation(program, ShaderConfig.COLOR_ATTRIBUTE_LOC, ShaderConfig.COLOR_ATTRIBUTE)
        if(usesTexture)
            GLES20.glBindAttribLocation(program, ShaderConfig.TEXCOORDS_ATTRIBUTE_LOC, ShaderConfig.TEXCOORDS_ATTRIBUTE)

       GLES20.glLinkProgram(program)
       if(BuildConfig.DEBUG)
           checkError()
    }

    private fun checkError() {
        var success = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, success, 0)
        if(success[0] == 0) {
            Log.e(TAG, "Program link error: \n ${GLES20.glGetProgramInfoLog(program)}")
            delete()
        }
    }

    fun start() {
        GLES20.glUseProgram(program)
    }

    fun stop() {
        GLES20.glUseProgram(0)
    }

    fun delete() {
        GLES20.glDeleteProgram(program)
        program = 0
    }

    fun getUniformLoc(name: String): Int {
        return GLES20.glGetUniformLocation(program, name).also { loc ->
            if(loc == -1)
                Log.e(TAG, "Uniform $String not on shader.")
        }
    }
}