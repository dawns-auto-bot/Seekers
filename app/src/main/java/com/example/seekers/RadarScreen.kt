package com.example.seekers

import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.seekers.general.CustomButton
import com.example.seekers.ui.theme.avatarBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun RadarScreen(
    vm: RadarViewModel = viewModel(),
    gameId: String
) {
    val scope = rememberCoroutineScope()
    val playersInGame by vm.players.observeAsState(listOf())
    val scanning by vm.scanningStatus.observeAsState(0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val text = when (scanning) {
            ScanningStatus.BEFORE_SCAN.value -> "Scan to find nearby players"
            ScanningStatus.SCANNING.value -> "Scanning..."
            ScanningStatus.SCANNING_STOPPED.value -> "Found ${playersInGame.size} players nearby"
            else -> ""
        }
        Text(
            text = text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        if (scanning == ScanningStatus.SCANNING.value) {
            ScanningLottie()
        } else {
            FoundPlayerList(playersAndDistance = playersInGame, vm = vm, gameId = gameId)
        }
        CustomButton(text = "Scan") {
            vm.updateScanStatus(ScanningStatus.SCANNING.value)
            scope.launch {
                val gotPlayers = withContext(Dispatchers.IO) {
                    vm.getPlayers(gameId)
                }
                delay(5000)
                if (gotPlayers) {
                    vm.updateScanStatus(ScanningStatus.SCANNING_STOPPED.value)
                }
            }
        }
    }
}

enum class ScanningStatus(val value: Int) {
    BEFORE_SCAN(0),
    SCANNING(1),
    SCANNING_STOPPED(2)
}

@Composable
fun ScanningLottie() {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.scanning_nearby))
    LottieAnimation(composition)
}

@Composable
fun FoundPlayerList(
    playersAndDistance: List<Pair<Player, Float>>,
    vm: RadarViewModel,
    gameId: String
) {
    val height = LocalConfiguration.current.screenHeightDp * 0.5

    LazyColumn(
        modifier = Modifier
            .padding(15.dp)
            .height(height.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(playersAndDistance) { player ->
            FoundPlayerCard(player = player.first, distance = player.second)
        }
    }
}

@Composable
fun FoundPlayerCard(
    player: Player,
    distance: Float
) {

    val avatarID = when (player.avatarId) {
        0 -> R.drawable.bee
        1 -> R.drawable.chameleon
        2 -> R.drawable.chick
        3 -> R.drawable.cow
        4 -> R.drawable.crab
        5 -> R.drawable.dog
        6 -> R.drawable.elephant
        7 -> R.drawable.fox
        8 -> R.drawable.koala
        9 -> R.drawable.lion
        10 -> R.drawable.penguin
        else -> R.drawable.whale
    }

    val backgroundColor = when (player.distanceStatus) {
        1 -> Color.Red
        2 -> Color.Yellow
        3 -> Color.Green
        else -> Color.White
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = 10.dp,
        backgroundColor = backgroundColor
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Card(
                shape = CircleShape,
                border = BorderStroke(2.dp, Color.Black),
                backgroundColor = avatarBackground,
                modifier = Modifier
                    .padding(10.dp)
                    .align(Alignment.CenterStart)
            ) {
                Image(
                    painter = painterResource(id = avatarID),
                    contentDescription = "avatar",
                    modifier = Modifier
                        .size(50.dp)
                        .padding(10.dp)
                )
            }
            Text(
                text = player.nickname,
                modifier = Modifier
                    .align(Alignment.Center)
            )
            Text(
                text = "${distance.toInt()} m",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
            )
        }
    }
}

class RadarViewModel() : ViewModel() {
    val firestore = FirebaseHelper
    private val _scanningStatus = MutableLiveData<Int>()
    val scanningStatus: LiveData<Int> = _scanningStatus
    val players = MutableLiveData(listOf<Pair<Player, Float>>())

    fun filterPlayersList(list: List<Player>, initialSeeker: Player) {
        viewModelScope.launch(Dispatchers.Default) {
            val seekersGeoPoint = initialSeeker.location
            Log.d("initialSeeker", seekersGeoPoint.toString())
            val seekerConvertedToLocation = Location(LocationManager.GPS_PROVIDER)
            seekerConvertedToLocation.latitude = seekersGeoPoint.latitude
            seekerConvertedToLocation.longitude = seekersGeoPoint.longitude

            val playersWithDistance = mutableListOf<Pair<Player, Float>>()

            list.forEach { player ->
                val playerConvertedToLocation = Location(LocationManager.GPS_PROVIDER)
                playerConvertedToLocation.latitude = player.location.latitude
                playerConvertedToLocation.longitude = player.location.longitude

                val distanceFromSeeker =
                    seekerConvertedToLocation.distanceTo(playerConvertedToLocation)
                Log.d("location", "compare to: $distanceFromSeeker")
                if (distanceFromSeeker <= 10) {
                    player.distanceStatus = PlayerDistance.WITHIN10.value
                    playersWithDistance.add(Pair(player, distanceFromSeeker))
                } else if (distanceFromSeeker > 10 && distanceFromSeeker <= 50) {
                    player.distanceStatus = PlayerDistance.WITHIN50.value
                    playersWithDistance.add(Pair(player, distanceFromSeeker))
                } else if (distanceFromSeeker > 50 && distanceFromSeeker <= 100) {
                    player.distanceStatus = PlayerDistance.WITHIN100.value
                    playersWithDistance.add(Pair(player, distanceFromSeeker))
                }
            }

            val playersFiltered =
                playersWithDistance.filter {
                    it.first.distanceStatus != PlayerDistance.NOT_IN_RADAR.value && it.first.playerId != FirebaseHelper.uid!!
                }
            players.postValue(playersFiltered.sortedBy { it.second })
        }
    }

    fun updateScanStatus(value: Int) {
        _scanningStatus.value = value
    }

    suspend fun getPlayers(gameId: String): Boolean {
        firestore.getPlayers(gameId)
            .get().addOnSuccessListener { list ->
                val playerList = list.toObjects(Player::class.java)
                val seekerScanning = playerList.find { it.inGameStatus == InGameStatus.SEEKER.value && it.playerId == FirebaseHelper.uid!! }
                if (seekerScanning != null) {
                    filterPlayersList(playerList, seekerScanning)
                }
            }
        return true
    }


}
