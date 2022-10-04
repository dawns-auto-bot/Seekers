package com.example.seekers

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.os.CountDownTimer
import android.os.Looper
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.firebase.Timestamp
import com.google.maps.android.compose.*
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.google.maps.android.ktx.utils.withSphericalOffset
import kotlin.math.*

@SuppressLint("MissingPermission")
@Composable
fun HeatMap(
    vm: HeatMapViewModel = viewModel(),
    mapControl: Boolean,
    gameId: String,
    navController: NavHostController
) {
    val context = LocalContext.current
    val locationData: Location? by vm.locationData.observeAsState(null)
    var mapSize by remember { mutableStateOf(IntSize.Zero) }
    val radius by vm.radius.observeAsState()
    var locationAllowed by remember { mutableStateOf(false) }
    var initialPosSet by remember { mutableStateOf(false) }
    val lobby by vm.lobby.observeAsState()
    var timer: CountDownTimer? by remember { mutableStateOf(null) }
    val timeRemaining by vm.timeRemaining.observeAsState()
    val center by vm.center.observeAsState()
    val heatPositions by vm.heatPositions.observeAsState(listOf())
    val movingPlayers by vm.movingPlayers.observeAsState(listOf())
    val cameraPositionState = rememberCameraPositionState()
    var minZoom by remember { mutableStateOf(17F) }
    val permissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { allowed ->
            locationAllowed = allowed
        }
    val tileProvider by remember {
        derivedStateOf {
            val provider = HeatmapTileProvider.Builder()
                .data(heatPositions)
                .build()
            provider.setRadius(200)
            provider
        }
    }

    var properties: MapProperties? by remember { mutableStateOf(null) }
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

    LaunchedEffect(Unit) {
        vm.getPlayers(gameId)
        vm.getLobby(gameId)
        vm.getTime(gameId)
        if (!locationAllowed) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        } else {
            locationAllowed = true
        }
    }

    LaunchedEffect(locationAllowed) {
        if (locationAllowed) {
            vm.startLocationUpdates()
        }
    }

    LaunchedEffect(timeRemaining) {
        timeRemaining?.let {
            vm.updateCountdown(it)
            timer = object : CountDownTimer(it * 1000L, 1000) {
                override fun onTick(p0: Long) {
                    if (p0 == 0L) {
                        vm.updateCountdown(0)
                        return
                    }
                    vm.updateCountdown(p0.div(1000).toInt())
                }
                override fun onFinish() {
                    Toast.makeText(context, "The game is over", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (!initialPosSet) {
        val density = LocalDensity.current
        val width =
            with(density) {
                LocalConfiguration.current.screenWidthDp.dp.toPx()
            }
                .times(0.7).toInt()
        LaunchedEffect(center) {
            lobby?.let {
                minZoom = getBoundsZoomLevel(
                    getBounds(
                        LatLng(
                            it.center.latitude,
                            it.center.longitude
                        ), it.radius
                    ),
                    Size(width, width)
                ).toFloat()
                properties = MapProperties(
                    mapType = MapType.SATELLITE,
                    isMyLocationEnabled = true,
                    maxZoomPreference = 18F,
                    minZoomPreference = minZoom,
                    latLngBoundsForCameraTarget =
                    getBounds(
                        LatLng(
                            it.center.latitude,
                            it.center.longitude
                        ), it.radius
                    )
                )
                initialPosSet = true
            }
        }
    }

    properties?.let {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (locationAllowed) {
                GoogleMap(
                    cameraPositionState = cameraPositionState.apply {
                        position = CameraPosition.fromLatLngZoom(center!!, minZoom)
                    },
                    properties = it,
                    uiSettings = uiSettings,
                ) {
                    TileOverlay(
                        tileProvider = tileProvider,
                        transparency = 0.3f
                    )
                    movingPlayers.forEach {
                        val res = avatarList[it.avatarId]
                        val bitmap = BitmapFactory.decodeResource(context.resources, res)
                        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 50, 50, false)
                        Marker(
                            state = MarkerState(
                                position = LatLng(
                                    it.location.latitude,
                                    it.location.longitude
                                )
                            ),
                            icon = BitmapDescriptorFactory.fromBitmap(resizedBitmap),
                            title = it.nickname,
                            visible = true
                        )
                    }
                    if (center != null && radius != null) {
                        Circle(
                            center = center!!,
                            radius = radius!!.toDouble(),
                            fillColor = Color(0x19FFDE00),
                            strokeColor = Color(0x8DBDA500)
                        )
                    }

                }
                timer?.let {
                    GameTimer(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        vm = vm
                    )
                    LaunchedEffect(Unit) {
                        it.start()
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(8.dp)
                ) {
                    Button(onClick = {
                        vm.updateUser(mapOf(Pair("currentGameId", "")), playerId)
                        navController.navigate(NavRoutes.StartGame.route)
                    }) {
                        Text(text = "Leave")
                    }
                }
            } else {
                Text(text = "Location permission needed")
            }
        }
    }

}

@Composable
fun GameTimer(modifier: Modifier = Modifier, vm: HeatMapViewModel) {
    val countdown by vm.countdown.observeAsState()
    countdown?.let {
        Card(modifier = modifier, backgroundColor = Color.LightGray, shape = RoundedCornerShape(25.dp)) {
            Row(Modifier.padding(8.dp)) {
                Text(text = secondsToText(it))
            }
        }

    }
}

fun getBounds(center: LatLng, radius: Int): LatLngBounds {
    val multiplier = cos(PI / 4)
    val sw = center.withSphericalOffset(radius.div(multiplier), 225.0)
    val ne = center.withSphericalOffset(radius.div(multiplier), 45.0)
    return LatLngBounds(sw, ne)
}

fun secondsToText(seconds: Int): String {
    if (seconds == 0) {
        return "Times up!"
    }
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds - hours * 3600 - minutes * 60

    if (seconds < 3600) {
        return "${minutes.toTimeString()}:${secs.toTimeString()}"
    }

    return "${hours.toTimeString()}:${minutes.toTimeString()}:${secs.toTimeString()}"
}

fun Int.toTimeString() = if (this < 10) "0$this" else this.toString()

class HeatMapViewModel(application: Application) : AndroidViewModel(application) {
    val TAG = "heatMapVM"

    companion object {
        val locationRequest: LocationRequest = LocationRequest.create().apply {
            interval = 5000
            isWaitForAccurateLocation = true
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }
    }

    val firestore = FirestoreHelper
    val lobby = MutableLiveData<Lobby>()
    val radius = Transformations.map(lobby) {
        it.radius
    }
    val lobbyStatus = Transformations.map(lobby) {
        it.status
    }
    val center = Transformations.map(lobby) {
        LatLng(it.center.latitude, it.center.longitude)
    }
    val timeRemaining = MutableLiveData<Int>()
    val players = MutableLiveData<List<Player>>()
    val playersWithoutSelf = Transformations.map(players) { players ->
        players.filter { it.playerId != playerId }
    }
    val heatPositions = Transformations.map(playersWithoutSelf) { players ->
        players.filter { it.inGameStatus == InGameStatus.PLAYER.value }
            .map { LatLng(it.location.latitude, it.location.longitude) }
    }
    val movingPlayers = Transformations.map(playersWithoutSelf) { players ->
        players.filter { it.inGameStatus == InGameStatus.MOVING.value }
    }

    val statuses = Transformations.map(players) { players ->
        players.map { it.inGameStatus }
    }
    val countdown = MutableLiveData<Int>()
    val locationData: MutableLiveData<Location> = MutableLiveData<Location>(null)
    val playAreaCenter: MutableLiveData<LatLng> =
        MutableLiveData<LatLng>(LatLng(60.224315, 24.757500))
//    val positions = MutableLiveData(
//        listOf(
//            LatLng(60.223751, 24.759106),
//            LatLng(60.223549, 24.758087),
//            LatLng(60.223091, 24.759321),
//            LatLng(60.223191, 24.759021),
//            LatLng(60.223291, 24.759521),
//            LatLng(60.223391, 24.759621),
//            LatLng(60.223491, 24.759421),
//            LatLng(60.223991, 24.759121),
//            LatLng(60.223691, 24.759821),
//        )
//    )

    fun getPlayers(gameId: String) {
        firestore.getPlayers(gameId = gameId)
            .addSnapshotListener { data, e ->
                data ?: run {
                    Log.e(TAG, "getPlayers: ", e)
                    return@addSnapshotListener
                }
                val playersFetched = data.toObjects(Player::class.java)
                players.postValue(playersFetched)
            }
    }

    fun getTime(gameId: String) {
        firestore.getLobby(gameId = gameId).get()
            .addOnSuccessListener {
                val lobby = it.toObject(Lobby::class.java)
                lobby?.let {
                    val startTime = lobby.startTime.toDate().time / 1000
                    val countdown = lobby.countdown
                    val timeLimit = lobby.timeLimit * 60
                    val gameEndTime = (startTime + countdown + timeLimit)
                    val now = Timestamp.now().toDate().time.div(1000)
                    timeRemaining.postValue(gameEndTime.minus(now).toInt())
                }
            }
    }

    fun updateCountdown(newVal: Int) {
        countdown.value = newVal
    }

    fun getLobby(gameId: String) {
        firestore.getLobby(gameId).addSnapshotListener { data, e ->
            data ?: run {
                Log.e(TAG, "getLobby: ", e)
                return@addSnapshotListener
            }
            val lobbyFetched = data.toObject(Lobby::class.java)
            lobby.postValue(lobbyFetched)
        }
    }

    fun updateUser(changeMap: Map<String, Any>, uid: String) =
        firestore.updateUser(changeMap = changeMap, userId = uid)

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
}

//source: https://stackoverflow.com/questions/6048975/google-maps-v3-how-to-calculate-the-zoom-level-for-a-given-bounds
fun getBoundsZoomLevel(bounds: LatLngBounds, mapDim: Size): Double {
    val WORLD_DIM = Size(256, 256)
    val ZOOM_MAX = 21.toDouble();

    fun latRad(lat: Double): Double {
        val sin = sin(lat * Math.PI / 180);
        val radX2 = ln((1 + sin) / (1 - sin)) / 2;
        return max(min(radX2, Math.PI), -Math.PI) / 2
    }

    fun zoom(mapPx: Int, worldPx: Int, fraction: Double): Double {
        return floor(ln(mapPx / worldPx / fraction) / ln(2.0))
    }

    val ne = bounds.northeast;
    val sw = bounds.southwest;

    val latFraction = (latRad(ne.latitude) - latRad(sw.latitude)) / Math.PI;

    val lngDiff = ne.longitude - sw.longitude;
    val lngFraction = if (lngDiff < 0) {
        (lngDiff + 360) / 360
    } else {
        (lngDiff / 360)
    }

    val latZoom = zoom(mapDim.height, WORLD_DIM.height, latFraction);
    val lngZoom = zoom(mapDim.width, WORLD_DIM.width, lngFraction);

    return minOf(latZoom, lngZoom, ZOOM_MAX)
}

