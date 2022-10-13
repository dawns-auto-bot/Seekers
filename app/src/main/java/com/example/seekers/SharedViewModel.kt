package com.example.seekers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class SharedViewModel(application: Application): AndroidViewModel(application) {
    val locService = MutableLiveData<GameService>()

    fun updateLocService(newVal: GameService) {
        locService.value = newVal
    }
}