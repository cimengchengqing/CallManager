package com.convenient.salescall.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationHelper {
    val CHANNEL_ID: String = "call_state_channel"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel: NotificationChannel = NotificationChannel(
                CHANNEL_ID,
                "电话状态监听",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.setDescription("用于监听电话状态的通知")

            val notificationManager: NotificationManager? = context.getSystemService(
                NotificationManager::class.java
            )
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}