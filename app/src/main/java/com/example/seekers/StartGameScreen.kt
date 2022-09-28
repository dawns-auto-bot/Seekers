package com.example.seekers

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Surface
import androidx.compose.material.Text
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

@Composable
fun StartAndJoinBtns(navController: NavController, vm: StartGameViewModel = viewModel()) {
    val currentGameId by vm.currentGameId.observeAsState()

    LaunchedEffect(Unit) {
        vm.checkCurrentGame(playerId)
    }

    LaunchedEffect(currentGameId) {
        currentGameId?.let {
            if (it.isNotBlank()) {
                navController.navigate(NavRoutes.LobbyQR.route + "/$it")
            }
        }
    }

    Surface {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CustomButton(text = "Create lobby") {
                navController.navigate(NavRoutes.LobbyCreation.route)
            }
            Spacer(modifier = Modifier.height(50.dp))
            CustomButton(text = "Join lobby") {
                navController.navigate(NavRoutes.AvatarPicker.route)
            }
        }
    }
}

class StartGameViewModel: ViewModel() {
    val TAG = "startGameVM"
    val firestore = FirestoreHelper
    val currentGameId = MutableLiveData<String>()

    fun checkCurrentGame(playerId: String) {
        firestore.getUser(playerId).get()
            .addOnFailureListener {
                Log.e(TAG, "checkCurrentGame: ", it)
            }
            .addOnSuccessListener {
                val gameId = it.get("currentGameId") as String?
                currentGameId.postValue(gameId)
            }
    }
}
