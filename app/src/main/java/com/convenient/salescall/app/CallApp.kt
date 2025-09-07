package com.convenient.salescall.app

import android.app.Application
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob


var TCP_CONNECT_IP: String = "47.109.84.18:8433"
var TCP_CONNECT_PORT: Int = 8434

class CallApp : Application() {
    // 全局协程作用域
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        lateinit var appContext: Context
            private set

        val TAG = "CallApp"
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext // 初始化全局 Context
    }
}