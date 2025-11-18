package com.example.myapplication.util

import com.example.myapplication.data.model.QRCodeType

object QRCodeContentBuilder {
    
    fun buildWiFiContent(ssid: String, password: String, security: String): String {
        val securityType = when (security.lowercase()) {
            "wpa", "wpa2" -> "WPA"
            else -> "nopass"
        }
        return "WIFI:T:$securityType;S:$ssid;P:$password;;"
    }
    
    fun buildSMSContent(phoneNumber: String, message: String): String {
        return "SMSTO:$phoneNumber:$message"
    }
    
    fun buildVCardContent(
        name: String,
        phone: String? = null,
        email: String? = null,
        organization: String? = null,
        address: String? = null
    ): String {
        val vcard = StringBuilder()
        vcard.append("BEGIN:VCARD\n")
        vcard.append("VERSION:3.0\n")
        vcard.append("FN:$name\n")
        
        if (!phone.isNullOrBlank()) {
            vcard.append("TEL:$phone\n")
        }
        if (!email.isNullOrBlank()) {
            vcard.append("EMAIL:$email\n")
        }
        if (!organization.isNullOrBlank()) {
            vcard.append("ORG:$organization\n")
        }
        if (!address.isNullOrBlank()) {
            vcard.append("ADR:$address\n")
        }
        
        vcard.append("END:VCARD")
        return vcard.toString()
    }
    
    fun detectQRCodeType(content: String): QRCodeType {
        return when {
            // URL
            content.startsWith("http://", ignoreCase = true) ||
            content.startsWith("https://", ignoreCase = true) -> QRCodeType.URL

            // WiFi
            content.startsWith("WIFI:", ignoreCase = true) -> QRCodeType.WIFI

            // SMS
            content.startsWith("SMSTO:", ignoreCase = true) -> QRCodeType.SMS

            // vCard (danh bạ)
            content.startsWith("BEGIN:VCARD", ignoreCase = true) -> QRCodeType.VCARD

            // Email (nhiều chuẩn khác nhau)
            content.startsWith("mailto:", ignoreCase = true) ||
            content.startsWith("MATMSG:", ignoreCase = true) -> QRCodeType.EMAIL

            // Gọi điện
            content.startsWith("tel:", ignoreCase = true) ||
            content.startsWith("TEL:", ignoreCase = true) -> QRCodeType.PHONE

            // Vị trí (map)
            content.startsWith("geo:", ignoreCase = true) -> QRCodeType.GEO

            // Sự kiện (calendar)
            content.startsWith("BEGIN:VEVENT", ignoreCase = true) -> QRCodeType.EVENT

            // Mặc định coi là text thuần
            else -> QRCodeType.TEXT
        }
    }
    
    fun parseWiFiContent(content: String): Triple<String, String, String>? {
        if (!content.startsWith("WIFI:")) return null
        
        val parts = content.removePrefix("WIFI:").split(";")
        var ssid = ""
        var password = ""
        var security = "nopass"
        
        parts.forEach { part ->
            when {
                part.startsWith("S:") -> ssid = part.removePrefix("S:")
                part.startsWith("P:") -> password = part.removePrefix("P:")
                part.startsWith("T:") -> security = part.removePrefix("T:")
            }
        }
        
        return Triple(ssid, password, security)
    }
    
    fun parseSMSContent(content: String): Pair<String, String>? {
        if (!content.startsWith("SMSTO:")) return null
        
        val parts = content.removePrefix("SMSTO:").split(":", limit = 2)
        return if (parts.size == 2) {
            Pair(parts[0], parts[1])
        } else {
            Pair(parts[0], "")
        }
    }
}

