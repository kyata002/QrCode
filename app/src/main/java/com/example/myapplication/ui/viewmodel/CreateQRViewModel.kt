package com.example.myapplication.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.QRCodeCustomization
import com.example.myapplication.data.model.QRCodeType
import com.example.myapplication.data.repository.QRCodeRepository
import com.example.myapplication.util.QRCodeContentBuilder
import com.example.myapplication.util.QRCodeGenerator
import kotlinx.coroutines.launch

class CreateQRViewModel(private val repository: QRCodeRepository) : ViewModel() {
    
    private val _qrCodeBitmap = MutableLiveData<Bitmap?>()
    val qrCodeBitmap: LiveData<Bitmap?> = _qrCodeBitmap
    
    private val _currentContent = MutableLiveData<String>()
    val currentContent: LiveData<String> = _currentContent
    
    private val _currentType = MutableLiveData<QRCodeType>(QRCodeType.TEXT)
    val currentType: LiveData<QRCodeType> = _currentType
    
    private val _customization = MutableLiveData<QRCodeCustomization>(QRCodeCustomization())
    val customization: LiveData<QRCodeCustomization> = _customization
    
    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess
    
    fun setQRType(type: QRCodeType) {
        _currentType.value = type
    }
    
    fun setContent(content: String) {
        _currentContent.value = content
    }
    
    fun updateCustomization(customization: QRCodeCustomization) {
        _customization.value = customization
    }
    
    fun generateQRCode(
        text: String? = null,
        url: String? = null,
        wifiSSID: String? = null,
        wifiPassword: String? = null,
        wifiSecurity: String? = null,
        smsNumber: String? = null,
        smsMessage: String? = null,
        vcardName: String? = null,
        vcardPhone: String? = null,
        vcardEmail: String? = null,
        vcardOrg: String? = null,
        vcardAddress: String? = null
    ) {
        val content = when (_currentType.value) {
            QRCodeType.TEXT -> text ?: ""
            QRCodeType.URL -> url ?: ""
            QRCodeType.WIFI -> {
                if (wifiSSID != null && wifiPassword != null && wifiSecurity != null) {
                    QRCodeContentBuilder.buildWiFiContent(wifiSSID, wifiPassword, wifiSecurity)
                } else ""
            }
            QRCodeType.SMS -> {
                if (smsNumber != null && smsMessage != null) {
                    QRCodeContentBuilder.buildSMSContent(smsNumber, smsMessage)
                } else ""
            }
            QRCodeType.VCARD -> {
                if (vcardName != null) {
                    QRCodeContentBuilder.buildVCardContent(
                        vcardName,
                        vcardPhone,
                        vcardEmail,
                        vcardOrg,
                        vcardAddress
                    )
                } else ""
            }
            else -> ""
        }
        
        if (content.isNotEmpty()) {
            _currentContent.value = content
            val customization = _customization.value ?: QRCodeCustomization()
            val bitmap = QRCodeGenerator.generateQRCode(content, customization)
            _qrCodeBitmap.value = bitmap
        }
    }
    
    fun saveQRCode() {
        val content = _currentContent.value
        val type = _currentType.value
        if (content != null && type != null && content.isNotEmpty()) {
            viewModelScope.launch {
                try {
                    repository.saveCreatedQR(content, type)
                    _saveSuccess.value = true
                } catch (e: Exception) {
                    _saveSuccess.value = false
                }
            }
        }
    }
    
    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }
}

