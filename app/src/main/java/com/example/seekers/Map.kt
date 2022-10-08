package com.example.seekers

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.seekers.general.CustomButton
import com.example.seekers.general.VerticalSlider
import com.example.seekers.ui.theme.*
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint
import com.google.maps.android.compose.*
import java.util.*

@SuppressLint("MissingPermission")
@Composable
fun Map(
    vm: MapViewModel = viewModel(),
    lobbyvm: LobbyCreationScreenViewModel = viewModel(),
    mapControl: Boolean
) {
    val context = LocalContext.current
    val locationData: Location? by vm.locationData.observeAsState(null)
    val playAreaCenter: LatLng? by vm.playAreaCenter.observeAsState(null)
    val playAreaRadius: Double? by vm.playAreaRadius.observeAsState(0.0)
    var sliderPosition by remember { mutableStateOf(100f) }
    var showMap by remember { mutableStateOf(true) }
    var isCenteredToPosition by remember { mutableStateOf(false) }
    val cameraPositionState = rememberCameraPositionState()
    vm.startLocationUpdates()

    val uiSettings by remember {
        mutableStateOf(
            MapUiSettings(
                compassEnabled = true,
                indoorLevelPickerEnabled = true,
                mapToolbarEnabled = true,
                myLocationButtonEnabled = mapControl,
                rotationGesturesEnabled = mapControl,
                scrollGesturesEnabled = mapControl,
                scrollGesturesEnabledDuringRotateOrZoom = mapControl,
                tiltGesturesEnabled = mapControl,
                zoomControlsEnabled = mapControl,
                zoomGesturesEnabled = mapControl
            )
        )
    }

    val properties by remember {
        mutableStateOf(
            MapProperties(
                mapType = MapType.SATELLITE,
                isMyLocationEnabled = true
            )
        )
    }

    if (!isCenteredToPosition) {
        LaunchedEffect(locationData) {
            locationData?.let {
                val latLng = it.let { LatLng(it.latitude, it.longitude) }
                cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 17F)
                isCenteredToPosition = true
            }
        }
    }

    if (showMap) {
        Box(
            Modifier.fillMaxSize()
        ) {
            GoogleMap(
                cameraPositionState = cameraPositionState,
                properties = properties,
                uiSettings = uiSettings
            ) {
                Circle(
                    center = LatLng(
                        cameraPositionState.position.target.latitude,
                        cameraPositionState.position.target.longitude
                    ),
                    radius = sliderPosition.toDouble(),
                    fillColor = Color(0x19FFDE00),
                    strokeColor = Color(0x8DBDA500),
                    clickable = true
                ) {
                    Toast.makeText(
                        context,
                        "Play area radius is ${sliderPosition.toDouble()} meters",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            Column(Modifier.align(Alignment.BottomCenter)) {
                CustomButton(
                    modifier = Modifier.width(150.dp),
                    text = "Define Area"
                ) {
                    vm.updateCenter(
                        LatLng(
                            cameraPositionState.position.target.latitude,
                            cameraPositionState.position.target.longitude
                        )
                    )
                    // vm.updateRadius(sliderPosition.toDouble())
                    lobbyvm.updateRadius(sliderPosition.toInt())
                    Toast.makeText(
                        context,
                        "Play area defined, time to set the rules!",
                        Toast.LENGTH_LONG
                    ).show()

                    Log.d(
                        "DEBUG",
                        "center: ${cameraPositionState.position.target.latitude}, ${cameraPositionState.position.target.longitude} - radius: ${sliderPosition.toDouble()}"
                    )
                }
                Spacer(Modifier.height(15.dp))
            }
            Row(Modifier.align(Alignment.CenterEnd)) {
                VerticalSlider(
                    value = sliderPosition,
                    onValueChange = {
                        sliderPosition = it
                    },
                    modifier = Modifier
                        .width(200.dp)
                        .height(50.dp)
                )
            }

        }
    }
}


class MapViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        val locationRequest: LocationRequest = LocationRequest.create().apply {
            interval = 30000
            isWaitForAccurateLocation = true
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }
    }

    val firestore = FirestoreHelper

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
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        Log.d("DEBUG", "started location updates")
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdatesForPlayer(playerId: String, gameId: String) {
        val locationCallback2 = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    val changeMap = mapOf(
                        Pair("location", GeoPoint(location.latitude, location.longitude))
                    )
                    firestore.updatePlayerLocation(changeMap, playerId, gameId)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback2,
            Looper.getMainLooper()
        )

        Log.d("DEBUG", "started location updates 2")
    }

    fun removeLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.d("DEBUG", "removed location updates")
    }

    fun updateRadius(value: Double) {
        playAreaRadius.postValue(value)
    }

    fun updateCenter(location: LatLng) {
        playAreaCenter.postValue(location)
    }

}
