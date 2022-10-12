package com.example.seekers.general

import android.content.Context
import android.graphics.*
import android.util.Log
import android.util.Size
import android.widget.FrameLayout
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.budiyev.android.codescanner.*
import com.example.seekers.ui.theme.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.ktx.utils.withSphericalOffset
import io.github.g0dkar.qrcode.QRCode
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.math.*

fun generateQRCode(data: String): Bitmap {
    val fileOut = ByteArrayOutputStream()

    QRCode(data)
        .render(cellSize = 50, margin = 25)
        .writeImage(fileOut)

    val imageBytes = fileOut.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
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
    passwordVisible: Boolean? = null,
    isError: Boolean = false,
    emailIsAvailable: Boolean? = null,
    modifier: Modifier? = null
) {

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        isError = isError,
        colors = TextFieldDefaults.outlinedTextFieldColors(
            errorBorderColor = SizzlingRed,
            errorLabelColor = SizzlingRed,
            errorCursorColor = SizzlingRed,
            errorLeadingIconColor = SizzlingRed,
            errorTrailingIconColor = SizzlingRed,
            focusedBorderColor = if (emailIsAvailable == true) emailAvailable else Raisin,
            focusedLabelColor = if (emailIsAvailable == true) emailAvailable else Raisin,
            unfocusedBorderColor = if (emailIsAvailable == true) emailAvailable else Raisin,
            unfocusedLabelColor = if (emailIsAvailable == true) emailAvailable else Raisin,
            trailingIconColor = if (emailIsAvailable == true) emailAvailable else Raisin
        ),
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
        modifier = modifier ?: Modifier
    )
}

fun isEmailValid(email: String) :Boolean {
    val EMAIL_REGEX = "^[A-Za-z](.*)([@]{1})(.{1,})(\\.)(.{1,})"
    val result = EMAIL_REGEX.toRegex().matches(email)
    Log.d("validation", result.toString())
    return result
}
fun isPasswordValid(password: String) :Boolean {
    val PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d]{8,}$"
    val result = PASSWORD_REGEX.toRegex().matches(password)
    Log.d("validation", result.toString())
    return result
}

@Composable
fun QRScanner(context: Context, onScanned: (String) -> Unit) {
    val scannerView = CodeScannerView(context)
    val codeScanner = CodeScanner(context, scannerView)
    codeScanner.camera = CodeScanner.CAMERA_BACK
    codeScanner.formats = CodeScanner.ALL_FORMATS
    codeScanner.autoFocusMode = AutoFocusMode.SAFE
    codeScanner.scanMode = ScanMode.SINGLE
    codeScanner.isAutoFocusEnabled = true
    codeScanner.isFlashEnabled = false
    codeScanner.decodeCallback = DecodeCallback {
        codeScanner.stopPreview()
        codeScanner.releaseResources()
        onScanned(it.text)
    }
    codeScanner.errorCallback = ErrorCallback { // or ErrorCallback.SUPPRESS
        Log.e("qrScanner", "QrScannerScreen: ", it)
    }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            val layout = FrameLayout(it)
            layout.addView(scannerView)
            codeScanner.startPreview()
            layout
        })
}

@Composable
fun CustomButton(modifier: Modifier = Modifier, text: String, onClick: () -> Unit) {
    val width = LocalConfiguration.current.screenWidthDp * 0.8
    Button(
        border = BorderStroke(1.dp, Raisin),
        onClick = onClick,
        modifier = modifier
            .width(width.dp)
            .height(50.dp),
        shape = RoundedCornerShape(15),
        colors = ButtonDefaults.outlinedButtonColors(Emerald, contentColor = Raisin)
    ) {
        Text(text = text.uppercase(Locale.ROOT))
    }
}

