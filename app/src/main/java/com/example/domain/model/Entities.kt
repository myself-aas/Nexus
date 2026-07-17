package com.example.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Conversation(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val providerId: String,
    val modelId: String,
    val systemPromptId: String? = null,
    val projectId: String? = null,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val selectedModeId: String = "daily_tasks"
)

@Serializable
data class Mode(
    val id: String,
    val name: String,
    val description: String,
    val targetStrategy: String, // "quick_reply", "casual", "daily_tasks", "hard_tasks", "thinking", "reasoning", "smart", or a pinned modelId
    val temperature: Float = 0.7f,
    val maxTokens: Int? = null,
    val topP: Float? = null,
    val isCustom: Boolean = false
)

@Serializable
data class Message(
    val id: String,
    val conversationId: String,
    val role: String, // "user", "assistant", "system", "tool"
    val content: String,
    val createdAt: Long,
    val tokenUsage: Int = 0,
    val reasoningContent: String? = null, // For thinking/reasoning models
    val attachments: List<Attachment> = emptyList(),
    val toolCalls: List<ToolCall> = emptyList(),
    val toolCallId: String? = null,
    val toolName: String? = null
)

@Serializable
data class Attachment(
    val id: String,
    val messageId: String,
    val type: String, // "image", "pdf", "text", "file"
    val localUri: String,
    val mimeType: String,
    val fileName: String? = null,
    val extractedText: String? = null
)

@Serializable
data class Skill(
    val id: String,
    val name: String,
    val description: String,
    val instructions: String,
    val triggerType: String = "manual", // "manual", "auto"
    val icon: String = "Extension", // Material Symbols icon name
    val isEnabled: Boolean = false
)

@Serializable
data class Agent(
    val id: String,
    val name: String,
    val description: String,
    val icon: String = "Face",
    val systemPrompt: String,
    val defaultProviderId: String,
    val defaultModelId: String,
    val enabledSkillIds: List<String> = emptyList(),
    val enabledToolIds: List<String> = emptyList(),
    val schedule: String? = null, // cron format or null
    val isEnabled: Boolean = true,
    val temperature: Float = 0.7f
)

@Serializable
data class Project(
    val id: String,
    val name: String,
    val description: String,
    val customInstructions: String,
    val createdAt: Long
)

@Serializable
data class Memory(
    val id: String,
    val scope: String, // "global", "project"
    val content: String,
    val createdAt: Long,
    val sourceConversationId: String? = null,
    val projectId: String? = null,
    val embedding: FloatArray? = null
)

@Serializable
data class ApiKeyRef(
    val providerId: String, // "nvidia_nim", "openai", etc.
    val keyAlias: String, // Keystore-backed alias
    val displayName: String,
    val baseUrl: String,
    val isCustom: Boolean = false,
    val customHeaders: String? = null
)
