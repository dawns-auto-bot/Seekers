package com.example.seekers

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.CodeScannerView
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.budiyev.android.codescanner.ScanMode

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
            Log.d("qrScreen","PERMISSION GRANTED")

        } else {
            cameraIsAllowed = false
            Log.d("qrScreen","PERMISSION DENIED")
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
            val firestore = FirestoreHelper
            val player = Player(nickname, avatarId, "159342", PlayerStatus.JOINED)
            firestore.addPlayer(player, it)
            navController.navigate(NavRoutes.LobbyQR.route + "/$it/false")
        }
    }
    
    if (cameraIsAllowed) {
        val scannerView = CodeScannerView(context)
        val codeScanner = CodeScanner(context, scannerView)
        codeScanner.camera = CodeScanner.CAMERA_BACK
        codeScanner.formats = CodeScanner.ALL_FORMATS
        codeScanner.autoFocusMode = AutoFocusMode.SAFE
        codeScanner.scanMode = ScanMode.SINGLE
        codeScanner.isAutoFocusEnabled = true
        codeScanner.isFlashEnabled = false
        codeScanner.decodeCallback = DecodeCallback {
            codeScanner.stopPreview()
            codeScanner.releaseResources()
            gameId = it.text
        }
        codeScanner.errorCallback = ErrorCallback { // or ErrorCallback.SUPPRESS
            Log.e("qrScanner", "QrScannerScreen: ", it)
        }
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                val layout = FrameLayout(it)
                layout.addView(scannerView)
                codeScanner.startPreview()
                layout
            })
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Need camera permission")
        }
    }
}


