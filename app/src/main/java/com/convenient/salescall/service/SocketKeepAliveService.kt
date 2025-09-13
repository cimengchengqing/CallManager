package com.convenient.salescall.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.convenient.salescall.app.TCP_CONNECT_IP
import com.convenient.salescall.app.TCP_CONNECT_PORT
import com.convenient.salescall.tools.HeartbeatManager
import com.convenient.salescall.tools.LogUtils
import com.convenient.salescall.tools.NettyClient

class SocketKeepAliveService : Service() {
    private var nettyClient: NettyClient? = null
    private val TAG = "SocketKeepAliveService"

    override fun onCreate() {
        super.onCreate()
        LogUtils.d(TAG, "onCreate: 服务开始创建")
        try {
            startForegroundServiceWithNotification()
            nettyClient = NettyClient(TCP_CONNECT_IP, TCP_CONNECT_PORT)
            HeartbeatManager.getInstance().init(nettyClient!!)
            Thread {
                try {
                    nettyClient?.start()
                } catch (e: Exception) {
                    LogUtils.e(TAG, "NettyClient 启动失败: ${e.message}")
                }
            }.start()
        } catch (e: Exception) {
            LogUtils.e(TAG, "服务创建失败: ${e.message}")
        }
    }

    override fun onDestroy() {
        try {
            HeartbeatManager.getInstance().stopHeartbeat()
            nettyClient?.shutdown()
        } catch (e: Exception) {
            LogUtils.e(TAG, "关闭服务失败: ${e.message}")
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundServiceWithNotification() {
        val channelId = "socket_keep_alive"
        val channelName = "Socket保活服务"
        val chan = NotificationChannel(
            channelId, channelName, NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(chan)
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("通话助手正在运行")
            .setContentText("正在保持与服务器的连接")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .build()
        startForeground(1, notification)
    }
}