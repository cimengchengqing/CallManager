package com.convenient.salescall.call_db

import android.content.Context
import androidx.room.Room

class CallRecordRepository(context: Context) {
    private val db = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java, "call_record_db"
    ).build()
    private val dao = db.callRecordDao()

    suspend fun insert(record: CallRecord) = dao.insert(record)
    suspend fun update(record: CallRecord) = dao.update(record)
    suspend fun delete(record: CallRecord) = dao.delete(record)
    suspend fun getByCallLogId(callLogId: Long) = dao.getByCallLogId(callLogId)
    suspend fun getAll() = dao.getAll()
}