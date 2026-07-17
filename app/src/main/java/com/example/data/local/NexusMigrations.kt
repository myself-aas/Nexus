package com.example.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object NexusMigrations {
    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Schema unchanged, version bump introduces explicit migration path.
        }
    }

    val ALL = arrayOf(MIGRATION_8_9)
}
