package com.example.seekers

import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import com.example.seekers.general.IconButton
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.seekers.general.CustomButton
import com.example.seekers.general.QRCodeComponent
import com.example.seekers.general.generateQRCode
import com.example.seekers.ui.theme.avatarBackground
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.GeoPoint
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun LobbyQRScreen(
    navController: NavHostController,
    vm: LobbyCreationScreenViewModel = viewModel(),
    gameId: String,
) {
    val context = LocalContext.current
    val bitmap = generateQRCode(gameId)
    val players by vm.players.observeAsState(listOf())
    val lobby by vm.lobby.observeAsState()
    val isCreator by vm.isCreator.observeAsState()
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showDismissDialog by remember { mutableStateOf(false) }
    var showEditRulesDialog by remember { mutableStateOf(false) }
    var showQRDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            vm.getPlayers(gameId)
            vm.getLobby(gameId)
            vm.getPlayer(gameId, FirebaseHelper.uid!!)
        }
    }

    LaunchedEffect(lobby) {
        lobby?.let {
            when (it.status) {
                LobbyStatus.DELETED.value -> {
                    if (isCreator != true) {
                        Toast.makeText(context, "The lobby was closed by the host", Toast.LENGTH_LONG)
                            .show()
                    }
                    vm.updateUser(FirebaseHelper.uid!!, mapOf(Pair("currentGameId", "")))
                    navController.navigate(NavRoutes.StartGame.route)
                }
                LobbyStatus.COUNTDOWN.value -> {
                    navController.navigate(NavRoutes.Countdown.route + "/$gameId")
                }
                LobbyStatus.ACTIVE.value -> {
                    navController.navigate(NavRoutes.Heatmap.route + "/$gameId")
                }
            }
        }
    }

    LaunchedEffect(players) {
        if (players.isNotEmpty()) {
            val currentPlayer = players.find { it.playerId == FirebaseHelper.uid!! }
            if (currentPlayer == null) {
                Toast.makeText(context, "You were kicked from the lobby", Toast.LENGTH_LONG).show()
                vm.updateUser(FirebaseHelper.uid!!, mapOf(Pair("currentGameId", "")))
                navController.navigate(NavRoutes.StartGame.route)
            }
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = {
                Text(
                    text = "Scan QR to join!",
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(15.dp)
                )
            },
            navigationIcon = {
                IconButton(onClick = {
                    showQRDialog = true
                }) {
                    Icon(Icons.Outlined.QrCode2, "QR", modifier = Modifier.size(40.dp))
                }
            },
            actions = {
                Button(onClick = {
                    if (isCreator == true) {
                        showDismissDialog = true
                    } else {
                        showLeaveDialog = true
                    }
                }) {
                    Text(text = "Leave")
                }
            },
            backgroundColor = Color.Transparent,
            elevation = 0.dp
        )
    }
    ) {
        Column(
            Modifier.padding(it),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Participants", fontSize = 20.sp, modifier = Modifier.padding(15.dp))
            Participants(
                Modifier
                    .weight(3f)
                    .padding(horizontal = 15.dp), players, isCreator == true, vm, gameId
            )
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column {
                    CustomButton(text = "${if (isCreator == true) "Edit" else "Check"} Rules") {
                        showEditRulesDialog = true
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (isCreator == true) {
                        CustomButton(text = "Start Game") {
                            vm.updateLobby(
                                mapOf(
                                    Pair("status", LobbyStatus.COUNTDOWN.value),
                                    Pair("startTime", FieldValue.serverTimestamp())
                                ),
                                gameId
                            )
                        }
                    }
                }
            }

        }
        if (showQRDialog) {
            QRDialog(
                bitmap = bitmap,
                onDismissRequest = { showQRDialog = false }
            )
        }
        if (showEditRulesDialog) {
            EditRulesDialog(
                vm,
                gameId,
                isCreator == true,
                onDismissRequest = { showEditRulesDialog = false })
        }
        if (showLeaveDialog) {
            LeaveGameDialog(onDismissRequest = { showLeaveDialog = false }, onConfirm = {
                vm.removePlayer(gameId, "")
                vm.updateUser(
                    FirebaseHelper.uid!!,
                    mapOf(Pair("currentGameId", ""))
                )
                navController.navigate(NavRoutes.StartGame.route)
            })
        }
        if (showDismissDialog) {
            DismissLobbyDialog(onDismissRequest = { showDismissDialog = false }, onConfirm = {
                val changeMap = mapOf(
                    Pair("status", LobbyStatus.DELETED.value)
                )
                vm.updateUser(
                    FirebaseHelper.uid!!,
                    mapOf(Pair("currentGameId", ""))
                )
                vm.updateLobby(changeMap, gameId)
            })
        }
    }

}

