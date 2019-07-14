package com.test.tear.imfilter.Renderer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.util.Size
import java.io.FileNotFoundException
import java.lang.RuntimeException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * @param absolutePath - absolute path to the texture file
 */
class Texture(bitmap: Bitmap?, private val target: Int = GLES20.GL_TEXTURE_2D) {
    private val texture = IntArray(1)
    private var size: Size = Size(0, 0)

    init {
        bitmap?.also { bm ->
            size = Size(bm.width, bm.height)

            // bitmap to buffer
            val buf = ByteBuffer.allocateDirect(bm.byteCount).apply {
                order(ByteOrder.nativeOrder())
                bm.copyPixelsToBuffer(this)
                position(0)
            }

            GLES20.glGenTextures(1, texture, 0)
            GLES20.glBindTexture(target, texture[0])
            GLES20.glTexImage2D(target, 0, GLES20.GL_RGBA, size.width, size.height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf)
            GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)

            GLES20.glBindTexture(target, 0)
        }
    }

    constructor(size: Size, allocate: Boolean = true, target: Int = GLES20.GL_TEXTURE_2D): this(null, target) {
        this.size = size

        GLES20.glGenTextures(1, texture, 0)
        GLES20.glBindTexture(target, texture[0])
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        if(allocate)
            GLES20.glTexImage2D(target, 0, GLES20.GL_RGBA , size.width, size.height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)
        GLES20.glBindTexture(target, 0)
    }

    fun bind() {
        GLES20.glBindTexture(target, texture[0])
    }

    fun unBind() {
        GLES20.glBindTexture(target, 0)
    }

    fun delete() {
        GLES20.glDeleteTextures(1, texture, 0)
        texture[0] = 0
    }

    fun getID(): Int {
        return texture[0]
    }

    fun getSize(): Size {
        return size
    }
}