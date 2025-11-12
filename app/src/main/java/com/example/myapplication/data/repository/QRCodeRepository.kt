package com.example.myapplication.data.repository

import com.example.myapplication.data.dao.QRCodeDao
import com.example.myapplication.data.model.QRCodeEntity
import com.example.myapplication.data.model.QRCodeType
import kotlinx.coroutines.flow.Flow

class QRCodeRepository(private val qrCodeDao: QRCodeDao) {
    
    fun getAllQRCodes(): Flow<List<QRCodeEntity>> = qrCodeDao.getAllQRCodes()
    
    fun getScannedQRCodes(): Flow<List<QRCodeEntity>> = qrCodeDao.getQRCodesByType(true)
    
    fun getCreatedQRCodes(): Flow<List<QRCodeEntity>> = qrCodeDao.getQRCodesByType(false)
    
    suspend fun getQRCodeById(id: Long): QRCodeEntity? = qrCodeDao.getQRCodeById(id)
    
    suspend fun insertQRCode(qrCode: QRCodeEntity): Long = qrCodeDao.insertQRCode(qrCode)
    
    suspend fun deleteQRCode(qrCode: QRCodeEntity) = qrCodeDao.deleteQRCode(qrCode)
    
    suspend fun deleteQRCodeById(id: Long) = qrCodeDao.deleteQRCodeById(id)
    
    suspend fun saveScannedQR(content: String, type: QRCodeType): Long {
        val qrCode = QRCodeEntity(
            content = content,
            type = type,
            isScanned = true
        )
        return insertQRCode(qrCode)
    }
    
    suspend fun saveCreatedQR(content: String, type: QRCodeType, imagePath: String? = null): Long {
        val qrCode = QRCodeEntity(
            content = content,
            type = type,
            isScanned = false,
            qrImagePath = imagePath
        )
        return insertQRCode(qrCode)
    }
}

