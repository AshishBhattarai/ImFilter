package com.test.tear.imfilter.Renderer.Shader

import android.opengl.GLES20
import android.util.Log
import com.test.tear.imfilter.BuildConfig

class Shader(strCode: String, private val type: Int) {
    private var shader:Int = 0
    private val TAG: String = "Shader"
    private var strType = ""

    init {
        strType = if(type == GLES20.GL_VERTEX_SHADER) "Vertex" else "Fragment"

        shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, strCode)
        GLES20.glCompileShader(shader)
            checkError()
    }

    private fun checkError() {
        val success = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, success, 0)
        if(success[0] == 0) {
            Log.e(TAG, "$strType Error:\n ${GLES20.glGetShaderInfoLog(shader)}")
            delete()
        }
    }

    fun getType(): Int {
        return type
    }

    fun getID(): Int {
        return shader
    }

    fun delete() {
        GLES20.glDeleteShader(shader)
        shader = 0
    }
}

