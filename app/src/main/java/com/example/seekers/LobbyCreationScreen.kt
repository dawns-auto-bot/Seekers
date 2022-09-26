package com.example.seekers

import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable
fun TestCreateLobby(navController: NavController) {
    CustomButton(text = "Create Lobby") {
     navController.navigate(NavRoutes.LobbyCreationQR.route)
    }
}