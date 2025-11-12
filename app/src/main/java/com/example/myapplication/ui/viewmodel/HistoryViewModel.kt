package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.QRCodeEntity
import com.example.myapplication.data.repository.QRCodeRepository
import kotlinx.coroutines.launch

class HistoryViewModel(private val repository: QRCodeRepository) : ViewModel() {
    
    val allQRCodes: LiveData<List<QRCodeEntity>> = repository.getAllQRCodes().asLiveData()
    val scannedQRCodes: LiveData<List<QRCodeEntity>> = repository.getScannedQRCodes().asLiveData()
    val createdQRCodes: LiveData<List<QRCodeEntity>> = repository.getCreatedQRCodes().asLiveData()
    
    fun deleteQRCode(qrCode: QRCodeEntity) {
        viewModelScope.launch {
            repository.deleteQRCode(qrCode)
        }
    }
    
    fun deleteQRCodeById(id: Long) {
        viewModelScope.launch {
            repository.deleteQRCodeById(id)
        }
    }
}

