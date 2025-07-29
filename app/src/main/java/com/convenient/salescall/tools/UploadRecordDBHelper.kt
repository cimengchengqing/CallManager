package com.convenient.salescall.tools

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

class UploadRecordDBHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "upload_records.db"
        const val DATABASE_VERSION = 1
        const val TABLE_NAME = "upload_records"

        const val COLUMN_ID = "id"
        const val COLUMN_FILE_PATH = "file_path"
        const val COLUMN_FILE_NAME = "file_name"
        const val COLUMN_FILE_HASH = "file_hash"
        const val COLUMN_FILE_SIZE = "file_size"
        const val COLUMN_UPLOAD_TIME = "upload_time"
        const val COLUMN_UPLOAD_STATUS = "upload_status"
        const val COLUMN_SERVER_ID = "server_id"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_FILE_PATH TEXT NOT NULL UNIQUE,
                $COLUMN_FILE_NAME TEXT NOT NULL,
                $COLUMN_FILE_HASH TEXT NOT NULL,
                $COLUMN_FILE_SIZE INTEGER NOT NULL,
                $COLUMN_UPLOAD_TIME INTEGER NOT NULL,
                $COLUMN_UPLOAD_STATUS TEXT NOT NULL,
                $COLUMN_SERVER_ID TEXT
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    // 计算文件MD5哈希
    fun calculateFileHash(file: File): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val fis = FileInputStream(file)
            val buffer = ByteArray(8192)
            var length: Int
            while (fis.read(buffer).also { length = it } != -1) {
                md.update(buffer, 0, length)
            }
            fis.close()
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }
}