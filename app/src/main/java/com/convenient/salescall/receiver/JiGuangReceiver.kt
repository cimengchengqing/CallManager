package com.convenient.salescall.receiver

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import cn.jpush.android.api.CustomMessage
import cn.jpush.android.service.JPushMessageReceiver
import com.convenient.salescall.app.CallApp

class JiGuangReceiver : JPushMessageReceiver() {

    override fun onMessage(context: Context?, customMessage: CustomMessage?) {
        super.onMessage(context, customMessage)
        val callNumber = customMessage?.message
        Log.d("JiGuangReceiver", "收到消息: $callNumber")

        callNumber?.let { number ->
            if (number.isNotEmpty() && isValidPhoneNumber(number)) {
                val localIntent = Intent("com.call.ACTION_MSG").apply {
                    putExtra("msg", number)
                }
                LocalBroadcastManager.getInstance(CallApp.appContext).sendBroadcast(localIntent)
            } else {
                Log.w("JiGuangReceiver", "无效的手机号: $number")
            }
        } ?: Log.w("JiGuangReceiver", "消息内容为空")
    }

    companion object {
        fun isValidPhoneNumber(phone: String?): Boolean {
            if (phone.isNullOrEmpty()) return false
            val regex = Regex("^1[3-9]\\d{9}$")
            return regex.matches(phone)
        }
    }
}