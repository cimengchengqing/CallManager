package com.convenient.salescall.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.convenient.salescall.tools.HeartbeatManager

class HeartbeatReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        HeartbeatManager.getInstance().sendHeartbeat()

        // 重新设置下一次心跳
        val manager = HeartbeatManager.getInstance()
        manager.nettyClient?.let { client ->
            manager.init(client)
        }
    }
}