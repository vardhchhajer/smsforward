package com.example.smsforwarderpro.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [MessageLog::class, ForwardAttempt::class, ForwardingRule::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageLogDao(): MessageLogDao
    abstract fun forwardAttemptDao(): ForwardAttemptDao
    abstract fun forwardingRuleDao(): ForwardingRuleDao
}
