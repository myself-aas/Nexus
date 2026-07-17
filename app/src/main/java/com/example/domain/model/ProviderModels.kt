package com.example.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ModelInfo(
    val id: String,          // e.g. "nvidia/llama-3.3-nemotron-super-49b-v1.5"
    val label: String,
    val contextWindow: Int?,
    val supportsTools: Boolean,
    val supportsVision: Boolean,
    val isFree: Boolean
)

@Serializable
data class ToolCall(
    val id: String,
    val name: String,
    val arguments: String
)

@Serializable
data class ChatRequest(
    val messages: List<Message>,
    val modelId: String,
    val temperature: Float = 0.7f,
    val systemPrompt: String? = null,
    val tools: List<Tool> = emptyList(), // We can pass a list of Tool interfaces
    val maxTokens: Int? = null,
    val topP: Float? = null
)

sealed class ApiError(val message: String) {
    class AuthError(msg: String = "Authentication failed. Please check your API key in Settings.") : ApiError(msg)
    class RateLimitError(msg: String = "Rate limit exceeded. Please wait a moment and try again.") : ApiError(msg)
    class ContextLengthError(msg: String = "Context length exceeded. Please trim the conversation or start a new one.") : ApiError(msg)
    class NetworkError(msg: String = "Network connection issue. Please check your internet connection and try again.") : ApiError(msg)
    class UnknownError(msg: String = "An unknown error occurred.") : ApiError(msg)
}

sealed class ChatStreamChunk {
    data class Content(val text: String) : ChatStreamChunk()
    data class Thinking(val text: String) : ChatStreamChunk() // Collapsible "thinking" tokens
    data class ToolCallChunk(val id: String?, val name: String?, val argumentsDelta: String) : ChatStreamChunk()
    data class Error(val message: String, val error: ApiError? = null) : ChatStreamChunk()
    data class RateLimited(val secondsToWait: Long) : ChatStreamChunk()
    object Done : ChatStreamChunk()
}
