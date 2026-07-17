package com.example.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class NexusMigrationsTest {
    @Test
    fun `nexus migration chain contains current upgrade path`() {
        assertEquals(8, NexusMigrations.MIGRATION_8_9.startVersion)
        assertEquals(9, NexusMigrations.MIGRATION_8_9.endVersion)
    }
}
