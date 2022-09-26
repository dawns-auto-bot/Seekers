package com.example.seekers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.seekers.general.CustomButton

@Composable
fun StartAndJoinBtns(navController: NavController) {
    Surface() {
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
                navController.navigate(NavRoutes.JoinLobby.route)
            }
        }
    }
}
