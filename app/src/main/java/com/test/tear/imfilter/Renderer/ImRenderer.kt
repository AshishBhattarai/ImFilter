package com.test.tear.imfilter.Renderer

import android.content.Context
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.graphics.Bitmap
import android.media.effect.EffectContext
import android.media.effect.EffectFactory
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import android.util.Size
import com.test.tear.imfilter.Renderer.Shader.ColorShader
import com.test.tear.imfilter.Renderer.Shader.EdgeShader
import com.test.tear.imfilter.Renderer.Shader.Shader
import com.test.tear.imfilter.Renderer.Shader.ShaderProgram
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

object EffectMode {
    const val NONE      =       0x0
    const val COLOR     =       0x1
    const val PRESET    =       0x2
    const val EDGE      =       0x4
}

class ImRenderer(private val context: Context, private val imSurfaceView: ImSurfaceView) : GLSurfaceView.Renderer {

    private val TAG = "ImRenderer"

    private val redClear    =   0.0f
    private val greenClear  =   0.0f
    private val blueClear   =   0.0f
    private val alphaClear  =   1.0f

    private var displaySize = Size(0, 0)

    private lateinit var effectFactory: EffectFactory

    private val imCamera: ImCamera = ImCamera()

    private lateinit var displaySurface: DisplaySurface         // texture mapping biased on texture size
    private lateinit var offscreenSurface: DisplaySurface       // texture 1:1 mapping to view port
    private lateinit var cameraSurface: DisplaySurface          // landscape / portrait texture coords
    private var currentSurface: DisplaySurface? = null          // currently using

    private lateinit var currentShaderProgram: ShaderProgram
    private lateinit var colorShaderProgram: ColorShader
    private lateinit var plainShaderProgram: ShaderProgram
    private lateinit var cameraShaderProgram: ShaderProgram
    private lateinit var edgeShaderProgram: EdgeShader

    private var cameraFrameBuffer: FrameBuffer? = null
    private var colorFrameBuffer: FrameBuffer? = null
    private var presetFrameBuffer: FrameBuffer? = null
    private var edgeFrameBuffer: FrameBuffer? = null
    private var finalFrameBuffer: FrameBuffer? = null


    private var currentTexture: Texture? = null // texture currently being processed (can be camera or loaded)
    private var cameraTexture: Texture? = null // camera output texture
    private var loadedTexture: Texture? = null // loaded from gallery

    private var currentPreset: String = ""
    private var renderMode = EffectMode.NONE
    private var cameraOn: Boolean = false // camera mode on

    /**
     * TODO:
     *  0) Framebuffer----------------------------------------------------(X)
     *  1) Postprocessor class for effects
     *  2) Multiple tools and camera + tools support----------------------(X)
     *  4) Optimize framebuffer and camera render(possibly MAYBE-0 helps?)
     *
     *  MAYBE:
     *  0) Separate onDrawFrame for camera and picture load ( picture load calcs effect only when needed)
     *  1) Create a class for EffectMode
     *
     *  (X) is done
     */

    init {}

    private fun renderCameraTexture() {
        synchronized(imCamera) {
            if(imCamera.stReady) {
                imCamera.surfaceTexture?.updateTexImage()
                imCamera.stReady = false
            }
        }
        currentShaderProgram = cameraShaderProgram
        currentTexture = cameraTexture
        drawFrame()
    }

    private fun drawImage() {
        /* Camera mode disable - offscreen render texture at texture res*/
        currentTexture = loadedTexture

        setViewPort(currentTexture!!.getSize())
    }

    private fun drawCamera() {
        setViewPort()
        cameraFrameBuffer?.delete()
        cameraFrameBuffer = FrameBuffer(displaySize)

        /* Using camera */
        cameraFrameBuffer?.bind()
        renderCameraTexture()

        currentTexture = cameraFrameBuffer?.getColorTexture()
    }

