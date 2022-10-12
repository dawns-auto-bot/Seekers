package com.example.seekers

import android.Manifest
import android.app.Application
import android.content.Context
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.seekers.general.CustomButton
import com.example.seekers.general.getPermissionLauncher
import com.example.seekers.general.isPermissionGranted
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StartGameScreen(navController: NavController, vm: PermissionsViewModel = viewModel()) {
    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.checkAllPermissions(context)
        if (!vm.allPermissionsAllowed()) {
            showPermissionDialog = true
        }
    }

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
                Button(onClick = {
                    Firebase.auth.signOut()
                    println("logged user: ${Firebase.auth.currentUser}")
                    navController.navigate(NavRoutes.MainScreen.route)
                }) {
                    Text("Log out")
                }
            }
        }
        if (showPermissionDialog) {
            PermissionsDialog(onDismiss = { showPermissionDialog = false }, vm = vm)
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
                Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth(), backgroundColor = Color.White, elevation = 4.dp) {
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
                CustomButton(modifier = Modifier.alpha(if (!allPermissionsOK) 0f else 1f), text = "Continue") {
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
            Icon(imageVector = iconVector, contentDescription = "isAllowed: $isAllowed", tint = color)
        }
        Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.LightGray))
    }
    
}