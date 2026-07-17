package com.example.domain.usecase

import com.example.data.repository.ApiKeyRepository
import com.example.data.repository.ConversationRepository
import com.example.data.repository.AgentRepository
import com.example.data.repository.ModelProviderRegistry
import com.example.domain.model.ChatRequest
import com.example.domain.model.ChatStreamChunk
import com.example.domain.model.Message
import com.example.domain.model.ToolCall
import com.example.domain.model.Attachment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import com.example.data.repository.ModeRepository
import com.example.data.local.CachedModelDao
import java.util.UUID
import javax.inject.Inject

class StreamChatUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val apiKeyRepository: ApiKeyRepository,
    private val providerRegistry: ModelProviderRegistry,
    private val agentRepository: AgentRepository,
    private val toolRegistry: ToolRegistry,
    private val composeSystemPromptUseCase: ComposeSystemPromptUseCase,
    private val modeRepository: ModeRepository,
    private val cachedModelDao: CachedModelDao
) {
    operator fun invoke(
        conversationId: String,
        userMessageContent: String,
        providerId: String,
        modelId: String,
        agentId: String? = null,
        projectId: String? = null,
        manuallyEnabledSkillIds: List<String> = emptyList(),
        userAttachments: List<Attachment> = emptyList(),
        temperature: Float? = null
    ): Flow<ChatStreamChunk> = flow {
        // 1. Insert User Message (unless it's an automated tool response call, in which case userMessageContent is empty)
        if (userMessageContent.isNotEmpty() || userAttachments.isNotEmpty()) {
            val userMsgId = UUID.randomUUID().toString()
            val userMsg = Message(
                id = userMsgId,
                conversationId = conversationId,
                role = "user",
                content = userMessageContent,
                createdAt = System.currentTimeMillis(),
                attachments = userAttachments.map { it.copy(messageId = userMsgId) }
            )
            conversationRepository.insertMessage(userMsg)
            
            userMsg.attachments.forEach {
                conversationRepository.insertAttachment(it)
            }
        }

        // 2. Fetch Chat History (take last 15 for context window)
        val messages = conversationRepository.getMessagesForConversation(conversationId).first()
            .takeLast(15)

        // 3. Compose system prompt
        val systemPrompt = composeSystemPromptUseCase(agentId, projectId, manuallyEnabledSkillIds, conversationId = conversationId)

        // 4. Resolve Tools based on Agent
        val activeTools = mutableListOf<com.example.domain.model.Tool>()
        var agentTemperature: Float? = null
        if (agentId != null) {
            val agent = agentRepository.getAgentById(agentId)
            if (agent != null) {
                agentTemperature = agent.temperature
                agent.enabledToolIds.forEach { toolId ->
                    toolRegistry.getTool(toolId)?.let { activeTools.add(it) }
                }
            }
        } else {
            // No agent, could be defaults, let's just pass all tools for now or we could add a toggle.
            // Let's not pass tools by default if no agent is selected for simplicity, unless we add a conversation-level tool toggle.
        }

        // 5. Retrieve API Key
        val apiKey = apiKeyRepository.getDecryptedApiKey(providerId) ?: ""
        if (apiKey.isEmpty()) {
            emit(ChatStreamChunk.Error("No API key configured for $providerId. Please enter your key in Settings."))
            return@flow
        }

        // 6. Get LLM Provider
        val provider = providerRegistry.getProvider(providerId)
        if (provider == null) {
            emit(ChatStreamChunk.Error("Model provider '$providerId' is not registered."))
            return@flow
        }

        val conversation = conversationRepository.getConversationById(conversationId)
        val modeId = conversation?.selectedModeId ?: "daily_tasks"
        val latestUserContent = userMessageContent.ifEmpty {
            messages.lastOrNull { it.role == "user" }?.content ?: ""
        }
        val resolvedSettings = resolveSettings(providerId, latestUserContent, modeId, modelId)
        
        val finalModelId = resolvedSettings.modelId
        val finalTemperature = temperature ?: agentTemperature ?: resolvedSettings.temperature
        val finalMaxTokens = resolvedSettings.maxTokens
        val finalTopP = resolvedSettings.topP
        
        val finalSystemPrompt = if (resolvedSettings.systemPromptAddendum != null) {
            systemPrompt + resolvedSettings.systemPromptAddendum
        } else {
            systemPrompt
        }

        // 7. Start Streaming from Provider
        val request = ChatRequest(
            messages = messages,
            modelId = finalModelId,
            temperature = finalTemperature,
            systemPrompt = finalSystemPrompt,
            tools = activeTools,
            maxTokens = finalMaxTokens,
            topP = finalTopP
        )

        val assistantMsgId = UUID.randomUUID().toString()
        var accumulatedContent = ""
        var accumulatedThinking = ""
        
        // Tool Calls Tracking
        val toolCallsMap = mutableMapOf<String, StringBuilder>()
        val toolCallNames = mutableMapOf<String, String>()

        try {
            provider.streamChat(request, apiKey).collect { chunk ->
                when (chunk) {
                    is ChatStreamChunk.Content -> {
                        accumulatedContent += chunk.text
                        emit(chunk)
                    }
                    is ChatStreamChunk.Thinking -> {
                        accumulatedThinking += chunk.text
                        emit(chunk)
                    }
                    is ChatStreamChunk.ToolCallChunk -> {
                        val id = chunk.id
                        if (id != null) {
                            toolCallsMap[id] = toolCallsMap.getOrDefault(id, StringBuilder()).append(chunk.argumentsDelta)
                            if (chunk.name != null) toolCallNames[id] = chunk.name
                        } else {
                            // Append to the last tool call id
                            val lastId = toolCallsMap.keys.lastOrNull()
                            if (lastId != null) {
                                toolCallsMap[lastId]?.append(chunk.argumentsDelta)
                            }
                        }
                        emit(chunk) // So the UI can show "Tool is running..."
                    }
                    is ChatStreamChunk.Error -> {
                        emit(chunk)
                    }
                    is ChatStreamChunk.RateLimited -> {
                        emit(chunk)
                    }
                    is ChatStreamChunk.Done -> {
                        // Persist final assistant message
                        val finalToolCalls = toolCallsMap.map { (id, args) ->
                            ToolCall(id, toolCallNames[id] ?: "unknown", args.toString())
                        }
                        
                        val assistantMsg = Message(
                            id = assistantMsgId,
                            conversationId = conversationId,
                            role = "assistant",
                            content = accumulatedContent,
                            createdAt = System.currentTimeMillis(),
                            reasoningContent = accumulatedThinking.ifEmpty { null },
                            toolCalls = finalToolCalls
                        )
                        conversationRepository.insertMessage(assistantMsg)
                        emit(chunk)
                    }
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            if (accumulatedContent.isNotEmpty() || accumulatedThinking.isNotEmpty()) {
                val assistantMsg = Message(
                    id = assistantMsgId,
                    conversationId = conversationId,
                    role = "assistant",
                    content = accumulatedContent,
                    createdAt = System.currentTimeMillis(),
                    reasoningContent = accumulatedThinking.ifEmpty { null }
                )
                conversationRepository.insertMessage(assistantMsg)
            }
            throw e
        } catch (e: Exception) {
            emit(ChatStreamChunk.Error("Streaming crashed: ${e.localizedMessage ?: "Unknown error"}"))
        }
    }

    private suspend fun resolveSettings(
        providerId: String,
        userMessageContent: String,
        modeId: String,
        defaultModelId: String
    ): ResolvedSettings {
        val mode = modeRepository.getModeById(modeId) ?: modeRepository.builtInModes.find { it.id == "smart" }!!
        
        val models = cachedModelDao.getModelsForProviderSync(providerId)
        val freeModels = models.filter { it.isFree }.ifEmpty { models }

        var resolvedModelId = defaultModelId
        var finalStrategy = mode.targetStrategy
        var promptAddendum: String? = null

        if (finalStrategy == "smart") {
            val lowerMsg = userMessageContent.lowercase()
            val hasCode = lowerMsg.contains("class ") || lowerMsg.contains("def ") || lowerMsg.contains("function ") || lowerMsg.contains("import ") || lowerMsg.contains("const ") || lowerMsg.contains("```")
            val isLong = userMessageContent.length > 250
            val hasMathOrComplexity = lowerMsg.contains("algorithm") || lowerMsg.contains("complexity") || lowerMsg.contains("optimize") || lowerMsg.contains("solve") || lowerMsg.contains("equation") || lowerMsg.contains("proof")
            
            finalStrategy = if (hasCode || isLong || hasMathOrComplexity) {
                "hard_tasks"
            } else if (lowerMsg.contains("why") || lowerMsg.contains("how to") || lowerMsg.contains("explain") || lowerMsg.contains("compare") || lowerMsg.contains("summary") || lowerMsg.contains("summarize") || lowerMsg.contains("list")) {
                "daily_tasks"
            } else {
                "casual"
            }
        }

        if (mode.isCustom) {
            if (finalStrategy.startsWith("pinned:")) {
                resolvedModelId = finalStrategy.substringAfter("pinned:")
            } else if (models.any { it.modelId == finalStrategy }) {
                resolvedModelId = finalStrategy
            } else {
                resolvedModelId = when (finalStrategy) {
                    "quick_reply" -> {
                        freeModels.firstOrNull {
                            val id = it.modelId.lowercase()
                            id.contains("8b") || id.contains("3b") || id.contains("1b") || id.contains("mini") || id.contains("flash") || id.contains("light")
                        }?.modelId ?: freeModels.firstOrNull()?.modelId ?: defaultModelId
                    }
                    "casual" -> {
                        freeModels.firstOrNull {
                            val id = it.modelId.lowercase()
                            (id.contains("70b") || id.contains("instruct") || id.contains("chat")) &&
                            !id.contains("reason") && !id.contains("think") && !id.contains("-r1") && !id.contains("coder")
                        }?.modelId ?: freeModels.firstOrNull()?.modelId ?: defaultModelId
                    }
                    "daily_tasks" -> {
                        freeModels.firstOrNull {
                            val id = it.modelId.lowercase()
                            id.contains("70b") || id.contains("instruct") || id.contains("gpt-4o") || id.contains("claude")
                        }?.modelId ?: freeModels.firstOrNull()?.modelId ?: defaultModelId
                    }
                    "hard_tasks" -> {
                        freeModels.firstOrNull {
                            val id = it.modelId.lowercase()
                            id.contains("405b") || id.contains("pro") || id.contains("large") || id.contains("coder")
                        }?.modelId ?: freeModels.firstOrNull { it.modelId.lowercase().contains("70b") }?.modelId ?: freeModels.firstOrNull()?.modelId ?: defaultModelId
                    }
                    "thinking", "reasoning" -> {
                        if (finalStrategy == "reasoning") {
                            promptAddendum = "\n\n[SYSTEM INSTRUCTION: Please perform explicit step-by-step reasoning before providing the final structured output.]\n"
                        }
                        freeModels.firstOrNull {
                            val id = it.modelId.lowercase()
                            id.contains("reason") || id.contains("think") || id.contains("deepseek-r") || id.contains("-r1") || id.contains("qwq")
                        }?.modelId ?: freeModels.firstOrNull {
                            val id = it.modelId.lowercase()
                            id.contains("405b") || id.contains("70b")
                        }?.modelId ?: freeModels.firstOrNull()?.modelId ?: defaultModelId
                    }
                    else -> defaultModelId
                }
            }
        } else {
            resolvedModelId = when (finalStrategy) {
                "quick_reply" -> {
                    freeModels.firstOrNull {
                        val id = it.modelId.lowercase()
                        id.contains("8b") || id.contains("3b") || id.contains("1b") || id.contains("mini") || id.contains("flash") || id.contains("light")
                    }?.modelId ?: freeModels.firstOrNull()?.modelId ?: defaultModelId
                }
                "casual" -> {
                    freeModels.firstOrNull {
                        val id = it.modelId.lowercase()
                        (id.contains("70b") || id.contains("instruct") || id.contains("chat")) &&
                        !id.contains("reason") && !id.contains("think") && !id.contains("-r1") && !id.contains("coder")
                    }?.modelId ?: freeModels.firstOrNull()?.modelId ?: defaultModelId
                }
                "daily_tasks" -> {
                    freeModels.firstOrNull {
                        val id = it.modelId.lowercase()
                        id.contains("70b") || id.contains("instruct") || id.contains("gpt-4o") || id.contains("claude")
                    }?.modelId ?: freeModels.firstOrNull()?.modelId ?: defaultModelId
                }
                "hard_tasks" -> {
                    freeModels.firstOrNull {
                        val id = it.modelId.lowercase()
                        id.contains("405b") || id.contains("pro") || id.contains("large") || id.contains("coder")
                    }?.modelId ?: freeModels.firstOrNull { it.modelId.lowercase().contains("70b") }?.modelId ?: freeModels.firstOrNull()?.modelId ?: defaultModelId
                }
                "thinking", "reasoning" -> {
                    if (finalStrategy == "reasoning") {
                        promptAddendum = "\n\n[SYSTEM INSTRUCTION: Please perform explicit step-by-step reasoning before providing the final structured output.]\n"
                    }
                    freeModels.firstOrNull {
                        val id = it.modelId.lowercase()
                        id.contains("reason") || id.contains("think") || id.contains("deepseek-r") || id.contains("-r1") || id.contains("qwq")
                    }?.modelId ?: freeModels.firstOrNull {
                        val id = it.modelId.lowercase()
                        id.contains("405b") || id.contains("70b")
                    }?.modelId ?: freeModels.firstOrNull()?.modelId ?: defaultModelId
                }
                else -> defaultModelId
            }
        }

        return ResolvedSettings(
            modelId = resolvedModelId,
            temperature = mode.temperature,
            maxTokens = mode.maxTokens,
            topP = mode.topP,
            systemPromptAddendum = promptAddendum
        )
    }

    private data class ResolvedSettings(
        val modelId: String,
        val temperature: Float,
        val maxTokens: Int?,
        val topP: Float?,
        val systemPromptAddendum: String?
    )
}
