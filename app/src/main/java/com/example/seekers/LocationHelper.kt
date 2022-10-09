package com.example.seekers

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.*

object LocationHelper {
    private val locationRequest: LocationRequest = LocationRequest.create().apply {
        interval = 15 * 1000
        isWaitForAccurateLocation = false
        priority = Priority.PRIORITY_HIGH_ACCURACY
    }

    fun checkPermissions(context: Context): Boolean {
        return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun requestLocationUpdates(
        client: FusedLocationProviderClient,
        locationCallback: LocationCallback
    ) {
        client.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    fun removeLocationUpdates(
        client: FusedLocationProviderClient,
        locationCallback: LocationCallback
    ) {
        client.removeLocationUpdates(locationCallback)
        Log.d("DEBUG", "removed location updates")
    }
}