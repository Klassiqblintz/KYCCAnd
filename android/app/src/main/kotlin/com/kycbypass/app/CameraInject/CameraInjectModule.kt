package com.kycbypass.app.CameraInject

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.facebook.react.bridge.*
import java.nio.ByteBuffer

class CameraInjectModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    companion object {
        private const val TAG = "CameraInject"
        private const val IMAGE_FORMAT = PixelFormat.RGBA_8888
        private const val MAX_BUFFER = 2
    }

    private val reactContext: ReactApplicationContext = reactContext
    private val mainExecutor = reactContext.mainExecutor
    private val mainHandler = Handler(Looper.getMainLooper())

    private var cameraManager: CameraManager? = null
    private var cameraDevice: CameraDevice? = null
    private var cameraSession: CameraCaptureSession? = null
    private var captureRequest: CaptureRequest? = null

    private var imageReader: ImageReader? = null
    private var imageReaderSurface: android.view.Surface? = null

    private var isInjecting = false
    private var frameWidth = 1920
    private var frameHeight = 1080
    private var pendingFrame: Bitmap? = null
    private val pendingFrameLock = Any()
    private var lastDequeueTime = 0L

    private var fpsCounter = 0
    private var currentFps = 0
    private var lastFpsUpdate = 0L

    private var externalCameraId: String? = null

    override fun getName(): String = "CameraInject"

    @ReactMethod
    fun startInject(width: Int, height: Int, fps: Int, promise: Promise) {
        try {
            frameWidth = width
            frameHeight = height

            cameraManager = reactContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            externalCameraId = findExternalCameraId()

            if (externalCameraId == null) {
                promise.reject("NO_EXTERNAL_CAMERA", "No external camera found. Try with a USB camera or virtual camera app.")
                return
            }

            imageReader = ImageReader.newInstance(width, height, IMAGE_FORMAT, MAX_BUFFER)
            imageReaderSurface = imageReader?.surface

            if (imageReaderSurface == null) {
                promise.reject("SURFACE_ERROR", "Failed to create ImageReader surface")
                return
            }

            cameraManager?.openCamera(externalCameraId!!, mainExecutor, cameraCallback)

            isInjecting = true
            currentFps = fps
            fpsCounter = 0

            promise.resolve(true)
        } catch (e: Exception) {
            isInjecting = false
            promise.reject("START_FAILED", "Failed to start injection: ${e.message}", e)
        }
    }

    @ReactMethod
    fun injectFrame(pngData: ReadableArray, promise: Promise) {
        if (!isInjecting) {
            promise.resolve(false)
            return
        }

        try {
            val bytes = ByteArray(pngData.size())
            for (i in 0 until pngData.size()) {
                bytes[i] = pngData.getDouble(i).toInt().toByte()
            }

            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

            if (bitmap != null) {
                val resized = if (bitmap.width != frameWidth || bitmap.height != frameHeight) {
                    Bitmap.createScaledBitmap(bitmap, frameWidth, frameHeight, true)
                } else {
                    bitmap
                }

                synchronized(pendingFrameLock) {
                    pendingFrame?.recycle()
                    pendingFrame = resized
                }

                dequeueFrameIfNeeded()
            }

            promise.resolve(bitmap != null)
        } catch (e: Exception) {
            promise.reject("FRAME_ERROR", "Failed to inject frame: ${e.message}", e)
        }
    }

    @ReactMethod
    fun injectFrameFromUri(uri: String, promise: Promise) {
        if (!isInjecting) {
            promise.resolve(false)
            return
        }

        try {
            val contentResolver = reactContext.contentResolver
            val inputStream = contentResolver.openInputStream(Uri.parse(uri))
            if (inputStream == null) {
                promise.reject("URI_ERROR", "Cannot open file: $uri")
                return
            }

            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) {
                promise.reject("DECODE_ERROR", "Cannot decode image from URI")
                return
            }

            val resized = Bitmap.createScaledBitmap(originalBitmap, frameWidth, frameHeight, true)
            originalBitmap.recycle()

            synchronized(pendingFrameLock) {
                pendingFrame?.recycle()
                pendingFrame = resized
            }

            dequeueFrameIfNeeded()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("FRAME_ERROR", "Failed to inject frame: ${e.message}", e)
        }
    }

    @ReactMethod
    fun stopInject(promise: Promise) {
        try {
            isInjecting = false
            cameraSession?.close()
            cameraSession = null
            cameraDevice?.close()
            cameraDevice = null
            imageReader?.close()
            imageReader = null
            imageReaderSurface = null

            synchronized(pendingFrameLock) {
                pendingFrame?.recycle()
                pendingFrame = null
            }

            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("STOP_ERROR", "Failed to stop injection: ${e.message}", e)
        }
    }

    @ReactMethod
    fun isInjecting(promise: Promise) {
        promise.resolve(isInjecting)
    }

    @ReactMethod
    fun getFpsStats(promise: Promise) {
        val map = Arguments.createMap().apply {
            putInt("current", currentFps)
            putInt("actual", calculateFps())
            putInt("width", frameWidth)
            putInt("height", frameHeight)
        }
        promise.resolve(map)
    }

    @ReactMethod
    fun getCameraIds(promise: Promise) {
        val ids = Arguments.createArray()
        try {
            cameraManager?.cameraIdList?.forEach { id ->
                ids.pushString(id)
            }
            promise.resolve(ids)
        } catch (e: Exception) {
            promise.reject("CAMERA_ERROR", e.message, e)
        }
    }

    private val cameraCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(device: CameraDevice) {
            cameraDevice = device
            createCameraSession()
        }

        override fun onDisconnected(device: CameraDevice) {
            device.close()
            cameraDevice = null
            cameraSession?.close()
            cameraSession = null
        }

        override fun onError(device: CameraDevice, error: Int) {
            device.close()
            cameraDevice = null
            cameraSession?.close()
            cameraSession = null
        }
    }

    private fun createCameraSession() {
        if (cameraDevice == null || imageReaderSurface == null || externalCameraId == null) return

        try {
            captureRequest = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)?.apply {
                addTarget(imageReaderSurface!!)
                set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            }?.build() ?: return

            cameraDevice?.createCaptureSession(
                listOf(imageReaderSurface!!),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        cameraSession = session
                        session.setRepeatingRequest(captureRequest!!, frameCallback, mainHandler)
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        // failed
                    }
                },
                mainHandler
            )
        } catch (e: Exception) {
            // failed
        }
    }

    private fun dequeueFrameIfNeeded() {
        if (cameraSession == null || captureRequest == null) return

        try {
            val image = imageReader?.acquireLatestImage()
            if (image == null) return

            val frame = synchronized(pendingFrameLock) { pendingFrame }

            if (frame != null) {
                val planes = image.planes
                val buffer = planes[0].buffer

                val pixelCount = frame.width * frame.height
                val pixels = IntArray(pixelCount)
                frame.getPixels(pixels, 0, frame.width, 0, 0, frame.width, frame.height)
                buffer.asIntBuffer().put(pixels)
                buffer.rewind()

                synchronized(pendingFrameLock) {
                    pendingFrame = null
                }
            }

            image.close()
        } catch (e: Exception) {
            // skip
        }
    }

    private fun findExternalCameraId(): String? {
        cameraManager ?: return null
        val ids = cameraManager!!.cameraIdList
        for (id in ids) {
            try {
                val characteristics = cameraManager!!.getCameraCharacteristics(id)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_EXTERNAL) {
                    return id
                }
            } catch (e: Exception) {
                // skip
            }
        }
        if (ids.isNotEmpty()) return ids[0]
        return null
    }

    private fun calculateFps(): Int {
        val now = System.currentTimeMillis()
        if (lastFpsUpdate == 0L) {
            lastFpsUpdate = now
            return 0
        }
        val elapsed = now - lastFpsUpdate
        if (elapsed < 1000) return currentFps

        val fpsValue: Int = (fpsCounter * 1000 / elapsed).toInt()
        fpsCounter = 0
        lastFpsUpdate = now
        return fpsValue
    }

    private val frameCallback: CameraCaptureSession.CaptureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            fpsCounter++
            val now = System.currentTimeMillis()
            val interval = if (currentFps > 0) 1000 / currentFps else 33
            if (now - lastDequeueTime > interval.toLong()) {
                lastDequeueTime = now
                dequeueFrameIfNeeded()
            }
        }

        override fun onCaptureFailed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            failure: CaptureFailure
        ) {
            // failed
        }
    }

    fun onResume() {
        if (isInjecting && cameraDevice != null) {
            createCameraSession()
        }
    }

    fun onPause() {
        if (isInjecting) {
            cameraSession?.close()
            cameraSession = null
        }
    }
}
