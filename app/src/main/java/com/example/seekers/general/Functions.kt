package com.example.seekers.general

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Build
import android.util.Log
import android.widget.FrameLayout
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.CodeScannerView
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.budiyev.android.codescanner.ScanMode
import com.example.seekers.ui.theme.*
import io.github.g0dkar.qrcode.QRCode
import java.io.ByteArrayOutputStream
import java.util.*

fun generateQRCode(data: String): Bitmap {
    val fileOut = ByteArrayOutputStream()

    QRCode(data)
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

@Composable
fun getPermissionLauncher(onResult: (Boolean) -> Unit): ManagedActivityResultLauncher<String, Boolean> {
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = onResult
    )
}

@Composable
fun PermissionDialog(onDismiss: () -> Unit, onContinue: () -> Unit, title: String, text: String) {
    Dialog(onDismissRequest = onDismiss) {
        Card(backgroundColor = Color.White, shape = RoundedCornerShape(8.dp)) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = title, style = MaterialTheme.typography.h6)
                Text(text = text)
                CustomButton(text = "Continue") {
                    onContinue()
                    onDismiss()
                }
            }
        }
    }
}
