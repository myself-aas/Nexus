package com.example.data.local

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey
import com.example.domain.model.*

@Fts4(contentEntity = ConversationEntity::class)
@Entity(tableName = "conversations_fts")
data class ConversationFts(
    val title: String,
    val providerId: String,
    val modelId: String
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val providerId: String,
    val modelId: String,
    val systemPromptId: String?,
    val projectId: String?,
    val isPinned: Boolean,
    val isArchived: Boolean,
    val selectedModeId: String = "daily_tasks"
) {
    fun toDomain() = Conversation(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        providerId = providerId,
        modelId = modelId,
        systemPromptId = systemPromptId,
        projectId = projectId,
        isPinned = isPinned,
        isArchived = isArchived,
        selectedModeId = selectedModeId
    )
}

fun Conversation.toEntity() = ConversationEntity(
    id = id,
    title = title,
    createdAt = createdAt,
    updatedAt = updatedAt,
    providerId = providerId,
    modelId = modelId,
    systemPromptId = systemPromptId,
    projectId = projectId,
    isPinned = isPinned,
    isArchived = isArchived,
    selectedModeId = selectedModeId
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String,
    val content: String,
    val createdAt: Long,
    val tokenUsage: Int,
    val reasoningContent: String?,
    val toolCalls: List<ToolCall> = emptyList(),
    val toolCallId: String? = null,
    val toolName: String? = null
) {
    fun toDomain(attachments: List<Attachment> = emptyList()) = Message(
        id = id,
        conversationId = conversationId,
        role = role,
        content = content,
        createdAt = createdAt,
        tokenUsage = tokenUsage,
        reasoningContent = reasoningContent,
        attachments = attachments,
        toolCalls = toolCalls,
        toolCallId = toolCallId,
        toolName = toolName
    )
}

fun MessageWithAttachments.toDomain() = message.toDomain(attachments.map { it.toDomain() })

fun Message.toEntity() = MessageEntity(
    id = id,
    conversationId = conversationId,
    role = role,
    content = content,
    createdAt = createdAt,
    tokenUsage = tokenUsage,
    reasoningContent = reasoningContent,
    toolCalls = toolCalls,
    toolCallId = toolCallId,
    toolName = toolName
)

@Entity(tableName = "attachments")
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val messageId: String,
    val type: String,
    val localUri: String,
    val mimeType: String,
    val fileName: String?,
    val extractedText: String?
) {
    fun toDomain() = Attachment(
        id = id,
        messageId = messageId,
        type = type,
        localUri = localUri,
        mimeType = mimeType,
        fileName = fileName,
        extractedText = extractedText
    )
}

fun Attachment.toEntity() = AttachmentEntity(
    id = id,
    messageId = messageId,
    type = type,
    localUri = localUri,
    mimeType = mimeType,
    fileName = fileName,
    extractedText = extractedText
)

@Entity(tableName = "skills")
data class SkillEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val instructions: String,
    val triggerType: String,
    val icon: String,
    val isEnabled: Boolean
) {
    fun toDomain() = Skill(
        id = id,
        name = name,
        description = description,
        instructions = instructions,
        triggerType = triggerType,
        icon = icon,
        isEnabled = isEnabled
    )
}

fun Skill.toEntity() = SkillEntity(
    id = id,
    name = name,
    description = description,
    instructions = instructions,
    triggerType = triggerType,
    icon = icon,
    isEnabled = isEnabled
)

@Entity(tableName = "agents")
data class AgentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val systemPrompt: String,
    val defaultProviderId: String,
    val defaultModelId: String,
    val enabledSkillIds: List<String>,
    val enabledToolIds: List<String>,
    val schedule: String?,
    val isEnabled: Boolean,
    val temperature: Float
) {
    fun toDomain() = Agent(
        id = id,
        name = name,
        description = description,
        icon = icon,
        systemPrompt = systemPrompt,
        defaultProviderId = defaultProviderId,
        defaultModelId = defaultModelId,
        enabledSkillIds = enabledSkillIds,
        enabledToolIds = enabledToolIds,
        schedule = schedule,
        isEnabled = isEnabled,
        temperature = temperature
    )
}

