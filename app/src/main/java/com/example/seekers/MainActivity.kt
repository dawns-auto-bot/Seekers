package com.example.seekers

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.seekers.ui.theme.SeekersTheme
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.seekers.general.getLocationPermission
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        getLocationPermission(this)
        setContent {
            SeekersTheme {
                MyAppNavHost()
            }
        }
    }
}

@Composable
fun MyAppNavHost() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavRoutes.MainScreen.route
    ) {
        composable(NavRoutes.MainScreen.route) {
            MainScreen(navController)
        }
        composable(NavRoutes.StartGame.route) {
            StartGameScreen(navController)
        }
        // Avatar picker screen
        composable(
            NavRoutes.AvatarPicker.route + "/{isCreator}",
            arguments = listOf(
                navArgument("isCreator") { type = NavType.BoolType }
            )
        ) {
            val isCreator = it.arguments!!.getBoolean("isCreator")
            AvatarPickerScreen(navController = navController, isCreator = isCreator)
        }
        composable(NavRoutes.LobbyCreation.route + "/{nickname}/{avatarId}",
            arguments = listOf(
                navArgument("nickname") {
                    type = NavType.StringType
                },
                navArgument("avatarId") {
                    type = NavType.IntType
                }
            )) {
            val nickname = it.arguments!!.getString("nickname")!!
            val avatarId = it.arguments!!.getInt("avatarId")
            LobbyCreationScreen(navController = navController, nickname = nickname, avatarId = avatarId)
        }

        //Lobby screen with QR
        composable(
            NavRoutes.LobbyQR.route + "/{gameId}",
            arguments = listOf(
                navArgument("gameId") { type = NavType.StringType },
            )
        ) {
            val gameId = it.arguments!!.getString("gameId")!!
            LobbyQRScreen(navController = navController, gameId = gameId)
        }
        //QR Scanner
        composable(NavRoutes.Scanner.route + "/{nickname}/{avatarId}",
            arguments = listOf(
                navArgument("nickname") {
                    type = NavType.StringType
                },
                navArgument("avatarId") {
                    type = NavType.IntType
                }
            )
        ) {
            val nickname = it.arguments!!.getString("nickname")!!
            val avatarId = it.arguments!!.getInt("avatarId")
            QrScannerScreen(navController, nickname = nickname, avatarId = avatarId)
        }
    }
}

@Composable
fun LoginBtn(navController: NavController) {
    Button(onClick = {
        navController.navigate(NavRoutes.StartGame.route)
    }) {
        Text(text = "fake login button")
    }
}

@Composable
fun MainScreen(navController: NavController) {
    LoginBtn(navController)
}
