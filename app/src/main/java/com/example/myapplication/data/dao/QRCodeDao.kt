package com.example.myapplication.data.dao

import androidx.room.*
import com.example.myapplication.data.model.QRCodeEntity
import com.example.myapplication.data.model.QRCodeType
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
    
    // Search and filter queries
    @Query("SELECT * FROM qr_codes WHERE content LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    suspend fun searchQRCodes(query: String): List<QRCodeEntity>
    
    @Query("SELECT * FROM qr_codes WHERE type = :type ORDER BY createdAt DESC")
    suspend fun filterByType(type: QRCodeType): List<QRCodeEntity>
    
    @Query("SELECT * FROM qr_codes WHERE isFavorite = 1 ORDER BY createdAt DESC")
    suspend fun getFavorites(): List<QRCodeEntity>
    
    @Query("UPDATE qr_codes SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)
    
    @Query("UPDATE qr_codes SET tags = :tags WHERE id = :id")
    suspend fun updateTags(id: Long, tags: String?)
    
    // Analytics queries - Fixed to return List instead of Map
    @Query("""
        SELECT type, COUNT(*) as count 
        FROM qr_codes 
        WHERE isScanned = 1 
        GROUP BY type 
        ORDER BY count DESC
    """)
    suspend fun getScanStatsByType(): List<TypeStatsResult>
    
    @Query("""
        SELECT DATE(createdAt/1000, 'unixepoch') as date, COUNT(*) as count 
        FROM qr_codes 
        WHERE isScanned = 1 
        GROUP BY date 
        ORDER BY date DESC
        LIMIT 30
    """)
    suspend fun getScansTimeline(): List<DateStatsResult>
    
    @Query("""
        SELECT * FROM qr_codes 
        WHERE isScanned = 1 
        ORDER BY createdAt DESC 
        LIMIT 10
    """)
    suspend fun getTopScannedQRCodes(): List<QRCodeEntity>
    
    @Query("SELECT COUNT(*) FROM qr_codes WHERE isScanned = 1")
    suspend fun getTotalScans(): Int
    
    @Query("SELECT COUNT(*) FROM qr_codes WHERE isScanned = 0")
    suspend fun getTotalGenerated(): Int
}

// Result classes for Room queries
data class TypeStatsResult(
    val type: QRCodeType,
    val count: Int
)

data class DateStatsResult(
    val date: String,
    val count: Int
)
