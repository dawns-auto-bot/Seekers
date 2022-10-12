package com.example.seekers

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.seekers.general.QRScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun QrScannerScreen(
    navController: NavHostController,
    vm: ScannerViewModel = viewModel(),
    nickname: String,
    avatarId: Int,
    permissionVM: PermissionsViewModel
) {
    val context = LocalContext.current
    val cameraIsAllowed by permissionVM.cameraPerm.observeAsState(false)
    var gameId: String? by remember { mutableStateOf(null) }
    val lobby by vm.lobby.observeAsState()
    val playersInLobby by vm.playersInLobby.observeAsState()

    LaunchedEffect(Unit) {
        permissionVM.checkAllPermissions(context)
    }

    LaunchedEffect(gameId) {
        gameId?.let {
            val firestore = FirebaseHelper
            val player = Player(
                nickname = nickname,
                avatarId = avatarId,
                playerId = FirebaseHelper.uid!!,
                inLobbyStatus = InLobbyStatus.JOINED.ordinal,
                inGameStatus = InGameStatus.PLAYER.ordinal
            )
            firestore.addPlayer(player, it)
            firestore.updateUser(
                FirebaseHelper.uid!!,
                mapOf(Pair("currentGameId", it))
            )
            navController.navigate(NavRoutes.LobbyQR.route + "/$it")

            val hasLobby = withContext(Dispatchers.IO) {
                vm.getLobby(it)
            }
            val hasPlayers = withContext(Dispatchers.IO) {
                vm.getNumberOfPlayersInLobby(it)
            }
            delay(1000)
            if (hasLobby && hasPlayers) {
                if (lobby?.maxPlayers == playersInLobby) {
                    Toast.makeText(
                        context,
                        "The lobby is currently full",
                        Toast.LENGTH_SHORT
                    ).show()
                    navController.navigate(NavRoutes.StartGame.route)
                } else {
                    vm.firestore.addPlayer(player, it)
                    vm.firestore.updateUser(
                        FirebaseHelper.uid!!,
                        mapOf(Pair("currentGameId", it))
                    )
                    navController.navigate(NavRoutes.LobbyQR.route + "/$it")
                }
            }


        }
    }

    if (cameraIsAllowed) {
        QRScanner(context = context, onScanned = { gameId = it })
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Need camera permission")
        }
    }
}

class ScannerViewModel : ViewModel() {
    companion object {
        const val TAG = "SCANNER_VIEW_MODEL"
    }

    val firestore = FirebaseHelper
    val lobby = MutableLiveData<Lobby>()
    val playersInLobby = MutableLiveData<Int>()

    fun getLobby(gameId: String): Boolean {
        firestore.getLobby(gameId).addSnapshotListener { data, e ->
            data ?: kotlin.run {
                Log.e(TAG, "getLobby: ", e)
                return@addSnapshotListener
            }
            lobby.value = data.toObject(Lobby::class.java)
        }
        return true
    }

    fun getNumberOfPlayersInLobby(gameId: String): Boolean {
        firestore.getPlayers(gameId).addSnapshotListener { data, e ->
            data ?: kotlin.run {
                Log.e(TAG, "getLobby: ", e)
                return@addSnapshotListener
            }
            playersInLobby.postValue(data.documents.size)
        }
        return true
    }
}