@Composable
fun EditRulesDialog(
    vm: LobbyCreationScreenViewModel,
    gameId: String,
    isCreator: Boolean,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val maxPlayers by vm.maxPlayers.observeAsState()
    val timeLimit by vm.timeLimit.observeAsState()
    val radius by vm.radius.observeAsState()
    val countdown by vm.countdown.observeAsState()
    val center by vm.center.observeAsState()

    Dialog(onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color.White,
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "${if (isCreator) "Edit" else "Check"} Rules")
                        Icon(
                            imageVector = Icons.Filled.Cancel,
                            contentDescription = "close dialog",
                            modifier = Modifier
                                .width(30.dp)
                                .height(30.dp)
                                .clickable { onDismissRequest() }
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    if (isCreator) {
                        EditRulesForm(vm = vm)
                        Spacer(modifier = Modifier.height(20.dp))
                        Box(modifier = Modifier.padding(40.dp, 0.dp, 40.dp, 0.dp)) {
                            CustomButton(text = "Save") {
                                if (maxPlayers != null && timeLimit != null && radius != null && countdown != null && center != null) {
                                    val centerGeoPoint = GeoPoint(center!!.latitude, center!!.longitude)
                                    val changeMap = mapOf(
                                        Pair("center", centerGeoPoint),
                                        Pair("maxPlayers", maxPlayers!!),
                                        Pair("timeLimit", timeLimit!!),
                                        Pair("radius", radius!!),
                                        Pair("countdown", countdown!!)
                                    )
                                    vm.updateLobby(changeMap, gameId = gameId)
                                    Toast.makeText(context, "Game rules updated", Toast.LENGTH_LONG)
                                        .show()
                                    onDismissRequest()
                                } else {
                                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                        }
                    } else {
                        ShowRules(vm = vm)
                    }
                }
            }
        }
    }
}

@Composable
fun ShowRules(vm: LobbyCreationScreenViewModel) {
    val lobby by vm.lobby.observeAsState()

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = "Maximum amount of players: ${lobby?.maxPlayers}")
        Text(text = "Time limit: ${lobby?.timeLimit}")
        Text(text = "Play area radius: ${lobby?.radius}")
        Text(text = "Time to hide: ${lobby?.countdown}")
    }
}

@Composable
fun EditRulesForm(vm: LobbyCreationScreenViewModel) {
    val context = LocalContext.current
    val maxPlayers by vm.maxPlayers.observeAsState()
    val timeLimit by vm.timeLimit.observeAsState()
    val radius by vm.radius.observeAsState()
    val countdown by vm.countdown.observeAsState()
    val showMap by vm.showMap.observeAsState(false)
    var isLocationAllowed by remember { mutableStateOf(false) }
    var showPermissionsDialog by remember { mutableStateOf(false) }
    var cameraState = rememberCameraPositionState()

    if (!showMap) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
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


}

@Composable
fun LeaveGameDialog(onDismissRequest: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        title = { Text(text = "Quit?") },
        text = { Text(text = "Are you sure you want to leave?") },
        onDismissRequest = onDismissRequest,
        dismissButton = {
            Button(onClick = { onDismissRequest() }) {
                Text(text = "Cancel")
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm() }) {
                Text(text = "Leave")
            }
        }
    )
}

@Composable
fun DismissLobbyDialog(onDismissRequest: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        title = { Text(text = "Quit?") },
        text = { Text(text = "Are you sure you want to dismiss this lobby?") },
        onDismissRequest = onDismissRequest,
        dismissButton = {
            Button(onClick = { onDismissRequest() }) {
                Text(text = "Keep")
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm() }) {
                Text(text = "Dismiss")
            }
        }
    )
}

@Composable
fun QRDialog(
    bitmap: Bitmap,
    onDismissRequest: () -> Unit
) {
    Dialog(onDismissRequest = { onDismissRequest() }) {
        Surface(
            color = Color.White,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(30.dp)
            ) {
                QRCodeComponent(bitmap = bitmap)
            }

        }

    }

}

@Composable
fun QRCodeComponent(modifier: Modifier = Modifier, bitmap: Bitmap) {

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "QR",
        modifier = modifier.size(250.dp)
    )
}


@Composable
fun Participants(
    modifier: Modifier = Modifier,
    players: List<Player>,
    isCreator: Boolean,
    vm: LobbyCreationScreenViewModel,
    gameId: String
) {
    var kickableIndex: Int? by remember { mutableStateOf(null) }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        itemsIndexed(players.sortedBy { it.inLobbyStatus }) { index, player ->
            PlayerCard(
                player = player,
                isCreator = isCreator,
                vm = vm,
                gameId = gameId,
                setKickableIndex = { kickableIndex = index },
                isKickable = kickableIndex == index
            )
        }
    }

}

