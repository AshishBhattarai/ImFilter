package com.test.tear.imfilter.Renderer

import android.opengl.GLES20
import android.util.Size

/* A Framebuffer with color buffer as a texture */

class FrameBuffer(private val size: Size) {
    private val fbo = IntArray(1) // frambuffer object
    private val colorBuf: Texture = Texture(size)

    init {
        GLES20.glGenFramebuffers(1, fbo, 0)
        bind()
        setColorAttachment()
        uBind()
    }

    private fun setColorAttachment() {
        /* Color buffer attachment as 2D texture */
        colorBuf.bind()
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D, colorBuf.getID(), 0
        )
        colorBuf.unBind()
    }

    fun getColorTexture(): Texture {
        return colorBuf
    }

    fun bind() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo[0])
    }

    fun uBind() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }

    fun delete() {
        colorBuf.delete()
        GLES20.glDeleteFramebuffers(1, fbo, 0)
    }
}