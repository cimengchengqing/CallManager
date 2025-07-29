package com.convenient.salescall.call_db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_record")
data class CallRecord(
    @PrimaryKey val callLogId: Long,      // 通话记录的_id，唯一主键
    val uuid: String,                   //业务用UUID
    val callerNumber: String,           //本机号码
    val mobile: String,                 //目标号码
    val callStartTime: Long,            //拨号时间(时间戳)
    val isConnected: Boolean,           //是否接通
    val callEndTime: Long,              //挂断时间(时间戳)
    val durationMs: Long,               //通话时长
    var isUploaded: Boolean = false,              // 是否已上传后台
    var recordFilePath: String            // 录音文件本地位置
)
