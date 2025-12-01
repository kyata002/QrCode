package com.example.myapplication.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object BatchQRGenerator {
    
    /**
     * Parse CSV file and return list of content strings
     */
    fun parseCSV(context: Context, uri: Uri): List<String> {
        val contents = mutableListOf<String>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.forEachLine { line ->
                        val trimmed = line.trim()
                        if (trimmed.isNotEmpty()) {
                            // Support CSV with multiple columns, take first column
                            val firstColumn = trimmed.split(",").firstOrNull()?.trim()
                            if (!firstColumn.isNullOrEmpty()) {
                                contents.add(firstColumn)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return contents
    }
    
    /**
     * Parse text input (one item per line)
     */
    fun parseTextInput(text: String): List<String> {
        return text.split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
    
    /**
     * Generate QR codes for batch of content
     */
    fun generateBatch(
        contents: List<String>,
        size: Int = 512,
        includeText: Boolean = true,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): List<Pair<String, Bitmap>> {
        val results = mutableListOf<Pair<String, Bitmap>>()
        
        contents.forEachIndexed { index, content ->
            val customization = com.example.myapplication.data.model.QRCodeCustomization(size = size)
            val qrBitmap = QRCodeGenerator.generateQRCode(content, customization)
            
            if (qrBitmap != null) {
                val finalBitmap = if (includeText) {
                    addTextLabel(qrBitmap, content)
                } else {
                    qrBitmap
                }
                results.add(content to finalBitmap)
            }
            
            onProgress(index + 1, contents.size)
        }
        
        return results
    }
    
    /**
     * Add text label below QR code
     */
    private fun addTextLabel(qrBitmap: Bitmap, text: String): Bitmap {
        val textHeight = 60
        val padding = 20
        
        val newBitmap = Bitmap.createBitmap(
            qrBitmap.width,
            qrBitmap.height + textHeight + padding,
            Bitmap.Config.ARGB_8888
        )
        
        val canvas = Canvas(newBitmap)
        
        // White background
        canvas.drawColor(Color.WHITE)
        
        // Draw QR code
        canvas.drawBitmap(qrBitmap, 0f, 0f, null)
        
        // Draw text
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        
        // Truncate text if too long
        val displayText = if (text.length > 50) {
            text.substring(0, 47) + "..."
        } else {
            text
        }
        
        canvas.drawText(
            displayText,
            newBitmap.width / 2f,
            qrBitmap.height + padding + 30f,
            paint
        )
        
        return newBitmap
    }
    
    /**
     * Export batch results as ZIP file
     */
    fun exportAsZip(
        results: List<Pair<String, Bitmap>>,
        zipFile: File,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Boolean {
        return try {
            ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
                results.forEachIndexed { index, (content, bitmap) ->
                    // Create safe filename from content
                    val filename = createSafeFilename(content, index)
                    
                    // Add entry to zip
                    val entry = ZipEntry("$filename.png")
                    zipOut.putNextEntry(entry)
                    
                    // Write bitmap to zip
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, zipOut)
                    zipOut.closeEntry()
                    
                    onProgress(index + 1, results.size)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Create safe filename from content string
     */
    private fun createSafeFilename(content: String, index: Int): String {
        // Remove invalid characters and limit length
        val safe = content
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            .take(30)
        
        return if (safe.isNotEmpty()) {
            "qr_${index + 1}_$safe"
        } else {
            "qr_${index + 1}"
        }
    }
}
