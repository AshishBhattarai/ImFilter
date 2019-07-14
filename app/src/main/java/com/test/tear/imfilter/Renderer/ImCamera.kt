package com.test.tear.imfilter.Renderer

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import com.test.tear.imfilter.MainActivity
import kotlinx.android.synthetic.main.content_main.*
import java.lang.RuntimeException
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/* ImCamera manages camera2. This class is meant to be used from ImRenderer */

class ImCamera {
    private val TAG = "ImCamera"
    private val LOC_TIMEOUT = 3000L // ms

    var surfaceTexture: SurfaceTexture? = null
    var stReady = false // surface texture ready?
    private var previewSize: Size = Size(1920, 1080) // default size
    private val cameraLock = Semaphore(1) // lock thread until camera is properly opened

    private val ORIENTATIONS: SparseIntArray = SparseIntArray()
    private var cameraID: String = ""
    private var cameraDevice: CameraDevice? = null
    private lateinit var cameraManager: CameraManager
    private lateinit var previewRequestBuilder: CaptureRequest.Builder
    private var cameraCaptureSession: CameraCaptureSession? = null

    private var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null


    init {
        /* Orientation of display(preview) and camera(still image) are not aligned*/
        ORIENTATIONS.append(Surface.ROTATION_0, 90)
        ORIENTATIONS.append(Surface.ROTATION_90, 0)
        ORIENTATIONS.append(Surface.ROTATION_180, 270)
        ORIENTATIONS.append(Surface.ROTATION_270, 180)
    }

    /* setup and open camera*/
    fun initCamera(texture: Texture, context: Context, imSurfaceView: ImSurfaceView, viewSize: Size) {
        surfaceTexture = SurfaceTexture(texture.getID())
        surfaceTexture?.setOnFrameAvailableListener {
            /* texture ready to render */
            synchronized(this@ImCamera) {
                imSurfaceView.requestRender()
                stReady = true
            }
        }

        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        var nViewSize: Size = viewSize
        if(viewSize.width < viewSize.height) {
            /* portrait */
            nViewSize = Size(viewSize.height, viewSize.width)
        }

        startBackgroundThread()
        calcPreviewSize(nViewSize)
        openCamera()
    }

    /* close camera and stop camera thread */
    fun destroyCamera() {
        stReady = false
        closeCamera()
        stopBackgroundThread()
        surfaceTexture?.release()
        Log.e(TAG, "Destroyed")
    }

    private fun calcPreviewSize(viewSize: Size) {
        try {
            for (id in cameraManager.cameraIdList) {
                /* Search ID for back camera - main cam*/
                val characteristics = cameraManager.getCameraCharacteristics(id)
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT)
                    continue
                cameraID = id
                Log.e(TAG, id)
                // get size
                val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: return
                for (size in map.getOutputSizes(SurfaceTexture::class.java)) {
                    if (size == viewSize) {
                        Log.e(TAG, "$cameraID ${viewSize.width} ${viewSize.height} ${size.width} ${size.height}")
                        previewSize = viewSize
                        break
                    }
                }
                break
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, "calcPreview - Access Exception")
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "calcPreview - Illegal Argument Exception")
        } catch (e: SecurityException) {
            Log.e(TAG, "calcPreview - Security Exception")
        }
    }

    private fun openCamera() {
        /*open camera and lock until opened*/
        try {
            if(!cameraLock.tryAcquire(LOC_TIMEOUT, TimeUnit.MILLISECONDS))
                throw RuntimeException("CameraLock timeout")

            /* camera callback */
            val cameraStateCallback = object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraLock.release()
                    cameraDevice = camera
                    createCameraPreview()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    cameraLock.release()
                    cameraDevice?.close()
                    cameraDevice = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    cameraLock.release()
                    cameraDevice?.close()
                    cameraDevice = null

                    Log.e(TAG, "Camera Error $error")
                }
            }

            cameraManager.openCamera(cameraID, cameraStateCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "openCamera - Access Exception")
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "openCamera - Illegal Argument Exception")
        } catch (e: SecurityException) {
            Log.e(TAG, "openCamera - Security Exception")
        } catch (e: InterruptedException) {
            Log.e(TAG, "openCamera - Interrupted Exception")
        }
    }

    private fun closeCamera() {
        try {
            cameraLock.acquire()
            cameraCaptureSession?.close()
            cameraCaptureSession = null

            cameraDevice?.close()
            cameraDevice = null
        } catch (e: InterruptedException) {
            Log.e(TAG, "closeCamera - Interrupted Exception")
        } finally {
            cameraLock.release()
        }
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraThread").also { it.start() }
        backgroundHandler = Handler(backgroundThread?.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, "stopBackgroundThread - Interrupted Exception")
        }
    }

    private fun createCameraPreview() {
        try {
            /* init surface to get output of preview*/
            surfaceTexture?.setDefaultBufferSize(previewSize.width, previewSize.height)
            val surface = Surface(surfaceTexture)

            /* we need to lock the camera until we can properly set request to CaptureSession */
            if(!cameraLock.tryAcquire(LOC_TIMEOUT, TimeUnit.MILLISECONDS)) return

            previewRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder.addTarget(surface)

            val captureCallback = object : CameraCaptureSession.StateCallback() {
                override fun onConfigureFailed(session: CameraCaptureSession) {}

                override fun onConfigured(session: CameraCaptureSession) {
                    // camera closed
                    if (cameraDevice == null) return

                    cameraCaptureSession = session
                    /* inits repeating capture request*/
                    updatePreview()
                }
            }

            cameraDevice?.createCaptureSession(Arrays.asList(surface), captureCallback, null)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "createCameraPreview - Camera Access Exception")
        } catch (e: InterruptedException) {
            Log.e(TAG, "createCameraPreview - Interrupted Exception")
        } finally {
            cameraLock.release()
        }
    }

    private fun updatePreview() {
        try {
            // AutoFocus(AF) modes
            previewRequestBuilder.set(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )
            // AutoExposure(AE) - auto flash
            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)

            /* Request endlessly repeating capture of images - preview*/
            cameraCaptureSession?.setRepeatingRequest(
                previewRequestBuilder.build(),
                null, // set for capturing still images -- see google example
                backgroundHandler
            )
        } catch (e: CameraAccessException) {
            Log.e(TAG, "updatePreview - Camera Access Exception")
        }
    }

}