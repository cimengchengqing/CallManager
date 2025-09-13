package com.convenient.salescall.tools

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import com.convenient.salescall.app.CallApp
import com.convenient.salescall.receiver.HeartbeatReceiver

class HeartbeatManager private constructor() {
    companion object {
        private const val TAG = "HeartbeatManager"
        private const val HEARTBEAT_INTERVAL = 25 * 1000L // 30秒
        private const val REQUEST_CODE = 1001

        @Volatile
        private var INSTANCE: HeartbeatManager? = null

        fun getInstance(): HeartbeatManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HeartbeatManager().also { INSTANCE = it }
            }
        }
    }

    private var alarmManager: AlarmManager? = null
    private var pendingIntent: PendingIntent? = null
    var nettyClient: NettyClient? = null

    fun init(nettyClient: NettyClient) {
        this.nettyClient = nettyClient
        alarmManager = CallApp.appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        setupHeartbeat()
    }

    private fun setupHeartbeat() {
        val intent = Intent(CallApp.appContext, HeartbeatReceiver::class.java)
        pendingIntent = PendingIntent.getBroadcast(
            CallApp.appContext,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = SystemClock.elapsedRealtime() + HEARTBEAT_INTERVAL

        // 确保 pendingIntent 不为 null
        pendingIntent?.let { pi ->
            alarmManager?.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerTime,
                pi
            )
            LogUtils.d(TAG, "心跳定时器已设置，间隔: ${HEARTBEAT_INTERVAL}ms")
        } ?: run {
            LogUtils.e(TAG, "PendingIntent 创建失败，无法设置心跳定时器")
        }
    }

    fun stopHeartbeat() {
        pendingIntent?.let {
            alarmManager?.cancel(it)
            LogUtils.d(TAG, "心跳定时器已停止")
        }
    }

    fun sendHeartbeat() {
        nettyClient?.sendHeartbeat()
    }
}