    private fun drawFrame() {
        currentShaderProgram.start()
        currentTexture?.bind()
        currentSurface?.draw()
    }

    private fun drawFrameBuffer(): FrameBuffer {
        /* create render framebuffer*/
        val nFrameBuffer = FrameBuffer(currentTexture!!.getSize())
        nFrameBuffer.bind()
        drawFrame()
        currentTexture = nFrameBuffer.getColorTexture()
        return nFrameBuffer
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        if(!cameraOn && (loadedTexture == null)) return // no render

        currentSurface = offscreenSurface
        if(cameraOn) drawCamera() else drawImage()

        /* Effect modes*/
        if((renderMode and EffectMode.EDGE) != EffectMode.NONE) {
            currentShaderProgram = edgeShaderProgram
            edgeFrameBuffer = drawFrameBuffer()
        }

        if((renderMode and EffectMode.PRESET) != EffectMode.NONE) {
            /* apply preset creates new texture must be deleted*/
            applyPreset()?.also { texture ->
                currentShaderProgram = plainShaderProgram
                currentTexture = texture
                presetFrameBuffer = drawFrameBuffer()
                texture.delete()
            }
        }

        if((renderMode and EffectMode.COLOR) != EffectMode.NONE) {
            currentShaderProgram = colorShaderProgram
            /* Using rgb color */
            colorFrameBuffer = drawFrameBuffer()
        }

        /* Render to final frame buffer - save for nex frame*/
        currentShaderProgram = plainShaderProgram
        finalFrameBuffer?.delete()
        finalFrameBuffer = drawFrameBuffer()
        finalFrameBuffer?.uBind()

        /* Render to display*/
        currentSurface = if(cameraOn) cameraSurface else displaySurface
        setViewPort(displaySize)
        drawFrame()

        /* delete all framebuffers(except final) -- TODO: Optimize */
        colorFrameBuffer?.run {delete(); null}
        presetFrameBuffer?.run {delete(); null}
        edgeFrameBuffer?.run {delete(); null}
        cameraFrameBuffer?.run {delete(); null}
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        displaySize = Size(width, height)
        setViewPort()
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(redClear, greenClear, blueClear, alphaClear)

        /* Init DisplaySurface*/
        displaySurface = DisplaySurface()
        offscreenSurface = DisplaySurface()
        offscreenSurface.updateTexVertices(false, true)

        /* Init Shader */
        val vertCode =  context.assets.open("filter.vert").run { readBytes().toString(Charsets.UTF_8) }
        val vertShader = Shader(vertCode, GLES20.GL_VERTEX_SHADER)

        var fragCode = context.assets.open("color.frag").run { readBytes().toString(Charsets.UTF_8) }
        var fragShader = Shader(fragCode, GLES20.GL_FRAGMENT_SHADER)
        colorShaderProgram = ColorShader(vertShader, fragShader)
        fragShader.delete()

        fragCode = context.assets.open("filter.frag").run { readBytes().toString(Charsets.UTF_8) }
        fragShader = Shader(fragCode, GLES20.GL_FRAGMENT_SHADER)
        plainShaderProgram = ShaderProgram(vertShader, fragShader)
        fragShader.delete()

        fragCode = context.assets.open("camera.frag").run { readBytes().toString(Charsets.UTF_8) }
        fragShader = Shader(fragCode, GLES20.GL_FRAGMENT_SHADER)
        cameraShaderProgram = ShaderProgram(vertShader, fragShader)
        fragShader.delete()

        fragCode = context.assets.open("edge.frag").run { readBytes().toString(Charsets.UTF_8) }
        fragShader = Shader(fragCode, GLES20.GL_FRAGMENT_SHADER)
        edgeShaderProgram = EdgeShader(vertShader, fragShader)
        fragShader.delete()

        currentShaderProgram = plainShaderProgram

        vertShader.delete()

        /* Init effect factory */
        effectFactory = EffectContext.createWithCurrentGlContext().factory
    }

