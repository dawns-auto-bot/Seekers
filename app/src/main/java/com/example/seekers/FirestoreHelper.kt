package com.example.seekers

import android.content.Context
import android.os.Parcelable
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.io.Serializable

object FirestoreHelper {
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
    
    fun addUser(map: HashMap<String, String>, uid: String) {
        usersRef.document(uid)
            .set(map)
            .addOnSuccessListener {
                Log.d(TAG, "addUser: $uid")
            }
    }

    fun updateUser(userId: String, changeMap: Map<String, Any>) {
        usersRef.document(userId)
            .set(changeMap)
            .addOnSuccessListener {
                Log.d(TAG, "updateUser: $userId updated successfully")
            }
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

class Player(val nickname: String = "", val avatarId: Int = 0, val playerId: String = "",val status: Int = 0) : Serializable

enum class PlayerStatus(val value: Int) {
    CREATOR(0),
    JOINED(1),
    SEEKER(2),
    PLAYING(3),
    ELIMINATED(4)
}

enum class LobbyStatus(val value: Int) {
    ACTIVE(0),
    COUNTDOWN(1),
    FINISHED(2),
    DELETED(3),
}

val playerId = "mikko"