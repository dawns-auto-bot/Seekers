package com.example.seekers

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.CustomCap
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.compose.*
import com.google.maps.android.heatmaps.HeatmapTileProvider

@OptIn(MapsComposeExperimentalApi::class)
@SuppressLint("MissingPermission")
@Composable
fun HeatMap(
    vm: HeatMapViewModel = viewModel(),
    mapControl: Boolean
) {
    val locationData: Location? by vm.locationData.observeAsState(null)
    val playAreaCenter: LatLng by vm.playAreaCenter.observeAsState(LatLng(60.224315, 24.757500))
    val playAreaRadius: Double by vm.playAreaRadius.observeAsState(300.0)
    var locationAllowed by remember { mutableStateOf(false) }
    var initialPosSet by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val positions by vm.positions.observeAsState(
        listOf(
            LatLng(60.223751, 24.759106),
            LatLng(60.223549, 24.758087),
            LatLng(60.223091, 24.759321),
            LatLng(60.223191, 24.759021),
            LatLng(60.223291, 24.759521),
            LatLng(60.223391, 24.759621),
            LatLng(60.223491, 24.759421),
            LatLng(60.223991, 24.759121),
            LatLng(60.223691, 24.759821),
        )
    )

    val cameraPositionState = rememberCameraPositionState()

    val permissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { allowed ->
            locationAllowed = allowed
        }

    LaunchedEffect(Unit) {
        if (!locationAllowed) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        } else {
            locationAllowed = true
        }
    }

    vm.startLocationUpdates()

    if (!initialPosSet) {
        LaunchedEffect(locationData) {
            locationData?.let {
                cameraPositionState.position =
                    CameraPosition.fromLatLngZoom(
                        LatLng(it.latitude, it.longitude),
                        playAreaRadius.toFloat() / 12.4f
                    )
                initialPosSet = true
            }
        }
    }

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
        derivedStateOf {
            MapProperties(
                mapType = MapType.SATELLITE,
                isMyLocationEnabled = true,
                maxZoomPreference = 18F,
//                minZoomPreference = playAreaRadius.toFloat() / 12.4f
            )
        }
    }


    val tileProvider by remember {
        derivedStateOf {
            val provider = HeatmapTileProvider.Builder()
                .data(positions)
                .build()
            provider.setRadius(200)
            provider
        }
    }


    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (locationAllowed) {
            GoogleMap(
                cameraPositionState = cameraPositionState,
                properties = properties,
                uiSettings = uiSettings,
                onMapLoaded = {
                }
            ) {
                TileOverlay(
                    tileProvider = tileProvider,
                    transparency = 0.3f
                )
//                positions.forEach {
//                    Circle(
//                        center = it,
//                        radius = 30.0,
//                        fillColor = Color(0x19FFDE00),
//                        strokeColor = Color(0x8DBDA500)
//                    )
//                }
//                var clusterManager by remember { mutableStateOf<ClusterManager<PositionMarker>?>(null) }
//                MapEffect(positions) { map ->
//                    val posList = positions.map { PositionMarker(it) }
//                    if (clusterManager == null) {
//                        clusterManager = ClusterManager<PositionMarker>(context, map)
//                    }
//                    clusterManager?.addItems(posList)
//                }
                Circle(
                    center = playAreaCenter,
                    radius = playAreaRadius,
                    fillColor = Color(0x19FFDE00),
                    strokeColor = Color(0x8DBDA500)
                )
            }
            Row(modifier = Modifier.align(Alignment.BottomCenter)) {
                Button(onClick = {
                    val newList = positions.map { LatLng(it.latitude + 0.001, it.longitude) }
                    vm.updatePositions(newList)
                }) {
                    Text(text = "Move players")
                }
            }

        } else {
            Text(text = "Location permission needed")
        }
    }
}

class PositionMarker(val latLng: LatLng): ClusterItem {

    override fun getPosition(): LatLng {
        return latLng
    }

    override fun getTitle(): String? {
        return null
    }

    override fun getSnippet(): String? {
        return null
    }

}

class HeatMapViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        val locationRequest: LocationRequest = LocationRequest.create().apply {
            interval = 5000
            isWaitForAccurateLocation = true
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }
    }

    val locationData: MutableLiveData<Location> = MutableLiveData<Location>(null)
    val playAreaCenter: MutableLiveData<LatLng> =
        MutableLiveData<LatLng>(LatLng(60.224315, 24.757500))
    val playAreaRadius: MutableLiveData<Double> = MutableLiveData(200.00)
    val positions = MutableLiveData(
        listOf(
            LatLng(60.223751, 24.759106),
            LatLng(60.223549, 24.758087),
            LatLng(60.223091, 24.759321),
            LatLng(60.223191, 24.759021),
            LatLng(60.223291, 24.759521),
            LatLng(60.223391, 24.759621),
            LatLng(60.223491, 24.759421),
            LatLng(60.223991, 24.759121),
            LatLng(60.223691, 24.759821),
        )
    )

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

    fun updatePositions(newList: List<LatLng>) {
        positions.value = newList
    }
}

@Preview
@Composable
fun HeatMapPrev() {
    HeatMap(mapControl = true)
}
