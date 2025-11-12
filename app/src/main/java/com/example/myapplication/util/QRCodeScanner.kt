package com.example.myapplication.util

import android.graphics.Bitmap
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.NotFoundException

object QRCodeScanner {
    
    fun scanQRCodeFromBitmap(bitmap: Bitmap): String? {
        if (bitmap == null || bitmap.isRecycled) {
            return null
        }
        
        // Try scanning with original size first
        var result = tryScan(bitmap)
        if (result != null) return result
        
        // If failed, try with resized versions
        val sizes = listOf(800, 1200, 1600)
        for (size in sizes) {
            val resized = resizeBitmap(bitmap, size)
            result = tryScan(resized)
            if (result != null) {
                if (resized != bitmap && !resized.isRecycled) {
                    resized.recycle()
                }
                return result
            }
            if (resized != bitmap && !resized.isRecycled) {
                resized.recycle()
            }
        }
        
        return null
    }
    
    private fun tryScan(bitmap: Bitmap): String? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            
            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            
            val reader = MultiFormatReader()
            val result = reader.decode(binaryBitmap)
            result.text
        } catch (e: NotFoundException) {
            // QR code not found in this image
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }
        
        val scale = minOf(maxSize.toFloat() / width, maxSize.toFloat() / height)
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}

