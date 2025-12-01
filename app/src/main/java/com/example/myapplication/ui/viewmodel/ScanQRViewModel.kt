package com.example.myapplication.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.QRCodeType
import com.example.myapplication.data.repository.QRCodeRepository
import com.example.myapplication.util.QRCodeContentBuilder
import com.example.myapplication.util.QRCodeScanner
import kotlinx.coroutines.launch

class ScanQRViewModel(private val repository: QRCodeRepository) : ViewModel() {
    
    private val _scannedContent = MutableLiveData<String?>()
    val scannedContent: LiveData<String?> = _scannedContent
    
    private val _scannedType = MutableLiveData<QRCodeType>()
    val scannedType: LiveData<QRCodeType> = _scannedType
    
    private val _scanError = MutableLiveData<String?>()
    val scanError: LiveData<String?> = _scanError
    
    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    private val _isBatchMode = MutableLiveData<Boolean>(false)
    val isBatchMode: LiveData<Boolean> = _isBatchMode

    fun setBatchMode(enabled: Boolean) {
        _isBatchMode.value = enabled
    }

    
    fun scanQRCodeFromBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                val content = QRCodeScanner.scanQRCodeFromBitmapAsync(bitmap)
                if (content != null) {
                    _scannedContent.value = content
                    _scannedType.value = QRCodeContentBuilder.detectQRCodeType(content)
                    _scanError.value = null
                } else {
                    _scannedContent.value = null
                    _scannedType.value = QRCodeType.UNKNOWN
                    _scanError.value = "Không thể quét QR code"
                }
            } catch (e: Exception) {
                _scannedContent.value = null
                _scannedType.value = QRCodeType.UNKNOWN
                _scanError.value = "Lỗi: ${e.message}"
            }
        }
    }
    
    fun saveScannedQR() {
        val content = _scannedContent.value
        val type = _scannedType.value
        if (content != null && type != null) {
            viewModelScope.launch {
                try {
                    repository.saveScannedQR(content, type)
                    _saveSuccess.value = true
                } catch (e: Exception) {
                    _saveSuccess.value = false
                }
            }
        }
    }
    
    fun resetScanResult() {
        _scannedContent.value = null
        _scannedType.value = QRCodeType.UNKNOWN
        _scanError.value = null
    }
    
    fun setScannedContent(content: String) {
        _scannedContent.value = content
        _scannedType.value = QRCodeContentBuilder.detectQRCodeType(content)
        _scanError.value = null
    }
    
    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }
}

