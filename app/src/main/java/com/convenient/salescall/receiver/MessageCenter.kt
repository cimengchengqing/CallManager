package com.convenient.salescall.receiver

import com.convenient.salescall.tools.LogUtils

object MessageCenter {
    private var lastMsg: String? = null
    private val listeners = mutableListOf<(String) -> Unit>()

    // 发送消息
    fun post(msg: String) {
        LogUtils.d("MessageCenter","发送消息")
        lastMsg = msg
        // 推送给所有已注册监听器
        listeners.forEach { it(msg) }
        // 推送后立即移除缓存
        lastMsg = null
    }

    // 注册监听器
    fun addListener(listener: (String) -> Unit) {
        listeners.add(listener)
        LogUtils.d("MessageCenter","注册监听器")
        // 注册时如果有未读消息，立即推送并移除
        lastMsg?.let {
            LogUtils.d("MessageCenter","立即推送并移除")
            listener(it)
            lastMsg = null
        }
    }

    // 移除监听器
    fun removeListener(listener: (String) -> Unit) {
        listeners.remove(listener)
        LogUtils.d("MessageCenter","移除监听器")
    }
}