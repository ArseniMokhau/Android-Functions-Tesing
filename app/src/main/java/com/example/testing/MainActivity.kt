package com.example.testing

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.Image
import android.media.ImageReader
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.Surface
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var captureButton: Button
    private lateinit var cameraManager: CameraManager
    private var cameraDevice: CameraDevice? = null
    private lateinit var backgroundHandler: Handler
    private lateinit var backgroundThread: HandlerThread
    private lateinit var imageReader: ImageReader
    private lateinit var frontCameraId: String
    private lateinit var backCameraId: String
    private var currentCameraId: String? = null
    private var photosTaken = 0
    private val NUM_FRAMES_FOR_CONVERGENCE = 30
    private lateinit var mDummyPreview: SurfaceTexture
    private lateinit var mDummySurface: Surface

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Get reference to the Button using findViewById
        captureButton = findViewById(R.id.captureButton)
        mDummyPreview = SurfaceTexture(1)
        mDummySurface = Surface(mDummyPreview)

        // Set up a click listener for the button
        captureButton.setOnClickListener {
            getCameraIds()
            photosTaken = 0
            takePhotos()
        }

        // Initialize CameraManager
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        // Set up background thread and handler
        backgroundThread = HandlerThread("CameraBackground").apply {
            start()
            backgroundHandler = Handler(looper)
        }

        // Set up ImageReader
        imageReader = ImageReader.newInstance(1920, 1080, ImageFormat.JPEG, 2)
        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            // Process the captured image here
            saveImageToGallery(image)
            image?.close()
        }, backgroundHandler)
    }

    // Retrieve camera IDs for front and back cameras
    private fun getCameraIds() {
        val cameraIds = cameraManager.cameraIdList
        for (id in cameraIds) {
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                frontCameraId = id
            } else if (lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                backCameraId = id
            }
        }
    }

    // Initiate the process of taking photos
    private fun takePhotos() {
        if (photosTaken >= 2) {
            // Limit to 2 photos (front and back)
            return
        }

        if (photosTaken == 0) {
            currentCameraId = backCameraId
        } else {
            currentCameraId = frontCameraId
        }

        try {
            // Check camera permission
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            cameraManager.openCamera(currentCameraId!!, stateCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e("CameraCapture", "Error accessing camera: ${e.message}")
        }
    }

    // Callback for camera device state changes
    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            createCaptureSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice?.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice?.close()
        }
    }

    // Create a capture session with necessary surfaces
    private fun createCaptureSession() {
        try {
            val surfaces = mutableListOf<Surface>()
            surfaces.add(imageReader.surface)
            surfaces.add(mDummySurface) // Add the dummy surface

            cameraDevice?.createCaptureSession(surfaces, captureSessionCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e("CameraCapture", "Error creating capture session: ${e.message}")
        }
    }

    // Callback for capture session state changes
    private val captureSessionCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            try {
                // Create a repeating request targeting the dummy preview
                val previewRequestBuilder =
                    cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                previewRequestBuilder?.addTarget(mDummySurface)
                previewRequestBuilder?.build()
                    ?.let { session.setRepeatingRequest(it, null, backgroundHandler) }

                // After N frames, submit the capture request for the JPEG
                backgroundHandler.postDelayed({
                    val captureRequestBuilder =
                        cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                    captureRequestBuilder?.addTarget(imageReader.surface)

                    // Enable flash
                    captureRequestBuilder?.set(
                        CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
                    )

                    captureRequestBuilder?.build()
                        ?.let { session.capture(it, captureCallback, backgroundHandler) }
                }, (NUM_FRAMES_FOR_CONVERGENCE * /*frame duration*/ 33).toLong()) // Adjust the frame duration

            } catch (e: CameraAccessException) {
                Log.e("CameraCapture", "Error configuring capture session: ${e.message}")
            }
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            Log.e("CameraCapture", "Capture session configuration failed")
        }
    }

    // Callback for capture results
    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            Log.i("CameraCapture", "Image captured successfully")

            cameraDevice?.close()
            photosTaken++

            if (photosTaken < 2) {
                takePhotos()
            }
        }

        override fun onCaptureFailed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            failure: CaptureFailure
        ) {
            Log.e("CameraCapture", "Capture failed: ${failure.reason}")
        }
    }

    // Save the captured image to the gallery
    private fun saveImageToGallery(image: Image) {
        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "IMG_$timeStamp.jpg"

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        val resolver = contentResolver
        var uri: Uri? = null

        try {
            uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    outputStream.write(bytes)
                }
            }

            Log.i("SaveImage", "Image saved to gallery")
        } catch (e: IOException) {
            Log.e("SaveImage", "Error saving image to gallery: ${e.localizedMessage}")
        } finally {
            image.close()
        }

        uri?.let { resolver.notifyChange(it, null) }
    }

    override fun onDestroy() {
        super.onDestroy()
        backgroundThread.quitSafely()
        cameraDevice?.close()
        imageReader.close()
    }
}
