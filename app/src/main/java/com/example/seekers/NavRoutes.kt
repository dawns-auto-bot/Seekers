package com.example.seekers

sealed class NavRoutes(val route: String) {
    object StartGame: NavRoutes("StartGame")
    object MainScreen: NavRoutes("MainScreen")
    object LobbyCreation: NavRoutes("LobbyCreation")
    object JoinLobby: NavRoutes("JoinLobby")
    object Lobby: NavRoutes("Lobby")
    object Scanner: NavRoutes("Scanner")
}
