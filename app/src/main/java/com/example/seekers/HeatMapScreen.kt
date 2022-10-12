package com.example.seekers

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.CountDownTimer
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.seekers.general.*
import com.example.seekers.general.QRCodeComponent
import com.example.seekers.ui.theme.Powder
import com.example.seekers.ui.theme.Raisin
import com.example.seekers.ui.theme.Emerald
import com.example.seekers.ui.theme.Mango
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.maps.android.compose.*
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.google.maps.android.ktx.utils.withSphericalOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.*

@OptIn(ExperimentalMaterialApi::class)
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
    val lobbyStatus by vm.lobbyStatus.observeAsState()
    var timer: CountDownTimer? by remember { mutableStateOf(null) }
    val timeRemaining by vm.timeRemaining.observeAsState()
    val center by vm.center.observeAsState()
    val isSeeker by vm.isSeeker.observeAsState()
    val playerStatus by vm.playerStatus.observeAsState()
    val players by vm.players.observeAsState()
    val heatPositions by vm.heatPositions.observeAsState(listOf())
    val movingPlayers by vm.movingPlayers.observeAsState(listOf())
    val eliminatedPlayers by vm.eliminatedPlayers.observeAsState(listOf())
    val news by vm.news.observeAsState()
    val hasNewNews by vm.hasNewNews.observeAsState(false)
    val cameraPositionState = rememberCameraPositionState()
    var minZoom by remember { mutableStateOf(17F) }
    var showRadar by remember { mutableStateOf(false) }
    var showLeaveGameDialog by remember { mutableStateOf(false) }
    var showQR by remember { mutableStateOf(false) }
    var showQRScanner by remember { mutableStateOf(false) }
    var showPlayerFound by remember { mutableStateOf(false) }
    var showPlayerList by remember { mutableStateOf(false) }
    var playerFound: Player? by remember { mutableStateOf(null) }
    var selfie: Bitmap? by remember { mutableStateOf(null) }
    var showSendSelfie by remember { mutableStateOf(false) }
    var showNews by remember { mutableStateOf(false) }
    var circleCoords by remember { mutableStateOf(listOf<LatLng>()) }

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { allowed ->
            locationAllowed = allowed
        }
    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { allowed ->
            cameraIsAllowed = allowed
        }
    val selfieLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicturePreview()) {
            it?.let {
                selfie = it
                showSendSelfie = true
            }
        }

    val tileProvider by remember {
        derivedStateOf {
            var provider: HeatmapTileProvider? = null
            if (heatPositions.isNotEmpty()) {
                provider = HeatmapTileProvider.Builder()
                    .data(heatPositions)
                    .build()
                provider.setRadius(200)
            }
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
                zoomControlsEnabled = false,
                zoomGesturesEnabled = mapControl
            )
        )
    }
    LaunchedEffect(Unit) {
        launch(Dispatchers.IO) {
            vm.getPlayers(gameId)
            vm.getLobby(gameId)
            vm.getTime(gameId)
            vm.getNews(gameId)
        }

//        vm.addMockPlayers(gameId)
        if (!locationAllowed) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            locationAllowed = true
        }
    }

    LaunchedEffect(lobbyStatus) {
        lobbyStatus?.let {
            when (it) {
                LobbyStatus.ACTIVE.value ->{
                    
                }

                LobbyStatus.FINISHED.value -> {
                    Toast.makeText(context, "The game has ended", Toast.LENGTH_LONG).show()
                    /*
                    * Steps
                    * Time survived
                    * Time as seeker
                    * Number of players found by me
                    * */
                }
            }
        }
    }

    LaunchedEffect(playerStatus) {
        println("playerStatus $playerStatus")
        playerStatus?.let {
            if (isSeeker == null) {
                val thisPlayerIsSeeker = (it == InGameStatus.SEEKER.value)
                vm.updateIsSeeker(thisPlayerIsSeeker)
                if (it != InGameStatus.ELIMINATED.value) {
                    vm.startService(
                        context = context,
                        gameId = gameId,
                        isSeeker = thisPlayerIsSeeker
                    )
                }
                return@LaunchedEffect
            }
            if (it == InGameStatus.ELIMINATED.value) {
                showQR = false
                vm.stopService(context)
                vm.setPlayerInGameStatus(InGameStatus.SEEKER.value, gameId, FirebaseHelper.uid!!)
                vm.updateIsSeeker(true)
                vm.startService(context = context, gameId = gameId, isSeeker = true)
                Toast.makeText(context, "You are now a seeker!", Toast.LENGTH_SHORT).show()
            }
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

                override fun onFinish() {}
            }
        }
    }

