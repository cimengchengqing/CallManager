package com.convenient.salescall.app

import android.app.Application
import android.content.Context
import cn.jiguang.api.utils.JCollectionAuth
import cn.jpush.android.api.JPushInterface

class CallApp : Application() {
    companion object {
        lateinit var appContext: Context
            private set
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext // 初始化全局 Context

        JPushInterface.setDebugMode(false)
        JCollectionAuth.setAuth(applicationContext,true)
        JPushInterface.init(this)
    }
}