package com.example.myapplication.data.model

import android.graphics.Bitmap

data class QRCodeCustomization(
    val foregroundColor: Int = 0xFF000000.toInt(), // Màu đen
    val backgroundColor: Int = 0xFFFFFFFF.toInt(), // Màu trắng
    val errorCorrectionLevel: ErrorCorrectionLevel = ErrorCorrectionLevel.MEDIUM,
    val size: Int = 512, // Kích thước QR code (pixels)
    val margin: Int = 4, // Lề
    val logo: Bitmap? = null, // Logo chèn vào giữa QR code
    val borderRadius: Int = 0 // Bo góc (0 = không bo)
)

enum class ErrorCorrectionLevel {
    LOW,      // ~7% có thể khôi phục
    MEDIUM,   // ~15% có thể khôi phục
    QUARTILE, // ~25% có thể khôi phục
    HIGH      // ~30% có thể khôi phục
}

