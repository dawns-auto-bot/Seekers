package com.example.seekers

import android.Manifest
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.seekers.general.*
import com.example.seekers.ui.theme.Powder
import com.example.seekers.ui.theme.Raisin
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@Composable
fun StartGameScreen(
    navController: NavController,
    permissionVM: PermissionsViewModel
) {
    val context = LocalContext.current
    var showLogOutDialog by remember { mutableStateOf(false) }
    val screenHeight = LocalConfiguration.current.screenHeightDp * 0.3

    LaunchedEffect(Unit) {
        permissionVM.checkAllPermissions(context)
    }

    Surface {
        Box(
            Modifier
                .fillMaxSize()
                .background(Powder)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(screenHeight.dp)
                        .padding(horizontal = 15.dp, vertical = 5.dp)
                        .clickable { navController.navigate(NavRoutes.AvatarPicker.route + "/true") },
                    elevation = 10.dp
                ) {
                    Box(Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(R.drawable.illustration1),
                            contentDescription = "illustration",
                            modifier = Modifier
                                .fillMaxSize(),
                            alignment = Alignment.CenterStart
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(32.dp)
                        ) {
                            Column {
                                Text(text = "CREATE\nLOBBY", fontSize = 22.sp)
                                Box(
                                    modifier = Modifier
                                        .width(50.dp)
                                        .height(1.dp)
                                        .background(color = Raisin)
                                )
                            }
                        }
                    }
                }
                // Spacer(modifier = Modifier.height(50.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(screenHeight.dp)
                        .padding(horizontal = 15.dp, vertical = 5.dp)

                        .clickable { navController.navigate(NavRoutes.AvatarPicker.route + "/false") },
                    elevation = 10.dp
                ) {
                    Box(Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(R.drawable.illustration2),
                            contentDescription = "illustration",
                            modifier = Modifier
                                .fillMaxSize(),
                            alignment = Alignment.CenterEnd
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(32.dp)
                        ) {
                            Column {
                                Text(text = "JOIN\nLOBBY", fontSize = 22.sp)
                                Box(
                                    modifier = Modifier
                                        .width(50.dp)
                                        .height(1.dp)
                                        .background(color = Raisin)
                                )
                            }
                        }
                    }

                    /*
                    CustomButton(text = "Join lobby") {
                        navController.navigate(NavRoutes.AvatarPicker.route + "/false")
                    } */
                }

            }
            Text(
                text = "${FirebaseHelper.uid}", modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(32.dp)
            ) {
                LogOutButton(text = "Log out") {
                    Firebase.auth.signOut()
                    println("logged user: ${Firebase.auth.currentUser}")
                    navController.navigate(NavRoutes.MainScreen.route)
                }
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