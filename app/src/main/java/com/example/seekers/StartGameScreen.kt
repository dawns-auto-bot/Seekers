package com.example.seekers

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.seekers.general.CustomButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@Composable
fun StartGameScreen(navController: NavController, vm: StartGameViewModel = viewModel()) {
    val gameStatus by vm.gameStatus.observeAsState()
    val gameId by vm.currentGameId.observeAsState()

    LaunchedEffect(Unit) {
        vm.checkCurrentGame(playerId)
    }

    LaunchedEffect(gameStatus) {
        gameStatus?.let {
            gameId ?: return@LaunchedEffect
            when (it) {
                LobbyStatus.CREATED.value -> {
                    navController.navigate(NavRoutes.LobbyQR.route + "/$gameId")
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

    Surface {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CustomButton(text = "Create lobby") {
                    navController.navigate(NavRoutes.AvatarPicker.route + "/true")
                }
                Spacer(modifier = Modifier.height(50.dp))
                CustomButton(text = "Join lobby") {
                    navController.navigate(NavRoutes.AvatarPicker.route+ "/false")
                }
            }
            Box(modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(32.dp)) {
                Button(onClick = {
                    Firebase.auth.signOut()
                    navController.navigate(NavRoutes.MainScreen.route)
                }) {
                    Text("Log out")
                }
            }

        }

    }
}

class StartGameViewModel: ViewModel() {
    val TAG = "startGameVM"
    val firestore = FirestoreHelper
    val currentGameId = MutableLiveData<String>()
    val gameStatus = MutableLiveData<Int>()

    fun checkCurrentGame(playerId: String) {
        firestore.getUser(playerId).get()
            .addOnFailureListener {
                Log.e(TAG, "checkCurrentGame: ", it)
            }
            .addOnSuccessListener {
                val gameId = it.getString("currentGameId")
                gameId?.let { id ->
                    if (id.isNotBlank()) {
                        currentGameId.postValue(id)
                        checkGameStatus(id)
                    }
                }
            }
    }

    fun checkGameStatus(gameId: String) {
        firestore.getLobby(gameId).get()
            .addOnSuccessListener {
                val lobby = it.toObject(Lobby::class.java)
                lobby?.let { gameStatus.postValue(lobby.status) }
            }
    }
}
