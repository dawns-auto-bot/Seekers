package com.example.seekers

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.seekers.general.CustomButton
import com.example.seekers.ui.theme.*
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import java.util.*

@SuppressLint("MissingPermission")
@Composable
fun Map(
    vm: MapViewModel = viewModel(),
    lobbyvm: LobbyCreationScreenViewModel = viewModel(),
    mapControl: Boolean
) {
    val locationData: Location? by vm.locationData.observeAsState(null)
    val playAreaCenter: LatLng? by vm.playAreaCenter.observeAsState(null)
    val playAreaRadius: Double? by vm.playAreaRadius.observeAsState(0.0)
    var sliderPosition by remember { mutableStateOf(100f) }
    var mapActive by remember { mutableStateOf(true) }

    vm.startLocationUpdates()

    val uiSettings by remember { mutableStateOf(MapUiSettings(
        compassEnabled = true,
        indoorLevelPickerEnabled = true,
        mapToolbarEnabled = true,
        myLocationButtonEnabled = mapControl,
        rotationGesturesEnabled = mapControl,
        scrollGesturesEnabled = mapControl,
        scrollGesturesEnabledDuringRotateOrZoom = mapControl,
        tiltGesturesEnabled = mapControl,
        zoomControlsEnabled = mapControl,
        zoomGesturesEnabled = mapControl ))
    }

    val properties by remember { mutableStateOf(MapProperties(
        mapType = MapType.NORMAL,
        isMyLocationEnabled = true))
    }

    val latLng = locationData?.let { LatLng(it.latitude, it.longitude) }
    val cameraPositionState = rememberCameraPositionState {
        if(latLng != null) {
            position = CameraPosition.fromLatLngZoom(latLng, 17F)
        }
    }
    if(mapActive) {
        Box(Modifier.fillMaxWidth().height(200.dp)) {
            GoogleMap(
                cameraPositionState = cameraPositionState,
                properties = properties,
                uiSettings = uiSettings
            ) { Circle(
                    center = LatLng(cameraPositionState.position.target.latitude, cameraPositionState.position.target.longitude),
                    radius = sliderPosition.toDouble(),
                    fillColor = Color(0x19FFDE00),
                    strokeColor = Color(0x8DBDA500)
                )
            }
        }
        Slider(value = sliderPosition, onValueChange = { sliderPosition = it;  }, valueRange = 50f..200f)
        CustomButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Set play area"
        ) {

            vm.updateCenter(LatLng(cameraPositionState.position.target.latitude, cameraPositionState.position.target.longitude))
            // vm.updateRadius(sliderPosition.toDouble())
            lobbyvm.updateRadius(sliderPosition.toInt())
            Log.d("DEBUG", "center: ${cameraPositionState.position.target.latitude}, ${cameraPositionState.position.target.longitude} - radius: ${sliderPosition.toDouble()}")
            mapActive = false
        }
    }
}


class MapViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        val locationRequest: LocationRequest = LocationRequest.create().apply {
            interval = 5000
            isWaitForAccurateLocation = true
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }
    }

    val locationData: MutableLiveData<Location> = MutableLiveData<Location>(null)
    val playAreaCenter: MutableLiveData<LatLng> = MutableLiveData<LatLng>(null)
    val playAreaRadius: MutableLiveData<Double> = MutableLiveData(0.0)
    private var fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            for (location in locationResult.locations) {
                locationData.postValue(location)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.also {
                    locationData.postValue(location)
                }
            }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    fun removeLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    fun updateRadius(value: Double) {
        playAreaRadius.postValue(value)
    }

    fun updateCenter(location: LatLng) {
        playAreaCenter.postValue(location)
    }
}
