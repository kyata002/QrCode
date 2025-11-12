package com.example.myapplication.data.dao

import androidx.room.*
import com.example.myapplication.data.model.QRCodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QRCodeDao {
    
    @Query("SELECT * FROM qr_codes ORDER BY createdAt DESC")
    fun getAllQRCodes(): Flow<List<QRCodeEntity>>
    
    @Query("SELECT * FROM qr_codes WHERE isScanned = :isScanned ORDER BY createdAt DESC")
    fun getQRCodesByType(isScanned: Boolean): Flow<List<QRCodeEntity>>
    
    @Query("SELECT * FROM qr_codes WHERE id = :id")
    suspend fun getQRCodeById(id: Long): QRCodeEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQRCode(qrCode: QRCodeEntity): Long
    
    @Delete
    suspend fun deleteQRCode(qrCode: QRCodeEntity)
    
    @Query("DELETE FROM qr_codes WHERE id = :id")
    suspend fun deleteQRCodeById(id: Long)
    
    @Query("DELETE FROM qr_codes")
    suspend fun deleteAll()
}