//    LaunchedEffect(eliminatedPlayers) {
//        eliminatedPlayers.find { it.playerId == FirebaseHelper.uid!! }?.let {
//            vm.stopService(context)
//        }
//    }

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

    val scope = rememberCoroutineScope()
    val drawerState = rememberBottomDrawerState(BottomDrawerValue.Closed)

    BottomDrawer(
        gesturesEnabled = false,
        drawerState = drawerState,
        drawerContent = {
            Surface(
                shape = RoundedCornerShape(28.dp, 28.dp, 0.dp, 0.dp),
                color = Emerald,
                border = BorderStroke(1.dp, Raisin),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = Emerald)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { scope.launch { drawerState.close() } },
                        content = {
                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Raisin
                                )
                            }
                        })
                    IconButton(
                        onClick = {
                            scope.launch { drawerState.close() }
                            showRadar = true
                        },
                        content = {
                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Radar,
                                    contentDescription = "Radar",
                                    tint = Raisin
                                )
                            }
                        })
                    IconButton(
                        onClick = {
                            scope.launch { drawerState.close() }
                            if (isSeeker == true) {
                                if (!cameraIsAllowed) {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                } else {
                                    cameraIsAllowed = true
                                }
                                if (!showQRScanner) {
                                    showQRScanner = true
                                }
                            } else {
                                if (!showQR) {
                                    showQR = true
                                }
                            }
                        },
                        modifier = Modifier.size(56.dp),
                        content = {
                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (isSeeker == true)
                                    Icon(
                                        Icons.Default.QrCodeScanner,
                                        contentDescription = "",
                                        tint = Raisin,
                                        modifier = Modifier.size(44.dp)
                                    )
                                else
                                    Icon(
                                        Icons.Default.QrCode,
                                        contentDescription = "",
                                        tint = Raisin,
                                        modifier = Modifier.size(44.dp)
                                    )
                            }
                        })
                    IconButton(
                        onClick = {
                            if (!showPlayerList) {
                                showPlayerList = true
                            }
                        },
                        content = {
                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.List, contentDescription = "", tint = Raisin)
                                // Text(text = "Players", color = Color.White)
                            }
                        })
                    IconButton(
                        onClick = {
                            vm.updateUser(mapOf(Pair("currentGameId", "")), FirebaseHelper.uid!!)
                            vm.stopService(context)
                            navController.navigate(NavRoutes.StartGame.route)
                        },
                        content = {
                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.ExitToApp,
                                    contentDescription = "",
                                    tint = Color.Red
                                )

                            }
                        })
                }
            }

        },
        drawerBackgroundColor = Color.Transparent,
        drawerElevation = 0.dp,
        content = {
            Scaffold(
                floatingActionButtonPosition = FabPosition.Center,
                isFloatingActionButtonDocked = true,
                floatingActionButton = {

                    FloatingActionButton(
                        elevation = FloatingActionButtonDefaults.elevation(8.dp),
                        modifier = Modifier.border(BorderStroke(1.dp, Raisin), shape = CircleShape),
                        shape = CircleShape,
                        backgroundColor = Emerald,
                        contentColor = Raisin,
                        onClick = {
                            scope.launch { drawerState.open() }
                        }
                    ) {
                        Icon(Icons.Filled.Dashboard, "", modifier = Modifier.size(38.dp))
                    }
                },
            ) {
                properties?.let { props ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (locationAllowed) {
                            HeatMap(
                                state = cameraPositionState,
                                center = center,
                                radius = radius,
                                properties = props,
                                uiSettings = uiSettings,
                                heatPositions = heatPositions,
                                movingPlayers = movingPlayers,
                                eliminatedPlayers = eliminatedPlayers,
                                tileProvider = tileProvider,
                                circleCoords = circleCoords
                            )
                            Card(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(12.dp)
                                    .height(44.dp)
                                    .fillMaxWidth(),
                                backgroundColor = Emerald,
                                border = BorderStroke(1.dp, Raisin),
                                shape = RoundedCornerShape(5.dp)
                            ) {
                                Row(
                                    Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row() {
                                        Icon(
                                            Icons.Default.PermIdentity,
                                            contentDescription = "",
                                            tint = Raisin
                                        )
                                        Text(text = "3/8", color = Raisin, fontSize = 20.sp)
                                    }

                                    timer?.let {
                                        GameTimer(vm = vm)
                                        LaunchedEffect(Unit) {
                                            it.start()
                                        }
                                    }

                                    NewsButton(onClick = {
                                        showNews = true
                                        vm.hasNewNews.value = false
                                    }, hasNew = hasNewNews)

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
                                QRScannerDialog(onDismiss = { showQRScanner = false }) { id ->
                                    vm.setPlayerFound(gameId, id)
                                    players?.let {
                                        val found = it.find { player -> player.playerId == id }
                                        playerFound = found
                                    }
                                    showQRScanner = false
                                    showPlayerFound = true
                                }
                            }

                            if (showPlayerFound) {
                                PlayerFoundDialog(playerFound = playerFound, onCancel = {
                                    playerFound = null
                                    showPlayerFound = false
                                }) {
                                    selfieLauncher.launch(null)
                                    showPlayerFound = false
                                }
                            }

                            if (showLeaveGameDialog) {
                                LeaveGameDialog(onDismissRequest = { showLeaveGameDialog = false }, onConfirm = {
                                    vm.updateUser(mapOf(Pair("currentGameId", "")), FirebaseHelper.uid!!)
                                    vm.stopService(context)
                                    navController.navigate(NavRoutes.StartGame.route)
                                })
                            }

                            selfie?.let {
                                if (showSendSelfie) {
                                    SendSelfieDialog(
                                        selfie = it,
                                        onDismiss = {
                                            playerFound = null
                                            selfie = null
                                            showSendSelfie = false
                                        },
                                        sendPic = {
                                            vm.sendSelfie(
                                                playerFound!!.playerId,
                                                gameId,
                                                it,
                                                playerFound!!.nickname
                                            )
                                            playerFound = null
                                            selfie = null
                                            showSendSelfie = false
                                        }) {
                                        selfieLauncher.launch(null)
                                    }
                                }
                            }

                            if (showNews && news != null) {
                                NewsDialog(newsList = news!!, gameId = gameId) {
                                    showNews = false
                                }
                            }

                            if (showPlayerList && players != null) {
                                PlayerListDialog(onDismiss = { showPlayerList = false }, players = players!!)
                            }

                            BackHandler(enabled = true) {
                                showLeaveGameDialog = true
                            }

                        } else {
                            Text(text = "Location permission needed")
                        }
                    }
                }
            }
        }
    )
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
fun NewsButton(modifier: Modifier = Modifier, onClick: () -> Unit, hasNew: Boolean) {
    Box {
        Card(shape = CircleShape, elevation = 4.dp, modifier = modifier.clickable { onClick() }) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = "qr",
                modifier = Modifier.padding(8.dp)
            )
        }
        if (hasNew) {
            Surface(
                color = Color.Red, shape = CircleShape, modifier = Modifier
                    .size(8.dp)
                    .align(
                        Alignment.TopEnd
                    )
            ) {

            }
        }
    }

}

