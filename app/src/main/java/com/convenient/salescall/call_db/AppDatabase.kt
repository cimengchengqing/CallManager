package com.convenient.salescall.call_db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CallRecord::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun callRecordDao(): CallRecordDao
}