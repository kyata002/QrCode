package com.example.myapplication.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import com.example.myapplication.data.model.ErrorCorrectionLevel
import com.example.myapplication.data.model.QRCodeCustomization
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel as ZXingErrorCorrectionLevel

object QRCodeGenerator {
    
    fun generateQRCode(
        content: String,
        customization: QRCodeCustomization = QRCodeCustomization()
    ): Bitmap? {
        return try {
            val hints = hashMapOf<EncodeHintType, Any>().apply {
                put(EncodeHintType.ERROR_CORRECTION, customization.errorCorrectionLevel.toZXingLevel())
                put(EncodeHintType.MARGIN, customization.margin)
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
            }
            
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, customization.size, customization.size, hints)
            
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(
                        x, y,
                        if (bitMatrix[x, y]) customization.foregroundColor else customization.backgroundColor
                    )
                }
            }
            
            // Thêm logo nếu có
            val finalBitmap = if (customization.logo != null) {
                addLogoToQRCode(bitmap, customization.logo)
            } else {
                bitmap
            }
            
            // Bo góc nếu cần
            if (customization.borderRadius > 0) {
                roundCorners(finalBitmap, customization.borderRadius)
            } else {
                finalBitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun addLogoToQRCode(qrBitmap: Bitmap, logo: Bitmap): Bitmap {
        val logoSize = qrBitmap.width / 5 // Logo chiếm 1/5 kích thước QR code
        val logoScaled = Bitmap.createScaledBitmap(logo, logoSize, logoSize, true)
        
        val canvas = Canvas(qrBitmap)
        val left = (qrBitmap.width - logoSize) / 2f
        val top = (qrBitmap.height - logoSize) / 2f
        
        // Vẽ nền trắng cho logo
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val padding = logoSize / 10f
        canvas.drawRect(
            left - padding,
            top - padding,
            left + logoSize + padding,
            top + logoSize + padding,
            paint
        )
        
        canvas.drawBitmap(logoScaled, left, top, null)
        return qrBitmap
    }
    
    private fun roundCorners(bitmap: Bitmap, radius: Int): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val rectF = RectF(rect)
        
        canvas.drawRoundRect(rectF, radius.toFloat(), radius.toFloat(), paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        
        return output
    }
    
    private fun ErrorCorrectionLevel.toZXingLevel(): ZXingErrorCorrectionLevel {
        return when (this) {
            ErrorCorrectionLevel.LOW -> ZXingErrorCorrectionLevel.L
            ErrorCorrectionLevel.MEDIUM -> ZXingErrorCorrectionLevel.M
            ErrorCorrectionLevel.QUARTILE -> ZXingErrorCorrectionLevel.Q
            ErrorCorrectionLevel.HIGH -> ZXingErrorCorrectionLevel.H
        }
    }
}

