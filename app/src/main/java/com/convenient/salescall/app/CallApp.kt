package com.convenient.salescall.app

import android.app.Application
import android.content.Context
import cn.jiguang.api.utils.JCollectionAuth
import cn.jpush.android.api.JPushInterface
import com.convenient.salescall.tools.LogUtils

class CallApp : Application() {
    companion object {
        lateinit var appContext: Context
            private set

        val TAG = "CallApp"
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext // 初始化全局 Context

        val processName = getProcessName()
        LogUtils.d(TAG, "onCreate:   processName=${processName}")

        if (processName.equals("getPackageName()")) {

            JPushInterface.setDebugMode(false)
            JCollectionAuth.setAuth(applicationContext, true)
            JPushInterface.init(this)
        }
    }
}