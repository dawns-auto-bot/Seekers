package com.example.seekers

import androidx.navigation.NavController
import com.example.seekers.general.CustomButton
import com.example.seekers.general.IconButton
import android.app.Application
import android.util.Log
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.firestore.GeoPoint

@Composable
fun LobbyCreationScreen(
    vm: LobbyCreationScreenViewModel = viewModel(),
    mapvm: MapViewModel = viewModel(),
    navController: NavController,
    nickname: String,
    avatarId: Int
) {
    val maxPlayers by vm.maxPlayers.observeAsState()
    val timeLimit by vm.timeLimit.observeAsState()
    val radius by vm.radius.observeAsState()
    val countdown by vm.countdown.observeAsState()
    val center by mapvm.playAreaCenter.observeAsState()

    var selected1 by remember { mutableStateOf(false) }
    var selected2 by remember { mutableStateOf(false) }
    val color1 = if (selected1) Color(0xFF838383) else Color.LightGray
    val color2 = if (selected2) Color(0xFF929292) else Color.LightGray

    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(id = R.string.lobby_creation),
            style = MaterialTheme.typography.h6
        )
        Spacer(Modifier.height(15.dp))
        Row(horizontalArrangement = Arrangement.SpaceEvenly) {
            Column(Modifier.weight(1f)) {
                IconButton(
                    resourceId = R.drawable.map,
                    buttonText = "Define Area",
                    buttonColor = color1,
                ) {
                    if(selected1) {
                        selected1 = !selected1
                        selected2 = false
                    } else if (selected2) {
                        selected1 = !selected1
                        selected2 = false
                    } else
                        selected1 = !selected1
                }
            }
            Spacer(Modifier.width(5.dp))
            Column(Modifier.weight(1f)) {
                IconButton(
                    resourceId = R.drawable.switches,
                    buttonText = "Set Rules",
                    buttonColor = color2,
                ) {
                    if(selected2) {
                        selected2 = !selected2
                        selected1 = false
                    } else if (selected1) {
                        selected2 = !selected2
                        selected1 = false
                    } else
                        selected2 = !selected2
                }
            }

        }

        if(selected1) {
            Spacer(Modifier.height(15.dp))
            Map(vm = mapvm, lobbyvm = vm, true)
        }

        if(selected2) {
            CreationForm(vm = vm)
        }

        CustomButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.create_lobby)
        ) {
            if (maxPlayers != null && timeLimit != null && radius != null && countdown != null) {
                val geoPoint = GeoPoint(center!!.latitude, center!!.longitude)
                val lobby = Lobby(
                    "",
                    geoPoint,
                    countdown!!,
                    maxPlayers!!,
                    timeLimit!!,
                    radius!!,
                    LobbyStatus.ACTIVE.value
                )
                val gameId = vm.addLobby(lobby)
                val player = Player(nickname, avatarId, playerId, PlayerStatus.CREATOR.value)
                vm.addPlayer(player, gameId)
                vm.updateUser(
                    playerId,
                    mapOf(Pair("currentGameId", gameId))
                )
                navController.navigate(NavRoutes.LobbyQR.route + "/$gameId")
            }
        }
    }
}

class LobbyCreationScreenViewModel(application: Application) : AndroidViewModel(application) {
    val firestore = FirestoreHelper
    val maxPlayers = MutableLiveData<Int>()
    val timeLimit = MutableLiveData<Int>()
    val radius = MutableLiveData<Int>()
    val countdown = MutableLiveData<Int>()

    fun updateMaxPlayers(newVal: Int?) {
        maxPlayers.value = newVal
    }

    fun updateTimeLimit(newVal: Int?) {
        timeLimit.value = newVal
    }

    fun updateRadius(newVal: Int?) {
        radius.value = newVal
    }

    fun updateCountdown(newVal: Int?) {
        countdown.value = newVal
    }

    fun addLobby(lobby: Lobby) = firestore.addLobby(lobby)

    fun addPlayer(player: Player, gameId: String) = firestore.addPlayer(player, gameId)

    fun updateUser(userId: String, changeMap: Map<String, Any>) =
        firestore.updateUser(userId, changeMap)
}

@Composable
fun CreationForm(vm: LobbyCreationScreenViewModel) {
    val maxPlayers by vm.maxPlayers.observeAsState()
    val timeLimit by vm.timeLimit.observeAsState()
    val radius by vm.radius.observeAsState()
    val countdown by vm.countdown.observeAsState()


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
            title = stringResource(id = R.string.radius),
            value = radius?.toString() ?: "",
            keyboardType = KeyboardType.Number,
            onChangeValue = { vm.updateRadius(it.toIntOrNull()) })

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


@Composable
fun CustomSlider(
    modifier: Modifier = Modifier,
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = modifier) {
        Text(text = title)
        Slider(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            onValueChange = onValueChange,
        )
    }
}

//@Preview
//@Composable
//fun InputPreview() {
//    SeekersTheme {
//        LobbyCreationScreen()
//    }
//}

//@Preview
//@Composable
//fun QrPrev() {
//    val bitmap = generateQRCode("test")
//    Image(modifier = Modifier.size(100.dp), bitmap = bitmap.asImageBitmap(), contentDescription = "test")
//}
