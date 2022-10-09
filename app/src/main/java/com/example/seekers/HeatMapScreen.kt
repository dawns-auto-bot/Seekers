package com.example.seekers

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import android.os.Build
import android.os.CountDownTimer
import android.os.Looper
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.seekers.general.QRCodeComponent
import com.example.seekers.general.QRScanner
import com.example.seekers.general.generateQRCode
import com.example.seekers.general.toGrayscale
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.maps.android.compose.*
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.google.maps.android.ktx.utils.withSphericalOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.*

@RequiresApi(Build.VERSION_CODES.Q)
@SuppressLint("MissingPermission")
@Composable
fun HeatMapScreen(
    vm: HeatMapViewModel = viewModel(),
    mapControl: Boolean,
    gameId: String,
    navController: NavHostController
) {
    val context = LocalContext.current
    val radius by vm.radius.observeAsState()
    var locationAllowed by remember { mutableStateOf(false) }
    var cameraIsAllowed by remember { mutableStateOf(false) }
    var initialPosSet by remember { mutableStateOf(false) }
    val lobby by vm.lobby.observeAsState()
    var timer: CountDownTimer? by remember { mutableStateOf(null) }
    val timeRemaining by vm.timeRemaining.observeAsState()
    val center by vm.center.observeAsState()
    val isSeeker by vm.isSeeker.observeAsState()
    val heatPositions by vm.heatPositions.observeAsState(listOf())
    val movingPlayers by vm.movingPlayers.observeAsState(listOf())
    val eliminatedPlayers by vm.eliminatedPlayers.observeAsState(listOf())
    val cameraPositionState = rememberCameraPositionState()
    var minZoom by remember { mutableStateOf(17F) }
    var showRadar by remember { mutableStateOf(false) }
    var showQR by remember { mutableStateOf(false) }
    var showQRScanner by remember { mutableStateOf(false) }
    var circleCoords by remember { mutableStateOf(listOf<LatLng>()) }

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { allowed ->
            locationAllowed = allowed
        }
    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { allowed ->
            cameraIsAllowed = allowed
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
                mapToolbarEnabled = false,
                myLocationButtonEnabled = false,
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
        vm.addMockPlayers(gameId)
        if (!locationAllowed) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            locationAllowed = true
        }
    }

    LaunchedEffect(isSeeker) {
        println("isSeeker $isSeeker")
        isSeeker?.let {
            vm.startService(context = context, gameId = gameId, isSeeker = it)
        }
    }

//    LaunchedEffect(locationAllowed) {
//        if (locationAllowed) {
//            vm.startLocationUpdatesForPlayer(FirestoreHelper.uid!!, gameId)
//        }
//    }

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

    LaunchedEffect(eliminatedPlayers) {
        eliminatedPlayers.find { it.playerId == FirestoreHelper.uid!! }?.let {
            vm.stopService(context)
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
                    maxZoomPreference = 17.5F,
                    minZoomPreference = minZoom,
                    latLngBoundsForCameraTarget =
                    getBounds(
                        LatLng(
                            it.center.latitude,
                            it.center.longitude
                        ), it.radius
                    )
                )
                cameraPositionState.position = CameraPosition.fromLatLngZoom(center!!, minZoom)
                initialPosSet = true
                launch(Dispatchers.Default) {
                    circleCoords = getCircleCoords(
                        LatLng(
                            it.center.latitude,
                            it.center.longitude
                        ), it.radius
                    )
                }

            }
        }
    }

    properties?.let { props ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (locationAllowed) {
                HeatMap(
                    state = cameraPositionState,
                    center = center,
                    radius = radius,
                    minZoom = minZoom,
                    properties = props,
                    uiSettings = uiSettings,
                    heatPositions = heatPositions,
                    movingPlayers = movingPlayers,
                    eliminatedPlayers = eliminatedPlayers,
                    tileProvider = tileProvider,
                    circleCoords = circleCoords
                )

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
                        vm.updateUser(mapOf(Pair("currentGameId", "")), FirestoreHelper.uid!!)
                        vm.stopService(context)
                        navController.navigate(NavRoutes.StartGame.route)
                    }) {
                        Text(text = "Leave")
                    }
                }

                Button(
                    onClick = {
                        showRadar = true
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Text(text = "Radar")
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QRButton {
                        if (!showQR) {
                            showQR = true
                        }
                    }
                    QRScanButton {
                        if (!cameraIsAllowed) {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        } else {
                            cameraIsAllowed = true
                        }
                        if (!showQRScanner) {
                            showQRScanner = true
                        }
                    }
                }

                if (showRadar) {
                    RadarDialog(gameId = gameId) { showRadar = false }
                }

                if (showQR) {
                    ShowMyQRDialog {
                        showQR = false
                    }
                }

                if (showQRScanner && cameraIsAllowed) {
                    QRScannerDialog(onDismiss = { showQRScanner = false }) {
                        vm.setPlayerFound(gameId, it)
                        showQRScanner = false
                    }
                }

            } else {
                Text(text = "Location permission needed")
            }
        }
    }

}

