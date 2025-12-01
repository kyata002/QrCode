package com.example.myapplication.util

import android.graphics.Bitmap
import android.graphics.Color
import com.example.myapplication.data.model.ErrorCorrectionLevel

/**
 * Custom QR Code Encoder
 * Implements QR code generation algorithm from scratch
 */
object QRCodeEncoder {
    
    // QR Code version (size) - we'll use version 1-10 for simplicity
    private const val VERSION_1 = 1  // 21x21
    private const val VERSION_2 = 2  // 25x25
    private const val VERSION_3 = 3  // 29x29
    private const val VERSION_4 = 4  // 33x33
    private const val VERSION_5 = 5  // 37x37
    
    // Mode indicators
    private const val MODE_NUMERIC = 0b0001
    private const val MODE_ALPHANUMERIC = 0b0010
    private const val MODE_BYTE = 0b0100
    private const val MODE_KANJI = 0b1000
    
    // Error correction level mapping
    private enum class ECLevel(val value: Int) {
        L(0b01),  // ~7% correction
        M(0b00),  // ~15% correction
        Q(0b11),  // ~25% correction
        H(0b10)   // ~30% correction
    }
    
    data class QRMatrix(val size: Int, val modules: Array<BooleanArray>) {
        fun get(x: Int, y: Int): Boolean = modules[y][x]
        fun set(x: Int, y: Int, value: Boolean) {
            modules[y][x] = value
        }
    }
    
