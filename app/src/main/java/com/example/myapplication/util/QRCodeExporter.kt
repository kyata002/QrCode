package com.example.myapplication.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

object QRCodeExporter {
    
    /**
     * Export QR code as SVG format
     */
    fun exportAsSVG(bitmap: Bitmap, file: File): Boolean {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            
            // Generate SVG content
            val svgContent = buildString {
                appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                appendLine("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"$width\" height=\"$height\" viewBox=\"0 0 $width $height\">")
                
                // Add white background
                appendLine("  <rect width=\"$width\" height=\"$height\" fill=\"white\"/>")
                
                // Convert bitmap pixels to SVG rectangles
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val pixel = bitmap.getPixel(x, y)
                        if (pixel == Color.BLACK) {
                            appendLine("  <rect x=\"$x\" y=\"$y\" width=\"1\" height=\"1\" fill=\"black\"/>")
                        }
                    }
                }
                
                appendLine("</svg>")
            }
            
            FileOutputStream(file).use { output ->
                output.write(svgContent.toByteArray())
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Export QR code as PDF format
     */
    fun exportAsPDF(bitmap: Bitmap, file: File): Boolean {
        return try {
            val pdfDocument = PdfDocument()
            
            // Create page info (A4 size: 595 x 842 points)
            val pageWidth = 595
            val pageHeight = 842
            
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            
            val canvas = page.canvas
            
            // Calculate scaling to fit QR code centered on page
            val qrSize = minOf(pageWidth, pageHeight) - 100 // 50pt margin on each side
            val left = (pageWidth - qrSize) / 2f
            val top = (pageHeight - qrSize) / 2f
            
            // Draw white background
            val paint = Paint()
            paint.color = Color.WHITE
            canvas.drawRect(left, top, left + qrSize, top + qrSize, paint)
            
            // Draw bitmap scaled to fit
            val destRect = android.graphics.RectF(left, top, left + qrSize, top + qrSize)
            canvas.drawBitmap(bitmap, null, destRect, null)
            
            pdfDocument.finishPage(page)
            
            // Write to file
            FileOutputStream(file).use { output ->
                pdfDocument.writeTo(output)
            }
            
            pdfDocument.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Export as PNG (standard format)
     */
    fun exportAsPNG(bitmap: Bitmap, file: File, quality: Int = 100): Boolean {
        return try {
            FileOutputStream(file).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, quality, output)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Export as JPEG
     */
    fun exportAsJPEG(bitmap: Bitmap, file: File, quality: Int = 90): Boolean {
        return try {
            FileOutputStream(file).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Get file extension based on export format
     */
    fun getFileExtension(format: ExportFormat): String {
        return when (format) {
            ExportFormat.PNG -> "png"
            ExportFormat.JPEG -> "jpg"
            ExportFormat.SVG -> "svg"
            ExportFormat.PDF -> "pdf"
        }
    }
    
    /**
     * Export bitmap in specified format
     */
    fun export(bitmap: Bitmap, format: ExportFormat, file: File): Boolean {
        return when (format) {
            ExportFormat.PNG -> exportAsPNG(bitmap, file)
            ExportFormat.JPEG -> exportAsJPEG(bitmap, file)
            ExportFormat.SVG -> exportAsSVG(bitmap, file)
            ExportFormat.PDF -> exportAsPDF(bitmap, file)
        }
    }
}

enum class ExportFormat {
    PNG,
    JPEG,
    SVG,
    PDF
}
