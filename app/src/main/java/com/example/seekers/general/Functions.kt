package com.example.seekers.general

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.github.g0dkar.qrcode.QRCode
import java.io.ByteArrayOutputStream

fun generateQRCode(gameId: String): Bitmap {
    val fileOut = ByteArrayOutputStream()

    QRCode(gameId)
        .render(cellSize = 50, margin = 25)
        .writeImage(fileOut)

    val imageBytes = fileOut.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}

fun getLocationPermission(context: Context) {
    if ((ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED)
    ) {
        ActivityCompat.requestPermissions(
            context as Activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            0
        )
    }
}

fun getActivityRecognitionPermission(context: Context) {
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_DENIED
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(android.Manifest.permission.ACTIVITY_RECOGNITION),
                1
            )
        }
    }
}

@Composable
fun CustomOutlinedTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    focusManager: FocusManager,
    label: String,
    placeholder: String,
    trailingIcon: @Composable() (() -> Unit)? = null,
    keyboardType: KeyboardType,
    passwordVisible: Boolean? = null
) {
    val width = LocalConfiguration.current.screenWidthDp * 0.8
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Done,
            keyboardType = keyboardType
        ),
        keyboardActions = KeyboardActions(
            onDone = { focusManager.clearFocus() }),
        label = { Text(text = label) },
        placeholder = { Text(text = placeholder) },
        visualTransformation = if (passwordVisible == true || passwordVisible == null) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = trailingIcon,
        modifier = Modifier
            .width(width.dp)
    )
}

@Composable
fun CustomButton(modifier: Modifier = Modifier, text: String, onClick: () -> Unit) {
    val width = LocalConfiguration.current.screenWidthDp * 0.8
    Button(
        onClick = onClick,
        modifier = modifier
            .width(width.dp)
            .height(50.dp)
            .clip(RoundedCornerShape(10.dp)),
        colors = ButtonDefaults.buttonColors(Color.LightGray, contentColor = Color.Black)
    ) {
        Text(text = text)
    }
}

@Composable
fun IconButton(
    modifier: Modifier = Modifier,
    resourceId: Int,
    buttonText: String,
    buttonColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp)),
        colors = ButtonDefaults.buttonColors(buttonColor, contentColor = Color.Black),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterStart)
            ) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(id = resourceId),
                    modifier = Modifier
                        .size(18.dp),
                    contentDescription = "drawable_icons",
                    tint = Color.Unspecified
                )
            }
            Text(text = buttonText)
        }
    }
}

@Composable
fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 50f..200f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors()
) {
    Column() {
        // Text(text = value.toString(), fontSize = 10.sp)
        Slider(
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF838383),
                activeTrackColor = Color(0xFF838383),
                inactiveTrackColor = Color.LightGray
            ),
            onValueChangeFinished = onValueChangeFinished,
            steps = steps,
            valueRange = valueRange,
            enabled = enabled,
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .graphicsLayer {
                    rotationZ = 270f
                    transformOrigin = TransformOrigin(0f, 0f)
                }
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(
                        Constraints(
                            minWidth = constraints.minHeight,
                            maxWidth = constraints.maxHeight,
                            minHeight = constraints.minWidth,
                            maxHeight = constraints.maxHeight,
                        )
                    )
                    layout(placeable.height, placeable.width) {
                        placeable.place(-placeable.width, 0)
                    }
                }
                .then(modifier)
        )
    }
}