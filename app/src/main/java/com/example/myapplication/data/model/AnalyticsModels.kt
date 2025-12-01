package com.example.myapplication.data.model

data class TypeStats(
    val type: QRCodeType,
    val count: Int
)

data class DateStats(
    val date: String,
    val count: Int
)

data class AnalyticsSummary(
    val totalScans: Int,
    val totalGenerated: Int,
    val typeStats: List<TypeStats>,
    val dateStats: List<DateStats>,
    val topQRCodes: List<QRCodeEntity>
)
