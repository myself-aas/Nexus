package com.example.data.remote

import android.util.Log
import com.example.domain.model.Message
import com.example.domain.model.ToolCall
import com.example.domain.model.ChatRequest
import com.example.domain.model.ChatStreamChunk
import com.example.domain.model.ModelInfo
import com.example.domain.model.ModelProvider
import com.example.domain.model.ApiError
import com.example.util.bearerAuthorizationValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NvidiaNimProvider @Inject constructor() : ModelProvider {
    private val TAG = "NvidiaNimProvider"
    
    private val rateLimitMutex = Mutex()
    private var lastRequestTime = 0L

    override val id: String = "nvidia_nim"
    override val displayName: String = "NVIDIA NIM"
    override val baseUrl: String = "https://integrate.api.nvidia.com/v1"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun listModels(apiKey: String): Result<List<ModelInfo>> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        if (!apiKey.startsWith("nvapi-")) {
            return@withContext Result.failure(Exception("Invalid API key. NVIDIA NIM API keys must start with 'nvapi-'"))
        }
        try {
            val request = Request.Builder()
                .url("$baseUrl/models")
                .header("Authorization", bearerAuthorizationValue(apiKey))
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("NVIDIA NIM API Error: HTTP ${response.code}"))
                }

                val bodyString = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response body"))
                val jsonObject = JSONObject(bodyString)
                val dataArray = jsonObject.optJSONArray("data") ?: return@withContext Result.success(emptyList())

                val models = mutableListOf<ModelInfo>()
                for (i in 0 until dataArray.length()) {
                    val modelObj = dataArray.getJSONObject(i)
                    val modelId = modelObj.getString("id")
                    
                    // Simple categorization logic
                    val label = modelId.split("/").last().replace("-", " ").replace("_", " ").capitalizeWords()
                    val supportsVision = modelId.contains("vision") || modelId.contains("vl") || modelId.contains("vila") || modelId.contains("llava")
                    val supportsTools = !modelId.contains("reason") && !modelId.contains("deepseek") // simple heuristic
                    
                    models.add(
                        ModelInfo(
                            id = modelId,
                            label = label,
                            contextWindow = 4096, // fallback
                            supportsTools = supportsTools,
                            supportsVision = supportsVision,
                            isFree = true // NVIDIA NIM endpoints usually provide free credits
                        )
                    )
                }
                Result.success(models.sortedBy { it.label })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing models", e)
            Result.failure(e)
        }
    }

    override fun streamChat(request: ChatRequest, apiKey: String): Flow<ChatStreamChunk> = flow {
        rateLimitMutex.withLock {
            val now = System.currentTimeMillis()
            val minInterval = 1500L
            val timeSinceLast = now - lastRequestTime
            if (timeSinceLast < minInterval) {
                val waitTime = (minInterval - timeSinceLast) / 1000L
                emit(ChatStreamChunk.RateLimited(waitTime.coerceAtLeast(1L)))
                kotlinx.coroutines.delay(minInterval - timeSinceLast)
            }
            lastRequestTime = System.currentTimeMillis()
        }

        if (!apiKey.startsWith("nvapi-")) {
            emit(ChatStreamChunk.Error("Invalid API key. NVIDIA NIM API keys must start with 'nvapi-'", com.example.domain.model.ApiError.AuthError()))
            return@flow
        }
        val messagesArray = JSONArray()

        // Inject System Prompt
        request.systemPrompt?.let { sysPrompt ->
            if (sysPrompt.isNotEmpty()) {
                messagesArray.put(JSONObject().apply {
                    put("role", "system")
                    put("content", sysPrompt)
                })
            }
        }

        // Map Chat Messages
        request.messages.forEach { msg ->
            messagesArray.put(JSONObject().apply {
                put("role", msg.role)
                if (msg.role == "tool") {
                    put("tool_call_id", msg.toolCallId ?: "")
                    put("content", msg.content)
                } else if (msg.toolCalls.isNotEmpty()) {
                    put("content", msg.content)
                    val toolCallsArray = JSONArray()
                    msg.toolCalls.forEach { tc ->
                        toolCallsArray.put(JSONObject().apply {
                            put("id", tc.id)
                            put("type", "function")
                            put("function", JSONObject().apply {
                                put("name", tc.name)
                                put("arguments", tc.arguments)
                            })
                        })
                    }
                    put("tool_calls", toolCallsArray)
                } else if (msg.attachments.any { it.type == "image" }) {
                    val contentArray = JSONArray()
                    contentArray.put(JSONObject().apply {
                        put("type", "text")
                        put("text", msg.content)
                    })
                    msg.attachments.filter { it.type == "image" }.forEach { attachment ->
                        val base64 = attachment.extractedText ?: ""
                        if (base64.isNotEmpty()) {
                            contentArray.put(JSONObject().apply {
                                put("type", "image_url")
                                put("image_url", JSONObject().apply {
                                    put("url", "data:${attachment.mimeType};base64,$base64")
                                })
                            })
                        }
                    }
                    put("content", contentArray)
                } else {
                    var finalContent = msg.content
                    // Inject PDF/text attachment content as text if not image
                    val textAttachments = msg.attachments.filter { it.type == "pdf" || it.type == "text" || it.type == "file" }
                    if (textAttachments.isNotEmpty()) {
                        val attachmentText = textAttachments.joinToString("\n\n") { "--- ${it.fileName} ---\n${it.extractedText}\n---" }
                        finalContent = "$finalContent\n\n[Attachments Context]\n$attachmentText"
                    }
                    put("content", finalContent)
                }
            })
        }

        val requestBodyJson = JSONObject().apply {
            put("model", request.modelId)
            put("messages", messagesArray)
            put("temperature", request.temperature)
            put("stream", true)
            request.maxTokens?.let { put("max_tokens", it) }
            request.topP?.let { put("top_p", it) }
            if (request.tools.isNotEmpty()) {
                val toolsArray = JSONArray()
                request.tools.forEach { tool ->
                    toolsArray.put(JSONObject().apply {
                        put("type", "function")
                        put("function", JSONObject().apply {
                            put("name", tool.name)
                            put("description", tool.description)
                            try {
                                put("parameters", JSONObject(tool.jsonSchema))
                            } catch (e: Exception) {
                                // fallback if schema parsing fails
                            }
                        })
                    })
                }
                put("tools", toolsArray)
            }
        }.toString().toRequestBody(jsonMediaType)

        val httpRequest = Request.Builder()
            .url("$baseUrl/chat/completions")
            .header("Authorization", bearerAuthorizationValue(apiKey))
            .header("Content-Type", "application/json")
            .post(requestBodyJson)
            .build()

        var attempts = 0
        val maxAttempts = 2
        var success = false
        var lastError: Exception? = null

        while (attempts < maxAttempts && !success) {
            attempts++
            try {
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                client.newCall(httpRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        val responseCode = response.code
                        val errorBody = response.body?.string() ?: ""
                        val apiError = parseApiError(responseCode, errorBody)
                        emit(ChatStreamChunk.Error(apiError.message, apiError))
                        return@flow
                    }

                    val reader = response.body?.charStream()?.let { BufferedReader(it) }
                    if (reader == null) {
                        throw Exception("Failed to open response stream")
                    }

                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        kotlinx.coroutines.currentCoroutineContext().ensureActive()
                        val trimmed = line?.trim() ?: continue
                        if (trimmed.startsWith("data: ")) {
                            val dataStr = trimmed.substring(6).trim()
                            if (dataStr == "[DONE]") {
                                success = true
                                break
                            }

                            try {
                                val chunkJson = JSONObject(dataStr)
                                val choices = chunkJson.optJSONArray("choices")
                                if (choices != null && choices.length() > 0) {
                                    val delta = choices.getJSONObject(0).optJSONObject("delta")
                                    if (delta != null) {
                                        // Extract tool calls
                                        val toolCalls = delta.optJSONArray("tool_calls")
                                        if (toolCalls != null && toolCalls.length() > 0) {
                                            for (i in 0 until toolCalls.length()) {
                                                val tc = toolCalls.getJSONObject(i)
                                                val function = tc.optJSONObject("function")
                                                val id = tc.optString("id", "")
                                                val name = function?.optString("name", "")
                                                val arguments = function?.optString("arguments", "") ?: ""
                                                emit(ChatStreamChunk.ToolCallChunk(
                                                    id = id.ifEmpty { null },
                                                    name = name?.ifEmpty { null },
                                                    argumentsDelta = arguments
                                                ))
                                            }
                                        }

                                        // Extract deep-thinking content
                                        val reasoning = delta.optString("reasoning_content", "")
                                            .ifEmpty { delta.optString("reasoning", "") }
                                            .ifEmpty { delta.optString("thinking", "") }
                                        
                                        val content = delta.optString("content", "")

                                        if (reasoning.isNotEmpty()) {
                                            emit(ChatStreamChunk.Thinking(reasoning))
                                        }
                                        if (content.isNotEmpty()) {
                                            emit(ChatStreamChunk.Content(content))
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                // Suppress JSON parse errors on partial lines
                            }
                        }
                    }
                    if (success) {
                        emit(ChatStreamChunk.Done)
                    } else {
                        throw java.io.IOException("Connection closed prematurely without [DONE] terminator")
                    }
                }
            } catch (e: Exception) {
                lastError = e
                if (e is kotlinx.coroutines.CancellationException) {
                    throw e
                }
                if (attempts < maxAttempts) {
                    // Retry once on mid-stream connection drops
                    Log.w(TAG, "Connection issues. Retrying... Attempt $attempts of $maxAttempts", e)
                    kotlinx.coroutines.delay(1000)
                }
            }
        }

        if (!success && lastError != null) {
            val apiError = parseNetworkException(lastError)
            emit(ChatStreamChunk.Error(apiError.message, apiError))
        }
    }.flowOn(Dispatchers.IO)

    private fun parseApiError(responseCode: Int, errorBody: String): com.example.domain.model.ApiError {
        return when (responseCode) {
            401, 403 -> com.example.domain.model.ApiError.AuthError()
            429 -> com.example.domain.model.ApiError.RateLimitError()
            400 -> {
                if (errorBody.contains("context", ignoreCase = true) || 
                    errorBody.contains("length", ignoreCase = true) || 
                    errorBody.contains("token", ignoreCase = true)) {
                    com.example.domain.model.ApiError.ContextLengthError()
                } else {
                    com.example.domain.model.ApiError.UnknownError("Request failed: $errorBody")
                }
            }
            else -> com.example.domain.model.ApiError.UnknownError("HTTP $responseCode: $errorBody")
        }
    }

    private fun parseNetworkException(e: Throwable): com.example.domain.model.ApiError {
        return if (e is java.io.IOException) {
            com.example.domain.model.ApiError.NetworkError(e.localizedMessage ?: "Network error. Please check your internet connection.")
        } else {
            com.example.domain.model.ApiError.UnknownError(e.localizedMessage ?: "An unexpected error occurred.")
        }
    }

    override fun supportsTools(): Boolean = true
    override fun supportsVision(): Boolean = true
    override fun isFreeTier(model: ModelInfo): Boolean = true

    private fun String.capitalizeWords(): String {
        return this.split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }
}
