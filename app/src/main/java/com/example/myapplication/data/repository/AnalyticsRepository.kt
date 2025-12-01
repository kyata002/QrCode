package com.example.myapplication.data.repository

import com.example.myapplication.data.dao.QRCodeDao
import com.example.myapplication.data.model.AnalyticsSummary
import com.example.myapplication.data.model.DateStats
import com.example.myapplication.data.model.TypeStats

class AnalyticsRepository(private val dao: QRCodeDao) {
    
    suspend fun getAnalyticsSummary(): AnalyticsSummary {
        val totalScans = dao.getTotalScans()
        val totalGenerated = dao.getTotalGenerated()
        val topQRCodes = dao.getTopScannedQRCodes()
        
        // Convert DAO results to model classes
        val typeStatsResults = dao.getScanStatsByType()
        val typeStats = typeStatsResults.map { TypeStats(it.type, it.count) }
        
        val dateStatsResults = dao.getScansTimeline()
        val dateStats = dateStatsResults.map { DateStats(it.date, it.count) }
        
        return AnalyticsSummary(
            totalScans = totalScans,
            totalGenerated = totalGenerated,
            typeStats = typeStats,
            dateStats = dateStats,
            topQRCodes = topQRCodes
        )
    }
}