    fun onPause() {
        imCamera.destroyCamera()
        cameraTexture?.delete()
        cameraTexture = null
        cameraOn = false
    }

    fun onResume() {}

    private fun calcRatio(width: Int, height: Int): Float {
        return (displaySize.width.toFloat() / displaySize.height) / (width.toFloat() / height)
    }

    private fun setViewPort(size: Size = displaySize) {
        GLES20.glViewport(0, 0, size.width, size.height)
    }

    /* preset color effects */
    private fun applyPreset(): Texture? {
        if(currentPreset.isEmpty()) return null

        // create effect texture biased on effectName
        return currentTexture?.let { texture ->
            val size = texture.getSize()

            val effectTexture = Texture(size) // create new texture
            effectTexture.bind()
            val effect = effectFactory.createEffect(currentPreset)
            effect.apply(texture.getID(), size.width, size.height,  effectTexture.getID())
            effect.release()

            /* effects.apply changes view port and binds effectTexture.id */
            setViewPort(texture.getSize())
            effectTexture.unBind()

            effectTexture
        }
    }

    /* load and start camera service */
    private fun loadCamera() {
        cameraSurface = DisplaySurface()
        cameraTexture?.delete()
        cameraTexture = Texture(displaySize, false, GLES11Ext.GL_TEXTURE_EXTERNAL_OES)

        imCamera.initCamera(cameraTexture!!, context, imSurfaceView, displaySize)
        cameraSurface.updatePosVertices() // 1:1

        if(context.resources.configuration.orientation == ORIENTATION_PORTRAIT)
            cameraSurface.updateTexVertices(true)

        loadPixelSize(cameraTexture!!.getSize())
    }

    private fun loadPixelSize(imageSize: Size) {
        val x = 1 / imageSize.width.toFloat()
        val y = 1 / imageSize.height.toFloat()

        edgeShaderProgram.start()
        edgeShaderProgram.loadPxielSize(x, y)
        edgeShaderProgram.stop()
    }

    /* methods to pass from activity */

    /* enable / disable camera */
    fun enableCamera(enable: Boolean) {
        if(enable) {
            loadedTexture?.delete()
            loadedTexture = null
            loadCamera()
        }
        else {
            imCamera.destroyCamera()
        }
        cameraOn = enable
    }

    fun loadNewImage(bitmap: Bitmap){
        loadedTexture?.delete()
        loadedTexture = Texture(bitmap)
        currentTexture = loadedTexture
        displaySurface.updatePosVertices(calcRatio(bitmap.width, bitmap.height))

        loadPixelSize(loadedTexture!!.getSize())
    }

    /* Effect Modes*/

    fun loadColorValues(r: Float, g: Float, b: Float) {
        colorShaderProgram.start()
        colorShaderProgram.loadColor(r, g, b)
        colorShaderProgram.start()

        renderMode = renderMode or EffectMode.COLOR
    }

    fun loadPresetEffect(effectName: String) {
        currentPreset = effectName

        renderMode = renderMode or EffectMode.PRESET
    }

    fun loadEdgeThreshold(threshold: Float) {
        edgeShaderProgram.start()
        edgeShaderProgram.loadThreshold(threshold)
        edgeShaderProgram.stop()

        renderMode = renderMode or EffectMode.EDGE
    }

    /**
     * Note: loadEffectMode doesn't ensure that a texture(loadedTexture) or an effect(currentPreset) is present and valid
     *
     * @param mode:- EffectMode - supports multiple modes at a time
     *
     */
    fun loadEffectMode(mode: Int) {

        renderMode = renderMode or mode
    }

    fun setEffectMode(mode: Int) {
        renderMode = mode
    }

    fun removeEffectMode(mode: Int) {
        renderMode = renderMode and mode.inv()
        // TODO: Erase data - shaders
    }

    fun getRenderMode(): Int {
        return renderMode
    }
}