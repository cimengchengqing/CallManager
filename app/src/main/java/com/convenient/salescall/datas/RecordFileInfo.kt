package com.convenient.salescall.datas

import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 录音文件信息数据类
 */
data class RecordFileInfo(
    val filePath: String,        // 文件完整路径
    val fileName: String,        // 文件名
    val fileSize: Long,          // 文件大小（字节）
    val createTime: Date,        // 创建时间
    val duration: Long = 0,      // 录音时长（毫秒）
    val fileType: String = "mp3",// 文件类型
    val remark: String = "",     // 备注信息
    var uploadStatus: UploadStatus = UploadStatus.PENDING // 上传状态
) {
    // Scoped Storage支持：ContentUri
    private var contentUri: Uri? = null

    /**
     * 设置ContentUri（用于Android 10+的MediaStore访问）
     */
    fun setContentUri(uri: Uri) {
        this.contentUri = uri
    }

    /**
     * 获取ContentUri
     */
    fun getContentUri(): Uri? = contentUri

    /**
     * 判断是否使用ContentUri访问
     */
    fun isUsingContentUri(): Boolean = contentUri != null

    // 格式化文件大小
    fun getFormattedSize(): String {
        return when {
            fileSize < 1024 -> "${fileSize}B"
            fileSize < 1024 * 1024 -> "${fileSize / 1024}KB"
            fileSize < 1024 * 1024 * 1024 -> "${fileSize / (1024 * 1024)}MB"
            else -> "${fileSize / (1024 * 1024 * 1024)}GB"
        }
    }

    // 格式化创建时间
    fun getFormattedTime(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return formatter.format(createTime)
    }

    // 获取上传状态文本
    fun getUploadStatusText(): String {
        return when (uploadStatus) {
            UploadStatus.PENDING -> "未上传"
            UploadStatus.UPLOADING -> "上传中"
            UploadStatus.UPLOAD_SUCCESS -> "上传成功"
            UploadStatus.UPLOAD_FAILED -> "上传失败"
        }
    }

    // 获取上传状态颜色
    fun getUploadStatusColor(): Int {
        return when (uploadStatus) {
            UploadStatus.PENDING -> 0xFFFFA500.toInt()    // 黄色
            UploadStatus.UPLOADING -> 0xFF2196F3.toInt()       // 蓝色
            UploadStatus.UPLOAD_SUCCESS -> 0xFF4CAF50.toInt()  // 绿色
            UploadStatus.UPLOAD_FAILED -> 0xFFF44336.toInt()   // 红色
        }
    }

    /**
     * 获取格式化的时长
     */
    fun getFormattedDuration(): String {
        if (duration <= 0) return "未知"

        val totalSeconds = duration / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60

        return if (minutes > 0) {
            "${minutes}分${seconds}秒"
        } else {
            "${seconds}秒"
        }
    }

    /**
     * 是否是通话录音文件（根据文件路径判断）
     */
    fun isCallRecord(): Boolean {
        // 标准化路径，处理不同的路径分隔符
        val normalizedPath = filePath.replace('\\', '/').lowercase()

        // MIUI通话录音的标准路径模式
        val callRecordPaths = arrayOf(
            "miui/sound_recorder/call_rec",     // 主要路径
            "miui/callrecord",                  // 备用路径1
            "callrecord",                       // 备用路径2
            "sound_recorder/call_rec",          // 简化路径
            "call_rec"                          // 最简路径
        )

        // 优先通过路径判断
        val isInCallRecordDir = callRecordPaths.any { pattern ->
            normalizedPath.contains(pattern)
        }

        // 如果路径已经确定是通话录音目录，直接返回true
        if (isInCallRecordDir) {
            return true
        }

        // 如果路径判断不出来，再用文件名作为备用判断
        val lowerFileName = fileName.lowercase()
        return lowerFileName.contains("call") ||
                lowerFileName.contains("通话") ||
                lowerFileName.startsWith("call_rec") ||
                lowerFileName.contains("callrecord")
    }

    /**
     * 获取通话对象（如果能从文件名解析）
     */
    fun getCallTarget(): String? {
        // 尝试从文件名中解析通话对象
        // 格式可能是：call_rec_13800138000_20231201_120000.mp3
        val parts = fileName.split("_")
        if (parts.size >= 3 && parts[0] == "call" && parts[1] == "rec") {
            val phoneNumber = parts[2]
            if (phoneNumber.matches(Regex("\\d{11}"))) {
                return phoneNumber
            }
        }
        return null
    }

    /**
     * 获取录音时间（从文件名解析）
     */
    fun getRecordTimeFromName(): Date? {
        // 尝试从文件名中解析时间
        // 格式：call_rec_phone_20231201_120000.mp3
        val parts = fileName.split("_")
        if (parts.size >= 5) {
            val datePart = parts[3] // 20231201
            val timePart = parts[4].split(".")[0] // 120000

            try {
                val dateTimeStr = "${datePart}_$timePart"
                val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                return formatter.parse(dateTimeStr)
            } catch (e: Exception) {
                // 解析失败，返回null
            }
        }
        return null
    }
}