@Composable
fun PlayerCard(
    player: Player,
    isCreator: Boolean,
    vm: LobbyCreationScreenViewModel,
    gameId: String,
    setKickableIndex: () -> Unit,
    isKickable: Boolean
) {

    val avaratID = when (player.avatarId) {
        0 -> R.drawable.bee
        1 -> R.drawable.chameleon
        2 -> R.drawable.chick
        3 -> R.drawable.cow
        4 -> R.drawable.crab
        5 -> R.drawable.dog
        6 -> R.drawable.elephant
        7 -> R.drawable.fox
        8 -> R.drawable.koala
        9 -> R.drawable.lion
        10 -> R.drawable.penguin
        else -> R.drawable.whale
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = 10.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .clickable {
                    if (isCreator) {
                        setKickableIndex()
                    }
                }
        ) {
            Card(
                shape = CircleShape,
                border = BorderStroke(2.dp, Color.Black),
                backgroundColor = avatarBackground,
                modifier = Modifier
                    .padding(10.dp)
            ) {
                Image(
                    painter = painterResource(id = avaratID),
                    contentDescription = "avatar",
                    modifier = Modifier
                        .size(50.dp)
                        .padding(10.dp)
                )
            }
            Text(text = "${player.nickname} ${if (player.inLobbyStatus == InLobbyStatus.CREATOR.value) "(Host)" else ""}")
            if (isKickable && player.inLobbyStatus == InLobbyStatus.JOINED.value) {
                Button(
                    onClick = {
                        vm.removePlayer(gameId = gameId, player.playerId)
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.Red,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .padding(10.dp)
                ) {
                    Text(text = "Kick")
                }
            } else {
                Button(
                    modifier = Modifier
                        .padding(10.dp)
                        .alpha(0f),
                    onClick = {},
                ) {
                    Text(text = "Kick")
                }
            }
        }
    }
}

//class LobbyViewModel() : ViewModel() {
//    val TAG = "LobbyVM"
//    val firestore = FirebaseHelper
//    val players = MutableLiveData(listOf<Player>())
//    val lobby = MutableLiveData<Lobby>()
//    val isCreator = MutableLiveData<Boolean>()
//    val playerId = firestore.uid
//    val maxPlayers = MutableLiveData<Int>()
//    val timeLimit = MutableLiveData<Int>()
//    val radius = MutableLiveData<Int>()
//    val countdown = MutableLiveData<Int>()
//
//    fun updateMaxPlayers(newVal: Int?) {
//        maxPlayers.value = newVal
//    }
//
//    fun updateTimeLimit(newVal: Int?) {
//        timeLimit.value = newVal
//    }
//
//    fun updateRadius(newVal: Int?) {
//        radius.value = newVal
//    }
//
//    fun updateCountdown(newVal: Int?) {
//        countdown.value = newVal
//    }
//
//    fun removePlayer(gameId: String, playerId: String) =
//        firestore.removePlayer(gameId = gameId, playerId = playerId)
//
//    fun getPlayers(gameId: String) {
//        firestore.getPlayers(gameId)
//            .addSnapshotListener { list, e ->
//                list ?: run {
//                    Log.e(TAG, "getPlayers: ", e)
//                    return@addSnapshotListener
//                }
//                val playerList = list.toObjects(Player::class.java)
//                players.postValue(playerList)
//            }
//    }
//
//    fun getLobby(gameId: String) {
//        firestore.getLobby(gameId).addSnapshotListener { data, e ->
//            data?.let {
//                lobby.postValue(it.toObject(Lobby::class.java))
//            }
//        }
//    }
//
//    fun updateLobby(changeMap: Map<String, Any>, gameId: String) =
//        firestore.updateLobby(changeMap, gameId)
//
//    fun getPlayer(gameId: String, playerId: String) {
//        firestore.getPlayer(gameId, playerId).get()
//            .addOnSuccessListener { data ->
//                val player = data.toObject(Player::class.java)
//                player?.let {
//                    isCreator.postValue(it.inLobbyStatus == InLobbyStatus.CREATOR.value)
//                }
//            }
//            .addOnFailureListener {
//                Log.e(TAG, "getPlayer: ", it)
//            }
//    }
//
//    fun updateUser(userId: String, changeMap: Map<String, Any>) =
//        firestore.updateUser(userId, changeMap)
//
//}