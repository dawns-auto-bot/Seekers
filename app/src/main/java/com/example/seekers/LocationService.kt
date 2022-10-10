package com.example.seekers

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LocationService : Service() {
    companion object {
        const val TAG = "LOCATION_SERVICE"
        var seekerNearbySent = false
        var outOfBoundsSent = false
        fun start(context: Context, gameId: String, isSeeker: Boolean) {
            val startIntent = Intent(context, LocationService::class.java)
            startIntent.putExtra("gameId", gameId)
            startIntent.putExtra("isSeeker", isSeeker)
            ContextCompat.startForegroundService(context, startIntent)
        }
        fun stop(context: Context) {
            val stopIntent = Intent(context, LocationService::class.java)
            context.stopService(stopIntent)
        }
    }
    private val firestore = FirebaseHelper
    private val SEEKER_NOTIFICATION = "SEEKER_NOTIFICATION"
    private val BOUNDS_NOTIFICATION = "BOUNDS_NOTIFICATION"
    var listenerReg: ListenerRegistration? = null
    var previousLoc: Location? = null
    var callback: LocationCallback? = null
    var isTracking = false
    val scope = CoroutineScope(Dispatchers.IO)

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
                                checkDistanceToSeeker(curLoc, gameId)
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
            firestore.updatePlayerLocation(
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
        if (distanceToPrev > 10f) {
            Log.d(TAG, "updateLoc: sent location")
            firestore.updatePlayerLocation(
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
                    InGameStatus.MOVING.value,
                    gameId,
                    firestore.uid!!
                )
            }
        } else {
            if (!isSeeker) {
                firestore.updatePlayerInGameStatus(
                    InGameStatus.PLAYER.value,
                    gameId,
                    firestore.uid!!
                )
            }
        }
    }

    fun checkDistanceToSeeker(ownLocation: Location, gameId: String) {
        firestore.getPlayers(gameId)
            .whereEqualTo("inGameStatus", InGameStatus.SEEKER.value)
            .get()
            .addOnSuccessListener {
                val seeker = it.toObjects(Player::class.java)[0]
                val results = FloatArray(1)
                Location.distanceBetween(
                    seeker.location.latitude,
                    seeker.location.longitude,
                    ownLocation.latitude,
                    ownLocation.longitude,
                    results
                )
                val distance = results[0]
                if (distance < 20f) {
                    seekerNearbySent = true
                    sendSeekerNearbyNotification()
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

//    fun listenToLobbyStatus(gameId: String, isSeeker: Boolean) {
//        Log.d(TAG, "listenToLobbyStatus")
//        listenerReg = firestore.getLobby(gameId)
//            .addSnapshotListener { data, e ->
//                data ?: kotlin.run {
//                    Log.e(TAG, "listenToStatus: ", e)
//                    return@addSnapshotListener
//                }
//                val lobby = data.toObject(Lobby::class.java)
//                lobby?.let {
//                    when (it.status) {
//                        LobbyStatus.ACTIVE.value -> {
//                            Log.d(TAG, "listenToLobbyStatus: Lobby active")
//                            startTracking(gameId, isSeeker)
//                        }
//                        LobbyStatus.FINISHED.value -> {
//                            Log.d(TAG, "listenToLobbyStatus: Lobby stopped")
//                            stopTracking(gameId, isSeeker)
//                        }
//                    }
//                }
//            }
//    }

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

    private fun sendSeekerNearbyNotification() {
        val notification = Notifications.createNotification(
            context = applicationContext,
            title = "Watch out!",
            content = "A seeker is nearby",
            channelId = SEEKER_NOTIFICATION,
            priority = NotificationManager.IMPORTANCE_HIGH,
            category = Notification.CATEGORY_EVENT,
            pendingIntent = getPendingIntent(),
            autoCancel = true
        )
        with(NotificationManagerCompat.from(applicationContext)) {
            notify(3, notification)
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
            notify(4, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: service started")
        val gameId = intent?.getStringExtra("gameId")
        val isSeeker = intent?.getBooleanExtra("isSeeker", false)
        Log.d(TAG, "onStartCommand: $gameId $isSeeker")
        Notifications.createNotificationChannel(
            context = applicationContext,
            SEEKER_NOTIFICATION,
            importanceLevel = NotificationManager.IMPORTANCE_HIGH
        )
        Notifications.createNotificationChannel(context = applicationContext)
        val notification = Notifications.createNotification(
            context = applicationContext,
            title = "Seekers - Game in progress",
            content = "Seekers is sending your location to the game",
            pendingIntent = getPendingIntent()
        )
        startForeground(1, notification)
        startTracking(gameId!!, isSeeker!!)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isTracking) {
            callback?.let {
                stopTracking(it)
            }
        }
    }
}