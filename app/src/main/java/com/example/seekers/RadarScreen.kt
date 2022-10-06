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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.seekers.general.CustomButton
import com.example.seekers.ui.theme.avatarBackground
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun RadarScreen(
    navController: NavController,
    vm: RadarViewModel = viewModel(),
    gameId: String
) {
    val scope = rememberCoroutineScope()
    val playersNearBy by vm.playersNearByCount.observeAsState()
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
            ScanningStatus.SCANNING_STOPPED.value -> "Found ${playersNearBy ?: 0} players nearby"
            else -> ""
        }
        Text(
            text = text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        FoundPlayerList(players = playersInGame, vm = vm, gameId = gameId)
        CustomButton(text = "Scan") {
            vm.updateScanStatus(ScanningStatus.SCANNING.value)
            scope.launch {
                val gotPlayers = withContext(Dispatchers.IO) {
                    vm.getPlayers(gameId)
                }
                if(gotPlayers) {
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
fun FoundPlayerList(
    players: List<Player>,
    vm: RadarViewModel,
    gameId: String
) {
    val seekersGeoPoint = GeoPoint(60.22382613352466, 24.758245842202495)
    val seekerConvertedToLocation = Location(LocationManager.GPS_PROVIDER)
    seekerConvertedToLocation.latitude = seekersGeoPoint.latitude
    seekerConvertedToLocation.longitude = seekersGeoPoint.longitude

    players.forEach { player ->
        val playerConvertedToLocation = Location(LocationManager.GPS_PROVIDER)
        playerConvertedToLocation.latitude = player.location.latitude
        playerConvertedToLocation.longitude = player.location.longitude

        val distanceFromSeeker = seekerConvertedToLocation.distanceTo(playerConvertedToLocation)
        Log.d("location", "compare to: $distanceFromSeeker")
        if (distanceFromSeeker <= 10) {
            val changeMap = mapOf(
                Pair("distanceStatus", PlayerDistance.WITHIN10.value)
            )
            vm.updatePlayerDistanceStatus(
                changeMap,
                player,
                gameId
            )
        } else if (distanceFromSeeker > 10 && distanceFromSeeker <= 50) {
            val changeMap = mapOf(
                Pair("distanceStatus", PlayerDistance.WITHIN50.value)
            )
            vm.updatePlayerDistanceStatus(
                changeMap,
                player,
                gameId
            )
        } else if (distanceFromSeeker > 50 && distanceFromSeeker <= 100) {
            val changeMap = mapOf(
                Pair("distanceStatus", PlayerDistance.WITHIN100.value)
            )
            vm.updatePlayerDistanceStatus(
                changeMap,
                player,
                gameId
            )
        } else {
            val changeMap = mapOf(
                Pair("distanceStatus", PlayerDistance.NOT_IN_RADAR.value)
            )
            vm.updatePlayerDistanceStatus(
                changeMap,
                player,
                gameId
            )
        }
    }



    LazyColumn(
        modifier = Modifier.padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(players) { player ->
            FoundPlayerCard(player = player)
        }
    }
}

@Composable
fun FoundPlayerCard(
    player: Player,
) {

    val avaratID = when (player.avatarId) {
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
        1 -> Color.Green
        2 -> Color.Yellow
        3 -> Color.Red
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
                    painter = painterResource(id = avaratID),
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
        }
    }
}

class RadarViewModel() : ViewModel() {
    val firestore = FirestoreHelper
    val playersNearByCount = MutableLiveData<Int>()
    private val _scanningStatus = MutableLiveData<Int>()
    val scanningStatus: LiveData<Int> = _scanningStatus

    //    private val _playersNearByList = MutableLiveData(listOf<Player>())
//    val playersNearByList: LiveData<List<Player>> = _playersNearByList
    val players = MutableLiveData(listOf<Player>())

    fun updateScanStatus(value: Int) {
        _scanningStatus.value = value
    }

    suspend fun getPlayers(gameId: String) :Boolean {
        firestore.getPlayers(gameId)
            .addSnapshotListener { list, e ->
                list ?: run {
                    Log.e("getPlayers", "getPlayers: ", e)
                    return@addSnapshotListener
                }
                val playerList = list.toObjects(Player::class.java)
                players.postValue(playerList)
            }
        return true
    }

    fun updatePlayerDistanceStatus(changeMap: Map<String, Any>, player: Player, gameId: String) {
        firestore.updateInGamePlayerDistanceStatus(changeMap, player, gameId)
    }


}
