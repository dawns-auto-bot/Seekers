package com.example.seekers

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.seekers.general.secondsToText
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GameService : Service() {
    companion object {
        const val TAG = "LOCATION_SERVICE"
        const val SEEKER_NOTIFICATION = "SEEKER_NOTIFICATION"
        const val BOUNDS_NOTIFICATION = "BOUNDS_NOTIFICATION"
        const val FOUND_NOTIFICATION = "FOUND_NOTIFICATION"
        const val MAIN_NOTIFICATION_ID = 1
        const val SEEKER_NOTIFICATION_ID = 2
        const val BOUNDS_NOTIFICATION_ID = 3
        const val FOUND_NOTIFICATION_ID = 4
        const val TIME_LEFT = "TIME_LEFT"
        const val COUNTDOWN_TICK = "COUNTDOWN_TICK"

        fun start(context: Context, gameId: String, isSeeker: Boolean) {
            val startIntent = Intent(context, GameService::class.java)
            startIntent.putExtra("gameId", gameId)
            startIntent.putExtra("isSeeker", isSeeker)
            ContextCompat.startForegroundService(context, startIntent)
        }
        fun stop(context: Context) {
            val stopIntent = Intent(context, GameService::class.java)
            context.stopService(stopIntent)
        }
    }
    private val firestore = FirebaseHelper
    var previousLoc: Location? = null
    var callback: LocationCallback? = null
    var isTracking = false
    val scope = CoroutineScope(Dispatchers.IO)
    var newsCount = 0
    var seekerNearbySent = false
    var outOfBoundsSent = false
    var timer: CountDownTimer? = null
    var currentGameId: String? = null

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private fun getClient() = LocationServices.getFusedLocationProviderClient(applicationContext)

    private fun getCallback(gameId: String, isSeeker: Boolean): LocationCallback {
        val locCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { curLoc ->
                    scope.launch {
                        updateLoc(previousLoc, curLoc, gameId, isSeeker)
                    }
                    if (!isSeeker) {
                        scope.launch {
                            if (!seekerNearbySent) {
                                checkDistanceToSeekers(curLoc, gameId)
                                delay(30 * 1000)
                                seekerNearbySent = false
                            }
                        }
                        scope.launch {
                            if (!outOfBoundsSent) {
                                checkOutOfBounds(gameId, curLoc)
                                delay(30 * 1000)
                                outOfBoundsSent = false
                            }
                        }
                    }
                }
            }
        }
        callback = locCallback
        return locCallback
    }

    fun updateLoc(prevLoc: Location?, curLoc: Location, gameId: String, isSeeker: Boolean) {
        if (prevLoc == null) {
            previousLoc = curLoc
            Log.d(TAG, "updateLoc: $curLoc")
            Log.d(TAG, "updateLoc: $isSeeker")
            firestore.updatePlayer(
                mapOf(
                    Pair(
                        "location",
                        GeoPoint(curLoc.latitude, curLoc.longitude)
                    )
                ),
                firestore.uid!!,
                gameId
            )
            return
        }
        val distanceToPrev = prevLoc.distanceTo(curLoc)
        if (distanceToPrev > 5f) {
            Log.d(TAG, "updateLoc: sent location")
            firestore.updatePlayer(
                mapOf(
                    Pair(
                        "location",
                        GeoPoint(curLoc.latitude, curLoc.longitude)
                    )
                ),
                firestore.uid!!,
                gameId
            )
            previousLoc = curLoc
            if (!isSeeker) {
                firestore.updatePlayerInGameStatus(
                    InGameStatus.MOVING.ordinal,
                    gameId,
                    firestore.uid!!
                )
            }
        } else {
            if (!isSeeker) {
                firestore.updatePlayerInGameStatus(
                    InGameStatus.PLAYER.ordinal,
                    gameId,
                    firestore.uid!!
                )
            }
        }
    }

    fun checkDistanceToSeekers(ownLocation: Location, gameId: String) {
        firestore.getPlayers(gameId)
            .whereEqualTo("inGameStatus", InGameStatus.SEEKER.ordinal)
            .get()
            .addOnSuccessListener {
                val seekers = it.toObjects(Player::class.java)
                val distances = seekers.map { player ->
                    val results = FloatArray(1)
                    Location.distanceBetween(
                        player.location.latitude,
                        player.location.longitude,
                        ownLocation.latitude,
                        ownLocation.longitude,
                        results
                    )
                    results[0]
                }
                val proximityCriteria = 20f
                val numOfSeekersNearby = distances.filter { dist -> dist <= proximityCriteria }.size
                if (numOfSeekersNearby > 0) {
                    seekerNearbySent = true
                    sendSeekerNearbyNotification(num = numOfSeekersNearby)
                }
            }
    }

    fun checkOutOfBounds(gameId: String, curLoc: Location) {
        firestore.getLobby(gameId).get()
            .addOnSuccessListener {
                val lobby = it.toObject(Lobby::class.java)
                lobby?.let { lob ->
                    val results = FloatArray(1)
                    Location.distanceBetween(
                        lob.center.latitude,
                        lob.center.longitude,
                        curLoc.latitude,
                        curLoc.longitude,
                        results
                    )
                    val distanceToCenter = results[0]
                    if (distanceToCenter > lob.radius) {
                        outOfBoundsSent = true
                        sendOutOfBoundsNotification()
                    }
                }
            }
    }

    private fun startTracking(gameId: String, isSeeker: Boolean) {
        Log.d(TAG, "startTracking")
        LocationHelper.requestLocationUpdates(
            getClient(),
            getCallback(gameId, isSeeker)
        )
        isTracking = true
    }

    private fun stopTracking(callback: LocationCallback) {
        LocationHelper.removeLocationUpdates(
            getClient(),
            callback
        )
    }

    private fun getPendingIntent(): PendingIntent = PendingIntent.getActivity(
        applicationContext,
        0,
        Intent(applicationContext, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE
    )

    private fun sendSeekerNearbyNotification(num: Int) {
        val text = if (num == 1) "A seeker is nearby" else "There are $num seekers nearby"
        val notification = Notifications.createNotification(
            context = applicationContext,
            title = "Watch out!",
            content = text,
            channelId = SEEKER_NOTIFICATION,
            priority = NotificationManager.IMPORTANCE_HIGH,
            category = Notification.CATEGORY_EVENT,
            pendingIntent = getPendingIntent(),
            autoCancel = true
        )
        with(NotificationManagerCompat.from(applicationContext)) {
            notify(SEEKER_NOTIFICATION_ID, notification)
        }
    }

    private fun sendOutOfBoundsNotification() {
        val notification = Notifications.createNotification(
            context = applicationContext,
            title = "Out of bounds!",
            content = "Return to the playing area ASAP!",
            channelId = BOUNDS_NOTIFICATION,
            priority = NotificationManager.IMPORTANCE_HIGH,
            category = Notification.CATEGORY_EVENT,
            pendingIntent = getPendingIntent(),
            autoCancel = true
        )
        with(NotificationManagerCompat.from(applicationContext)) {
            notify(BOUNDS_NOTIFICATION_ID, notification)
        }
    }

    private fun sendNewsNotification(content: String) {
        val notification = Notifications.createNotification(
            context = applicationContext,
            title = "Player found!",
            content = content,
            channelId = FOUND_NOTIFICATION,
            priority = NotificationManager.IMPORTANCE_HIGH,
            category = Notification.CATEGORY_EVENT,
            pendingIntent = getPendingIntent(),
            autoCancel = true
        )
        with(NotificationManagerCompat.from(applicationContext)) {
            notify(FOUND_NOTIFICATION_ID, notification)
        }
    }

    fun listenForNews(gameId: String) {
        FirebaseHelper.getNews(gameId)
            .addSnapshotListener { data, e ->
                Log.d(TAG, "listenForNews: player found ${data?.size()}")
                data ?: kotlin.run {
                    Log.e(TAG, "listenForNews: ", e)
                    return@addSnapshotListener
                }
                val newsList = data.toObjects(News::class.java)
                if (newsList.size > newsCount) {
                    Log.d(TAG, "listenForNews: send notif")
                    val latestNews = newsList[0]
                    sendNewsNotification(latestNews.text)
                    newsCount = newsList.size
                }
            }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: service started")
        val gameId = intent?.getStringExtra("gameId")!!
        currentGameId = gameId
        val isSeeker = intent.getBooleanExtra("isSeeker", false)
        scope.launch {
            listenForNews(gameId)
        }
        Log.d(TAG, "onStartCommand: $gameId $isSeeker")
        Notifications.createNotificationChannel(
            context = applicationContext,
            SEEKER_NOTIFICATION,
            importanceLevel = NotificationManager.IMPORTANCE_HIGH
        )
        Notifications.createNotificationChannel(
            context = applicationContext,
            FOUND_NOTIFICATION,
            importanceLevel = NotificationManager.IMPORTANCE_HIGH
        )
        Notifications.createNotificationChannel(context = applicationContext)
        val notification = buildMainNotification(null)
        startForeground(MAIN_NOTIFICATION_ID, notification)
        startTracking(gameId, isSeeker)
        getTime(gameId)
        return START_NOT_STICKY
    }

    private fun buildMainNotification(timeLeft: Int?): Notification {
        val timeText = timeLeft?.let { secondsToText(it) } ?: "Initializing the timer"
        return Notifications.createNotification(
            context = applicationContext,
            title = "Seekers - Game in progress",
            content = timeText,
            pendingIntent = getPendingIntent(),
        )
    }

    private fun getTime(gameId: String) {
        val now = Timestamp.now().toDate().time.div(1000)
        firestore.getLobby(gameId = gameId).get()
            .addOnSuccessListener {
                val lobby = it.toObject(Lobby::class.java)
                lobby?.let {
                    val startTime = lobby.startTime.toDate().time / 1000
                    val countdown = lobby.countdown
                    val timeLimit = lobby.timeLimit * 60
                    val gameEndTime = (startTime + countdown + timeLimit)
                    val timeLeft = (gameEndTime.minus(now).toInt() + 1)
                    startTimer(timeLeft)
                }
            }
    }

    private fun startTimer(timeLeft: Int) {
        timer = object: CountDownTimer(timeLeft * 1000L, 1000) {
            override fun onTick(p0: Long) {
                if (p0 == 0L) {
                    updateMainNotification(0)
                    broadcastCountdown(0)
                    this.cancel()
                    return
                }
                val seconds = p0.div(1000).toInt()
                updateMainNotification(seconds)
                broadcastCountdown(seconds)
            }
            override fun onFinish() {
                this.cancel()
                endGame()
            }
        }
        timer?.start()
    }

    fun broadcastCountdown(seconds: Int) {
        val countDownIntent = Intent()
        countDownIntent.action = COUNTDOWN_TICK
        countDownIntent.putExtra(TIME_LEFT, seconds)
        sendBroadcast(countDownIntent)
    }

    private fun stopTimer() {
        timer?.cancel()
    }

    fun updateMainNotification(timeLeft: Int) {
        with(NotificationManagerCompat.from(applicationContext)) {
            notify(MAIN_NOTIFICATION_ID, buildMainNotification(timeLeft))
        }
    }

    fun endGame() {
        currentGameId?.let { id ->
        firestore.getPlayers(gameId = id).get()
            .addOnSuccessListener { data ->
                val players = data.toObjects(Player::class.java)
                val creator = players.find { it.inLobbyStatus == InLobbyStatus.CREATOR.ordinal }
                if (firestore.uid == creator?.playerId) {
                    val changeMap = mapOf(Pair("status", LobbyStatus.FINISHED.ordinal))
                    firestore.updateLobby(changeMap, gameId = id)
                }
                scope.launch {
                    delay(2000)
                    stop(applicationContext)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isTracking) {
            callback?.let {
                stopTracking(it)
                stopTimer()
            }
        }
    }
}