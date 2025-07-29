package com.convenient.salescall.call_db

import androidx.room.*

@Dao
interface CallRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) // 避免重复插入
    fun insert(record: CallRecord)

    @Update
    fun update(record: CallRecord)

    @Delete
    fun delete(record: CallRecord)

    @Query("SELECT * FROM call_record WHERE callLogId = :callLogId")
    fun getByCallLogId(callLogId: Long): CallRecord?

    @Query("SELECT * FROM call_record")
    fun getAll(): List<CallRecord>
}