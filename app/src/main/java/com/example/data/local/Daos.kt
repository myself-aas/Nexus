package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE isArchived = 0 ORDER BY updatedAt DESC")
    fun getActiveConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE isArchived = 1 ORDER BY updatedAt DESC")
    fun getArchivedConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversationById(id: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversationById(id: String)

    @Query("SELECT * FROM conversations WHERE projectId = :projectId ORDER BY updatedAt DESC")
    fun getConversationsByProject(projectId: String): Flow<List<ConversationEntity>>

    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations()
}

@Dao
interface MessageDao {
    @Transaction
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<MessageWithAttachments>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC LIMIT :limit OFFSET :offset")
    suspend fun getMessagesForConversationPage(conversationId: String, limit: Int, offset: Int): List<MessageWithAttachments>

    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId")
    suspend fun getMessageCountForConversation(conversationId: String): Int

    @Transaction
    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getMessageById(id: String): MessageWithAttachments?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: String)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessageById(id: String)
}

data class MessageWithAttachments(
    @Embedded val message: MessageEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "messageId"
    )
    val attachments: List<AttachmentEntity>
)


@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE messageId = :messageId")
    suspend fun getAttachmentsForMessage(messageId: String): List<AttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: AttachmentEntity)

    @Query("DELETE FROM attachments WHERE messageId = :messageId")
    suspend fun deleteAttachmentsForMessage(messageId: String)
}

@Dao
interface SkillDao {
    @Query("SELECT * FROM skills ORDER BY name ASC")
    fun getAllSkills(): Flow<List<SkillEntity>>

    @Query("SELECT * FROM skills WHERE isEnabled = 1")
    fun getEnabledSkills(): Flow<List<SkillEntity>>

    @Query("SELECT * FROM skills WHERE id = :id")
    suspend fun getSkillById(id: String): SkillEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkill(skill: SkillEntity)

    @Query("DELETE FROM skills WHERE id = :id")
    suspend fun deleteSkillById(id: String)
}

@Dao
interface AgentDao {
    @Query("SELECT * FROM agents ORDER BY name ASC")
    fun getAllAgents(): Flow<List<AgentEntity>>

    @Query("SELECT * FROM agents WHERE id = :id")
    suspend fun getAgentById(id: String): AgentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgent(agent: AgentEntity)

    @Query("DELETE FROM agents WHERE id = :id")
    suspend fun deleteAgentById(id: String)
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY name ASC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY createdAt DESC")
    fun getAllMemories(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE scope = :scope ORDER BY createdAt DESC")
    fun getMemoriesByScope(scope: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE id = :id")
    suspend fun getMemoryById(id: String): MemoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteMemoryById(id: String)

    @Query("DELETE FROM memories")
    suspend fun deleteAllMemories()

    @Query("SELECT * FROM memories")
    suspend fun getAllMemoriesSync(): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE scope = 'global' OR projectId = :projectId")
    suspend fun getMemoriesForProjectSync(projectId: String): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE scope = 'global' AND (projectId IS NULL OR projectId = '')")
    suspend fun getGlobalMemoriesSync(): List<MemoryEntity>
}

@Dao
interface SearchDao {
    @Query("""
        SELECT conversations.* FROM conversations
        JOIN conversations_fts ON conversations.id = conversations_fts.rowid
        WHERE conversations_fts MATCH :query
    """)
    fun searchConversations(query: String): Flow<List<ConversationEntity>>
}

@Dao
interface ApiKeyRefDao {
    @Query("SELECT * FROM api_key_refs")
    fun getAllApiKeyRefs(): Flow<List<ApiKeyRefEntity>>

    @Query("SELECT * FROM api_key_refs WHERE providerId = :providerId")
    suspend fun getApiKeyRef(providerId: String): ApiKeyRefEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApiKeyRef(apiKeyRef: ApiKeyRefEntity)

    @Query("DELETE FROM api_key_refs WHERE providerId = :providerId")
    suspend fun deleteApiKeyRef(providerId: String)
}

@Dao
interface CachedModelDao {
    @Query("SELECT * FROM cached_models WHERE providerId = :providerId ORDER BY label ASC")
    fun getModelsForProvider(providerId: String): Flow<List<CachedModelEntity>>

    @Query("SELECT * FROM cached_models WHERE providerId = :providerId ORDER BY label ASC")
    suspend fun getModelsForProviderSync(providerId: String): List<CachedModelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModels(models: List<CachedModelEntity>)

    @Query("DELETE FROM cached_models WHERE providerId = :providerId")
    suspend fun deleteModelsForProvider(providerId: String)
}

@Dao
interface CustomModeDao {
    @Query("SELECT * FROM custom_modes ORDER BY name ASC")
    fun getAllCustomModes(): Flow<List<CustomModeEntity>>

    @Query("SELECT * FROM custom_modes WHERE id = :id")
    suspend fun getCustomModeById(id: String): CustomModeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomMode(mode: CustomModeEntity)

    @Query("DELETE FROM custom_modes WHERE id = :id")
    suspend fun deleteCustomModeById(id: String)
}

@Dao
interface RecentExportDao {
    @Query("SELECT * FROM recent_exports ORDER BY timestamp DESC")
    fun getAllRecentExports(): Flow<List<RecentExportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentExport(export: RecentExportEntity)

    @Query("DELETE FROM recent_exports WHERE id = :id")
    suspend fun deleteRecentExportById(id: String)

    @Query("DELETE FROM recent_exports")
    suspend fun deleteAllRecentExports()
}

