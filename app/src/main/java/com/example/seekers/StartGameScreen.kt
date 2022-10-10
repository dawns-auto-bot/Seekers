package com.example.seekers

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.seekers.general.CustomButton
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StartGameScreen(navController: NavController) {

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
                    navController.navigate(NavRoutes.AvatarPicker.route + "/false")
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(32.dp)
            ) {
                Button(onClick = {
                    Firebase.auth.signOut()
                    println("logged user: ${Firebase.auth.currentUser}")
                    navController.navigate(NavRoutes.MainScreen.route)
                }) {
                    Text("Log out")
                }
            }
        }
    }
}
