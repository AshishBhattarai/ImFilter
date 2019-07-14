package com.test.tear.imfilter.Renderer

import android.opengl.GLES20
import com.test.tear.imfilter.Renderer.Shader.ShaderConfig
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DisplaySurface {

    // Default values
    companion object DefaultVertices {
        // default window/surface (x0, y0) = (-1.0f, -1.0f) | (x1, y1) = (1.0f, 1.0f)
        val POS = floatArrayOf(-1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f)
        val TEX = floatArrayOf(0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f)

        // portrait mode
        val TEX_POT = floatArrayOf(1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f)

        val TEX_FLIP = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f)
        val TEX_POT_FLIP = floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f)

        val SIZE = POS.size * 4
        const val DIM = 2
        const val DRAW_COUNT = 4
    }


    private var posVertices = ByteBuffer.allocateDirect(SIZE).run {
        order(ByteOrder.nativeOrder())
        asFloatBuffer().apply {
            put(POS)
            position(0)
        }
    }

    private var texVertices = ByteBuffer.allocateDirect(SIZE).run {
        order(ByteOrder.nativeOrder())
        asFloatBuffer().apply {
            put(TEX)
            position(0)
        }
    }

    /**
     * @param aspectRatio:- Aspect ratio of display & image (display_ratio/image_ratio)
     */
    fun updatePosVertices(aspectRatio: Float) {
        // calculate new window/surface
        var x0: Float; var y0: Float; var x1: Float; var y1: Float
        if(aspectRatio > 1.0f) {
            // display_ratio > image_ratio
            x0 = -1.0f / aspectRatio
            y0 = -1.0f
            x1 =  1.0f / aspectRatio
            y1 =  1.0f
        } else {
            // display_ratio < image_ration
            x0 = -1.0f
            y0 = -aspectRatio
            x1 =  1.0f
            y1 =  aspectRatio
        }
        val nPos = floatArrayOf(x0, y0, x0, y1, x1, y0, x1, y1)
        posVertices.put(nPos).position(0)
    }

    fun updatePosVertices() {
        posVertices.put(POS).position(0)
    }

    fun updateTexVertices(portrait: Boolean, flip: Boolean = false) {
        if(flip)
            if(portrait)
                texVertices.put(TEX_POT_FLIP).position(0)
            else
                texVertices.put(TEX_FLIP).position(0)
        else
            if(portrait)
                texVertices.put(TEX_POT).position(0)
            else
                texVertices.put(TEX).position(0)
    }

    fun draw() {
        GLES20.glEnableVertexAttribArray(ShaderConfig.POSITION_ATTRIBUTE_LOC)
        GLES20.glEnableVertexAttribArray(ShaderConfig.TEXCOORDS_ATTRIBUTE_LOC)

        GLES20.glVertexAttribPointer(ShaderConfig.POSITION_ATTRIBUTE_LOC, DIM, GLES20.GL_FLOAT, false, 0, posVertices)
        GLES20.glVertexAttribPointer(ShaderConfig.TEXCOORDS_ATTRIBUTE_LOC, DIM, GLES20.GL_FLOAT, false, 0, texVertices)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, DRAW_COUNT)

        GLES20.glDisableVertexAttribArray(ShaderConfig.POSITION_ATTRIBUTE_LOC)
        GLES20.glDisableVertexAttribArray(ShaderConfig.TEXCOORDS_ATTRIBUTE_LOC)
    }
}