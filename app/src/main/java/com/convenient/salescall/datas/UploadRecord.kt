package com.convenient.salescall.datas

// 上传状态数据类
data class UploadRecord(
    val id: Long = 0,
    val filePath: String,           // 文件路径
    val fileName: String,           // 文件名
    val fileHash: String,           // 文件MD5哈希值
    val fileSize: Long,             // 文件大小
    val uploadTime: Long,           // 上传时间戳
    val uploadStatus: UploadStatus, // 上传状态
    val serverId: String? = null    // 服务器返回的文件ID
)

enum class UploadStatus {
    PENDING,    // 待上传
    UPLOADING,  // 上传中
    UPLOAD_SUCCESS,    // 上传成功
    UPLOAD_FAILED      // 上传失败
}