@Composable
fun NewsDialog(newsList: List<News>, gameId: String, onDismiss: () -> Unit) {
    val screenHeight = LocalConfiguration.current.screenHeightDp
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .height((screenHeight * 0.8).dp)
                .fillMaxWidth(), backgroundColor = Color.White, shape = RoundedCornerShape(8.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Events", style = MaterialTheme.typography.h6, modifier = Modifier.padding(vertical = 16.dp))
                LazyColumn(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(newsList) {
                        NewsItem(news = it, gameId = gameId)
                    }
                }
            }

        }
    }

}

@Composable
fun NewsItem(news: News, gameId: String) {
    var bitmap: Bitmap? by remember { mutableStateOf(null) }
    LaunchedEffect(Unit) {
        val ONE_MEGABYTE: Long = 1024 * 1024
        FirebaseHelper.getSelfieImage(gameId = gameId, news.picId)
            .getBytes(ONE_MEGABYTE)
            .addOnSuccessListener {
                val retrievedBitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                bitmap = retrievedBitmap
            }
    }
    bitmap?.let {
        Card(
            shape = RoundedCornerShape(8.dp),
            backgroundColor = Color.LightGray,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "selfie",
                    modifier = Modifier
                        .aspectRatio(it.width.toFloat() / it.height)
                        .fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = news.text)
                    Text(
                        text = "${timeStampToTimeString(news.timestamp)}",
                        style = MaterialTheme.typography.subtitle1,
                        modifier = Modifier
                            .align(
                                Alignment.TopEnd
                            )
                            .padding(4.dp)
                    )
                }

            }
        }

    }
}

