package com.convenient.salescall.tools

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.convenient.salescall.datas.RecordFileInfo
import com.convenient.salescall.datas.UploadStatus
import java.util.Date

/**
 * 适配Scoped Storage的录音文件获取工具类
 * 从Android 11开始使用MediaStore API替代直接文件访问
 */
object ScopedStorageRecordFileUtil {
    private const val TAG = "ScopedStorageRecordFileUtil"

    /**
     * 使用MediaStore API获取音频文件
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun getAudioFilesFromMediaStore(context: Context): List<RecordFileInfo> {
        LogUtils.d(TAG, "使用MediaStore API获取音频文件")

        val audioFiles = mutableListOf<RecordFileInfo>()

        // 查询外部存储的音频文件
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA, // 文件路径（API 29及以下可用）
            MediaStore.Audio.Media.RELATIVE_PATH // API 29+ 相对路径
        )

        // 查询条件：寻找可能的录音文件
        val selection = buildString {
            append("(")
            append("${MediaStore.Audio.Media.DISPLAY_NAME} LIKE ? OR ")
            append("${MediaStore.Audio.Media.DISPLAY_NAME} LIKE ? OR ")
            append("${MediaStore.Audio.Media.DISPLAY_NAME} LIKE ? OR ")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                append("${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ? OR ")
                append("${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ? OR ")
                append("${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?")
            } else {
                append("${MediaStore.Audio.Media.DATA} LIKE ? OR ")
                append("${MediaStore.Audio.Media.DATA} LIKE ? OR ")
                append("${MediaStore.Audio.Media.DATA} LIKE ?")
            }
            append(")")
        }

        val selectionArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(
                "%call%", "%record%", "%rec%", // 文件名包含
                "%call%", "%record%", "%sound_recorder%" // 路径包含
            )
        } else {
            arrayOf(
                "%call%", "%record%", "%rec%", // 文件名包含
                "%call%", "%record%", "%sound_recorder%" // 路径包含
            )
        }

        val sortOrder = "${MediaStore.Audio.Media.DATE_MODIFIED} DESC"

        try {
            val cursor: Cursor? = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.use { c ->
                LogUtils.d(TAG, "MediaStore查询到 ${c.count} 个音频文件")

                while (c.moveToNext()) {
                    val id = c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                    val displayName =
                        c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME))
                    val size = c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE))
                    val dateModified =
                        c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED))
                    val duration =
                        c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))

                    val filePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // Android 10+ 使用相对路径
                        val relativePath =
                            c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH))
                                ?: ""
                        "$relativePath$displayName"
                    } else {
                        // Android 9及以下使用绝对路径
                        c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)) ?: ""
                    }

                    LogUtils.d(TAG, "找到音频文件: $displayName, 大小: $size, 路径: $filePath")

                    // 创建URI用于后续访问
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                    )

                    audioFiles.add(
                        RecordFileInfo(
                            filePath = filePath,
                            fileName = displayName,
                            fileSize = size,
                            createTime = Date(dateModified * 1000),
                            duration = duration,
                            fileType = getFileExtension(displayName),
                            uploadStatus = UploadStatus.PENDING
                        ).apply {
                            // 保存URI用于后续访问
                            setContentUri(contentUri)
                        })
                }
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "MediaStore查询失败: ${e.message}")
        }

        LogUtils.d(TAG, "最终找到 ${audioFiles.size} 个疑似录音文件")
        return audioFiles
    }

    /**
     * 查询指定目录的音频文件
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun getAudioFilesInPath(
        context: Context, relativePath: String = "/storage/emulated/0/MIUI/sound_recorder/call_rec"
    ): List<RecordFileInfo> {
        LogUtils.d(TAG, "查询指定路径的音频文件: $relativePath")

        val audioFiles = mutableListOf<RecordFileInfo>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.RELATIVE_PATH
        )

        val selection = "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%$relativePath%")
        val sortOrder = "${MediaStore.Audio.Media.DATE_MODIFIED} DESC"

        try {
            val cursor: Cursor? = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                    val displayName =
                        c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME))
                    val size = c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE))
                    val dateModified =
                        c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED))
                    val duration =
                        c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
                    val pathFromCursor =
                        c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH))
                            ?: ""

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                    )

                    audioFiles.add(
                        RecordFileInfo(
                            filePath = "$pathFromCursor$displayName",
                            fileName = displayName,
                            fileSize = size,
                            createTime = Date(dateModified * 1000),
                            duration = duration,
                            fileType = getFileExtension(displayName)
                        ).apply {
                            setContentUri(contentUri)
                        })
                }
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "查询指定路径失败: ${e.message}")
        }

        return audioFiles
    }

    /**
     * 根据SDK版本选择合适的方法
     */
    fun getRecordFiles(context: Context): List<RecordFileInfo> {
        LogUtils.d(TAG, "根据SDK版本选择合适的方法")
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Android 11+ 优先使用MediaStore
                LogUtils.d(TAG, "Android 11+: 使用MediaStore API")
                val mediaStoreFiles = getAudioFilesFromMediaStore(context)
                // 如果MediaStore没找到，尝试传统方式（需要MANAGE_EXTERNAL_STORAGE权限）
                if (mediaStoreFiles.isEmpty() && android.os.Environment.isExternalStorageManager()) {
                    LogUtils.d(TAG, "MediaStore未找到文件，尝试传统File API")
                    PhoneRecordFileUtils.getRecordFileList()
                } else {
                    mediaStoreFiles
                }
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Android 10 使用MediaStore
                LogUtils.d(TAG, "Android 10: 使用MediaStore API")
                getAudioFilesFromMediaStore(context)
            }

            else -> {
                // Android 9及以下使用传统File API
                LogUtils.d(TAG, "Android 9及以下: 使用File API")
                PhoneRecordFileUtils.getRecordFileList()
            }
        }
    }

    private fun getFileExtension(fileName: String): String {
        return fileName.substringAfterLast('.', "")
    }
}