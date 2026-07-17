package com.example.domain.usecase

import android.util.Log
import com.example.data.repository.ApiKeyRepository
import com.example.data.repository.ConversationRepository
import com.example.data.repository.MemoryRepository
import com.example.data.repository.ModelProviderRegistry
import com.example.domain.model.ChatRequest
import com.example.domain.model.ChatStreamChunk
import com.example.domain.model.Message
import com.example.domain.model.Memory
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExtractMemoryUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val memoryRepository: MemoryRepository,
    private val apiKeyRepository: ApiKeyRepository,
    private val providerRegistry: ModelProviderRegistry
) {
    suspend operator fun invoke(
        conversationId: String,
        providerId: String,
        modelId: String,
        projectId: String? = null
    ) {
        try {
            // 1. Fetch recent messages for context
            val allMessages = conversationRepository.getMessagesForConversation(conversationId).first()
            if (allMessages.isEmpty()) return

            // Look at the last few messages (e.g. up to 10) to extract context
            val recentMessages = allMessages.takeLast(10)

            // Format the messages into a clean transcript
            val transcript = recentMessages.joinToString("\n") { msg ->
                val roleName = when (msg.role) {
                    "user" -> "User"
                    "assistant" -> "Assistant"
                    else -> msg.role.replaceFirstChar { it.uppercase() }
                }
                "$roleName: ${msg.content}"
            }

            val systemPrompt = """
                You are a background on-device assistant for Nexus AI. Your task is to analyze the conversation history between the User and the Assistant and extract any long-term, durable facts or preferences about the user (such as their name, coding preferences, tech stack, interests, recurring constraints, or ongoing projects) that would be useful to remember across future chat sessions.

                Rules:
                - Extract ONLY 1 to 3 short, concise bulleted statements (each starting with a dash '-').
                - Each statement must contain exactly one fact, written in 3rd person (e.g., 'The user prefers Kotlin for Android development', 'The user's name is John').
                - Do NOT include any metadata, headings, formatting besides dashes, or conversational filler.
                - If there are no new long-term, durable facts or preferences mentioned, reply with exactly: NONE
            """.trimIndent()

            val extractionRequestMessage = Message(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                role = "user",
                content = "Extract long-term facts or preferences from this transcript:\n\n$transcript",
                createdAt = System.currentTimeMillis()
            )

            val apiKey = apiKeyRepository.getDecryptedApiKey(providerId) ?: ""
            if (apiKey.isEmpty()) return

            val provider = providerRegistry.getProvider(providerId) ?: return

            val request = ChatRequest(
                messages = listOf(extractionRequestMessage),
                modelId = modelId,
                temperature = 0.1f,
                systemPrompt = systemPrompt,
                maxTokens = 150
            )

            val responseBuilder = StringBuilder()
            provider.streamChat(request, apiKey).collect { chunk ->
                if (chunk is ChatStreamChunk.Content) {
                    responseBuilder.append(chunk.text)
                }
            }

            val result = responseBuilder.toString().trim()
            if (result.isEmpty() || result.equals("NONE", ignoreCase = true)) {
                Log.d("ExtractMemory", "No durable facts extracted.")
                return
            }

            // Parse lines and save
            val lines = result.split("\n")
                .map { it.trim() }
                .filter { it.startsWith("-") || it.startsWith("*") || it.isNotEmpty() }

            lines.forEach { line ->
                val content = line.trim().removePrefix("-").removePrefix("*").trim()
                if (content.isNotEmpty() && !content.equals("NONE", ignoreCase = true)) {
                    // Let's compute a simple TF-IDF or keyword embedding placeholder
                    val words = content.lowercase().split(Regex("[^a-zA-Z0-9]+")).filter { it.length > 2 }
                    val dummyEmbedding = FloatArray(16) { i ->
                        if (i < words.size) words[i].hashCode().toFloat() else 0.0f
                    }

                    val memory = Memory(
                        id = UUID.randomUUID().toString(),
                        scope = if (projectId != null) "project" else "global",
                        content = content,
                        createdAt = System.currentTimeMillis(),
                        sourceConversationId = conversationId,
                        projectId = projectId,
                        embedding = dummyEmbedding
                    )
                    memoryRepository.insertMemory(memory)
                    Log.d("ExtractMemory", "Successfully stored memory: $content")
                }
            }

        } catch (e: Exception) {
            Log.e("ExtractMemory", "Failed to extract memory: ${e.localizedMessage}")
        }
    }
}
