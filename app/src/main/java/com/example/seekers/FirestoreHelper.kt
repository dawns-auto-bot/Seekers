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
}

class Lobby(
    var id: String = "",
    val center: GeoPoint = GeoPoint(0.0, 0.0),
    val maxPlayers: Int = 0,
    val timeLimit: Int = 0,
    val radius: Int = 0
) : Serializable

class Player(val nickname: String = "", val avatarId: Int = 0, val playerId: String = "", status: Int = 0) : Serializable

object PlayerStatus {
    const val CREATOR = 0
    const val JOINED = 1
    const val SEEKER = 2
    const val PLAYING = 3
    const val ELIMINATED = 4
}