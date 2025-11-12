package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "qr_codes")
data class QRCodeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val type: QRCodeType,
    val isScanned: Boolean, // true nếu là QR đã quét, false nếu là QR đã tạo
    val createdAt: Long = System.currentTimeMillis(),
    val qrImagePath: String? = null // Đường dẫn đến ảnh QR code đã lưu
)

enum class QRCodeType {
    TEXT,
    URL,
    WIFI,
    SMS,
    VCARD,
    UNKNOWN
}

