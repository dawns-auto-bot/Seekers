package com.example.seekers

import android.Manifest
import androidx.navigation.NavController
import com.example.seekers.general.CustomButton
import com.example.seekers.general.IconButton
import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.seekers.general.PermissionDialog
import com.example.seekers.general.getPermissionLauncher
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun LobbyCreationScreen(
    vm: LobbyCreationScreenViewModel = viewModel(),
    navController: NavController,
    nickname: String,
    avatarId: Int
) {
    val context = LocalContext.current
    val maxPlayers by vm.maxPlayers.observeAsState()
    val timeLimit by vm.timeLimit.observeAsState()
    val radius by vm.radius.observeAsState()
    val countdown by vm.countdown.observeAsState()
    val center by vm.center.observeAsState()
    val currentLocation by vm.currentLocation.observeAsState()
    val showMap by vm.showMap.observeAsState(false)

    var showPermissionsDialog by remember { mutableStateOf(false) }
    var dialogShown by remember { mutableStateOf(false) }
    var isLocationAllowed by remember { mutableStateOf(false) }
    var initialLocationSet by remember { mutableStateOf(false) }
    var cameraState = rememberCameraPositionState()
    val locationPermissionLauncher = getPermissionLauncher(
        onResult = {
            isLocationAllowed = it
        }
    )

    LaunchedEffect(dialogShown) {
        if (dialogShown) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(isLocationAllowed) {
        if (isLocationAllowed) {
            vm.requestLoc()
        }
    }

    if (!initialLocationSet) {
        LaunchedEffect(currentLocation) {
            currentLocation?.let {
                cameraState.position = CameraPosition.fromLatLngZoom(it, 16f)
                initialLocationSet = true
            }
        }
    }

    if (!showMap) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(3f), contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.lobby_creation),
                    style = MaterialTheme.typography.h6
                )
            }

            Column(Modifier.fillMaxWidth()) {
                CreationForm(vm = vm)
                Spacer(modifier = Modifier.height(16.dp))
                IconButton(
                    resourceId = R.drawable.map,
                    buttonText = "Define Area",
                    buttonColor = if (showMap) Color(0xFF838383) else Color.LightGray,
                ) {
                    if (LocationHelper.checkPermissions(context)) {
                        isLocationAllowed = true
                        vm.updateShowMap(true)
                    } else {
                        showPermissionsDialog = true
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            CustomButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.create_lobby)
            ) {
                if (maxPlayers != null && timeLimit != null && radius != null && countdown != null) {
                    if (center == null) {
                        Toast.makeText(
                            context,
                            "Please set a location for the game",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        vm.removeLocationUpdates()
                        val geoPoint = GeoPoint(center!!.latitude, center!!.longitude)
                        val lobby = Lobby(
                            id = "",
                            center = geoPoint,
                            maxPlayers = maxPlayers!!,
                            timeLimit = timeLimit!!,
                            radius = radius!!,
                            status = LobbyStatus.CREATED.value,
                            countdown = countdown!!
                        )
                        val gameId = vm.addLobby(lobby)
                        val player = Player(
                            nickname = nickname,
                            avatarId = avatarId,
                            playerId = FirebaseHelper.uid!!,
                            inLobbyStatus = InLobbyStatus.CREATOR.value,
                            inGameStatus = InGameStatus.SEEKER.value
                        )
                        vm.addPlayer(player, gameId)
                        vm.updateUser(
                            FirebaseHelper.uid!!,
                            mapOf(Pair("currentGameId", gameId))
                        )
                        navController.navigate(NavRoutes.LobbyQR.route + "/$gameId")
                    }
                }
            }
        }
    } else {
        if (isLocationAllowed) {
            Box(modifier = Modifier.fillMaxSize()) {
                Icon(imageVector = Icons.Filled.Cancel, contentDescription = "cancel",
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .clickable {
                            vm.updateShowMap(false)
                        }
                )
                AreaSelectionMap(
                    vm = vm,
                    properties = MapProperties(
                        mapType = MapType.SATELLITE,
                        isMyLocationEnabled = true
                    ),
                    settings = MapUiSettings(
                        zoomControlsEnabled = true,
                        zoomGesturesEnabled = true,
                        rotationGesturesEnabled = false,
                        scrollGesturesEnabled = true
                    ),
                    state = cameraState
                )
            }

        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Please allow location to set a playing area")
            }
        }
    }


    if (showPermissionsDialog) {
        PermissionDialog(
            onDismiss = { showPermissionsDialog = false },
            onContinue = {
                showPermissionsDialog = false
                dialogShown = true
            },
            title = "Location Permission",
            text = "Seekers needs your location to enhance your Hide and Seek games!"
        )
    }
}

