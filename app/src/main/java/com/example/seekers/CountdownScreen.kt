package com.example.seekers

import android.media.MediaPlayer
import android.os.CountDownTimer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProgressIndicatorDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.firebase.Timestamp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CountdownScreen(
    gameId: String,
    navController: NavHostController,
    vm: CountdownViewModel = viewModel(),
) {
    val initialValue by vm.initialValue.observeAsState()
    val countdown by vm.countdown.observeAsState()
    var timesUp by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var countdownTimer: CountDownTimer? by remember { mutableStateOf(null) }
    val mediaPlayerHidingPhaseMusic: MediaPlayer = MediaPlayer.create(context, R.raw.countdown_music)
    val mediaPlayerCountdown : MediaPlayer = MediaPlayer.create(context, R.raw.counting_down)

    LaunchedEffect(Unit) {
        vm.getInitialValue(gameId)
        mediaPlayerHidingPhaseMusic.start()
    }

    LaunchedEffect(initialValue) {
        initialValue?.let {
            vm.updateCountdown(it)
            countdownTimer = object : CountDownTimer(it * 1000L, 1000) {
                override fun onTick(p0: Long) {
                    if (p0 == 0L) {
                        vm.updateCountdown(0)
                        return
                    }
                    vm.updateCountdown((p0 / 1000).toInt())
                    if((p0 / 1000).toInt() == 10) {
                    scope.launch {
                            mediaPlayerCountdown.start()
                        }
                    }

                }

                override fun onFinish() {
                    timesUp = true
                    scope.launch {
                        delay(1500)
                        vm.updateLobby(
                            mapOf(Pair("status", LobbyStatus.ACTIVE.ordinal)),
                            gameId
                        )
                        mediaPlayerHidingPhaseMusic.stop()
                        mediaPlayerCountdown.stop()
                        navController.navigate(NavRoutes.Heatmap.route + "/$gameId")
                    }
                }
            }
        }

    }

    countdownTimer?.let {
        LaunchedEffect(Unit) {
            it.start()
        }
        Box(
            Modifier
                .fillMaxSize()
                .padding(32.dp), contentAlignment = Alignment.Center
        ) {
            CountdownTimerUI(countdown = countdown!!, initialTime = initialValue!!)
        }
    }
}

@Composable
fun CountdownTimerUI(countdown: Int, initialTime: Int) {
    val floatLeft = if (countdown == 0) 0f else (countdown.toFloat() / initialTime)
    val animatedProgress = animateFloatAsState(
        targetValue = floatLeft,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    ).value
    var size by remember { mutableStateOf(IntSize.Zero) }

    Box(
        Modifier
            .fillMaxWidth()
            .height(200.dp), contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = animatedProgress,
            strokeWidth = 10.dp,
            color = Color.LightGray,
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp)
                .height(0.dp)
                .onGloballyPositioned {
                    size = it.size
                }
                .offset(0.dp, -(with(LocalDensity.current) { size.width.toDp() }) / 2)
        )
        Text(text = convertToClock(countdown), style = MaterialTheme.typography.h1)
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            Text(text = "Hiding phase")
        }
    }
}

fun convertToClock(seconds: Int): String {
    val minutes = (seconds / 60)
    val minutesString = if (minutes < 10) "0$minutes" else minutes.toString()
    val secs = seconds % 60
    val secsString = if (secs < 10) "0$secs" else secs.toString()
    return "$minutesString:$secsString"
}

class CountdownViewModel : ViewModel() {
    val firestore = FirebaseHelper
    val initialValue = MutableLiveData<Int>()
    val countdown = MutableLiveData<Int>()

    fun updateCountdown(seconds: Int) {
        countdown.value = seconds
    }

    fun updateLobby(changeMap: Map<String, Any>, gameId: String) =
        firestore.updateLobby(changeMap = changeMap, gameId = gameId)

    fun getInitialValue(gameId: String) {
        firestore.getLobby(gameId).get()
            .addOnSuccessListener {
                val lobby = it.toObject(Lobby::class.java)
                lobby ?: return@addOnSuccessListener
                val start = lobby.startTime.toDate().time / 1000
                val countdownVal = lobby.countdown
                val now = Timestamp.now().toDate().time / 1000
                val remainingCountdown = start + countdownVal - now + 2
                println("remaining $remainingCountdown")
                initialValue.postValue(remainingCountdown.toInt())
            }
    }
}

//@Preview
//@Composable
//fun CountdownPrev() {
//    SeekersTheme() {
//        CountdownScreen(seconds = 10, navController = rememberNavController())
//    }
//}