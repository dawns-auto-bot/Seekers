package com.example.seekers

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import java.io.ByteArrayOutputStream
import java.io.Serializable

object FirebaseHelper {
    val lobbiesRef = Firebase.firestore.collection("lobbies")
    val usersRef = Firebase.firestore.collection("users")
    val TAG = "firestoreHelper"
    val uid get() = Firebase.auth.uid

    fun addLobby(lobby: Lobby): String {
        val ref = lobbiesRef.document()
        val lobbyWithId = lobby.apply {
            id = ref.id
        }
        ref
            .set(lobbyWithId)
            .addOnSuccessListener {
                Log.d(TAG, "addLobby: " + "success (${ref.id})")
            }
            .addOnFailureListener {
                Log.e(TAG, "addLobby: ", it)
            }

        return ref.id
    }

    fun updateLobby(changeMap: Map<String, Any>, gameId: String) {
        val ref = lobbiesRef.document(gameId)
        ref
            .update(changeMap)
            .addOnSuccessListener {
                Log.d(TAG, "update: " + "success (${ref.id})")
            }
            .addOnFailureListener {
                Log.e(TAG, "update: ", it)
            }
    }

    fun getLobby(gameId: String): DocumentReference {
        return lobbiesRef.document(gameId)
    }

    fun addPlayer(player: Player, gameId: String) {
        val playerRef = lobbiesRef.document(gameId).collection("players").document(player.playerId)
        playerRef
            .set(player)
            .addOnSuccessListener {
                Log.d(TAG, "addPlayer: " + "success (${playerRef.id})")
            }
            .addOnFailureListener {
                Log.e(TAG, "addPlayer: ", it)
            }
    }

    fun getPlayers(gameId: String): CollectionReference {
        return lobbiesRef.document(gameId).collection("players")
    }

    fun getPlayer(gameId: String, playerId: String): DocumentReference {
        return lobbiesRef.document(gameId).collection("players").document(playerId)
    }

    fun removePlayer(gameId: String, playerId: String) {
        val playerRef = lobbiesRef.document(gameId).collection("players").document(playerId)
        playerRef.delete()
    }

    fun getUser(playerId: String): DocumentReference {
        return usersRef.document(playerId)
    }

    fun getUsers(): CollectionReference {
        return usersRef
    }

    fun addUser(map: Map<String, Any>, uid: String) {
        usersRef.document(uid)
            .set(map)
            .addOnSuccessListener {
                Log.d(TAG, "addUser: $uid")
            }
    }

    fun updateUser(userId: String, changeMap: Map<String, Any>) {
        usersRef.document(userId)
            .update(changeMap)
            .addOnSuccessListener {
                Log.d(TAG, "updateUser: $userId updated successfully")
            }
    }

    fun getPlayerStatus(gameId: String, playerId: String): DocumentReference {
        return lobbiesRef.document(gameId).collection("players").document(playerId)
    }

    fun updatePlayer(changeMap: Map<String, Any>, playerId: String, gameId: String) {
        val playerRef = lobbiesRef.document(gameId).collection("players").document(playerId)
        playerRef.update(changeMap)
            .addOnSuccessListener {
                Log.d(TAG, "updatePlayerLocation: $playerId location updated")
            }
            .addOnFailureListener {
                Log.e(TAG, "update: ", it)
            }
    }

    fun updatePlayerInGameStatus(inGameStatus: Int, gameId: String, playerId: String) {
        val playerRef = lobbiesRef.document(gameId).collection("players").document(playerId)
        playerRef.update(mapOf(Pair("inGameStatus", inGameStatus)))
            .addOnSuccessListener {
                Log.d(TAG, "updatePlayerInGameStatus: $playerId status updated")
            }
            .addOnFailureListener {
                Log.e(TAG, "updatePlayerInGameStatus: ", it)
            }
    }

    fun sendSelfie(playerId: String, gameId: String, selfie: Bitmap, nickname: String) {
        val storageRef = Firebase.storage.reference.child("lobbies").child(gameId).child(playerId)
        val baos = ByteArrayOutputStream()
        selfie.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val bytes = baos.toByteArray()
        storageRef.putBytes(bytes)
            .addOnSuccessListener {
                Log.d(TAG, "sendSelfie: picture uploaded ($playerId)")
                val news = News(
                    picId = playerId,
                    text = "$nickname was caught!",
                    timestamp = Timestamp.now()
                )
                addFoundNews(news = news, gameId)
            }
    }

    fun addFoundNews(news: News, gameId: String) {
        lobbiesRef.document(gameId).collection("news").document(news.picId)
            .set(news)
            .addOnSuccessListener {
                Log.d(TAG, "addFoundNews: ${news.picId}")
            }
    }

    fun getNews(gameId: String): Query {
        return lobbiesRef.document(gameId).collection("news")
            .orderBy("timestamp", Query.Direction.DESCENDING)
    }

    fun getSelfieImage(gameId: String, picId: String): StorageReference {
        return Firebase.storage.reference.child("lobbies").child(gameId).child(picId)
    }
}

class Lobby(
    var id: String = "",
    val center: GeoPoint = GeoPoint(0.0, 0.0),
    val maxPlayers: Int = 0,
    val timeLimit: Int = 0,
    val radius: Int = 0,
    val status: Int = 0,
    val startTime: Timestamp = Timestamp.now(),
    val countdown: Int = 0
) : Serializable

class Player(
    val nickname: String = "",
    val avatarId: Int = 0,
    val playerId: String = "",
    val inLobbyStatus: Int = 0,
    val inGameStatus: Int = 0,
    var distanceStatus: Int = 0,
    val location: GeoPoint = GeoPoint(0.0, 0.0),
    val timeOfElimination: Timestamp = Timestamp.now()
) : Serializable

class News(
    val picId: String = "",
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now(),
) : Serializable

enum class InLobbyStatus(val value: Int) {
    CREATOR(0),
    JOINED(1),
}

enum class InGameStatus(val value: Int) {
    SEEKER(0),
    PLAYER(1),
    MOVING(2),
    ELIMINATED(3)
}

enum class PlayerDistance(val value: Int) {
    NOT_IN_RADAR(0),
    WITHIN10(1),
    WITHIN50(2),
    WITHIN100(3)
}

enum class LobbyStatus(val value: Int) {
    CREATED(0),
    ACTIVE(1),
    COUNTDOWN(2),
    FINISHED(3),
    DELETED(4),
}