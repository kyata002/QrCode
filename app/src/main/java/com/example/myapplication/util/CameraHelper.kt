package com.example.myapplication.util

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraHelper(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView
) {

    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: return

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        imageCapture = ImageCapture.Builder().build()

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
            Toast.makeText(context, "Lỗi khởi động camera", Toast.LENGTH_SHORT).show()
        }
    }
    
    private var imageAnalysis: androidx.camera.core.ImageAnalysis? = null
    private var isAutoScanEnabled = false
    private var lastScanTime = 0L
    private val scanDebounceMs = 2000L // 2 seconds between scans
    private var autoScanCallback: ((String) -> Unit)? = null
    
    fun enableAutoScan(enable: Boolean, onQRDetected: ((String) -> Unit)? = null) {
        isAutoScanEnabled = enable
        autoScanCallback = onQRDetected
        
        if (enable) {
            startAutoScan()
        } else {
            stopAutoScan()
        }
    }
    
    private fun startAutoScan() {
        val cameraProvider = cameraProvider ?: return
        
        imageAnalysis = androidx.camera.core.ImageAnalysis.Builder()
            .setBackpressureStrategy(androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .apply {
                setAnalyzer(cameraExecutor) { imageProxy ->
                    val currentTime = System.currentTimeMillis()
                    
                    // Debounce: only scan if enough time has passed
                    if (isAutoScanEnabled && currentTime - lastScanTime > scanDebounceMs) {
                        val bitmap = imageProxy.toBitmap()
                        val result = QRCodeScanner.scanQRCodeFromBitmap(bitmap)
                        
                        if (result != null) {
                            lastScanTime = currentTime
                            autoScanCallback?.invoke(result)
                        }
                    }
                    
                    imageProxy.close()
                }
            }
        
        try {
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture,
                imageAnalysis
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start auto scan", e)
        }
    }
    
    private fun stopAutoScan() {
        imageAnalysis = null
        bindCameraUseCases() // Rebind without ImageAnalysis
    }

    fun takePicture(
        onImageSaved: (File) -> Unit,
        onError: (String) -> Unit
    ) {
        val imageCapture = imageCapture ?: return

        val outputFile = File(context.cacheDir, "QR_SCAN_${System.currentTimeMillis()}.jpg")
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        imageCapture.takePicture(
            outputFileOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    onImageSaved(outputFile)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                    onError(exception.message ?: "Unknown error")
                }
            }
        )
    }


    fun enableFlashlight(enable: Boolean) {
        try {
            camera?.cameraControl?.enableTorch(enable)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to control flashlight", e)
        }
    }
    
    fun toggleFlashlight(): Boolean {
        val currentState = camera?.cameraInfo?.torchState?.value == androidx.camera.core.TorchState.ON
        val newState = !currentState
        enableFlashlight(newState)
        return newState
    }
    
    fun isFlashlightOn(): Boolean {
        return camera?.cameraInfo?.torchState?.value == androidx.camera.core.TorchState.ON
    }

    fun shutdown() {
        enableFlashlight(false) // Turn off flashlight before shutdown
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraHelper"
    }
}
