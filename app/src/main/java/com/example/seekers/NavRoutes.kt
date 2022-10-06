package com.example.seekers

sealed class NavRoutes(val route: String) {
    object StartGame: NavRoutes("StartGame")
    object MainScreen: NavRoutes("MainScreen")
    object LobbyCreation: NavRoutes("LobbyCreation")
    object AvatarPicker: NavRoutes("AvatarPicker")
    object LobbyQR: NavRoutes("LobbyQR")
    object Scanner: NavRoutes("Scanner")
    object Radar: NavRoutes("Radar")
    object Countdown: NavRoutes("Countdown")
    object Heatmap: NavRoutes("Heatmap")
}
