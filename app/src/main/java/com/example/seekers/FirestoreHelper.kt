package com.example.seekers

import android.content.Context
import android.os.Parcelable
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.io.Serializable

object FirestoreHelper {
    val lobbyRef = Firebase.firestore.collection("lobbies")
    val TAG = "firestoreHelper"

    fun addLobby(lobby: Lobby): String {
        val ref = lobbyRef.document()
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
        val ref = lobbyRef.document(gameId)
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
        return lobbyRef.document(gameId)
    }

    fun addPlayer(player: Player, gameId: String) {
        val playerRef = lobbyRef.document(gameId).collection("players").document(player.playerId)
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
        return lobbyRef.document(gameId).collection("players")
    }

    fun removePlayer(gameId: String, playerId: String) {
        val playerRef = lobbyRef.document(gameId).collection("players").document(playerId)
        playerRef.delete()
    }

}

class Lobby(
    var id: String = "",
    val center: GeoPoint = GeoPoint(0.0, 0.0),
    val maxPlayers: Int = 0,
    val timeLimit: Int = 0,
    val radius: Int = 0,
    val status: Int = 0
) : Serializable

class Player(val nickname: String = "", val avatarId: Int = 0, val playerId: String = "", status: Int = 0) : Serializable

enum class PlayerStatus(val value: Int) {
    CREATOR(0),
    JOINED(1),
    SEEKER(2),
    PLAYING(3),
    ELIMINATED(4)
}

enum class LobbyStatus(val value: Int) {
    ACTIVE(0),
    FINISHED(1),
    DELETED(2),
}