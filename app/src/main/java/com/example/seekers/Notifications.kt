package com.example.seekers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import com.example.seekers.ui.theme.Emerald
import com.google.android.gms.location.Priority
import java.util.*

object Notifications {
    private val CHANNEL_ID = "FOREGROUND_SERVICE"

    fun createNotificationChannel(
        context: Context,
        channelId: String = CHANNEL_ID,
        importanceLevel: Int = NotificationManager.IMPORTANCE_NONE,
        lockscreenVisibility: Int = Notification.VISIBILITY_PUBLIC,
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
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.chick_with_background))
            .setPriority(priority)
            .setCategory(category)
            .setContentIntent(pendingIntent)
            .setAutoCancel(autoCancel)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setColor(Emerald.toArgb())
            .setColorized(true)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .build()
    }
}