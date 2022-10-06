package com.example.seekers

import android.util.Log
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.io.Serializable

object FirestoreHelper {
    val lobbiesRef = Firebase.firestore.collection("lobbies")
    val usersRef = Firebase.firestore.collection("users")
    val TAG = "firestoreHelper"

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

    fun updateInGamePlayerDistanceStatus(changeMap: Map<String, Any>,player: Player, gameId: String) {
        val playerRef = lobbiesRef.document(gameId).collection("players").document(player.playerId)
        playerRef.update(changeMap)
            .addOnSuccessListener {
                Log.d(TAG, "updateInGameDistanceStatus: ${player.playerId} distance updated")
            }
            .addOnFailureListener {
                Log.e(TAG, "update: ", it)
            }
    }

    fun updatePlayerLocation(changeMap: Map<String, Any>, playerId: String, gameId: String) {
        val playerRef = lobbiesRef.document(gameId).collection("players").document(playerId)
        playerRef.update(changeMap)
            .addOnSuccessListener {
                Log.d(TAG, "updatePlayerLocation: $playerId location updated")
            }
            .addOnFailureListener {
                Log.e(TAG, "update: ", it)
            }
    }

}

class Lobby(
    var id: String = "",
    val center: GeoPoint = GeoPoint(0.0, 0.0),
    val countdown: Int = 0,
    val maxPlayers: Int = 0,
    val timeLimit: Int = 0,
    val radius: Int = 0,
    val status: Int = 0
) : Serializable

class Player(
    val nickname: String = "",
    val avatarId: Int = 0,
    val playerId: String = "",
    val status: Int = 0,
    val location: GeoPoint = GeoPoint(0.0,0.0),
    val distanceStatus: Int = 0
) : Serializable

enum class PlayerStatus(val value: Int) {
    CREATOR(0),
    JOINED(1),
    SEEKER(2),
    PLAYING(3),
    ELIMINATED(4)
}

enum class PlayerDistance(val value: Int) {
    NOT_IN_RADAR(0),
    WITHIN10(1),
    WITHIN50(2),
    WITHIN100(3)
}

enum class LobbyStatus(val value: Int) {
    ACTIVE(0),
    FINISHED(1),
    DELETED(2),
}

val playerId = "testPlayer"