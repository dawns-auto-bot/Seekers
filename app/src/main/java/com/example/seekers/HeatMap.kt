package com.example.seekers

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.seekers.general.getCornerCoords
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
    movingPlayers: List<Player>,
    tileProvider: HeatmapTileProvider?,
    circleCoords: List<LatLng>,
) {
    val context = LocalContext.current
    GoogleMap(
        cameraPositionState = state,
        properties = properties,
        uiSettings = uiSettings,
    ) {
        tileProvider?.let {
            TileOverlay(
                tileProvider = it,
                transparency = 0.3f
            )
        }

        movingPlayers.forEach {
            val res = avatarListWithBg[it.avatarId]
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