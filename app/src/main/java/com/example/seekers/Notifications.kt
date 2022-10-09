package com.example.seekers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.Priority
import java.util.*

object Notifications {
    private var id: Int = UUID.randomUUID().hashCode()
    private val CHANNEL_ID = "LOCATION_SERVICE"

    fun createNotificationChannel(
        context: Context,
        channelId: String = CHANNEL_ID,
        importanceLevel: Int = NotificationManager.IMPORTANCE_NONE,
        lockscreenVisibility: Int = Notification.VISIBILITY_PRIVATE,
    ) {
        val channel = NotificationChannel(
            channelId,
            "Notification",
            importanceLevel
        ).apply {
            description = "description"
        }
        channel.lockscreenVisibility = lockscreenVisibility
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun createNotification(
        context: Context,
        title: String,
        content: String,
        channelId: String = CHANNEL_ID,
        priority: Int = NotificationManager.IMPORTANCE_MIN,
        category: String = Notification.CATEGORY_SERVICE,
        pendingIntent: PendingIntent? = null,
        autoCancel: Boolean = false
    ): Notification {
        val builder = NotificationCompat.Builder(context, channelId)
        return builder
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.map)
            .setPriority(priority)
            .setCategory(category)
            .setContentIntent(pendingIntent)
            .setAutoCancel(autoCancel)
            .build()
    }
}