    /**
     * Encode content into QR code matrix
     */
    fun encode(
        content: String,
        errorCorrectionLevel: ErrorCorrectionLevel = ErrorCorrectionLevel.MEDIUM,
        margin: Int = 4
    ): QRMatrix? {
        return try {
            // Determine best mode for content
            val mode = determineMode(content)
            
            // Determine minimum version needed
            val version = determineVersion(content, mode, errorCorrectionLevel)
            
            // Calculate matrix size
            val moduleCount = 21 + (version - 1) * 4
            val totalSize = moduleCount + margin * 2
            
            // Initialize matrix
            val matrix = QRMatrix(totalSize, Array(totalSize) { BooleanArray(totalSize) })
            
            // Encode data
            val encodedData = encodeData(content, mode, version)
            
            // Add error correction
            val ecLevel = mapErrorCorrectionLevel(errorCorrectionLevel)
            val dataWithECC = addErrorCorrection(encodedData, version, ecLevel)
            
            // Place data in matrix
            placeFinderPatterns(matrix, margin)
            placeTimingPatterns(matrix, margin, moduleCount)
            placeAlignmentPatterns(matrix, margin, version, moduleCount)
            placeDarkModule(matrix, margin, moduleCount)
            placeData(matrix, margin, moduleCount, dataWithECC)
            
            // Apply best mask pattern
            val bestMask = selectBestMask(matrix, margin, moduleCount)
            applyMask(matrix, margin, moduleCount, bestMask)
            
            // Add format information
            placeFormatInfo(matrix, margin, moduleCount, ecLevel, bestMask)
            
            matrix
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun determineMode(content: String): Int {
        // Check if numeric only
        if (content.all { it.isDigit() }) {
            return MODE_NUMERIC
        }
        
        // Check if alphanumeric
        val alphanumericChars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:"
        if (content.all { it in alphanumericChars }) {
            return MODE_ALPHANUMERIC
        }
        
        // Default to byte mode
        return MODE_BYTE
    }
    
    private fun determineVersion(content: String, mode: Int, ecLevel: ErrorCorrectionLevel): Int {
        // Simplified version determination based on content length
        val length = content.length
        
        return when (mode) {
            MODE_NUMERIC -> when {
                length <= 41 -> VERSION_1
                length <= 77 -> VERSION_2
                length <= 127 -> VERSION_3
                length <= 187 -> VERSION_4
                else -> VERSION_5
            }
            MODE_ALPHANUMERIC -> when {
                length <= 25 -> VERSION_1
                length <= 47 -> VERSION_2
                length <= 77 -> VERSION_3
                length <= 114 -> VERSION_4
                else -> VERSION_5
            }
            MODE_BYTE -> when {
                length <= 17 -> VERSION_1
                length <= 32 -> VERSION_2
                length <= 53 -> VERSION_3
                length <= 78 -> VERSION_4
                else -> VERSION_5
            }
            else -> VERSION_1
        }
    }
    
    private fun mapErrorCorrectionLevel(level: ErrorCorrectionLevel): ECLevel {
        return when (level) {
            ErrorCorrectionLevel.LOW -> ECLevel.L
            ErrorCorrectionLevel.MEDIUM -> ECLevel.M
            ErrorCorrectionLevel.QUARTILE -> ECLevel.Q
            ErrorCorrectionLevel.HIGH -> ECLevel.H
        }
    }
    
    private fun encodeData(content: String, mode: Int, version: Int): ByteArray {
        val bits = mutableListOf<Boolean>()
        
        // Add mode indicator
        addBits(bits, mode, 4)
        
        // Add character count indicator
        val characterCountBits = when (mode) {
            MODE_NUMERIC -> if (version < 10) 10 else 12
            MODE_ALPHANUMERIC -> if (version < 10) 9 else 11
            MODE_BYTE -> if (version < 10) 8 else 16
            else -> 8
        }
        addBits(bits, content.length, characterCountBits)
        
        // Encode data based on mode
        when (mode) {
            MODE_NUMERIC -> encodeNumeric(bits, content)
            MODE_ALPHANUMERIC -> encodeAlphanumeric(bits, content)
            MODE_BYTE -> encodeByte(bits, content)
        }
        
        // Add terminator
        addBits(bits, 0, minOf(4, bits.size % 8))
        
        // Pad to byte boundary
        while (bits.size % 8 != 0) {
            bits.add(false)
        }
        
        // Convert to bytes
        return bitsToBytes(bits)
    }
    
    private fun encodeNumeric(bits: MutableList<Boolean>, content: String) {
        var i = 0
        while (i < content.length) {
            val chunk = content.substring(i, minOf(i + 3, content.length))
            val value = chunk.toInt()
            val bitCount = when (chunk.length) {
                1 -> 4
                2 -> 7
                else -> 10
            }
            addBits(bits, value, bitCount)
            i += 3
        }
    }
    
    private fun encodeAlphanumeric(bits: MutableList<Boolean>, content: String) {
        val charMap = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:"
        var i = 0
        while (i < content.length) {
            if (i + 1 < content.length) {
                val value = charMap.indexOf(content[i]) * 45 + charMap.indexOf(content[i + 1])
                addBits(bits, value, 11)
                i += 2
            } else {
                addBits(bits, charMap.indexOf(content[i]), 6)
                i++
            }
        }
    }
    
    private fun encodeByte(bits: MutableList<Boolean>, content: String) {
        val bytes = content.toByteArray(Charsets.UTF_8)
        for (byte in bytes) {
            addBits(bits, byte.toInt() and 0xFF, 8)
        }
    }
    
    private fun addBits(bits: MutableList<Boolean>, value: Int, count: Int) {
        for (i in count - 1 downTo 0) {
            bits.add((value shr i) and 1 == 1)
        }
    }
    
    private fun bitsToBytes(bits: List<Boolean>): ByteArray {
        val bytes = ByteArray((bits.size + 7) / 8)
        for (i in bits.indices) {
            if (bits[i]) {
                bytes[i / 8] = (bytes[i / 8].toInt() or (1 shl (7 - i % 8))).toByte()
            }
        }
        return bytes
    }
    
    private fun addErrorCorrection(data: ByteArray, version: Int, ecLevel: ECLevel): ByteArray {
        // Simplified: just return data as-is for now
        // Full implementation would add Reed-Solomon error correction codes
        return data
    }
    
    private fun placeFinderPatterns(matrix: QRMatrix, margin: Int) {
        // Place 3 finder patterns (top-left, top-right, bottom-left)
        placeFinderPattern(matrix, margin, margin)
        placeFinderPattern(matrix, matrix.size - 7 - margin, margin)
        placeFinderPattern(matrix, margin, matrix.size - 7 - margin)
    }
    
    private fun placeFinderPattern(matrix: QRMatrix, x: Int, y: Int) {
        for (dy in -1..7) {
            for (dx in -1..7) {
                val px = x + dx
                val py = y + dy
                if (px >= 0 && px < matrix.size && py >= 0 && py < matrix.size) {
                    val isDark = when {
                        dx in 0..6 && dy in 0..6 -> {
                            // 7x7 finder pattern
                            (dx == 0 || dx == 6 || dy == 0 || dy == 6) ||
                            (dx in 2..4 && dy in 2..4)
                        }
                        else -> false
                    }
                    matrix.set(px, py, isDark)
                }
            }
        }
    }
    
    private fun placeTimingPatterns(matrix: QRMatrix, margin: Int, moduleCount: Int) {
        // Horizontal timing pattern
        for (i in 8 until moduleCount - 8) {
            matrix.set(margin + i, margin + 6, i % 2 == 0)
        }
        // Vertical timing pattern
        for (i in 8 until moduleCount - 8) {
            matrix.set(margin + 6, margin + i, i % 2 == 0)
        }
    }
    
    private fun placeAlignmentPatterns(matrix: QRMatrix, margin: Int, version: Int, moduleCount: Int) {
        // Version 1 has no alignment patterns
        if (version == 1) return
        
        // Simplified: place one alignment pattern in bottom-right for versions > 1
        if (version >= 2) {
            val pos = moduleCount - 7
            placeAlignmentPattern(matrix, margin + pos, margin + pos)
        }
    }
    
    private fun placeAlignmentPattern(matrix: QRMatrix, x: Int, y: Int) {
        for (dy in -2..2) {
            for (dx in -2..2) {
                val isDark = (dx == -2 || dx == 2 || dy == -2 || dy == 2) || (dx == 0 && dy == 0)
                matrix.set(x + dx, y + dy, isDark)
            }
        }
    }
    
    private fun placeDarkModule(matrix: QRMatrix, margin: Int, moduleCount: Int) {
        matrix.set(margin + 8, margin + moduleCount - 8, true)
    }
    
    private fun placeData(matrix: QRMatrix, margin: Int, moduleCount: Int, data: ByteArray) {
        var byteIndex = 0
        var bitIndex = 7
        
        // Place data in zigzag pattern from bottom-right
        var x = moduleCount - 1
        while (x > 0) {
            if (x == 6) x-- // Skip vertical timing column
            
            for (y in (0 until moduleCount).reversed()) {
                for (xOffset in 0..1) {
                    val px = margin + x - xOffset
                    val py = margin + y
                    
                    // Skip if already filled (finder, timing, etc.)
                    if (!isReserved(x - xOffset, y, moduleCount)) {
                        var dark = false
                        if (byteIndex < data.size) {
                            dark = ((data[byteIndex].toInt() shr bitIndex) and 1) == 1
                        }
                        matrix.set(px, py, dark)
                        
                        bitIndex--
                        if (bitIndex < 0) {
                            byteIndex++
                            bitIndex = 7
                        }
                    }
                }
            }
            x -= 2
        }
    }
    
    private fun isReserved(x: Int, y: Int, moduleCount: Int): Boolean {
        // Check if position is reserved for patterns
        // Finder patterns
        if ((x < 9 && y < 9) || (x >= moduleCount - 8 && y < 9) || (x < 9 && y >= moduleCount - 8)) {
            return true
        }
        // Timing patterns
        if (x == 6 || y == 6) {
            return true
        }
        return false
    }
    
    private fun selectBestMask(matrix: QRMatrix, margin: Int, moduleCount: Int): Int {
        // Simplified: return mask 0
        return 0
    }
    
    private fun applyMask(matrix: QRMatrix, margin: Int, moduleCount: Int, maskPattern: Int) {
        // Apply mask pattern to data area
        for (y in 0 until moduleCount) {
            for (x in 0 until moduleCount) {
                if (!isReserved(x, y, moduleCount)) {
                    val shouldMask = when (maskPattern) {
                        0 -> (x + y) % 2 == 0
                        1 -> y % 2 == 0
                        2 -> x % 3 == 0
                        3 -> (x + y) % 3 == 0
                        4 -> (y / 2 + x / 3) % 2 == 0
                        5 -> (x * y) % 2 + (x * y) % 3 == 0
                        6 -> ((x * y) % 2 + (x * y) % 3) % 2 == 0
                        7 -> ((x + y) % 2 + (x * y) % 3) % 2 == 0
                        else -> false
                    }
                    if (shouldMask) {
                        val current = matrix.get(margin + x, margin + y)
                        matrix.set(margin + x, margin + y, !current)
                    }
                }
            }
        }
    }
    
    private fun placeFormatInfo(matrix: QRMatrix, margin: Int, moduleCount: Int, ecLevel: ECLevel, maskPattern: Int) {
        // Simplified: skip format information for now
        // Full implementation would encode and place format info around finder patterns
    }
}
