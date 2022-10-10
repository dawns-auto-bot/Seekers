package com.example.seekers

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.example.seekers.general.QRScanner

@Composable
fun QrScannerScreen(navController: NavHostController, nickname: String, avatarId: Int) {
    val context = LocalContext.current
    var cameraIsAllowed by remember { mutableStateOf(false) }
    var gameId: String? by remember { mutableStateOf(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            cameraIsAllowed = true
            Log.d("qrScreen", "PERMISSION GRANTED")

        } else {
            cameraIsAllowed = false
            Log.d("qrScreen", "PERMISSION DENIED")
        }
    }

    LaunchedEffect(Unit) {
        if (context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            launcher.launch(Manifest.permission.CAMERA)
        } else {
            cameraIsAllowed = true
        }
    }

    LaunchedEffect(gameId) {
        gameId?.let {
            val firestore = FirebaseHelper
            val player = Player(
                nickname = nickname,
                avatarId = avatarId,
                playerId = FirebaseHelper.uid!!,
                inLobbyStatus = InLobbyStatus.JOINED.value,
                inGameStatus = InGameStatus.PLAYER.value
            )
            firestore.addPlayer(player, it)
            firestore.updateUser(
                FirebaseHelper.uid!!,
                mapOf(Pair("currentGameId", it))
            )
            navController.navigate(NavRoutes.LobbyQR.route + "/$it")
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


