package com.convenient.salescall.datas

data class CallLogItem(
    val number: String, // 电话号码
    val type: String,   // 呼叫类型
    val date: Long,     // 通话时间（时间戳）
    val duration: Int   // 通话时长（s）
)