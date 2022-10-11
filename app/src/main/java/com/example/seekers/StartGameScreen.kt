package com.example.seekers

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.seekers.general.CustomButton
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StartGameScreen(navController: NavController) {
    val context = LocalContext.current
    var showLogOutDialog by remember { mutableStateOf(false) }

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
                    showLogOutDialog = true
                }) {
                    Text("Log out")
                }
            }
        }
        if (showLogOutDialog) {
            LogOutDialog(onDismissRequest = { showLogOutDialog = false }, onConfirm = {
                Firebase.auth.signOut()
                println("logged user: ${Firebase.auth.currentUser}")
                navController.navigate(NavRoutes.MainScreen.route)
            })
        }
        BackHandler(enabled = true) {
            showLogOutDialog = true
        }
    }
}

@Composable
fun LogOutDialog(onDismissRequest: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        title = { Text(text = "Logging out?") },
        text = { Text(text = "Are you sure you want to log out?") },
        onDismissRequest = onDismissRequest,
        dismissButton = {
            Button(onClick = { onDismissRequest() }) {
                Text(text = "Cancel")
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm() }) {
                Text(text = "Log Out")
            }
        }
    )
}
