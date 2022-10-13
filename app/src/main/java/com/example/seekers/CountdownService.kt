package com.example.seekers

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.*
import androidx.core.app.NotificationManagerCompat
import com.example.seekers.general.secondsToText
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CountdownService: Service() {
    companion object {

        fun start(context: Context, gameId: String) {
            val intent = Intent(context, CountdownService::class.java)
            intent.putExtra("gameId", gameId)
            context.startForegroundService(intent)
        }
        fun stop(context: Context) {
            val intent = Intent(context, CountdownService::class.java)
            context.stopService(intent)
        }
    }

    val firestore = FirebaseHelper
    var timer: CountDownTimer? = null
    val scope = CoroutineScope(Dispatchers.Default)
    var mediaPlayerHidingPhaseMusic: MediaPlayer? = null
    var mediaPlayerCountdown: MediaPlayer? = null

    fun mediaPlayerHidingPhaseMusic(): MediaPlayer = MediaPlayer.create(applicationContext, R.raw.countdown_music)
    fun mediaPlayerCountdown() : MediaPlayer = MediaPlayer.create(applicationContext, R.raw.counting_down)

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    fun vibrate() {
        val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        vib.vibrate(VibrationEffect.createOneShot(3000,1))
    }

    fun getIsSeeker(gameId: String) {
        firestore.getPlayerStatus(gameId, firestore.uid!!).get()
            .addOnSuccessListener {
                val player = it.toObject(Player::class.java)
                val isSeeker = player?.inGameStatus == InGameStatus.SEEKER.ordinal
                getInitialValue(gameId, isSeeker)
            }
    }

    fun getInitialValue(gameId: String, isSeeker: Boolean) {
        firestore.getLobby(gameId).get()
            .addOnSuccessListener {
                val lobby = it.toObject(Lobby::class.java)
                lobby ?: return@addOnSuccessListener
                val start = lobby.startTime.toDate().time / 1000
                val countdownVal = lobby.countdown
                val now = Timestamp.now().toDate().time / 1000
                val remainingCountdown = start + countdownVal - now + 3
                if (timer == null) {
                    startTimer(timeLeft = remainingCountdown.toInt(), gameId, isSeeker)
                }
            }
    }

    private fun startTimer(timeLeft: Int, gameId: String, isSeeker: Boolean) {
        mediaPlayerHidingPhaseMusic = mediaPlayerHidingPhaseMusic().apply {
            isLooping = true
            this.start()
        }
        timer = object: CountDownTimer(timeLeft * 1000L, 1000) {
            override fun onTick(p0: Long) {
                if (p0 == 0L) {
                    updateMainNotification(0)
                    broadcastCountdown(0)
                    this.cancel()
                    return
                }
                val seconds = p0.div(1000).toInt()
                if (seconds == 10) {
                    mediaPlayerCountdown = mediaPlayerCountdown().apply {
                        this.start()
                    }
                }
                updateMainNotification(seconds)
                broadcastCountdown(seconds)
            }
            override fun onFinish() {
                scope.launch {
                    vibrate()
                }
                if (isSeeker) {
                    firestore.updateLobby(
                        mapOf(Pair("status", LobbyStatus.ACTIVE.ordinal)),
                        gameId
                    )
                }
                this.cancel()
            }
        }
        timer?.start()
    }

    fun broadcastCountdown(seconds: Int) {
        val countDownIntent = Intent()
        countDownIntent.action = GameService.COUNTDOWN_TICK
        countDownIntent.putExtra(GameService.TIME_LEFT, seconds)
        sendBroadcast(countDownIntent)
    }

    private fun getPendingIntent(): PendingIntent = PendingIntent.getActivity(
        applicationContext,
        0,
        Intent(applicationContext, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE
    )

    private fun buildMainNotification(timeLeft: Int?): Notification {
        val timeText = timeLeft?.let { secondsToText(it) } ?: "Initializing the timer"
        return Notifications.createNotification(
            context = applicationContext,
            title = "Seekers - Time to hide!",
            content = timeText,
            pendingIntent = getPendingIntent(),
        )
    }

    fun updateMainNotification(timeLeft: Int) {
        with(NotificationManagerCompat.from(applicationContext)) {
            notify(GameService.MAIN_NOTIFICATION_ID, buildMainNotification(timeLeft))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val gameId = intent?.getStringExtra("gameId")!!
        Notifications.createNotificationChannel(context = applicationContext)
        val notification = buildMainNotification(null)
        startForeground(GameService.MAIN_NOTIFICATION_ID, notification)
        getIsSeeker(gameId)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayerHidingPhaseMusic?.stop()
        mediaPlayerHidingPhaseMusic?.release()
        mediaPlayerCountdown?.stop()
        mediaPlayerCountdown?.release()
    }
}