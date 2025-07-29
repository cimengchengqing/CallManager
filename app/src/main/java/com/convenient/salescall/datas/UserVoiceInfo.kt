package com.convenient.salescall.datas

data class UserVoiceInfo(
    val userId: Int,
    val userName: String,
    val voiceCount: Int,
    val connectedCount: Int,
    val unconnectedCount: Int,
    val durationMs: Long,
    val durationMsFormat: String,
    val dimensionType: Int
)
