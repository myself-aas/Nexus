package com.example.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object AppDatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Schema unchanged, version bump introduces explicit migration path.
        }
    }

    val ALL = arrayOf(MIGRATION_1_2)
}