@Composable
fun LogOutButton(modifier: Modifier = Modifier, text: String, onClick: () -> Unit) {
    Button(
        border = BorderStroke(1.dp, Color.White),
        onClick = onClick,
        modifier = modifier
            .width(150.dp)
            .height(50.dp),
        shape = RoundedCornerShape(15),
        colors = ButtonDefaults.outlinedButtonColors(SizzlingRed, contentColor = Color.White)
    ) {
        Icon(Icons.Default.ArrowBack, contentDescription = "", tint = Color.White)
        Text(text = text.uppercase(Locale.ROOT))
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
        border = BorderStroke(1.dp, Raisin),
        onClick = onClick,
        modifier = modifier
            .height(50.dp),
        shape = RoundedCornerShape(15),
        colors = ButtonDefaults.buttonColors(buttonColor, contentColor = Raisin),
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
            Text(text = buttonText.uppercase(Locale.ROOT))
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
                thumbColor = Emerald,
                activeTrackColor = Emerald,
                inactiveTrackColor = Raisin
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

@Composable
fun QRCodeComponent(modifier: Modifier = Modifier, bitmap: Bitmap) {
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "QR",
        modifier = modifier.size(250.dp)
    )
}

fun Bitmap.toGrayscale():Bitmap{

    val matrix = ColorMatrix().apply {
        setSaturation(0f)
    }
    val filter = ColorMatrixColorFilter(matrix)

    val paint = Paint().apply {
        colorFilter = filter
    }

    Canvas(this).drawBitmap(this, 0f, 0f, paint)
    return this
}

fun secondsToText(seconds: Int): String {
    if (seconds == 0) {
        return "Time's up!"
    }
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds - hours * 3600 - minutes * 60

    if (seconds < 3600) {
        return "${minutes.toTimeString()}:${secs.toTimeString()}"
    }

    return "${hours.toTimeString()}:${minutes.toTimeString()}:${secs.toTimeString()}"
}

fun Int.toTimeString() = if (this < 10) "0$this" else this.toString()

fun getBounds(center: LatLng, radius: Int): LatLngBounds {
    val multiplier = cos(PI / 4)
    val sw = center.withSphericalOffset(radius.div(multiplier), 225.0)
    val ne = center.withSphericalOffset(radius.div(multiplier), 45.0)
    return LatLngBounds(sw, ne)
}

fun getCornerCoords(center: LatLng, radius: Int): List<LatLng> {
    val ne = center.withSphericalOffset(radius * 10.0, 45.0)
    val se = center.withSphericalOffset(radius * 10.0, 135.0)
    val sw = center.withSphericalOffset(radius * 10.0, 225.0)
    val nw = center.withSphericalOffset(radius * 10.0, 315.0)
    return listOf(ne, se, sw, nw)
}

fun getCircleCoords(center: LatLng, radius: Int): List<LatLng> {
    val list = mutableListOf<LatLng>()
    (0..360).forEach {
        list.add(center.withSphericalOffset(radius.toDouble() + 1.0, it.toDouble()))
    }
    return list
}

@Composable
fun AvatarIcon(modifier: Modifier = Modifier, imgModifier: Modifier = Modifier, resourceId: Int) {
    Card(
        shape = CircleShape,
        border = BorderStroke(2.dp, Color.Black),
        backgroundColor = avatarBackground,
        modifier = modifier
    ) {
        Image(
            painter = painterResource(id = resourceId),
            contentDescription = "avatar",
            modifier = imgModifier
        )
    }
}

//source: https://stackoverflow.com/questions/6048975/google-maps-v3-how-to-calculate-the-zoom-level-for-a-given-bounds
fun getBoundsZoomLevel(bounds: LatLngBounds, mapDim: Size): Double {
    val WORLD_DIM = Size(256, 256)
    val ZOOM_MAX = 21.toDouble()

    fun latRad(lat: Double): Double {
        val sin = sin(lat * Math.PI / 180)
        val radX2 = ln((1 + sin) / (1 - sin)) / 2
        return max(min(radX2, Math.PI), -Math.PI) / 2
    }

    fun zoom(mapPx: Int, worldPx: Int, fraction: Double): Double {
        return floor(ln(mapPx / worldPx / fraction) / ln(2.0))
    }

    val ne = bounds.northeast
    val sw = bounds.southwest

    val latFraction = (latRad(ne.latitude) - latRad(sw.latitude)) / Math.PI

    val lngDiff = ne.longitude - sw.longitude
    val lngFraction = if (lngDiff < 0) {
        (lngDiff + 360) / 360
    } else {
        (lngDiff / 360)
    }

    val latZoom = zoom(mapDim.height, WORLD_DIM.height, latFraction)
    val lngZoom = zoom(mapDim.width, WORLD_DIM.width, lngFraction)

    return minOf(latZoom, lngZoom, ZOOM_MAX)
}

