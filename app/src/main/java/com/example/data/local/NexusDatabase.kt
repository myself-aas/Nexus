package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ConversationEntity::class,
        ConversationFts::class,
        MessageEntity::class,
        AttachmentEntity::class,
        SkillEntity::class,
        AgentEntity::class,
        ProjectEntity::class,
        MemoryEntity::class,
        ApiKeyRefEntity::class,
        CachedModelEntity::class,
        CustomModeEntity::class,
        RecentExportEntity::class
    ],
    version = 9,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class NexusDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun skillDao(): SkillDao
    abstract fun agentDao(): AgentDao
    abstract fun projectDao(): ProjectDao
    abstract fun memoryDao(): MemoryDao
    abstract fun apiKeyRefDao(): ApiKeyRefDao
    abstract fun searchDao(): SearchDao
    abstract fun cachedModelDao(): CachedModelDao
    abstract fun customModeDao(): CustomModeDao
    abstract fun recentExportDao(): RecentExportDao
}
