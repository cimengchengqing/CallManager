package com.convenient.salescall.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.convenient.salescall.R
import com.convenient.salescall.app.CallApp
import com.convenient.salescall.tools.LogUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallStateService : Service() {
    private var telephonyManager: TelephonyManager? = null
    private var phoneStateListener: PhoneStateListener? = null
    private var audioManager: AudioManager? = null
    private var isForeground = false

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel(this)
        startForeground(NOTIFICATION_ID, createNotification())
        isForeground = true

        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        phoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                when (state) {
                    TelephonyManager.CALL_STATE_IDLE -> {
                        LogUtils.d(TAG, "通话结束：${formatTime(System.currentTimeMillis())}")
                        val localIntent = Intent("com.call.ACTION_DOWN")
                        LocalBroadcastManager.getInstance(CallApp.appContext).sendBroadcast(localIntent)
                    }

                    TelephonyManager.CALL_STATE_RINGING -> {
                        LogUtils.d(TAG, "电话响铃：${formatTime(System.currentTimeMillis())}")
                    }

                    TelephonyManager.CALL_STATE_OFFHOOK -> {
                        LogUtils.d(TAG, "通话开始：${formatTime(System.currentTimeMillis())}")
                    }
                }
            }
        }

        // 注册监听器
        telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 确保服务在前台运行
        if (!isForeground) {
            startForeground(NOTIFICATION_ID, createNotification())
            isForeground = true
        }
        return START_STICKY
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "电话状态监听",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "用于监听电话状态的服务"
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("电话状态监听")
            .setContentText("正在监听电话状态")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    override fun onDestroy() {
        super.onDestroy()
        isForeground = false
        telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        public const val TAG = "测试"
        private const val CHANNEL_ID = "call_state_channel"
        private const val NOTIFICATION_ID = 1
    }
}