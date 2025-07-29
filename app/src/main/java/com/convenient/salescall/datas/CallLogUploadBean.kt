package com.convenient.salescall.datas

data class CallLogUploadBean(
    val uuid: String,   //上传的UUID
    val callStartTime: Long,    //拨号时间(时间戳)
    val isConnected: Boolean,     //是否接通
    val callEndTime: Long,        //挂断时间(时间戳)
    val durationMs: Long,          //通话时长
    val callerNumber: String,    //本机号码
    val mobile: String     //目标号码
)