@Composable
fun HeatMap(
    state: CameraPositionState,
    center: LatLng?,
    radius: Int?,
    minZoom: Float,
    properties: MapProperties,
    uiSettings: MapUiSettings,
    heatPositions: List<LatLng>,
    movingPlayers: List<Player>,
    tileProvider: HeatmapTileProvider,
    circleCoords: List<LatLng>,
    eliminatedPlayers: List<Player>
) {
    val context = LocalContext.current
    GoogleMap(
        cameraPositionState = state,
        properties = properties,
        uiSettings = uiSettings,
    ) {
        if (heatPositions.isNotEmpty()) {
            TileOverlay(
                tileProvider = tileProvider,
                transparency = 0.3f
            )
        }
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
                visible = true,
                anchor = Offset(0.5f, 0.5f)
            )
        }
        eliminatedPlayers.forEach {
            val res = avatarList[it.avatarId]
            val bitmap = BitmapFactory.decodeResource(context.resources, res)
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 50, 50, false)
            val grayedBitmap = resizedBitmap.toGrayscale()
            Marker(
                state = MarkerState(
                    position = LatLng(
                        it.location.latitude,
                        it.location.longitude
                    )
                ),
                icon = BitmapDescriptorFactory.fromBitmap(grayedBitmap),
                title = it.nickname + " (eliminated)",
                visible = true,
                anchor = Offset(0.5f, 0.5f)
            )
        }
        if (center != null && radius != null) {
            if (circleCoords.isNotEmpty()) {
                Polygon(
                    points = getCornerCoords(center, radius),
                    fillColor = Color(0x8D000000),
                    holes = listOf(circleCoords),
                    strokeWidth = 0f,
                )
            }

            Circle(
                center = center,
                radius = radius.toDouble(),
                strokeColor = Color(0x8DBDA500),
            )
        }

    }
}

@Composable
fun RadarDialog(
    gameId: String,
    onDismiss: () -> Unit
) {

    val height = LocalConfiguration.current.screenHeightDp * 0.8

    Dialog(onDismissRequest = { onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.height(height.dp)
        ) {
            RadarScreen(gameId = gameId)
        }

    }
}

@Composable
fun QRButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(shape = CircleShape, elevation = 4.dp, modifier = modifier.clickable { onClick() }) {
        Icon(
            imageVector = Icons.Filled.QrCode,
            contentDescription = "qr",
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Composable
fun ShowMyQRDialog(onDismiss: () -> Unit) {
    val playerId = FirestoreHelper.uid!!
//    val playerId = "player 1"
    val qrBitmap = generateQRCode(playerId)
    Dialog(onDismissRequest = onDismiss) {
        Card(backgroundColor = Color.White, shape = RoundedCornerShape(8.dp)) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Oh no, you got caught!")
                Spacer(modifier = Modifier.height(16.dp))
                QRCodeComponent(bitmap = qrBitmap)
            }
        }
    }
}

