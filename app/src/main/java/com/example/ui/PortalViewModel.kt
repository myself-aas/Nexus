package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.ChatEntity
import com.example.data.database.DocumentEntity
import com.example.data.database.MessageEntity
import com.example.data.database.UserEntity
import com.example.data.repository.ChatRepository
import com.example.network.MultiProviderClient
import com.example.security.MfaHelper
import com.example.ui.export.ExportHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

@Deprecated(
    message = "Legacy ViewModel stack. Prefer presentation/* ViewModels backed by data/local repositories."
)
class PortalViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = ChatRepository(application)
    private val context: Context get() = getApplication()

    // Active User State
    val activeUser: StateFlow<UserEntity?> = repository.activeUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Chats List Flow
    val allChats: StateFlow<List<ChatEntity>> = repository.allChats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Chat State
    private val _selectedChatId = MutableStateFlow<String?>(null)
    val selectedChatId: StateFlow<String?> = _selectedChatId.asStateFlow()

    // Current Messages Flow based on selected Chat ID
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentChatMessages: StateFlow<List<MessageEntity>> = _selectedChatId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getMessagesForChat(id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Streaming & Loading States
    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    // Document Database Flow
    val allDocuments: StateFlow<List<DocumentEntity>> = repository.allDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Secure Keys Exist Status (checks securely without storing plain text keys in memory)
    private val _savedKeys = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val savedKeys: StateFlow<Map<String, Boolean>> = _savedKeys.asStateFlow()

    // Generated MFA Secret Flow
    private val _mfaSecretState = MutableStateFlow<String?>(null)
    val mfaSecretState: StateFlow<String?> = _mfaSecretState.asStateFlow()

    init {
        refreshKeysStatus()
        viewModelScope.launch {
            // Select the most recent chat on startup if available
            allChats.collect { list ->
                if (_selectedChatId.value == null && list.isNotEmpty()) {
                    _selectedChatId.value = list.first().id
                }
            }
        }
    }

    // --- CREDENTIALS MANAGEMENT (END-TO-END ENCRYPTED) ---
    
    fun refreshKeysStatus() {
        viewModelScope.launch {
            val providers = repository.getSavedProviders()
            val statusMap = mapOf(
                "gemini" to providers.contains("gemini"),
                "openai" to providers.contains("openai"),
                "anthropic" to providers.contains("anthropic"),
                "nvidia" to providers.contains("nvidia")
            )
            _savedKeys.value = statusMap
        }
    }

    fun saveApiKey(provider: String, key: String) {
        viewModelScope.launch {
            if (key.trim().isNotEmpty()) {
                repository.saveCredential(provider.lowercase(), key.trim())
                refreshKeysStatus()
            }
        }
    }

    fun deleteApiKey(provider: String) {
        viewModelScope.launch {
            repository.deleteCredential(provider.lowercase())
            refreshKeysStatus()
        }
    }

    // --- CHAT MANAGEMENT ---

    fun selectChat(chatId: String) {
        _selectedChatId.value = chatId
    }

    fun createChat(provider: String, model: String) {
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            val defaultTitle = "New Chat (${provider.uppercase()})"
            repository.createChat(id, defaultTitle, provider, model)
            _selectedChatId.value = id
        }
    }

    fun updateChatTitle(chatId: String, newTitle: String) {
        viewModelScope.launch {
            repository.updateChatTitle(chatId, newTitle)
        }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            repository.deleteChat(chatId)
            if (_selectedChatId.value == chatId) {
                _selectedChatId.value = allChats.value.firstOrNull { it.id != chatId }?.id
            }
        }
    }

    // --- CONVERSATION & STREAMING LOGIC ---

    fun sendMessage(chatId: String, content: String) {
        if (content.trim().isEmpty() || _isStreaming.value) return

        viewModelScope.launch {
            _isStreaming.value = true
            
            // 1. Insert User Message
            val userMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                chatId = chatId,
                role = "user",
                content = content.trim(),
                timestamp = System.currentTimeMillis(),
                type = "TEXT"
            )
            repository.insertMessage(userMsg)

            // 2. Fetch Chat Details
            val chat = allChats.value.find { it.id == chatId } ?: return@launch
            
            // 3. Vector Similarity Search for context mapping
            val relevantDocs = repository.searchSimilarDocuments(content.trim(), limit = 3)
            val contextEntities = relevantDocs.map { it.first }

            // 4. Retrieve Encrypted Key for Provider
            val apiKey = repository.getDecryptedCredential(chat.provider.lowercase()) ?: ""

            // 5. Initialize Assistant Response Message Placholder
            val assistantMsgId = UUID.randomUUID().toString()
            val initialAssistantMsg = MessageEntity(
                id = assistantMsgId,
                chatId = chatId,
                role = "assistant",
                content = "",
                timestamp = System.currentTimeMillis() + 10,
                type = "MARKDOWN" // Claude default render
            )
            repository.insertMessage(initialAssistantMsg)

            // 6. Accumulate streamed response
            val messagesList = repository.getMessagesForChat(chatId).first()
            val streamedContent = StringBuilder()

            try {
                MultiProviderClient.streamResponse(
                    provider = chat.provider,
                    model = chat.model,
                    apiKey = apiKey,
                    messages = messagesList,
                    contextDocs = contextEntities,
                    onChunk = { chunk ->
                        streamedContent.append(chunk)
                        viewModelScope.launch {
                            repository.insertMessage(
                                initialAssistantMsg.copy(content = streamedContent.toString())
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                streamedContent.append("\nError streaming content: ${e.message}")
                repository.insertMessage(initialAssistantMsg.copy(content = streamedContent.toString()))
            } finally {
                _isStreaming.value = false
                // Auto update Chat title to user message first few words if title was default
                if (chat.title.startsWith("New Chat")) {
                    val words = content.trim().split(" ")
                    val newTitle = words.take(4).joinToString(" ") + if (words.size > 4) "..." else ""
                    repository.updateChatTitle(chatId, newTitle)
                }
            }
        }
    }

    // --- MULTI-MODAL GENERATION (IMAGES, AUDIO, VIDEO) ---

    fun generateMultimedia(chatId: String, prompt: String, type: String) {
        if (prompt.trim().isEmpty() || _isStreaming.value) return

        viewModelScope.launch {
            _isStreaming.value = true
            
            // Create user message showing prompt
            val userMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                chatId = chatId,
                role = "user",
                content = "Generate $type: \"$prompt\"",
                timestamp = System.currentTimeMillis(),
                type = "TEXT"
            )
            repository.insertMessage(userMsg)

            val chat = allChats.value.find { it.id == chatId } ?: return@launch
            val apiKey = repository.getDecryptedCredential(chat.provider.lowercase()) ?: ""

            // Trigger generation
            val mediaPath = MultiProviderClient.generateMedia(
                context = context,
                prompt = prompt,
                type = type.uppercase(),
                apiKey = apiKey
            )

            // Insert assistant response showing multimedia card
            val assistantMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                chatId = chatId,
                role = "assistant",
                content = "Successfully generated $type under local path: $mediaPath",
                timestamp = System.currentTimeMillis() + 10,
                type = type.uppercase(), // IMAGE, AUDIO, VIDEO
                mediaUrl = mediaPath
            )
            repository.insertMessage(assistantMsg)
            _isStreaming.value = false
        }
    }

    // --- DOCUMENT MANAGEMENT ---

    fun importDocument(name: String, content: String, mimeType: String, size: Long) {
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            repository.addDocument(id, name, content, mimeType, size)
        }
    }

    fun deleteDocument(id: String) {
        viewModelScope.launch {
            repository.deleteDocument(id)
        }
    }

    // --- AUTHENTICATION & LOGIN FLOWS ---

    fun loginWithGoogle(email: String, displayName: String, photoUrl: String? = null) {
        viewModelScope.launch {
            val userId = email.hashCode().toString()
            repository.registerOrLoginUser(userId, email, displayName, photoUrl)
        }
    }

    fun generateMfaSetup() {
        viewModelScope.launch {
            val user = repository.getActiveUserSync() ?: return@launch
            val secret = MfaHelper.generateSecret()
            _mfaSecretState.value = secret
        }
    }

    fun verifyAndEnableMfa(code: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val user = repository.getActiveUserSync() ?: return@launch
            val secret = _mfaSecretState.value ?: return@launch
            val isValid = MfaHelper.verifyCode(secret, code)
            if (isValid) {
                repository.updateMfaSecret(user.id, secret)
                _mfaSecretState.value = null
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun verifyMfaLogin(code: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val user = repository.getActiveUserSync() ?: return@launch
            val secret = user.mfaSecret ?: return@launch
            val isValid = MfaHelper.verifyCode(secret, code)
            onResult(isValid)
        }
    }

    fun disableMfa() {
        viewModelScope.launch {
            val user = repository.getActiveUserSync() ?: return@launch
            repository.updateMfaSecret(user.id, null)
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }

    // --- EXPORT CHAT HISTORIES ---

    fun exportChat(chatId: String, format: String): File? {
        val chat = allChats.value.find { it.id == chatId } ?: return null
        val messages = currentChatMessages.value
        return if (format.uppercase() == "PDF") {
            ExportHelper.exportToPdf(context, chat, messages)
        } else {
            ExportHelper.exportToJson(context, chat, messages)
        }
    }
}
