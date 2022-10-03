package com.example.seekers

import android.os.CountDownTimer
import android.widget.Toast
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.seekers.ui.theme.SeekersTheme

@Composable
fun CountdownScreen(
    seconds: Int,
    navController: NavHostController,
    vm: CountdownViewModel = viewModel(),
    isSeeker: Boolean = false
) {
    val timeLeft by vm.timeLeft.observeAsState(seconds)
    val context = LocalContext.current

    val countDownTimer = object : CountDownTimer(seconds * 1000L, 1000) {
        override fun onTick(p0: Long) {
            if (p0 == 0L) {
                vm.updateTime(0)
                return
            }
            vm.updateTime((p0 / 1000).toInt())
        }

        override fun onFinish() {
            Toast.makeText(context, "Countdown over", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        vm.updateTime(seconds)
        countDownTimer.start()
    }

    Box(
        Modifier
            .fillMaxSize()
            .padding(32.dp), contentAlignment = Alignment.Center
    ) {
        CountdownTimerUI(timeLeft = timeLeft, seconds = seconds, isSeeker = isSeeker)
    }
}

@Composable
fun CountdownTimerUI(timeLeft: Int, seconds: Int, isSeeker: Boolean) {
    val floatLeft = if (timeLeft == 0) 0f else (timeLeft.toFloat() / seconds)
    val animatedProgress = animateFloatAsState(
        targetValue = floatLeft,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    ).value
    var size by remember { mutableStateOf(IntSize.Zero) }

    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
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
        Text(text = convertToClock(timeLeft), style = MaterialTheme.typography.h1)
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            Text(text = if (isSeeker) "Participants are finding a hiding spot" else "Go and Hide!")
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
    val timeLeft = MutableLiveData<Int>()

    fun updateTime(seconds: Int) {
        timeLeft.value = seconds
    }
}

@Preview
@Composable
fun CountdownPrev() {
    SeekersTheme() {
        CountdownScreen(seconds = 10, navController = rememberNavController())
    }
}