@Composable
fun CreationForm(modifier: Modifier = Modifier, vm: LobbyCreationScreenViewModel) {
    val maxPlayers by vm.maxPlayers.observeAsState()
    val timeLimit by vm.timeLimit.observeAsState()
    val countdown by vm.countdown.observeAsState()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Input(
            title = stringResource(id = R.string.max_players),
            value = maxPlayers?.toString() ?: "",
            keyboardType = KeyboardType.Number,
            onChangeValue = { vm.updateMaxPlayers(it.toIntOrNull()) })

        Input(
            title = stringResource(id = R.string.time_limit),
            value = timeLimit?.toString() ?: "",
            keyboardType = KeyboardType.Number,
            onChangeValue = { vm.updateTimeLimit(it.toIntOrNull()) })
        Input(
            title = stringResource(id = R.string.countdown),
            value = countdown?.toString() ?: "",
            keyboardType = KeyboardType.Number,
            onChangeValue = { vm.updateCountdown(it.toIntOrNull()) })
    }
}

@Composable
fun Input(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChangeValue: (String) -> Unit
) {
    Column(modifier = modifier) {
        Text(text = title)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color.LightGray,
            elevation = 0.dp,
            shape = MaterialTheme.shapes.medium
        ) {
            TextField(
                value = value,
                onValueChange = onChangeValue,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
            )
        }
    }
}

class LobbyCreationScreenViewModel(application: Application) : AndroidViewModel(application) {
    val firestore = FirebaseHelper
    val maxPlayers = MutableLiveData<Int>()
    val timeLimit = MutableLiveData<Int>()
    val countdown = MutableLiveData<Int>()
    val center: MutableLiveData<LatLng> = MutableLiveData<LatLng>(null)
    val radius = MutableLiveData(50)
    val showMap = MutableLiveData(false)
    val currentLocation = MutableLiveData<LatLng>()
    val client = LocationServices.getFusedLocationProviderClient(application)
    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult) {
            p0.lastLocation?.let {
                currentLocation.postValue(LatLng(it.latitude, it.longitude))
            }
        }
    }

    fun updateCenter(location: LatLng) {
        center.value = location
    }

    fun updateMaxPlayers(newVal: Int?) {
        maxPlayers.value = newVal
    }

    fun updateTimeLimit(newVal: Int?) {
        timeLimit.value = newVal
    }

    fun updateRadius(newVal: Int) {
        radius.value = newVal
    }

    fun updateCountdown(newVal: Int?) {
        countdown.value = newVal
    }

    fun addLobby(lobby: Lobby) = firestore.addLobby(lobby)

    fun addPlayer(player: Player, gameId: String) = firestore.addPlayer(player, gameId)

    fun updateUser(userId: String, changeMap: Map<String, Any>) =
        firestore.updateUser(userId, changeMap)

    fun updateShowMap(newVal: Boolean) {
        showMap.value = newVal
    }

    fun requestLoc() {
        LocationHelper.requestLocationUpdates(
            client = client,
            locationCallback = locationCallback
        )
    }

    fun removeLocationUpdates() {
        LocationHelper.removeLocationUpdates(client, locationCallback)
    }
}