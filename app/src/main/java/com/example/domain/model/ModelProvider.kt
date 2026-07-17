package com.example.domain.model

import kotlinx.coroutines.flow.Flow

interface ModelProvider {
    val id: String                     // "nvidia_nim", "openai", "custom_1"
    val displayName: String
    val baseUrl: String
    suspend fun listModels(apiKey: String): Result<List<ModelInfo>>
    fun streamChat(request: ChatRequest, apiKey: String): Flow<ChatStreamChunk>
    fun supportsTools(): Boolean
    fun supportsVision(): Boolean
    fun isFreeTier(model: ModelInfo): Boolean
}
