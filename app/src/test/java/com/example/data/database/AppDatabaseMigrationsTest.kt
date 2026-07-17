package com.example.data.database

import org.junit.Assert.assertEquals
import org.junit.Test

class AppDatabaseMigrationsTest {
    @Test
    fun `legacy app database migration path is defined`() {
        assertEquals(1, AppDatabaseMigrations.MIGRATION_1_2.startVersion)
        assertEquals(2, AppDatabaseMigrations.MIGRATION_1_2.endVersion)
    }
}
