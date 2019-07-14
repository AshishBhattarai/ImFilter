package com.test.tear.imfilter.Renderer

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import com.test.tear.imfilter.MainActivity

class ImSurfaceView(context: Context, attrs: AttributeSet) : GLSurfaceView(context, attrs) {

    private val EGL_VERSION    =       2

    /* Buffer size in bits */
    private val redSize        =       8
    private val greenSize      =       8
    private val blueSize       =       8
    private val alphaSize      =       8
    private val depthSize      =       0
    private val stencilSize    =       0

    private var imRenderer: ImRenderer

    init {
        /* Setup EGL*/
        setEGLConfigChooser(redSize, greenSize, blueSize, alphaSize, depthSize, stencilSize)
        setEGLContextClientVersion(EGL_VERSION)

        /* setup renderer*/
        imRenderer = ImRenderer(context, this)
        setRenderer(imRenderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    override fun onPause() {
        imRenderer.onPause()
        super.onPause()
    }

    override fun onResume() {
        imRenderer.onResume()
        super.onResume()
    }

    fun getRenderer(): ImRenderer {
        return imRenderer
    }
}