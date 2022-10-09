package com.example.seekers

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.seekers.general.CustomButton
import com.example.seekers.general.VerticalSlider
import com.example.seekers.general.getPermissionLauncher
import com.example.seekers.ui.theme.*
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint
import com.google.maps.android.compose.*
import java.time.temporal.ValueRange
import java.util.*

@SuppressLint("MissingPermission")
@Composable
fun AreaSelectionMap(
    vm: LobbyCreationScreenViewModel = viewModel(),
    properties: MapProperties,
    settings: MapUiSettings,
    state: CameraPositionState
) {
    val radius by vm.radius.observeAsState(50)
    Box(
        Modifier.fillMaxSize()
    ) {
        GoogleMap(
            cameraPositionState = state,
            properties = properties,
            uiSettings = settings
        ) {
            Circle(
                center = LatLng(
                    state.position.target.latitude,
                    state.position.target.longitude
                ),
                radius = radius.toDouble(),
                fillColor = Color(0x19FFDE00),
                strokeColor = Color(0x8DBDA500),
                clickable = true
            )
        }
        Column(Modifier.align(Alignment.BottomCenter)) {
            CustomButton(
                modifier = Modifier.width(150.dp),
                text = "Define Area"
            ) {
                vm.updateCenter(
                    LatLng(
                        state.position.target.latitude,
                        state.position.target.longitude
                    )
                )
                vm.updateShowMap(false)
            }
            Spacer(Modifier.height(15.dp))
        }
        Row(Modifier.align(Alignment.CenterEnd)) {
            VerticalSlider(
                value = radius.toFloat(),
                onValueChange = {
                    vm.updateRadius(it.toInt())
                },
                valueRange = 20f..500f,
                modifier = Modifier
                    .width(200.dp)
                    .height(50.dp)
            )
        }
    }
}
