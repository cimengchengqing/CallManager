package com.convenient.salescall.tools

import android.content.ContentValues
import android.content.Context
import android.util.Log
import com.convenient.salescall.datas.RecordFileInfo
import com.convenient.salescall.datas.UploadRecord
import com.convenient.salescall.datas.UploadStatus
import java.io.File

class UploadRecordManager(private val context: Context) {
    private val dbHelper = UploadRecordDBHelper(context)

    // 检查文件是否已上传成功
    fun isFileUploaded(filePath: String): Boolean {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            UploadRecordDBHelper.TABLE_NAME,
            arrayOf(UploadRecordDBHelper.COLUMN_UPLOAD_STATUS),
            "${UploadRecordDBHelper.COLUMN_FILE_PATH} = ?",
            arrayOf(filePath),
            null, null, null
        )

        var isUploaded = false
        if (cursor.moveToFirst()) {
            val status = cursor.getString(0)
            isUploaded = status == UploadStatus.UPLOAD_SUCCESS.name
        }
        cursor.close()
        db.close()
        return isUploaded
    }

    // 检查文件是否正在上传
    fun isFileUploading(filePath: String): Boolean {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            UploadRecordDBHelper.TABLE_NAME,
            arrayOf(UploadRecordDBHelper.COLUMN_UPLOAD_STATUS),
            "${UploadRecordDBHelper.COLUMN_FILE_PATH} = ?",
            arrayOf(filePath),
            null, null, null
        )

        var isUploading = false
        if (cursor.moveToFirst()) {
            val status = cursor.getString(0)
            isUploading = status == UploadStatus.UPLOADING.name
        }
        cursor.close()
        db.close()
        return isUploading
    }

    // 检查文件是否上传失败
    fun isFileFailed(filePath: String): Boolean {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            UploadRecordDBHelper.TABLE_NAME,
            arrayOf(UploadRecordDBHelper.COLUMN_UPLOAD_STATUS),
            "${UploadRecordDBHelper.COLUMN_FILE_PATH} = ?",
            arrayOf(filePath),
            null, null, null
        )

        var isFailed = false
        if (cursor.moveToFirst()) {
            val status = cursor.getString(0)
            isFailed = status == UploadStatus.UPLOAD_FAILED.name
        }
        cursor.close()
        db.close()
        return isFailed
    }

    // 通过文件哈希检查是否已上传（防止重命名后重复上传）
    fun isFileHashUploaded(fileHash: String): Boolean {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            UploadRecordDBHelper.TABLE_NAME,
            arrayOf(UploadRecordDBHelper.COLUMN_UPLOAD_STATUS),
            "${UploadRecordDBHelper.COLUMN_FILE_HASH} = ? AND ${UploadRecordDBHelper.COLUMN_UPLOAD_STATUS} = ?",
            arrayOf(fileHash, UploadStatus.UPLOAD_SUCCESS.name),
            null, null, null
        )

        val isUploaded = cursor.count > 0
        cursor.close()
        db.close()
        return isUploaded
    }

    // 添加上传记录
    fun addUploadRecord(recordFile: RecordFileInfo): Long {
        val file = File(recordFile.filePath)
        val fileHash = dbHelper.calculateFileHash(file)

        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(UploadRecordDBHelper.COLUMN_FILE_PATH, recordFile.filePath)
            put(UploadRecordDBHelper.COLUMN_FILE_NAME, recordFile.fileName)
            put(UploadRecordDBHelper.COLUMN_FILE_HASH, fileHash)
            put(UploadRecordDBHelper.COLUMN_FILE_SIZE, recordFile.fileSize)
            put(UploadRecordDBHelper.COLUMN_UPLOAD_TIME, System.currentTimeMillis())
            put(UploadRecordDBHelper.COLUMN_UPLOAD_STATUS, UploadStatus.PENDING.name)
        }

        val id = db.replace(UploadRecordDBHelper.TABLE_NAME, null, values)
        db.close()
        return id
    }

    // 查询数据库情况
    fun queryFiles() {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            UploadRecordDBHelper.TABLE_NAME,
            arrayOf(
                UploadRecordDBHelper.COLUMN_FILE_PATH,
                UploadRecordDBHelper.COLUMN_UPLOAD_STATUS
            ),
            null, null, null, null, null
        )
        while (cursor.moveToNext()) {
            Log.d(
                "UploadRecordManager",
                "DB filePath: ${cursor.getString(0)}, status: ${cursor.getString(1)}"
            )
        }
        cursor.close()
        db.close()
    }

    // 更新上传状态
    fun updateUploadStatus(filePath: String, status: UploadStatus, serverId: String? = null) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(UploadRecordDBHelper.COLUMN_UPLOAD_STATUS, status.name)
            put(UploadRecordDBHelper.COLUMN_UPLOAD_TIME, System.currentTimeMillis())
            if (serverId != null) {
                put(UploadRecordDBHelper.COLUMN_SERVER_ID, serverId)
            }
        }

        val affectedRows = db.update(
            UploadRecordDBHelper.TABLE_NAME,
            values,
            "${UploadRecordDBHelper.COLUMN_FILE_PATH} = ?",
            arrayOf(filePath)
        )
        Log.d(
            "UploadRecordManager",
            "updateUploadStatus: filePath=$filePath, status=$status, affectedRows=$affectedRows"
        )
        db.close()
    }

    // 获取待上传的文件
    fun getPendingUploads(): List<UploadRecord> {
        val records = mutableListOf<UploadRecord>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            UploadRecordDBHelper.TABLE_NAME,
            null,
            "${UploadRecordDBHelper.COLUMN_UPLOAD_STATUS} IN (?, ?)",
            arrayOf(UploadStatus.PENDING.name, UploadStatus.UPLOAD_FAILED.name),
            null, null, "${UploadRecordDBHelper.COLUMN_UPLOAD_TIME} DESC"
        )

        while (cursor.moveToNext()) {
            records.add(
                UploadRecord(
                    id = cursor.getLong(0),
                    filePath = cursor.getString(1),
                    fileName = cursor.getString(2),
                    fileHash = cursor.getString(3),
                    fileSize = cursor.getLong(4),
                    uploadTime = cursor.getLong(5),
                    uploadStatus = UploadStatus.valueOf(cursor.getString(6)),
                    serverId = cursor.getString(7)
                )
            )
        }
        cursor.close()
        db.close()
        return records
    }
}