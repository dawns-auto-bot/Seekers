package com.example.seekers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class SharedViewModel(application: Application): AndroidViewModel(application) {
    val locService = MutableLiveData<ForegroundService>()

    fun updateLocService(newVal: ForegroundService) {
        locService.value = newVal
    }
}