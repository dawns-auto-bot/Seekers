package com.example.seekers

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.seekers.general.toGrayscale
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.google.maps.android.heatmaps.HeatmapTileProvider

@Composable
fun HeatMap(
    state: CameraPositionState,
    center: LatLng?,
    radius: Int?,
    properties: MapProperties,
    uiSettings: MapUiSettings,
    heatPositions: List<LatLng>,
    movingPlayers: List<Player>,
    tileProvider: HeatmapTileProvider,
    circleCoords: List<LatLng>,
    eliminatedPlayers: List<Player>
) {
    val context = LocalContext.current
    GoogleMap(
        cameraPositionState = state,
        properties = properties,
        uiSettings = uiSettings,
    ) {
        if (heatPositions.isNotEmpty()) {
            TileOverlay(
                tileProvider = tileProvider,
                transparency = 0.3f
            )
        }
        movingPlayers.forEach {
            val res = avatarList[it.avatarId]
            val bitmap = BitmapFactory.decodeResource(context.resources, res)
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 50, 50, false)
            Marker(
                state = MarkerState(
                    position = LatLng(
                        it.location.latitude,
                        it.location.longitude
                    )
                ),
                icon = BitmapDescriptorFactory.fromBitmap(resizedBitmap),
                title = it.nickname,
                visible = true,
                anchor = Offset(0.5f, 0.5f)
            )
        }
        eliminatedPlayers.forEach {
            val res = avatarList[it.avatarId]
            val bitmap = BitmapFactory.decodeResource(context.resources, res)
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 50, 50, false)
            val grayedBitmap = resizedBitmap.toGrayscale()
            Marker(
                state = MarkerState(
                    position = LatLng(
                        it.location.latitude,
                        it.location.longitude
                    )
                ),
                icon = BitmapDescriptorFactory.fromBitmap(grayedBitmap),
                title = it.nickname + " (eliminated)",
                visible = true,
                anchor = Offset(0.5f, 0.5f)
            )
        }
        if (center != null && radius != null) {
            if (circleCoords.isNotEmpty()) {
                Polygon(
                    points = getCornerCoords(center, radius),
                    fillColor = Color(0x8D000000),
                    holes = listOf(circleCoords),
                    strokeWidth = 0f,
                )
            }

            Circle(
                center = center,
                radius = radius.toDouble(),
                strokeColor = Color(0x8DBDA500),
            )
        }
    }
}