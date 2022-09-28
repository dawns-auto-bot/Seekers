package com.example.seekers

import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.seekers.general.CustomButton
import com.example.seekers.general.generateQRCode
import com.example.seekers.ui.theme.avatarBackground

@Composable
fun LobbyQRScreen(
    navController: NavHostController,
    vm: LobbyViewModel = viewModel(),
    gameId: String,
    isCreator: Boolean
) {
    val context = LocalContext.current
    val bitmap = generateQRCode(gameId)
    val players by vm.players.observeAsState(listOf())
    val lobby by vm.lobby.observeAsState()
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showDismissDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.getPlayers(gameId)
        vm.getLobby(gameId)
    }

    LaunchedEffect(lobby) {
        lobby?.let {
            if (it.status == LobbyStatus.DELETED.value) {
                Toast.makeText(context, "The lobby was closed by the host", Toast.LENGTH_LONG).show()
                navController.navigate(NavRoutes.StartGame.route)
            }
        }
    }

    LaunchedEffect(players) {
        val currentPlayer = players.find { it.playerId == "testuser" }
        if(currentPlayer == null) {
            Toast.makeText(context, "You were kicked from the lobby", Toast.LENGTH_LONG).show()
            navController.navigate(NavRoutes.StartGame.route)
        }
    }


    Scaffold(topBar = {
        TopAppBar(
            title = {
                Text(text = "Scan to join!", fontSize = 20.sp, modifier = Modifier.padding(15.dp))
            },
            actions = {
                Button(onClick = {
                    if (isCreator) {
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
            QRCodeComponent(modifier = Modifier.weight(3f), bitmap)
            Text(text = "Participants", fontSize = 20.sp, modifier = Modifier.padding(15.dp))
            Participants(Modifier.weight(3f), players, isCreator, vm, gameId)
            CustomButton(modifier = Modifier.weight(1f), text = "Start Game") {
                Toast.makeText(context, "You have started the game", Toast.LENGTH_SHORT).show()
            }
        }
        if (showLeaveDialog) {
            LeaveGameDialog(onDismissRequest = { showLeaveDialog = false }, onConfirm = {
                vm.removePlayer(gameId, "")
                navController.navigate(NavRoutes.StartGame.route)
            })
        }
        if (showDismissDialog) {
            DismissLobbyDialog(onDismissRequest = { showDismissDialog = false }, onConfirm = {
                val changeMap = mapOf(
                    Pair("status", LobbyStatus.DELETED.value)
                )
                vm.updateLobby(changeMap, gameId)
            })
        }
    }

}

@Composable
fun LeaveGameDialog(onDismissRequest: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        title = { Text(text = "Quit?") }, text = { Text(text = "Are you sure you want to leave?") }, onDismissRequest = onDismissRequest,
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
        title = { Text(text = "Quit?") }, text = { Text(text = "Are you sure you want to dismiss this lobby?") }, onDismissRequest = onDismissRequest,
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
fun QRCodeComponent(modifier: Modifier = Modifier, bitmap: Bitmap) {

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "QR",
        modifier = modifier.size(250.dp)
    )
}


@Composable
fun Participants(modifier: Modifier = Modifier, players: List<Player>, isCreator: Boolean, vm: LobbyViewModel, gameId: String) {

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        items(players) { player ->
            PlayerCard(player = player, isCreator = isCreator, vm, gameId = gameId)
        }
    }

}

@Composable
fun PlayerCard(player: Player, isCreator: Boolean, vm: LobbyViewModel, gameId: String) {

    val showRemovePlayerBtn by vm.showRemovePlayerBtn.observeAsState(false)

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
            .fillMaxWidth()
            .padding(15.dp),
        elevation = 10.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Card(
                shape = CircleShape,
                border = BorderStroke(2.dp, Color.Black),
                backgroundColor = avatarBackground,
                modifier = Modifier
                    .padding(10.dp)
                    .clickable {
                        if (isCreator) {
                            vm.updateRemoveBtn(true)
                        }
                    }
            ) {
                Image(
                    painter = painterResource(id = avaratID),
                    contentDescription = "avatar",
                    modifier = Modifier
                        .size(50.dp)
                        .padding(10.dp)
                )
            }

            Text(text = player.nickname)
            if (showRemovePlayerBtn && player.status == PlayerStatus.PLAYING.value) {
                Button(
                    onClick = {
                              vm.removePlayer(gameId = gameId, player.playerId)
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red, contentColor = Color.White),
                    modifier = Modifier.padding(10.dp)
                ) {
                    Text(text = "Kick")
                }
            }
        }
    }
}

class LobbyViewModel() : ViewModel() {
    val TAG = "LobbyVM"
    val firestore = FirestoreHelper
    val players = MutableLiveData(listOf<Player>())
    val lobby = MutableLiveData<Lobby>()

    private val _showRemovePlayerBtn = MutableLiveData(false)
    val showRemovePlayerBtn: LiveData<Boolean> = _showRemovePlayerBtn

    fun updateRemoveBtn(value: Boolean) {
        _showRemovePlayerBtn.value = value
    }

    fun removePlayer(gameId: String, playerId: String) =
        firestore.removePlayer(gameId = gameId, playerId = playerId)

    fun getPlayers(gameId: String) {
        firestore.getPlayers(gameId)
            .addSnapshotListener { list, e ->
                list ?: run {
                    Log.e(TAG, "getPlayers: ", e)
                    return@addSnapshotListener
                }
                val playerList = list.toObjects(Player::class.java)
                players.postValue(playerList)
            }
    }

    fun getLobby(gameId: String) {
        firestore.getLobby(gameId).addSnapshotListener { data, e ->
            data?.let {
                 lobby.postValue(it.toObject(Lobby::class.java))
            }
        }
    }

    fun updateLobby(changeMap: Map<String, Any>, gameId: String) = firestore.updateLobby(changeMap, gameId)

}