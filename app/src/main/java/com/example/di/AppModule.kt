package com.example.di

import android.content.Context
import androidx.room.Room
import com.example.data.local.*
import com.example.util.ConnectivityMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideNexusDatabase(@ApplicationContext context: Context): NexusDatabase {
        return Room.databaseBuilder(
            context,
            NexusDatabase::class.java,
            "nexus_ai_db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideConversationDao(db: NexusDatabase): ConversationDao = db.conversationDao()

    @Provides
    @Singleton
    fun provideMessageDao(db: NexusDatabase): MessageDao = db.messageDao()

    @Provides
    @Singleton
    fun provideAttachmentDao(db: NexusDatabase): AttachmentDao = db.attachmentDao()

    @Provides
    @Singleton
    fun provideSkillDao(db: NexusDatabase): SkillDao = db.skillDao()

    @Provides
    @Singleton
    fun provideAgentDao(db: NexusDatabase): AgentDao = db.agentDao()

    @Provides
    @Singleton
    fun provideProjectDao(db: NexusDatabase): ProjectDao = db.projectDao()

    @Provides
    @Singleton
    fun provideMemoryDao(db: NexusDatabase): MemoryDao = db.memoryDao()

    @Provides
    @Singleton
    fun provideConnectivityMonitor(@ApplicationContext context: Context): ConnectivityMonitor = ConnectivityMonitor(context)

    @Provides
    @Singleton
    fun provideApiKeyRefDao(db: NexusDatabase): ApiKeyRefDao = db.apiKeyRefDao()

    @Provides
    @Singleton
    fun provideSearchDao(db: NexusDatabase): SearchDao = db.searchDao()

    @Provides
    @Singleton
    fun provideCachedModelDao(db: NexusDatabase): CachedModelDao = db.cachedModelDao()

    @Provides
    @Singleton
    fun provideCustomModeDao(db: NexusDatabase): CustomModeDao = db.customModeDao()

    @Provides
    @Singleton
    fun provideRecentExportDao(db: NexusDatabase): RecentExportDao = db.recentExportDao()

    @Provides
    @Singleton
    fun provideMemoryScorer(): com.example.domain.usecase.MemoryScorer = com.example.domain.usecase.TfIdfMemoryScorer()
}
