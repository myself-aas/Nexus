package com.example.data.remote

import android.util.Log
import com.example.domain.model.Message
import com.example.domain.model.ToolCall
import com.example.domain.model.ChatRequest
import com.example.domain.model.ChatStreamChunk
import com.example.domain.model.ModelInfo
import com.example.domain.model.ModelProvider
import com.example.util.bearerAuthorizationValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.util.concurrent.TimeUnit

class OpenAiCompatProvider(
    override val id: String,
    override val displayName: String,
    override val baseUrl: String,
    private val customHeaders: String? = null
) : ModelProvider {
    private val TAG = "OpenAiCompatProvider"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun Request.Builder.addCustomHeaders(customHeaders: String?): Request.Builder {
        customHeaders?.lineSequence()
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() && it.contains(":") }
            ?.forEach { line ->
                val parts = line.split(":", limit = 2)
                if (parts.size == 2) {
                    this.header(parts[0].trim(), parts[1].trim())
                }
            }
        return this
    }

    override suspend fun listModels(apiKey: String): Result<List<ModelInfo>> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/models")
                .header("Authorization", bearerAuthorizationValue(apiKey))
                .addCustomHeaders(customHeaders)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Provider API Error: HTTP ${response.code}"))
                }

                val bodyString = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response body"))
                val jsonObject = JSONObject(bodyString)
                val dataArray = jsonObject.optJSONArray("data") ?: return@withContext Result.success(emptyList())

                val models = mutableListOf<ModelInfo>()
                for (i in 0 until dataArray.length()) {
                    val modelObj = dataArray.getJSONObject(i)
                    val modelId = modelObj.getString("id")
                    
                    val label = modelId.split("/").last().replace("-", " ").replace("_", " ").capitalizeWords()
                    val supportsVision = modelId.contains("vision") || modelId.contains("vl") || modelId.contains("lava")
                    val supportsTools = !modelId.contains("reason") && !modelId.contains("thinking")
                    
                    val tempModel = ModelInfo(
                        id = modelId,
                        label = label,
                        contextWindow = 4096,
                        supportsTools = supportsTools,
                        supportsVision = supportsVision,
                        isFree = false
                    )

                    models.add(
                        ModelInfo(
                            id = modelId,
                            label = label,
                            contextWindow = 4096,
                            supportsTools = supportsTools,
                            supportsVision = supportsVision,
                            isFree = isFreeTier(tempModel)
                        )
                    )
                }
                Result.success(models.sortedBy { it.label })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing models from $displayName", e)
            Result.failure(e)
        }
    }

    override fun streamChat(request: ChatRequest, apiKey: String): Flow<ChatStreamChunk> = flow {
        val messagesArray = JSONArray()

        request.systemPrompt?.let { sysPrompt ->
            if (sysPrompt.isNotEmpty()) {
                messagesArray.put(JSONObject().apply {
                    put("role", "system")
                    put("content", sysPrompt)
                })
            }
        }

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
            .addCustomHeaders(customHeaders)
            .post(requestBodyJson)
            .build()

        try {
            client.newCall(httpRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorMsg = response.body?.string() ?: "HTTP ${response.code}"
                    emit(ChatStreamChunk.Error("Error from $displayName: $errorMsg"))
                    return@flow
                }

                val reader = response.body?.charStream()?.let { BufferedReader(it) }
                if (reader == null) {
                    emit(ChatStreamChunk.Error("Failed to open response stream"))
                    return@flow
                }

                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val trimmed = line?.trim() ?: continue
                    if (trimmed.startsWith("data: ")) {
                        val dataStr = trimmed.substring(6).trim()
                        if (dataStr == "[DONE]") {
                            break
                        }

                        try {
                            val chunkJson = JSONObject(dataStr)
                            val choices = chunkJson.optJSONArray("choices")
                            if (choices != null && choices.length() > 0) {
                                val delta = choices.getJSONObject(0).optJSONObject("delta")
                                if (delta != null) {
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

                                    val content = delta.optString("content", "")
                                    val reasoning = delta.optString("reasoning_content", "")
                                        .ifEmpty { delta.optString("reasoning", "") }

                                    if (reasoning.isNotEmpty()) {
                                        emit(ChatStreamChunk.Thinking(reasoning))
                                    }
                                    if (content.isNotEmpty()) {
                                        emit(ChatStreamChunk.Content(content))
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // Ignore malformed partial chunks
                        }
                    }
                }
                emit(ChatStreamChunk.Done)
            }
        } catch (e: Exception) {
            emit(ChatStreamChunk.Error("Network error: ${e.localizedMessage ?: "Unknown connection error"}"))
        }
    }.flowOn(Dispatchers.IO)

    override fun supportsTools(): Boolean = true
    override fun supportsVision(): Boolean = true

    private val knownPaidModelIds = setOf(
        "gpt-4", "gpt-4-turbo", "gpt-4o", "gpt-4o-mini", "o1-preview", "o1-mini", "o1", "o3-mini",
        "claude-3-opus", "claude-3-sonnet", "claude-3-5-sonnet", "claude-3-haiku", "claude-3-5-haiku",
        "gemini-1.5-pro", "gemini-1.5-flash", "gemini-2.0-flash", "gemini-2.0-pro"
    )

    override fun isFreeTier(model: ModelInfo): Boolean {
        val lowerId = model.id.lowercase()
        if (lowerId.contains(":free")) {
            return true
        }
        for (paidId in knownPaidModelIds) {
            if (lowerId.contains(paidId)) {
                return false
            }
        }
        return model.isFree
    }

    private fun String.capitalizeWords(): String {
        return this.split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }
}
