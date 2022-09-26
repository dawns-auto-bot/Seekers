package com.example.seekers

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.firestore.GeoPoint
import io.github.g0dkar.qrcode.QRCode
import java.io.ByteArrayOutputStream

@Composable
fun LobbyCreationScreen(vm: LobbyCreationScreenViewModel = viewModel()) {
    val maxPlayers by vm.maxPlayers.observeAsState()
    val timeLimit by vm.timeLimit.observeAsState()
    val radius by vm.radius.observeAsState()
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(id = R.string.lobby_creation),
            style = MaterialTheme.typography.h6
        )
        CreationForm(vm = vm)
        CustomButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.create_lobby)
        ) {
            if (maxPlayers != null && timeLimit != null && radius != null) {
                val geoPoint = GeoPoint(60.224165, 24.758388)
                val lobby = Lobby("", geoPoint, maxPlayers!!, timeLimit!!, radius!!)
                val gameId = vm.addLobby(lobby)
                println(gameId)
                val bitmap = generateQRCode(gameId)
                println(bitmap.toString())
                vm.addQr(bitmap, gameId)
            }
        }
    }
}

class LobbyCreationScreenViewModel(application: Application) : AndroidViewModel(application) {
    val firestore = FirestoreHelper
    val storage = FirebaseStorageHelper
    val maxPlayers = MutableLiveData<Int>()
    val timeLimit = MutableLiveData<Int>()
    val radius = MutableLiveData<Int>()

    fun updateMaxPlayers(newVal: Int?) {
        maxPlayers.value = newVal
    }

    fun updateTimeLimit(newVal: Int?) {
        timeLimit.value = newVal
    }

    fun updateRadius(newVal: Int?) {
        radius.value = newVal
    }

    fun addLobby(lobby: Lobby) = firestore.addLobby(lobby)

    fun addQr(bitmap: Bitmap, name: String) = storage.uploadImg(bitmap, name)

}

@Composable
fun CreationForm(vm: LobbyCreationScreenViewModel) {
    val maxPlayers by vm.maxPlayers.observeAsState()
    val timeLimit by vm.timeLimit.observeAsState()
    val radius by vm.radius.observeAsState()

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Input(
            title = stringResource(id = R.string.max_players),
            value = maxPlayers?.toString() ?: "",
            keyboardType = KeyboardType.Number,
            onChangeValue = { vm.updateMaxPlayers(it.toIntOrNull()) })

        Input(
            title = stringResource(id = R.string.time_limit),
            value = timeLimit?.toString() ?: "",
            keyboardType = KeyboardType.Number,
            onChangeValue = { vm.updateTimeLimit(it.toIntOrNull()) })

        Input(
            title = stringResource(id = R.string.radius),
            value = radius?.toString() ?: "",
            keyboardType = KeyboardType.Number,
            onChangeValue = { vm.updateRadius(it.toIntOrNull()) })
    }
}

@Composable
fun Input(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChangeValue: (String) -> Unit
) {
    Column(modifier = modifier) {
        Text(text = title)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color.LightGray,
            elevation = 0.dp,
            shape = MaterialTheme.shapes.medium
        ) {
            TextField(
                value = value,
                onValueChange = onChangeValue,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
            )
        }
    }
}

@Composable
fun CustomButton(modifier: Modifier = Modifier, text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp)),
        colors = ButtonDefaults.buttonColors(Color.LightGray, contentColor = Color.White)
    ) {
        Text(text = text)
    }
}

@Composable
fun CustomSlider(
    modifier: Modifier = Modifier,
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = modifier) {
        Text(text = title)
        Slider(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            onValueChange = onValueChange,
        )
    }
}

fun generateQRCode(gameId: String): Bitmap {
    val fileOut = ByteArrayOutputStream()

    QRCode(gameId)
        .render(cellSize = 50, margin = 25)
        .writeImage(fileOut)

    val imageBytes = fileOut.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}

//@Preview
//@Composable
//fun InputPreview() {
//    SeekersTheme {
//        LobbyCreationScreen()
//    }
//}

@Preview
@Composable
fun QrPrev() {
    val bitmap = generateQRCode("test")
    Image(modifier = Modifier.size(100.dp), bitmap = bitmap.asImageBitmap(), contentDescription = "test")
}