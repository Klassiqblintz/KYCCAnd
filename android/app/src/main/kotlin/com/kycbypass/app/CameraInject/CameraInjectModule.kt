package com.kycbypass.app.CameraInject

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.PixelFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Size
import com.facebook.react.bridge.*
import java.nio.ByteBuffer
import kotlin.math.min

/**
 * Native Expo module that injects camera frames into Android's external camera.
 * Works by:
 * 1. Creating an ImageReader with target resolution
 * 2. Opening the external (USB/virtual) camera via Camera2 API
 * 3. Setting ImageReader's surface as the camera's output target
 * 4. Feeding frames from our app's camera into the ImageReader
 * 5. Any app that opens the external camera sees our feed
 */
class CameraInjectModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    companion object {
        private const val TAG = "CameraInject"
        private const val IMAGE_FORMAT = ImageFormat.RGBA_8888
        private const val MAX_BUFFER = 2
    }

    private val reactContext: ReactApplicationContext = reactContext
    private val mainExecutor = reactContext.mainExecutor
    private val mainHandler = Handler(Looper.getMainLooper())

    // Camera2 components
    private var cameraManager: CameraManager? = null
    private var cameraDevice: CameraDevice? = null
    private var cameraSession: CameraCaptureSession? = null
    private var captureRequest: CaptureRequest? = null

    // ImageReader for frame buffer
    private var imageReader: ImageReader? = null
    private var imageReaderSurface: android.view.Surface? = null

    // Injection state
    private var isInjecting = false
    private var frameWidth = 1920
    private var frameHeight = 1080
    private var pendingFrame: Bitmap? = null
    private val pendingFrameLock = Any()
    private var lastDequeueTime = 0L

    // Frame rate tracking
    private var fpsCounter = 0
    private var currentFps = 0
    private var lastFpsUpdate = 0L

    // Camera ID for external camera
    private var externalCameraId: String? = null

    override fun getName(): String = "CameraInject"

    /**
     * Start injection at the specified resolution.
     */
    @ReactMethod
    fun startInject(width: Int, height: Int, fps: Int, promise: Promise) {
        try {
            frameWidth = width
            frameHeight = height

            // Initialize CameraManager
            cameraManager = reactContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager

            // Find external camera (LENS_FACING_EXTERNAL)
            externalCameraId = findExternalCameraId()

            if (externalCameraId == null) {
                promise.reject("NO_EXTERNAL_CAMERA",
                    "No external camera found. Try with a USB camera or virtual camera app.")
                return
            }

            // Create ImageReader for frame buffer
            imageReader = ImageReader.newInstance(width, height, IMAGE_FORMAT, MAX_BUFFER)
            imageReaderSurface = imageReader?.surface

            if (imageReaderSurface == null) {
                promise.reject("SURFACE_ERROR", "Failed to create ImageReader surface")
                return
            }

            // Open external camera
            cameraManager?.openCamera(externalCameraId!!, cameraCallback, mainExecutor)

            isInjecting = true
            currentFps = fps
            fpsCounter = 0

            promise.resolve(true)
        } catch (e: Exception) {
            isInjecting = false
            promise.reject("START_FAILED", "Failed to start injection: ${e.message}", e)
        }
    }

    /**
     * Inject a frame as RGBA byte array.
     */
    @ReactMethod
    fun injectFrame(pngData: ReadableArray, promise: Promise) {
        if (!isInjecting) {
            promise.resolve(false)
            return
        }

        try {
            // Convert ReadableArray to byte array
            val bytes = ByteArray(pngData.size())
            for (i in 0 until pngData.size()) {
                bytes[i] = pngData.getDouble(i).toInt().toByte()
            }

            // Decode to bitmap
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

            if (bitmap != null) {
                // Resize to target if needed
                val resized = if (bitmap.width != frameWidth || bitmap.height != frameHeight) {
                    Bitmap.createScaledBitmap(bitmap, frameWidth, frameHeight, true)
                } else {
                    bitmap
                }

                // Synchronously update if camera session is ready
                synchronized(pendingFrameLock) {
                    pendingFrame?.recycle()
                    pendingFrame = resized
                }

                // Signal the camera to dequeue and display
                dequeueFrameIfNeeded()
            }

            promise.resolve(bitmap != null)
        } catch (e: Exception) {
            promise.reject("FRAME_ERROR", "Failed to inject frame: ${e.message}", e)
        }
    }

    /**
     * Inject a frame from a PNG URI (simpler path).
     */
    @ReactMethod
    fun injectFrameFromUri(uri: String, promise: Promise) {
        if (!isInjecting) {
            promise.resolve(false)
            return
        }

        try {
            // Load bitmap from URI
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

            // Resize to target
            val resized = Bitmap.createScaledBitmap(
                originalBitmap, frameWidth, frameHeight, true
            )
            originalBitmap.recycle()

            // Update pending frame
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

    /**
     * Stop injection and clean up resources.
     */
    @ReactMethod
    fun stopInject(promise: Promise) {
        try {
            isInjecting = false

            // Close session
            cameraSession?.close()
            cameraSession = null

            // Close camera
            cameraDevice?.close()
            cameraDevice = null

            // Close image reader
            imageReader?.close()
            imageReader = null
            imageReaderSurface = null

            // Clear pending frame
            synchronized(pendingFrameLock) {
                pendingFrame?.recycle()
                pendingFrame = null
            }

            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("STOP_ERROR", "Failed to stop injection: ${e.message}", e)
        }
    }

    /**
     * Check if injection is active.
     */
    @ReactMethod
    fun isInjecting(promise: Promise) {
        promise.resolve(isInjecting)
    }

    /**
     * Get current frame rate stats.
     */
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

    /**
     * Get available camera IDs.
     */
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

    // ── Camera2 Callbacks ──────────────────────────────────────────

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

    /**
     * Create a capture session with our ImageReader surface as the target.
     */
    private fun createCameraSession() {
        if (cameraDevice == null || imageReaderSurface == null || externalCameraId == null) return

        try {
            // Build output request targeting ImageReader surface
            captureRequest = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)?.apply {
                target(imageReaderSurface!!)
                set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            }?.build() ?: return

            // Create capture session with preview surface only
            cameraDevice?.createCaptureSession(
                listOf(imageReaderSurface!!),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        cameraSession = session
                        session.setRepeatingRequest(captureRequest!!, frameCallback, mainHandler)
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        // Session failed to configure
                    }
                },
                mainHandler
            )
        } catch (e: Exception) {
            // Session creation failed
        }
    }

    /**
     * Dequeue frame from ImageReader and update with pending bitmap.
     */
    private fun dequeueFrameIfNeeded() {
        if (cameraSession == null || captureRequest == null) return

        try {
            val image = imageReader?.acquireLatestImage()
            if (image == null) return

            // Get pending frame
            val frame = synchronized(pendingFrameLock) {
                pendingFrame
            }

            if (frame != null) {
                // Copy pixel data from bitmap to image buffer
                val planes = image.planes
                val buffer = planes[0].buffer
                val rowStride = planes[0].rowStride

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
            // Image acquisition failed, skip frame
        }
    }

    /**
     * Find the external camera ID.
     */
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
                // Skip this camera
            }
        }
        // Fallback: try first camera that's not the main internal one
        if (ids.isNotEmpty()) {
            return ids[0]
        }
        return null
    }

    /**
     * Calculate actual frames per second.
     */
    private fun calculateFps(): Int {
        val now = System.currentTimeMillis()
        if (lastFpsUpdate == 0L) {
            lastFpsUpdate = now
            return 0
        }
        val elapsed = now - lastFpsUpdate
        if (elapsed < 1000) return currentFps

        val fps = if (elapsed > 0) (fpsCounter * 1000 / elapsed) else 0
        fpsCounter = 0
        lastFpsUpdate = now
        return fps
    }

    /**
     * Callback for frame dequeuing (runs on main thread).
     */
    private val frameCallback = object : CameraCaptureSession.RepeatingCallback() {
        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            result: CaptureResult
        ) {
            fpsCounter++
            // Periodically dequeue frames
            val now = System.currentTimeMillis()
            val interval = if (currentFps > 0) 1000 / currentFps else 33
            if (now - lastDequeueTime > interval.toLong()) {
                lastDequeueTime = now
                dequeueFrameIfNeeded()
            }
        }

        override fun onCaptureFailed(
            session: CameraCaptureSession,
            failure: CameraCaptureSession.CaptureFailure
        ) {
            // Frame capture failed
        }

        override operator fun compareTo(other: CameraCaptureSession.RepeatingCallback): Int = 0
    }

    // ── Lifecycle ──────────────────────────────────────────────────

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
