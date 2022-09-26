package com.example.seekers

import android.R.attr
import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storage
import java.io.ByteArrayOutputStream


object FirebaseStorageHelper {
    val TAG = "storageHelper"
    val storageRef = Firebase.storage.reference
    fun uploadImg(bitmap: Bitmap, name: String) {
        val ref = storageRef.child("qr").child(name)
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()
        val uploadTask = ref.putBytes(data)
        uploadTask.addOnFailureListener {
            Log.e(TAG, "uploadImg: ", it)
        }.addOnSuccessListener { taskSnapshot ->
            Log.d(TAG, "uploadImg: " + "success (${ref.name})")
        }
    }
}