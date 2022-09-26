package com.example.seekers

import android.content.Context
import android.os.Parcelable
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.io.Serializable

object FirestoreHelper {

    fun addLobby(lobby: Lobby): String {
        val TAG = "firestoreHelper"
        val ref = Firebase.firestore.collection("lobbies").document()
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

}

class Lobby(
    var id: String,
    val center: GeoPoint,
    val maxPlayers: Int,
    val timeLimit: Int,
    val radius: Int
): Serializable