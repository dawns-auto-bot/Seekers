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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.seekers.general.CustomButton
import com.example.seekers.general.LogOutButton
import com.example.seekers.ui.theme.Powder
import com.example.seekers.ui.theme.Raisin
import com.example.seekers.general.getPermissionLauncher
import com.example.seekers.general.isPermissionGranted
import com.example.seekers.ui.theme.Emerald
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.*

@Composable
fun StartGameScreen(navController: NavController, vm: PermissionsViewModel = viewModel()) {
    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showLogOutDialog by remember { mutableStateOf(false) }
    val screenHeight = LocalConfiguration.current.screenHeightDp * 0.3
    var showTutorial by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.checkAllPermissions(context)
        if (!vm.allPermissionsAllowed()) {
            showPermissionDialog = true
        }
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
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((screenHeight*0.5).dp)
                        .padding(horizontal = 15.dp, vertical = 5.dp)
                        .clickable { showTutorial = true },
                    elevation = 10.dp
                ) {
                    Box(Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(R.drawable.tutorial_icon),
                            contentDescription = "tutorial",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            alignment = Alignment.CenterEnd
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(text = "QUICK-START\nGUIDE", fontSize = 22.sp)
                                Box(
                                    modifier = Modifier
                                        .width(80.dp)
                                        .height(1.dp)
                                        .background(color = Raisin)
                                )
                            }
                        }
                    }
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
        if (showPermissionDialog) {
            PermissionsDialog(onDismiss = { showPermissionDialog = false }, vm = vm)
        }
        if (showTutorial) {
            TutorialDialog() {
                showTutorial = false
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

@OptIn(ExperimentalPagerApi::class, ExperimentalComposeUiApi::class)
@Composable
fun TutorialDialog(onDismiss: () -> Unit) {
    val screenHeight = LocalConfiguration.current.screenHeightDp * .9
    val screenWidth = LocalConfiguration.current.screenWidthDp * .9
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(5.dp)
                .background(Powder)
                .fillMaxSize()) {
            val pagerState = rememberPagerState()

            // Display 10 items
            HorizontalPager(
                count = 5,
                state = pagerState,
                // Add 32.dp horizontal padding to 'center' the pages
                contentPadding = PaddingValues(horizontal = 32.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { page ->
                PagerSampleItem(
                    page = page,
                    modifier = Modifier
                        .fillMaxSize(),
                )
            }

            HorizontalPagerIndicator(
                pagerState = pagerState,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(16.dp),
            )


        }
    }

}


@Composable
internal fun PagerSampleItem(
    page: Int,
    modifier: Modifier = Modifier,
) {
    if(page == 0) {
        Column(modifier, verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Create a lobby to host your own game as the Seeker, and share the QR code for all your friends!", modifier = Modifier.padding(5.dp))
            Image(
                painter = painterResource(id = R.drawable.tutorial1),
                contentDescription = "tutorial",
                alignment = Alignment.Center
            )
            Text(text = "Or join a lobby of a friend with a QR code and get ready to hide!", modifier = Modifier.padding(5.dp))

        }
    } else if(page == 1) {
        Column(modifier, verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Remember to pick a funny nickname so your friends can recognize you!", modifier = Modifier.padding(5.dp))
            Image(
                painter = painterResource(id = R.drawable.tutorial2_1),
                contentDescription = "tutorial",
                alignment = Alignment.Center
            )
            Text(text = "And most importantly, a cute avatar", modifier = Modifier.padding(5.dp))
            Image(
                painter = painterResource(id = R.drawable.tutorial2_2),
                contentDescription = "tutorial",
                alignment = Alignment.Center
            )
        }
    } else if(page == 2) {
        Column(modifier, verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Set the rules for your game", modifier = Modifier.padding(5.dp))
            Image(
                painter = painterResource(id = R.drawable.tutorial3),
                contentDescription = "tutorial",
                alignment = Alignment.Center
            )
            Text(text = "And don't forget to define the play area before creating the lobby!", modifier = Modifier.padding(5.dp))

        }
    } else if(page == 3) {
        Column(modifier, verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Setting the play area is simple", modifier = Modifier.padding(5.dp))
            Image(
                painter = painterResource(id = R.drawable.tutorial4),
                contentDescription = "tutorial",
                alignment = Alignment.Center
            )
            Text(text = "Just move the camera to a favorable position, set the radius with the vertical slider and you're good to go!", modifier = Modifier.padding(5.dp))
        }
    } else if(page == 4) {
        Column(modifier, verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "While waiting for your friends, open up the QR code for them to scan from your phone", modifier = Modifier.padding(5.dp))
            Image(
                painter = painterResource(id = R.drawable.tutorial5_1),
                contentDescription = "tutorial",
                alignment = Alignment.Center
            )
            Text(text = "In case you want to change the rules, here's the option to do that!", modifier = Modifier.padding(5.dp))
            Image(
                painter = painterResource(id = R.drawable.tutorial5_3),
                contentDescription = "tutorial",
                alignment = Alignment.Center
            )
            Text(text = "Now start the game and off you go, hide quickly!", modifier = Modifier.padding(5.dp))
        }
    }
}

class PermissionsViewModel : ViewModel() {
    val coarseLocPerm = MutableLiveData<Boolean>()
    val fineLocPerm = MutableLiveData<Boolean>()
    val backgroundLocPerm = MutableLiveData<Boolean>()
    val cameraPerm = MutableLiveData<Boolean>()
    val activityRecPerm = MutableLiveData<Boolean>()
    val foregroundServPerm = MutableLiveData<Boolean>()

    fun getLiveData(requiredPermission: RequiredPermission): MutableLiveData<Boolean> {
        return when (requiredPermission) {
            RequiredPermission.COARSE_LOCATION -> coarseLocPerm
            RequiredPermission.FINE_LOCATION -> fineLocPerm
            RequiredPermission.BACKGROUND_LOCATION -> backgroundLocPerm
            RequiredPermission.CAMERA -> cameraPerm
            RequiredPermission.ACTIVITY_RECOGNITION -> activityRecPerm
            RequiredPermission.FOREGROUND_SERVICE -> foregroundServPerm
        }
    }

    fun checkAllPermissions(context: Context) {
        RequiredPermission.values().forEach {
            getLiveData(it).value = isPermissionGranted(context, it.value)
        }
    }

    fun allPermissionsAllowed(): Boolean {
        return coarseLocPerm.value == true
                && fineLocPerm.value == true
                && backgroundLocPerm.value == true
                && cameraPerm.value == true
                && activityRecPerm.value == true
                && foregroundServPerm.value == true
    }
}

enum class RequiredPermission(val value: String, val text: String, val explanation: String) {
    COARSE_LOCATION(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        "\uD83D\uDCCD Coarse location",
        "to locate other players on the map"
    ),
    FINE_LOCATION(
        Manifest.permission.ACCESS_FINE_LOCATION,
        "\uD83D\uDCCD Fine location",
        "to get accurate locations"
    ),
    BACKGROUND_LOCATION(
        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        "\uD83D\uDCCD Background location (All the time)",
        "to send locations even when your screen is off"
    ),
    CAMERA(
        Manifest.permission.CAMERA, "\uD83D\uDCF7 Camera",
        "to scan QR codes and take pictures"
    ),
    ACTIVITY_RECOGNITION(
        Manifest.permission.ACTIVITY_RECOGNITION,
        "\uD83D\uDC5F Activity recognition",
        "to count your steps"
    ),
    FOREGROUND_SERVICE(
        Manifest.permission.FOREGROUND_SERVICE, "\uD83D\uDD14 Foreground Service",
        "to keep the game on even when your screen is off"
    ),
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PermissionsDialog(onDismiss: () -> Unit, vm: PermissionsViewModel) {
    val coarseLocPerm by vm.coarseLocPerm.observeAsState()
    val fineLocPerm by vm.fineLocPerm.observeAsState()
    val backgroundLocPerm by vm.backgroundLocPerm.observeAsState()
    val cameraPerm by vm.cameraPerm.observeAsState()
    val activityRecPerm by vm.activityRecPerm.observeAsState()
    val foregroundServPerm by vm.foregroundServPerm.observeAsState()
    val allPermissionsOK by remember {
        derivedStateOf {
            coarseLocPerm == true
                    && fineLocPerm == true
                    && backgroundLocPerm == true
                    && cameraPerm == true
                    && activityRecPerm == true
                    && foregroundServPerm == true
        }
    }

    var permLaunchers: List<ManagedActivityResultLauncher<String, Boolean>> by remember {
        mutableStateOf(
            listOf()
        )
    }
    permLaunchers = getLauncherList(vm = vm)

    fun getPermissionValue(requiredPermission: RequiredPermission): Boolean? {
        return when (requiredPermission) {
            RequiredPermission.COARSE_LOCATION -> coarseLocPerm
            RequiredPermission.FINE_LOCATION -> fineLocPerm
            RequiredPermission.BACKGROUND_LOCATION -> backgroundLocPerm
            RequiredPermission.CAMERA -> cameraPerm
            RequiredPermission.ACTIVITY_RECOGNITION -> activityRecPerm
            RequiredPermission.FOREGROUND_SERVICE -> foregroundServPerm
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(backgroundColor = Color.White, modifier = Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Permissions needed",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Please press on the missing permissions to allow them",
                        fontSize = 16.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )
                }
                Card(
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color.White,
                    elevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clipToBounds()
                    ) {
                        RequiredPermission.values().forEach {
                            PermissionTile(
                                permission = it.text,
                                isAllowed = getPermissionValue(it) == true,
                                onClick = {
                                    permLaunchers[it.ordinal].launch(it.value)
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
                CustomButton(
                    modifier = Modifier.alpha(if (!allPermissionsOK) 0f else 1f),
                    text = "Continue"
                ) {
                    if (allPermissionsOK) {
                        onDismiss()
                    }
                }
            }
        }
    }
}

@Composable
fun getLauncherList(vm: PermissionsViewModel): MutableList<ManagedActivityResultLauncher<String, Boolean>> {
    val launcherList = mutableListOf<ManagedActivityResultLauncher<String, Boolean>>()
    RequiredPermission.values().forEach { perm ->
        val launcher = getPermissionLauncher(onResult = { vm.getLiveData(perm).value = it })
        launcherList.add(launcher)
    }
    return launcherList
}

@Composable
fun PermissionTile(permission: String, isAllowed: Boolean, onClick: () -> Unit) {
    val iconVector =
        if (isAllowed) Icons.Filled.CheckCircleOutline else Icons.Filled.ErrorOutline
    val color = if (isAllowed) Color.Green else Color.Red
    Column(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = permission)
            Icon(
                imageVector = iconVector,
                contentDescription = "isAllowed: $isAllowed",
                tint = color
            )
        }
        Spacer(modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.LightGray))
    }

}