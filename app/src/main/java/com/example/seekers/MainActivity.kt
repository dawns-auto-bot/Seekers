package com.example.seekers

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.seekers.ui.theme.SeekersTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
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
            StartAndJoinBtns(navController)
        }
        composable(NavRoutes.JoinLobby.route) {
            TestJoinLobby()
        }
        composable(NavRoutes.LobbyCreation.route) {
            TestCreateLobby(navController)
        }
        composable(NavRoutes.LobbyCreationQR.route) {
            MainQRScreen()
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