fun timeStampToTimeString(timestamp: Timestamp): String? {
    val localDateTime = timestamp.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    return localDateTime.format(formatter)
}

@Composable
fun ShowMyQRDialog(onDismiss: () -> Unit) {
    val playerId = FirebaseHelper.uid!!
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

@Composable
fun PlayerFoundDialog(playerFound: Player?, onCancel: () -> Unit, onTakePic: () -> Unit) {
    Dialog(onDismissRequest = onCancel) {
        Card(backgroundColor = Color.White, shape = RoundedCornerShape(8.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(text = "You found ${playerFound?.nickname}")
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CustomButton(text = "Take a selfie") {
                        onTakePic()
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    CustomButton(text = "Close") {
                        onCancel()
                    }
                }
            }
        }
    }
}

@Composable
fun SendSelfieDialog(
    selfie: Bitmap,
    onDismiss: () -> Unit,
    sendPic: () -> Unit,
    takeNew: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(backgroundColor = Color.White, shape = RoundedCornerShape(8.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        bitmap = selfie.asImageBitmap(),
                        contentDescription = "selfie",
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .aspectRatio(selfie.width.toFloat() / selfie.height)
                            .fillMaxWidth()
                    )
                    CustomButton(text = "Cancel") {
                        onDismiss()
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    CustomButton(text = "Take another") {
                        takeNew()
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    CustomButton(text = "Send") {
                        sendPic()
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
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
fun GameTimer(vm: HeatMapViewModel) {
    val countdown by vm.countdown.observeAsState()
    countdown?.let {
        Row(modifier = Modifier.border(BorderStroke(1.dp, Raisin)).padding(2.dp)) {
            Icon(Icons.Default.Alarm, contentDescription = "", tint = Raisin)
            Text(text = secondsToText(it), color = Raisin, fontSize = 20.sp)
        }

    }
}

class HeatMapViewModel(application: Application) : AndroidViewModel(application) {
    val TAG = "heatMapVM"

    companion object {
        val locationRequest: LocationRequest = LocationRequest.create().apply {
            interval = 10 * 1000
            isWaitForAccurateLocation = false
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }
    }

    var newsCount = 0
    val news = MutableLiveData<List<News>>()
    val hasNewNews = MutableLiveData<Boolean>()
    val firestore = FirebaseHelper
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
    val playerStatus = Transformations.map(players) { list ->
        list.find { it.playerId == firestore.uid!! }?.inGameStatus
    }
    val isSeeker = MutableLiveData<Boolean>()
    val playersWithoutSelf = Transformations.map(players) { players ->
        players.filter { it.playerId != FirebaseHelper.uid!! }
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
            }
    }

    fun updateIsSeeker(newVal: Boolean) {
        isSeeker.value = newVal
    }

    fun getTime(gameId: String) {
        val now = Timestamp.now().toDate().time.div(1000)
        firestore.getLobby(gameId = gameId).get()
            .addOnSuccessListener {
                val lobby = it.toObject(Lobby::class.java)
                lobby?.let {
                    val startTime = lobby.startTime.toDate().time / 1000
                    val countdown = lobby.countdown
                    val timeLimit = lobby.timeLimit * 60
                    val gameEndTime = (startTime + countdown + timeLimit)
                    timeRemaining.postValue(gameEndTime.minus(now).toInt() + 1)
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

    fun setPlayerInGameStatus(status: Int, gameId: String, playerId: String) {
        firestore.updatePlayerInGameStatus(
            inGameStatus = status,
            gameId = gameId,
            playerId = playerId
        )
    }

    fun setPlayerFound(gameId: String, playerId: String) {
        val changeMap = mapOf(
            Pair("inGameStatus", InGameStatus.ELIMINATED.value),
            Pair("timeOfElimination", Timestamp.now())
        )
        firestore.updatePlayer(changeMap, playerId, gameId)
    }

    fun sendSelfie(foundPlayerId: String, gameId: String, selfie: Bitmap, nickname: String) {
        firestore.sendSelfie(foundPlayerId, gameId, selfie, nickname)
    }

    fun getNews(gameId: String) {
        firestore.getNews(gameId).addSnapshotListener { data, e ->
            data ?: kotlin.run {
                Log.e(ForegroundService.TAG, "listenForNews: ", e)
                return@addSnapshotListener
            }
            val newsList = data.toObjects(News::class.java)
            if (newsList.size > newsCount) {
                news.value = newsList
                hasNewNews.value = true
            }
        }
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
        ForegroundService.start(
            context = context,
            gameId = gameId,
            isSeeker = isSeeker
        )
    }

    fun stopService(context: Context) {
        ForegroundService.stop(
            context = context,
        )
    }

}

//source: https://stackoverflow.com/questions/6048975/google-maps-v3-how-to-calculate-the-zoom-level-for-a-given-bounds
fun getBoundsZoomLevel(bounds: LatLngBounds, mapDim: Size): Double {
    val WORLD_DIM = Size(256, 256)
    val ZOOM_MAX = 21.toDouble()

    fun latRad(lat: Double): Double {
        val sin = sin(lat * Math.PI / 180)
        val radX2 = ln((1 + sin) / (1 - sin)) / 2
        return max(min(radX2, Math.PI), -Math.PI) / 2
    }

    fun zoom(mapPx: Int, worldPx: Int, fraction: Double): Double {
        return floor(ln(mapPx / worldPx / fraction) / ln(2.0))
    }

    val ne = bounds.northeast
    val sw = bounds.southwest

    val latFraction = (latRad(ne.latitude) - latRad(sw.latitude)) / Math.PI

    val lngDiff = ne.longitude - sw.longitude
    val lngFraction = if (lngDiff < 0) {
        (lngDiff + 360) / 360
    } else {
        (lngDiff / 360)
    }

    val latZoom = zoom(mapDim.height, WORLD_DIM.height, latFraction)
    val lngZoom = zoom(mapDim.width, WORLD_DIM.width, lngFraction)

    return minOf(latZoom, lngZoom, ZOOM_MAX)
}

class StepCounterVewModel(context: Context): ViewModel(){
    var steps = MutableLiveData("0")
    var distance = MutableLiveData(0.0F)
    var running  = MutableLiveData(false)
    val stepLength = 0.78F

    var initialSteps = MutableLiveData(-2)
    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val stepCounterSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    //https://www.geeksforgeeks.org/proximity-sensor-in-android-app-using-jetpack-compose/
    private val sensorEventListener = object: SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if(event.sensor == stepCounterSensor){
                if(running.value == true){
                    event.values.firstOrNull()?.toInt().let { newSteps ->
                        if (initialSteps.value == -2) {
                            initialSteps.value = newSteps!!
                        }
                        val currentSteps = newSteps?.minus(initialSteps.value!!)
                        if (currentSteps != null) {
                            steps.value = currentSteps.toString()
                            Log.d("steps", steps.value!!)
                        }
                    }
                }
                //steps.value = event.values[0]
            }
        }


        override fun onAccuracyChanged(sensor: Sensor?, p1: Int) {
            Log.d("something", "something")
        }
    }

    fun startStepCounter(){
        running.value=true
        sensorManager.registerListener(
            sensorEventListener,
            stepCounterSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        //Toast.makeText(context, "Start", Toast.LENGTH_SHORT).show()
    }

    fun stopStepCounter(){
        running.value=false
        sensorManager.unregisterListener(sensorEventListener)
        //Toast.makeText(context, "Stop", Toast.LENGTH_SHORT).show()
        initialSteps.value = -2
    }
    fun countDistance(){
        val length = stepLength.times(steps.value!!.toFloat())
        distance.value=length
    }
}