fun Agent.toEntity() = AgentEntity(
    id = id,
    name = name,
    description = description,
    icon = icon,
    systemPrompt = systemPrompt,
    defaultProviderId = defaultProviderId,
    defaultModelId = defaultModelId,
    enabledSkillIds = enabledSkillIds,
    enabledToolIds = enabledToolIds,
    schedule = schedule,
    isEnabled = isEnabled,
    temperature = temperature
)

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val customInstructions: String,
    val createdAt: Long
) {
    fun toDomain() = Project(
        id = id,
        name = name,
        description = description,
        customInstructions = customInstructions,
        createdAt = createdAt
    )
}

fun Project.toEntity() = ProjectEntity(
    id = id,
    name = name,
    description = description,
    customInstructions = customInstructions,
    createdAt = createdAt
)

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey val id: String,
    val scope: String,
    val content: String,
    val createdAt: Long,
    val sourceConversationId: String? = null,
    val projectId: String? = null,
    val embedding: FloatArray? = null
) {
    fun toDomain() = Memory(
        id = id,
        scope = scope,
        content = content,
        createdAt = createdAt,
        sourceConversationId = sourceConversationId,
        projectId = projectId,
        embedding = embedding
    )
}

fun Memory.toEntity() = MemoryEntity(
    id = id,
    scope = scope,
    content = content,
    createdAt = createdAt,
    sourceConversationId = sourceConversationId,
    projectId = projectId,
    embedding = embedding
)

@Entity(tableName = "api_key_refs")
data class ApiKeyRefEntity(
    @PrimaryKey val providerId: String,
    val keyAlias: String,
    val displayName: String,
    val baseUrl: String,
    val isCustom: Boolean,
    val customHeaders: String? = null
) {
    fun toDomain() = ApiKeyRef(
        providerId = providerId,
        keyAlias = keyAlias,
        displayName = displayName,
        baseUrl = baseUrl,
        isCustom = isCustom,
        customHeaders = customHeaders
    )
}

fun ApiKeyRef.toEntity() = ApiKeyRefEntity(
    providerId = providerId,
    keyAlias = keyAlias,
    displayName = displayName,
    baseUrl = baseUrl,
    isCustom = isCustom,
    customHeaders = customHeaders
)

@Entity(tableName = "cached_models")
data class CachedModelEntity(
    @PrimaryKey val uniqueId: String, // providerId + "_" + modelId
    val providerId: String,
    val modelId: String,
    val label: String,
    val contextWindow: Int?,
    val supportsTools: Boolean,
    val supportsVision: Boolean,
    val isFree: Boolean,
    val timestamp: Long
) {
    fun toDomain() = ModelInfo(
        id = modelId,
        label = label,
        contextWindow = contextWindow,
        supportsTools = supportsTools,
        supportsVision = supportsVision,
        isFree = isFree
    )
}

fun ModelInfo.toEntity(providerId: String, timestamp: Long) = CachedModelEntity(
    uniqueId = "${providerId}_${id}",
    providerId = providerId,
    modelId = id,
    label = label,
    contextWindow = contextWindow,
    supportsTools = supportsTools,
    supportsVision = supportsVision,
    isFree = isFree,
    timestamp = timestamp
)

@Entity(tableName = "custom_modes")
data class CustomModeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val targetStrategy: String,
    val temperature: Float,
    val maxTokens: Int?,
    val topP: Float?
) {
    fun toDomain() = Mode(
        id = id,
        name = name,
        description = description,
        targetStrategy = targetStrategy,
        temperature = temperature,
        maxTokens = maxTokens,
        topP = topP,
        isCustom = true
    )
}

fun Mode.toEntity() = CustomModeEntity(
    id = id,
    name = name,
    description = description,
    targetStrategy = targetStrategy,
    temperature = temperature,
    maxTokens = maxTokens,
    topP = topP
)

@Entity(tableName = "recent_exports")
data class RecentExportEntity(
    @PrimaryKey val id: String,
    val fileName: String,
    val filePath: String,
    val mimeType: String,
    val timestamp: Long,
    val fileSize: Long,
    val language: String
)

