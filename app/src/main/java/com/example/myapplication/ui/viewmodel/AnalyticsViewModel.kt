package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.AnalyticsSummary
import com.example.myapplication.data.repository.AnalyticsRepository
import kotlinx.coroutines.launch

class AnalyticsViewModel(private val repository: AnalyticsRepository) : ViewModel() {
    
    private val _analyticsSummary = MutableLiveData<AnalyticsSummary>()
    val analyticsSummary: LiveData<AnalyticsSummary> = _analyticsSummary
    
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    fun loadAnalytics() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val summary = repository.getAnalyticsSummary()
                _analyticsSummary.value = summary
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