@Composable
fun QRScanButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(shape = CircleShape, elevation = 4.dp, modifier = modifier.clickable { onClick() }) {
        Icon(
            imageVector = Icons.Filled.QrCodeScanner,
            contentDescription = "qrScan",
            modifier = Modifier.padding(8.dp)
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun QRScannerDialog(onDismiss: () -> Unit, onScanned: (String) -> Unit) {
    val context = LocalContext.current
    Dialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss
    ) {
        Box(Modifier.fillMaxSize()) {
            QRScanner(context = context, onScanned = onScanned)
            Button(onClick = onDismiss, modifier = Modifier.align(Alignment.BottomCenter)) {
                Text(text = "Cancel")
            }
        }
    }
}

@Composable
fun GameTimer(modifier: Modifier = Modifier, vm: HeatMapViewModel) {
    val countdown by vm.countdown.observeAsState()
    countdown?.let {
        Card(
            modifier = modifier,
            backgroundColor = Color.LightGray,
            shape = RoundedCornerShape(25.dp)
        ) {
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

fun getCornerCoords(center: LatLng, radius: Int): List<LatLng> {
    val ne = center.withSphericalOffset(radius * 10.0, 45.0)
    val se = center.withSphericalOffset(radius * 10.0, 135.0)
    val sw = center.withSphericalOffset(radius * 10.0, 225.0)
    val nw = center.withSphericalOffset(radius * 10.0, 315.0)
    return listOf(ne, se, sw, nw)
}

fun getCircleCoords(center: LatLng, radius: Int): List<LatLng> {
    val list = mutableListOf<LatLng>()
    (0..360).forEach {
        list.add(center.withSphericalOffset(radius.toDouble() + 1.0, it.toDouble()))
    }
    return list
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
            interval = 10 * 1000
            isWaitForAccurateLocation = false
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
    val isSeeker = MutableLiveData<Boolean>()
    val playersWithoutSelf = Transformations.map(players) { players ->
        players.filter { it.playerId != FirestoreHelper.uid!! }
    }
    val heatPositions = Transformations.map(playersWithoutSelf) { players ->
        players.filter { it.inGameStatus == InGameStatus.PLAYER.value }
            .map { LatLng(it.location.latitude, it.location.longitude) }
    }
    val movingPlayers = Transformations.map(playersWithoutSelf) { players ->
        players.filter { it.inGameStatus == InGameStatus.MOVING.value }
    }
    val eliminatedPlayers = Transformations.map(playersWithoutSelf) { players ->
        players.filter { it.inGameStatus == InGameStatus.ELIMINATED.value }
    }
    val statuses = Transformations.map(players) { players ->
        players.map { it.inGameStatus }
    }
    val countdown = MutableLiveData<Int>()

    fun addMockPlayers(gameId: String) {
        val mockPlayers = listOf(
            Player(
                nickname = "player 1",
                avatarId = 1,
                inGameStatus = InGameStatus.PLAYER.value,
                location = GeoPoint(60.22338389989929, 24.756749169655805),
                playerId = "player 1",
                distanceStatus = PlayerDistance.WITHIN50.value
            ),
            Player(
                nickname = "player 2",
                avatarId = 5,
                inGameStatus = InGameStatus.MOVING.value,
                location = GeoPoint(60.22374887627318, 24.759200708558442),
                playerId = "player 2",
                distanceStatus = PlayerDistance.WITHIN100.value
            ),
            Player(
                nickname = "player 3",
                avatarId = 1,
                inGameStatus = InGameStatus.PLAYER.value,
                location = GeoPoint(60.223032239987354, 24.758830563735074),
                playerId = "player 3",
                distanceStatus = PlayerDistance.WITHIN10.value
            ),
            Player(
                nickname = "player 4",
                avatarId = 1,
                inGameStatus = InGameStatus.MOVING.value,
                location = GeoPoint(60.224550744400226, 24.756561415035257),
                playerId = "player 4",
                distanceStatus = PlayerDistance.WITHIN50.value
            ),
            Player(
                nickname = "player 5",
                avatarId = 1,
                inGameStatus = InGameStatus.ELIMINATED.value,
                location = GeoPoint(60.223405212500005, 24.75958158221728),
                playerId = "player 5",
                distanceStatus = PlayerDistance.WITHIN50.value
            ),
            Player(
                nickname = "player 6",
                avatarId = 1,
                inGameStatus = InGameStatus.PLAYER.value,
                location = GeoPoint(60.223841983003645, 24.759626485065098),
                playerId = "player 6",
                distanceStatus = PlayerDistance.WITHIN50.value
            ),
            Player(
                nickname = "player 7",
                avatarId = 5,
                inGameStatus = InGameStatus.MOVING.value,
                location = GeoPoint(60.22357557804847, 24.756681419911455),
                playerId = "player 7",
                distanceStatus = PlayerDistance.WITHIN100.value
            ),
            Player(
                nickname = "player 8",
                avatarId = 1,
                inGameStatus = InGameStatus.PLAYER.value,
                location = GeoPoint(60.22314399742664, 24.757781125478843),
                playerId = "player 8",
                distanceStatus = PlayerDistance.WITHIN10.value
            ),
            Player(
                nickname = "player 9",
                avatarId = 1,
                inGameStatus = InGameStatus.MOVING.value,
                location = GeoPoint(60.22311735646131, 24.759814239674167),
                playerId = "player 9",
                distanceStatus = PlayerDistance.WITHIN50.value
            ),
            Player(
                nickname = "player 10",
                avatarId = 1,
                inGameStatus = InGameStatus.ELIMINATED.value,
                location = GeoPoint(60.223405212500005, 24.75958158221728),
                playerId = "player 10",
                distanceStatus = PlayerDistance.WITHIN50.value
            ),
        )
        mockPlayers.forEach {
            firestore.addPlayer(it, gameId)
        }
    }

    fun getPlayers(gameId: String) {
        firestore.getPlayers(gameId = gameId)
            .addSnapshotListener { data, e ->
                data ?: run {
                    Log.e(TAG, "getPlayers: ", e)
                    return@addSnapshotListener
                }
                val playersFetched = data.toObjects(Player::class.java)
                players.postValue(playersFetched)
                if (isSeeker.value == null) {
                    val myStatus = playersFetched.find { it.playerId == firestore.uid }?.inGameStatus
                    myStatus?.let {
                        isSeeker.value = (it == InGameStatus.SEEKER.value)
                    }
                }

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

    fun removePlayerFromLobby(gameId: String, playerId: String) {
        firestore.removePlayer(gameId, playerId)
    }

    fun setPlayerFound(gameId: String, playerId: String) {
        firestore.updatePlayerInGameStatus(
            inGameStatus = InGameStatus.ELIMINATED.value,
            gameId = gameId,
            playerId = playerId
        )
//        sendFoundNotification()
    }

//    private var fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
//    private lateinit var locationCallback2: LocationCallback

//    @SuppressLint("MissingPermission")
//    fun startLocationUpdatesForPlayer(playerId: String, gameId: String) {
//        locationCallback2 = object : LocationCallback() {
//            override fun onLocationResult(locationResult: LocationResult) {
//                for (location in locationResult.locations) {
//                    val changeMap = mapOf(
//                        Pair("location", GeoPoint(location.latitude, location.longitude))
//                    )
//                    firestore.updatePlayerLocation(changeMap, playerId, gameId)
//                }
//            }
//        }
//
//        fusedLocationClient.requestLocationUpdates(
//            locationRequest,
//            locationCallback2,
//            Looper.getMainLooper()
//        )
//        Log.d("DEBUG", "started location updates 2")
//    }

//    fun removeLocationUpdates() {
//        fusedLocationClient.removeLocationUpdates(locationCallback2)
//        Log.d("DEBUG", "removed location updates")
//    }

    fun startService(context: Context, gameId: String, isSeeker: Boolean) {
        LocationService.start(
            context = context,
            gameId = gameId,
            isSeeker = isSeeker
        )
    }

    fun stopService(context: Context) {
        LocationService.stop(
            context = context,
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

