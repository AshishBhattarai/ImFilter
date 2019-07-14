package com.test.tear.imfilter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import com.test.tear.imfilter.Renderer.ImRenderer
import com.test.tear.imfilter.Renderer.ImSurfaceView
import com.test.tear.imfilter.Renderer.EffectMode
import kotlinx.android.synthetic.main.content_main.*
import java.io.FileNotFoundException
import java.io.IOException

/* Stores data sent to renderer for pause/resume */

object STORE_KEY {
    const val CURRENT_IMG       = "current_image"
    const val CURRENT_PRESET    = "current_effect"
    const val CURRENT_MODE      = "current_mode"
    const val CAMERA_ON         = "camera_on"
    const val CURRENT_EDGET     = "current_edget"
}

class RenderDataManager(private val mainActivity: MainActivity) {

    private val TAG = "RenderDataManager"
    private val queueDelay = 500L
    private val imSurfaceView: ImSurfaceView = mainActivity.imSurfaceView
    private val imRenderer: ImRenderer = mainActivity.imSurfaceView.getRenderer()
    private val handler: Handler = Handler()

    private var currentImage: Uri? = null
    private var currentMode: Int = EffectMode.NONE
    private var cameraOn: Boolean = false

    var r = 1.0f; var g = 1.0f; var b = 1.0f
    private var currentPreset: String = ""
    private var currentEdgeT: Float = 0.0f

    /**
     * Load Image
     *
     * Check if image is valid and set currentImage. - not needed using gallery picker always correct?
     * Actual loading is done in onResume() -> called after the gallery pick ends
     *
     */
    fun loadImage(uri: Uri) {
//        if(validImage(uri)) currentImage = uri
        currentImage = uri
    }

    fun loadColor(r: Float, g: Float, b: Float) {
        this.r = r; this.g = g; this.b = b
        loadEvent { imRenderer.loadColorValues(r, g, b); requestRender()}
    }

    fun loadPreset(effectName: String) {
        currentPreset = effectName
        if(effectName.isNotEmpty())
            loadEvent { imRenderer.loadPresetEffect(effectName); requestRender() }
        else
            loadEvent { imRenderer.removeEffectMode(EffectMode.PRESET); requestRender()}
    }

    // new tab/tool selected
    fun loadMode(mode: Int) {
        currentMode = currentMode or mode
        loadEvent { imRenderer.loadEffectMode(mode);}
    }

    fun loadThreshold(threshold: Float) {
        currentEdgeT = threshold
        currentMode = currentMode or EffectMode.EDGE
        loadEvent { imRenderer.loadEdgeThreshold(threshold); requestRender()}
    }

    fun enableCamera(enable: Boolean) {
        cameraOn = enable
        loadEvent { imRenderer.enableCamera(enable) }
    }

    fun removeMode(mode: Int) {
        currentMode = currentMode and mode.inv()
        loadEvent { imRenderer.removeEffectMode(mode); requestRender()}
    }

    /* on resume reload render data. */
    fun onResume() {
        // load data to renderer
        loadEventDelayed {
            /* prev bitmap & effect */
            val bitmap = loadBitmapUri(currentImage)
            bitmap?.also { it -> imRenderer.loadNewImage(it) }
            if(currentPreset.isNotEmpty()) imRenderer.loadPresetEffect(currentPreset)
            if(currentEdgeT != 0.0f) imRenderer.loadEdgeThreshold(currentEdgeT)
            if(cameraOn) imRenderer.enableCamera(cameraOn)
            imRenderer.loadColorValues(r, g, b)

            imRenderer.setEffectMode(currentMode)
            imSurfaceView.requestRender()
        }
    }

    // save render data
    fun onSaveInstanceState(outState: Bundle?) {
        outState?.also { state ->
            /* image uri */
            currentImage?.toString().also { str -> state.putString(STORE_KEY.CURRENT_IMG, str) }
            /* effect string */
            state.putString(STORE_KEY.CURRENT_PRESET, currentPreset)
            /* mode flags */
            state.putInt(STORE_KEY.CURRENT_MODE, currentMode)
            /* threshold */
            state.putFloat(STORE_KEY.CURRENT_EDGET, currentEdgeT)
            /* camera on */
            state.putBoolean(STORE_KEY.CAMERA_ON, cameraOn)
        }
    }

    // restore render data
    fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        savedInstanceState?.also { instance ->
            instance.getString(STORE_KEY.CURRENT_IMG)?.also {str -> currentImage = Uri.parse(str) }
            currentPreset = instance.getString(STORE_KEY.CURRENT_PRESET, "")
            currentMode = instance.getInt(STORE_KEY.CURRENT_MODE, EffectMode.NONE)
            currentEdgeT = instance.getFloat(STORE_KEY.CURRENT_EDGET, 0.0f)
            cameraOn = instance.getBoolean(STORE_KEY.CAMERA_ON, false)
        }
    }

    /* Delay queue events - waiting for OGL thread to resume*/
    private fun loadEventDelayed(r: () -> Unit) {
        handler.postDelayed({ mainActivity.imSurfaceView.queueEvent(r) }, queueDelay)
    }

    /* load events normally */
    private fun loadEvent(r: () -> Unit) {
        mainActivity.imSurfaceView.queueEvent(r)
    }

    private fun requestRender() {
        if(!cameraOn)  imSurfaceView.requestRender()
    }

    /* load bitmap from uri */
    private fun loadBitmapUri(uri: Uri?): Bitmap? {
        var bitmap: Bitmap? = null
        if(uri == null) return bitmap

        try {
            val inputStream = mainActivity.contentResolver.openInputStream(uri)
            bitmap = BitmapFactory.Options().run {
                inScaled = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
                BitmapFactory.decodeStream(inputStream).also { inputStream?.close() }
            }
        } catch (e: FileNotFoundException) {
            Toast.makeText(mainActivity, e.message, Toast.LENGTH_SHORT).show()
            return null
        } catch (e: IOException) {
            Toast.makeText(mainActivity, e.message, Toast.LENGTH_SHORT).show()
            return null
        }

        return bitmap
    }
}