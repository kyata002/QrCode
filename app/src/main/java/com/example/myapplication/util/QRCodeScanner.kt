package com.example.myapplication.util

import android.graphics.Bitmap
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.tasks.await

object QRCodeScanner {
    
    /**
     * Scan QR code from bitmap using ML Kit Barcode Scanning API
     * @param bitmap The bitmap to scan for QR codes
     * @return The decoded QR code content, or null if no QR code found
     */
    fun scanQRCodeFromBitmap(bitmap: Bitmap): String? {
        return try {
            // Configure barcode scanner to only detect QR codes
            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
            
            val scanner = BarcodeScanning.getClient(options)
            val image = InputImage.fromBitmap(bitmap, 0)
            
            // Synchronous scanning - blocks until complete
            val task = scanner.process(image)
            
            // Wait for result (this is blocking, but it's fast)
            var result: String? = null
            task.addOnSuccessListener { barcodes ->
                // Get the first QR code found
                result = barcodes.firstOrNull()?.rawValue
            }.addOnFailureListener {
                // Scanning failed
                result = null
            }
            
            // Wait for task to complete (simple blocking approach)
            while (!task.isComplete) {
                Thread.sleep(10)
            }
            
            result
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Scan QR code from bitmap asynchronously using coroutines
     * @param bitmap The bitmap to scan for QR codes
     * @return The decoded QR code content, or null if no QR code found
     */
    suspend fun scanQRCodeFromBitmapAsync(bitmap: Bitmap): String? {
        return try {
            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
            
            val scanner = BarcodeScanning.getClient(options)
            val image = InputImage.fromBitmap(bitmap, 0)
            
            val barcodes = scanner.process(image).await()
            barcodes.firstOrNull()?.rawValue
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
