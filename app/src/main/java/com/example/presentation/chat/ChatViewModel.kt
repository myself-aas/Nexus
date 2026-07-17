package com.example.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AgentRepository
import com.example.data.repository.SkillRepository
import com.example.data.repository.ApiKeyRepository
import com.example.data.repository.ConversationRepository
import com.example.data.repository.ProjectRepository
import com.example.domain.model.*
import com.example.domain.usecase.StreamChatUseCase
import com.example.domain.usecase.ValidateApiKeyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

import com.example.data.repository.SettingsRepository
import com.example.data.remote.NvidiaNimProvider
import com.example.data.remote.OpenAiCompatProvider

import com.example.domain.usecase.FileExtractionUseCase
import android.net.Uri

import com.example.data.local.CachedModelDao
import com.example.data.local.toEntity
import com.example.data.repository.ModeRepository

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val apiKeyRepository: ApiKeyRepository,
    private val projectRepository: ProjectRepository,
    private val agentRepository: AgentRepository,
    private val skillRepository: SkillRepository,
    private val streamChatUseCase: StreamChatUseCase,
    private val validateApiKeyUseCase: ValidateApiKeyUseCase,
    private val settingsRepository: SettingsRepository,
    private val fileExtractionUseCase: FileExtractionUseCase,
    private val toolRegistry: com.example.domain.usecase.ToolRegistry,
    private val connectivityMonitor: com.example.util.ConnectivityMonitor,
    private val cachedModelDao: CachedModelDao,
    private val modeRepository: ModeRepository,
    private val recentExportDao: com.example.data.local.RecentExportDao,
    private val extractMemoryUseCase: com.example.domain.usecase.ExtractMemoryUseCase
) : ViewModel() {

    val isConnected = connectivityMonitor.isConnected
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val allModes: StateFlow<List<Mode>> = modeRepository.getAllModes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), modeRepository.builtInModes)

    val recentExports: StateFlow<List<com.example.data.local.RecentExportEntity>> = recentExportDao.getAllRecentExports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveRecentExport(fileName: String, filePath: String, mimeType: String, language: String, fileSize: Long) {
        viewModelScope.launch {
            val export = com.example.data.local.RecentExportEntity(
                id = UUID.randomUUID().toString(),
                fileName = fileName,
                filePath = filePath,
                mimeType = mimeType,
                timestamp = System.currentTimeMillis(),
                fileSize = fileSize,
                language = language
            )
            recentExportDao.insertRecentExport(export)
        }
    }

    fun deleteRecentExport(id: String) {
        viewModelScope.launch {
            recentExportDao.deleteRecentExportById(id)
        }
    }

    // Conversations Flow
    val allConversations: StateFlow<List<Conversation>> = conversationRepository.allConversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Conversation ID
    private val _selectedConversationId = MutableStateFlow<String?>(null)
    val selectedConversationId: StateFlow<String?> = _selectedConversationId.asStateFlow()

    // All Skills Flow
    val allSkills: StateFlow<List<Skill>> = skillRepository.allSkills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All Agents Flow
    val allAgents: StateFlow<List<Agent>> = agentRepository.allAgents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Map of conversationId to list of manually enabled skill IDs
    private val _manuallyEnabledSkillIdsMap = MutableStateFlow<Map<String, List<String>>>(emptyMap())

    val manuallyEnabledSkillIds: StateFlow<List<String>> = combine(
        _selectedConversationId,
        _manuallyEnabledSkillIdsMap
    ) { convoId, map ->
        if (convoId != null) map[convoId] ?: emptyList() else emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleManualSkill(skillId: String) {
        val convoId = _selectedConversationId.value ?: return
        val currentMap = _manuallyEnabledSkillIdsMap.value
        val listForConvo = currentMap[convoId] ?: emptyList()
        val newList = if (skillId in listForConvo) {
            listForConvo - skillId
        } else {
            listForConvo + skillId
        }
        _manuallyEnabledSkillIdsMap.value = currentMap + (convoId to newList)
    }

    // Active Conversation Message List
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentMessages: StateFlow<List<Message>> = _selectedConversationId
        .flatMapLatest { conversationId ->
            if (conversationId != null) {
                conversationRepository.getMessagesForConversation(conversationId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Conversation details
    val activeConversation: StateFlow<Conversation?> = combine(
        allConversations,
        _selectedConversationId
    ) { list, id ->
        list.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Streaming and Thinking States
    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    // Staged Attachments
    data class StagedAttachment(
        val uri: String,
        val type: String, // "image", "pdf", "file"
        val mimeType: String,
        val fileName: String? = null,
        val extractedText: String? = null // For images, this will hold base64 string
    )

    private val _stagedAttachments = MutableStateFlow<List<StagedAttachment>>(emptyList())
    val stagedAttachments: StateFlow<List<StagedAttachment>> = _stagedAttachments.asStateFlow()

    fun addStagedAttachment(attachment: StagedAttachment) {
        _stagedAttachments.value = _stagedAttachments.value + attachment
    }
    
    fun processFileAttachment(uri: Uri, mimeType: String?) {
        val actualMimeType = mimeType ?: "application/octet-stream"
        val type = when {
            actualMimeType.startsWith("image/") -> "image"
            actualMimeType == "application/pdf" -> "pdf"
            actualMimeType.startsWith("text/") -> "text"
            else -> "file"
        }
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val fileName = fileExtractionUseCase.getFileName(uri)
            val extractedData = fileExtractionUseCase.extractTextOrBase64(uri, actualMimeType)
            
            val attachment = StagedAttachment(
                uri = uri.toString(),
                type = type,
                mimeType = actualMimeType,
                fileName = fileName,
                extractedText = extractedData
            )
            addStagedAttachment(attachment)
        }
    }

    fun removeStagedAttachment(uri: String) {
        _stagedAttachments.value = _stagedAttachments.value.filter { it.uri != uri }
    }

    // Currently Streamed Response builder
    private val _streamedResponse = MutableStateFlow("")
    val streamedResponse: StateFlow<String> = _streamedResponse.asStateFlow()

    private val _streamedThinking = MutableStateFlow("")
    val streamedThinking: StateFlow<String> = _streamedThinking.asStateFlow()

    private val _errorText = MutableStateFlow<String?>(null)
    val errorText: StateFlow<String?> = _errorText.asStateFlow()

    // Model and Provider Switches
    private val _selectedProviderId = MutableStateFlow(settingsRepository.getDefaultProviderId())
    val selectedProviderId: StateFlow<String> = _selectedProviderId.asStateFlow()

    private val _selectedModelId = MutableStateFlow(settingsRepository.getDefaultModelId())
    val selectedModelId: StateFlow<String> = _selectedModelId.asStateFlow()

    // SWR Discovery States
    private val _isRefreshingModels = MutableStateFlow(false)
    val isRefreshingModels = _isRefreshingModels.asStateFlow()

    private val _modelDiscoveryError = MutableStateFlow<String?>(null)
    val modelDiscoveryError = _modelDiscoveryError.asStateFlow()

    private val _selectedModeId = MutableStateFlow("smart")
    val selectedModeId = _selectedModeId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val availableModels: StateFlow<List<ModelInfo>> = _selectedProviderId
        .flatMapLatest { providerId ->
            cachedModelDao.getModelsForProvider(providerId)
                .map { entities ->
                    if (entities.isEmpty()) {
                        if (providerId == "nvidia_nim") {
                            listOf(
                                ModelInfo("meta/llama-3.3-70b-instruct", "Llama 3.3 70B Instruct", 4096, true, false, true),
                                ModelInfo("deepseek/deepseek-r1", "DeepSeek R1 (Reasoning)", 8192, false, false, true),
                                ModelInfo("nvidia/llama-3.1-nemotron-70b-instruct", "Nemotron 70B Instruct", 4096, true, false, true)
                            )
                        } else emptyList()
                    } else {
                        entities.map { it.toDomain() }
                    }
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Configured Providers List Flow
    val configuredProviders: StateFlow<List<ApiKeyRef>> = apiKeyRepository.allApiKeyRefs
        .map { refs ->
            val hasNvidia = refs.any { it.providerId == "nvidia_nim" }
            val baseList = if (!hasNvidia) {
                listOf(
                    ApiKeyRef(
                        providerId = "nvidia_nim",
                        keyAlias = "secure_alias_nvidia_nim",
                        displayName = "NVIDIA NIM",
                        baseUrl = "https://integrate.api.nvidia.com/v1",
                        isCustom = false
                    )
                )
            } else emptyList()
            baseList + refs
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf(
            ApiKeyRef(
                providerId = "nvidia_nim",
                keyAlias = "secure_alias_nvidia_nim",
                displayName = "NVIDIA NIM",
                baseUrl = "https://integrate.api.nvidia.com/v1",
                isCustom = false
            )
        ))

    // Sheet Selection State
    private val _sheetSelectedProviderId = MutableStateFlow("nvidia_nim")
    val sheetSelectedProviderId = _sheetSelectedProviderId.asStateFlow()

    fun updateSheetProvider(providerId: String) {
        _sheetSelectedProviderId.value = providerId
        refreshModelsForProvider(providerId)
    }

    fun openModelSwitcher() {
        val currentProvider = _selectedProviderId.value
        _sheetSelectedProviderId.value = currentProvider
        refreshModelsForProvider(currentProvider)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val sheetAvailableModels: StateFlow<List<ModelInfo>> = _sheetSelectedProviderId
        .flatMapLatest { providerId ->
            cachedModelDao.getModelsForProvider(providerId)
                .map { entities ->
                    if (entities.isEmpty()) {
                        if (providerId == "nvidia_nim") {
                            listOf(
                                ModelInfo("meta/llama-3.3-70b-instruct", "Llama 3.3 70B Instruct", 4096, true, false, true),
                                ModelInfo("deepseek/deepseek-r1", "DeepSeek R1 (Reasoning)", 8192, false, false, true),
                                ModelInfo("nvidia/llama-3.1-nemotron-70b-instruct", "Nemotron 70B Instruct", 4096, true, false, true)
                            )
                        } else emptyList()
                    } else {
                        entities.map { it.toDomain() }
                    }
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getBestModelForMode(modeId: String, models: List<ModelInfo>): String? {
        if (models.isEmpty()) return null
        return when (modeId) {
            "quick_reply" -> {
                models.firstOrNull { 
                    val id = it.id.lowercase()
                    id.contains("8b") || id.contains("mini") || id.contains("flash") || id.contains("light")
                }?.id ?: models.firstOrNull()?.id
            }
            "casual" -> {
                models.firstOrNull {
                    val id = it.id.lowercase()
                    (id.contains("70b") || id.contains("instruct") || id.contains("chat")) && 
                    !id.contains("reason") && !id.contains("think") && !id.contains("-r1")
                }?.id ?: models.firstOrNull()?.id
            }
            "daily_tasks" -> {
                models.firstOrNull {
                    val id = it.id.lowercase()
                    id.contains("70b") || id.contains("instruct") || id.contains("gpt-4o")
                }?.id ?: models.firstOrNull()?.id
            }
            "hard_tasks" -> {
                models.firstOrNull {
                    val id = it.id.lowercase()
                    id.contains("405b") || id.contains("pro") || id.contains("large") || id.contains("coder")
                }?.id ?: models.firstOrNull { it.id.lowercase().contains("70b") }?.id ?: models.firstOrNull()?.id
            }
            "thinking", "reasoning" -> {
                models.firstOrNull {
                    val id = it.id.lowercase()
                    id.contains("reason") || id.contains("think") || id.contains("deepseek-r") || id.contains("-r1") || id.contains("qwq")
                }?.id ?: models.firstOrNull { it.id.lowercase().contains("deepseek") }?.id ?: models.firstOrNull()?.id
            }
            else -> models.firstOrNull()?.id
        }
    }

    fun getTemperatureForMode(modeId: String): Float {
        return when (modeId) {
            "quick_reply" -> 0.3f
            "casual" -> 0.7f
            "daily_tasks" -> 0.5f
            "hard_tasks" -> 0.5f
            "thinking", "reasoning" -> 0.6f
            else -> 0.7f
        }
    }

    fun refreshModelsForProvider(providerId: String) {
        viewModelScope.launch {
            _isRefreshingModels.value = true
            _modelDiscoveryError.value = null
            
            val apiKey = apiKeyRepository.getDecryptedApiKey(providerId) ?: ""
            if (apiKey.isEmpty()) {
                _isRefreshingModels.value = false
                return@launch
            }

            val provider = if (providerId == "nvidia_nim") {
                NvidiaNimProvider()
            } else {
                val ref = apiKeyRepository.getApiKeyRef(providerId)
                if (ref != null) {
                    OpenAiCompatProvider(ref.providerId, ref.displayName, ref.baseUrl, ref.customHeaders)
                } else null
            }

            if (provider != null) {
                provider.listModels(apiKey).onSuccess { models ->
                    if (models.isNotEmpty()) {
                        cachedModelDao.deleteModelsForProvider(providerId)
                        cachedModelDao.insertModels(models.map { it.toEntity(providerId, System.currentTimeMillis()) })
                        
                        // Set selected model if the user is refreshing the active provider
                        if (providerId == _selectedProviderId.value) {
                            val bestModelId = getBestModelForMode(_selectedModeId.value, models)
                            if (bestModelId != null) {
                                _selectedModelId.value = bestModelId
                            }
                        }
                    }
                    _isRefreshingModels.value = false
                    _modelDiscoveryError.value = null
                }.onFailure { error ->
                    _modelDiscoveryError.value = "Failed to load latest models: ${error.localizedMessage ?: "Network error"}"
                    _isRefreshingModels.value = false
                }
            } else {
                _isRefreshingModels.value = false
            }
        }
    }

    init {
        // Auto select first conversation on startup
        viewModelScope.launch {
            allConversations.collect { list ->
                if (_selectedConversationId.value == null && list.isNotEmpty()) {
                    _selectedConversationId.value = list.first().id
                }
            }
        }

        // Auto fetch models when provider changes
        viewModelScope.launch {
            _selectedProviderId.collectLatest { providerId ->
                fetchModelsForProvider(providerId)
            }
        }
    }

    fun selectConversation(id: String) {
        _selectedConversationId.value = id
        // Restore model, provider, and mode selection
        val conversation = allConversations.value.find { it.id == id }
        if (conversation != null) {
            _selectedProviderId.value = conversation.providerId
            _selectedModelId.value = conversation.modelId
            _selectedModeId.value = conversation.selectedModeId
        }
    }

    fun createNewConversation(
        providerId: String = _selectedProviderId.value,
        modelId: String = _selectedModelId.value,
        agentId: String? = null,
        projectId: String? = null,
        modeId: String = _selectedModeId.value
    ) {
        viewModelScope.launch {
            val agent = if (agentId != null) agentRepository.getAgentById(agentId) else null
            val resolvedProviderId = agent?.defaultProviderId ?: providerId
            val resolvedModelId = agent?.defaultModelId ?: modelId

            val id = UUID.randomUUID().toString()
            val title = if (agent != null) {
                "Chat as ${agent.name}"
            } else {
                "New Conversation"
            }

            val convo = Conversation(
                id = id,
                title = title,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                providerId = resolvedProviderId,
                modelId = resolvedModelId,
                systemPromptId = agentId,
                projectId = projectId,
                selectedModeId = modeId
            )
            conversationRepository.insertConversation(convo)
            _selectedConversationId.value = id
            
            _selectedProviderId.value = resolvedProviderId
            _selectedModelId.value = resolvedModelId
            _selectedModeId.value = modeId
        }
    }

    private var streamingJob: kotlinx.coroutines.Job? = null

    fun sendMessage(content: String) {
        if (!isConnected.value) {
            _errorText.value = "You are offline. Please check your internet connection."
            return
        }
        
        val convoId = _selectedConversationId.value
        if (convoId == null) {
            // Auto create a conversation if none selected
            viewModelScope.launch {
                val newId = UUID.randomUUID().toString()
                val convo = Conversation(
                    id = newId,
                    title = "New Conversation",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    providerId = _selectedProviderId.value,
                    modelId = _selectedModelId.value,
                    selectedModeId = _selectedModeId.value
                )
                conversationRepository.insertConversation(convo)
                _selectedConversationId.value = newId
                sendMessageInternal(newId, content)
            }
        } else {
            sendMessageInternal(convoId, content)
        }
    }

    private fun sendMessageInternal(conversationId: String, content: String) {
        if (_isStreaming.value) return
        if (content.trim().isEmpty() && _stagedAttachments.value.isEmpty()) return
        
        val attachmentsToSend = _stagedAttachments.value.map { staged ->
            Attachment(
                id = UUID.randomUUID().toString(),
                messageId = "", // set in usecase
                type = staged.type,
                localUri = staged.uri,
                mimeType = staged.mimeType,
                fileName = staged.fileName,
                extractedText = staged.extractedText
            )
        }
        _stagedAttachments.value = emptyList()

        streamingJob = viewModelScope.launch {
            _isStreaming.value = true
            _streamedResponse.value = ""
            _streamedThinking.value = ""
            _errorText.value = null

            val convo = allConversations.value.find { it.id == conversationId } ?: return@launch

            streamChatUseCase(
                conversationId = conversationId,
                userMessageContent = content.trim(),
                providerId = convo.providerId,
                modelId = convo.modelId,
                agentId = convo.systemPromptId,
                projectId = convo.projectId,
                manuallyEnabledSkillIds = _manuallyEnabledSkillIdsMap.value[conversationId] ?: emptyList(),
                userAttachments = attachmentsToSend,
                temperature = getTemperatureForMode(_selectedModeId.value)
            ).collect { chunk ->
                when (chunk) {
                    is ChatStreamChunk.Content -> {
                        _streamedResponse.value += chunk.text
                    }
                    is ChatStreamChunk.Thinking -> {
                        _streamedThinking.value += chunk.text
                    }
                    is ChatStreamChunk.ToolCallChunk -> {
                        // Optionally update some UI state indicating tool is running
                    }
                    is ChatStreamChunk.RateLimited -> {
                        _errorText.value = "Rate limited. Retrying in ${chunk.secondsToWait} seconds..."
                    }
                    is ChatStreamChunk.Error -> {
                        _errorText.value = if (chunk.error is com.example.domain.model.ApiError.ContextLengthError) {
                            "Context length exceeded. Please start a new conversation."
                        } else {
                            chunk.message
                        }
                        _isStreaming.value = false
                    }
                    is ChatStreamChunk.Done -> {
                        _isStreaming.value = false
                        
                        // Trigger local memory extraction asynchronously
                        viewModelScope.launch {
                            extractMemoryUseCase(
                                conversationId = conversationId,
                                providerId = convo.providerId,
                                modelId = convo.modelId,
                                projectId = convo.projectId
                            )
                        }

                        // Check if the last assistant message has tool calls
                        val messages = conversationRepository.getMessagesForConversation(conversationId).first()
                        val lastMessage = messages.lastOrNull()
                        if (lastMessage != null && lastMessage.role == "assistant" && lastMessage.toolCalls.isNotEmpty()) {
                            // Execute tools sequentially and append results
                            viewModelScope.launch {
                                lastMessage.toolCalls.forEach { tc ->
                                    val tool = toolRegistry.getTool(tc.name)
                                    val result = tool?.execute(tc.arguments) ?: "Error: Tool ${tc.name} not found."
                                    val toolMessage = Message(
                                        id = UUID.randomUUID().toString(),
                                        conversationId = conversationId,
                                        role = "tool",
                                        content = result,
                                        createdAt = System.currentTimeMillis(),
                                        toolCallId = tc.id,
                                        toolName = tc.name
                                    )
                                    conversationRepository.insertMessage(toolMessage)
                                }
                                // Re-trigger generation automatically
                                sendMessageInternal(conversationId, "")
                            }
                        }

                        // Auto rename conversation on first message
                        if (convo.title == "New Conversation" && content.isNotBlank()) {
                            val newTitle = if (content.trim().isNotEmpty()) {
                                val words = content.trim().split(" ")
                                words.take(4).joinToString(" ") + if (words.size > 4) "..." else ""
                            } else if (attachmentsToSend.isNotEmpty()) {
                                "Attachment"
                            } else {
                                "Chat"
                            }
                            conversationRepository.updateConversationTitle(conversationId, newTitle)
                        }
                    }
                }
            }
        }
    }

    fun stopGenerating() {
        streamingJob?.cancel()
        _isStreaming.value = false
    }

    fun clearError() {
        _errorText.value = null
    }

    fun selectModel(providerId: String, modelId: String) {
        _selectedProviderId.value = providerId
        _selectedModelId.value = modelId
        
        // Update active conversation model selection
        val convoId = _selectedConversationId.value
        if (convoId != null) {
            viewModelScope.launch {
                val convo = allConversations.value.find { it.id == convoId }
                if (convo != null) {
                    conversationRepository.insertConversation(
                        convo.copy(providerId = providerId, modelId = modelId)
                    )
                }
            }
        }
    }

    fun selectMode(modeId: String) {
        _selectedModeId.value = modeId
        
        // Update active conversation mode selection
        val convoId = _selectedConversationId.value
        if (convoId != null) {
            viewModelScope.launch {
                val convo = allConversations.value.find { it.id == convoId }
                if (convo != null) {
                    conversationRepository.insertConversation(
                        convo.copy(selectedModeId = modeId)
                    )
                }
            }
        }
    }

    fun createCustomMode(
        name: String,
        description: String,
        targetStrategy: String,
        temperature: Float,
        maxTokens: Int?,
        topP: Float?
    ) {
        viewModelScope.launch {
            val customMode = Mode(
                id = UUID.randomUUID().toString(),
                name = name,
                description = description,
                targetStrategy = targetStrategy,
                temperature = temperature,
                maxTokens = maxTokens,
                topP = topP,
                isCustom = true
            )
            modeRepository.insertCustomMode(customMode)
        }
    }

    fun deleteCustomMode(id: String) {
        viewModelScope.launch {
            modeRepository.deleteCustomModeById(id)
            if (_selectedModeId.value == id) {
                selectMode("smart")
            }
        }
    }

    fun renameConversation(id: String, newTitle: String) {
        viewModelScope.launch {
            conversationRepository.updateConversationTitle(id, newTitle)
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            conversationRepository.deleteConversationById(id)
            if (_selectedConversationId.value == id) {
                _selectedConversationId.value = allConversations.value.firstOrNull { it.id != id }?.id
            }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            conversationRepository.deleteMessageById(messageId)
        }
    }

    fun editAndResendMessage(messageId: String, newContent: String) {
        viewModelScope.launch {
            val targetMessage = conversationRepository.getMessageById(messageId) ?: return@launch
            val conversationId = targetMessage.conversationId
            val allMessages = conversationRepository.getMessagesForConversationList(conversationId)
            
            // Delete all messages created after the target message
            allMessages.filter { it.createdAt > targetMessage.createdAt }.forEach {
                conversationRepository.deleteMessageById(it.id)
            }
            
            // Also delete the target message itself, so that when we send newContent,
            // the StreamChatUseCase inserts a fresh user message with the edited text.
            conversationRepository.deleteMessageById(targetMessage.id)
            
            // Trigger generation
            sendMessageInternal(conversationId, newContent)
        }
    }

    fun regenerateMessage(messageId: String) {
        viewModelScope.launch {
            val targetMessage = conversationRepository.getMessageById(messageId) ?: return@launch
            if (targetMessage.role != "assistant") return@launch
            val conversationId = targetMessage.conversationId
            val allMessages = conversationRepository.getMessagesForConversationList(conversationId)
            
            val sortedMessages = allMessages.sortedBy { it.createdAt }
            val targetIndex = sortedMessages.indexOfFirst { it.id == messageId }
            if (targetIndex < 0) return@launch
            
            // Find preceding user message
            val precedingUserMessage = sortedMessages.take(targetIndex).lastOrNull { it.role == "user" } ?: return@launch
            
            // Delete target message and all messages after it
            sortedMessages.drop(targetIndex).forEach {
                conversationRepository.deleteMessageById(it.id)
            }
            
            // Delete preceding user message so we can resend without duplication
            conversationRepository.deleteMessageById(precedingUserMessage.id)
            
            // Re-generate
            sendMessageInternal(conversationId, precedingUserMessage.content)
        }
    }

    fun branchConversation(messageId: String) {
        viewModelScope.launch {
            val targetMessage = conversationRepository.getMessageById(messageId) ?: return@launch
            val originalConvoId = targetMessage.conversationId
            val originalConvo = allConversations.value.find { it.id == originalConvoId } ?: return@launch
            
            val allMessages = conversationRepository.getMessagesForConversationList(originalConvoId)
            val sortedMessages = allMessages.sortedBy { it.createdAt }
            val targetIndex = sortedMessages.indexOfFirst { it.id == messageId }
            if (targetIndex < 0) return@launch
            
            val messagesToCopy = sortedMessages.take(targetIndex + 1)
            
            val newConvoId = UUID.randomUUID().toString()
            val newConvo = originalConvo.copy(
                id = newConvoId,
                title = "${originalConvo.title} (Branch)",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            // Insert conversation
            conversationRepository.insertConversation(newConvo)
            
            // Copy messages
            messagesToCopy.forEach { msg ->
                val newMsg = msg.copy(
                    id = UUID.randomUUID().toString(),
                    conversationId = newConvoId
                )
                conversationRepository.insertMessage(newMsg)
            }
            
            // Select new conversation
            _selectedConversationId.value = newConvoId
        }
    }

    private fun fetchModelsForProvider(providerId: String) {
        refreshModelsForProvider(providerId)
    }
}
