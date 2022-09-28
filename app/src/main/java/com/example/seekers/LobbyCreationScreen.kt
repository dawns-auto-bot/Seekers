package com.example.seekers

import androidx.navigation.NavController
import com.example.seekers.general.CustomButton

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    navController: NavController,
    nickname: String,
    avatarId: Int
) {
    val maxPlayers by vm.maxPlayers.observeAsState()
    val timeLimit by vm.timeLimit.observeAsState()
    val radius by vm.radius.observeAsState()

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

        CreationForm(vm = vm)
        CustomButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.create_lobby)
        ) {
            if (maxPlayers != null && timeLimit != null && radius != null) {
                val geoPoint = GeoPoint(60.224165, 24.758388)
                val lobby = Lobby(
                    "",
                    geoPoint,
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

    fun updateMaxPlayers(newVal: Int?) {
        maxPlayers.value = newVal
    }

    fun updateTimeLimit(newVal: Int?) {
        timeLimit.value = newVal
    }

    fun updateRadius(newVal: Int?) {
        radius.value = newVal
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
