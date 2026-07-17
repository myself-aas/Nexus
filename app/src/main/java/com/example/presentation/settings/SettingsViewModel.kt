package com.example.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ApiKeyRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.MemoryRepository
import com.example.data.repository.ProjectRepository
import com.example.domain.model.ApiKeyRef
import com.example.domain.model.ModelInfo
import com.example.domain.model.Memory
import com.example.domain.model.Project
import com.example.domain.usecase.ValidateApiKeyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val apiKeyRepository: ApiKeyRepository,
    private val validateApiKeyUseCase: ValidateApiKeyUseCase,
    private val settingsRepository: SettingsRepository,
    private val memoryRepository: MemoryRepository,
    private val projectRepository: ProjectRepository
) : ViewModel() {

    val apiKeyRefs = apiKeyRepository.allApiKeyRefs
    val allMemories = memoryRepository.allMemories
    val allProjects = projectRepository.allProjects

    private val _nvidiaKeyStatus = MutableStateFlow<ValidationStatus>(ValidationStatus.Idle)
    val nvidiaKeyStatus: StateFlow<ValidationStatus> = _nvidiaKeyStatus.asStateFlow()

    private val _customKeyStatus = MutableStateFlow<ValidationStatus>(ValidationStatus.Idle)
    val customKeyStatus: StateFlow<ValidationStatus> = _customKeyStatus.asStateFlow()

    private val _testStatus = MutableStateFlow<ValidationStatus>(ValidationStatus.Idle)
    val testStatus: StateFlow<ValidationStatus> = _testStatus.asStateFlow()

    private val _availableModels = MutableStateFlow<List<ModelInfo>>(emptyList())
    val availableModels: StateFlow<List<ModelInfo>> = _availableModels.asStateFlow()

    // Global Default Configuration States
    private val _defaultProviderId = MutableStateFlow(settingsRepository.getDefaultProviderId())
    val defaultProviderId: StateFlow<String> = _defaultProviderId.asStateFlow()

    private val _defaultModelId = MutableStateFlow(settingsRepository.getDefaultModelId())
    val defaultModelId: StateFlow<String> = _defaultModelId.asStateFlow()

    init {
        // Automatically fetch models if Nvidia key is present
        viewModelScope.launch {
            apiKeyRefs.collectLatest { refs ->
                val hasNvidia = refs.any { it.providerId == "nvidia_nim" }
                if (hasNvidia) {
                    fetchModelsForProvider("nvidia_nim")
                }
            }
        }
    }

    fun setDefaultProviderId(providerId: String) {
        settingsRepository.setDefaultProviderId(providerId)
        _defaultProviderId.value = providerId
    }

    fun setDefaultModelId(modelId: String) {
        settingsRepository.setDefaultModelId(modelId)
        _defaultModelId.value = modelId
    }

    fun validateAndSaveNvidiaKey(apiKey: String) {
        if (!apiKey.startsWith("nvapi-")) {
            _nvidiaKeyStatus.value = ValidationStatus.Error("API key must start with 'nvapi-'")
            return
        }

        _nvidiaKeyStatus.value = ValidationStatus.Loading
        viewModelScope.launch {
            val result = validateApiKeyUseCase("nvidia_nim", apiKey, "https://integrate.api.nvidia.com/v1")
            if (result.isSuccess) {
                apiKeyRepository.saveApiKey(
                    providerId = "nvidia_nim",
                    apiKey = apiKey,
                    displayName = "NVIDIA NIM",
                    baseUrl = "https://integrate.api.nvidia.com/v1",
                    isCustom = false
                )
                _nvidiaKeyStatus.value = ValidationStatus.Success
                fetchModelsForProvider("nvidia_nim")
            } else {
                _nvidiaKeyStatus.value = ValidationStatus.Error(result.exceptionOrNull()?.localizedMessage ?: "Validation failed")
            }
        }
    }

    fun testConnection(displayName: String, baseUrl: String, apiKey: String, customHeaders: String? = null) {
        if (displayName.isEmpty() || baseUrl.isEmpty() || apiKey.isEmpty()) {
            _testStatus.value = ValidationStatus.Error("All fields are required")
            return
        }
        _testStatus.value = ValidationStatus.Loading
        viewModelScope.launch {
            val providerId = displayName.lowercase().replace(" ", "_")
            val result = validateApiKeyUseCase(providerId, apiKey, baseUrl, customHeaders)
            if (result.isSuccess) {
                _testStatus.value = ValidationStatus.Success
            } else {
                _testStatus.value = ValidationStatus.Error(result.exceptionOrNull()?.localizedMessage ?: "Test connection failed")
            }
        }
    }

    fun resetTestStatus() {
        _testStatus.value = ValidationStatus.Idle
    }

    fun saveCustomProvider(displayName: String, baseUrl: String, apiKey: String, customHeaders: String? = null) {
        if (displayName.isEmpty() || baseUrl.isEmpty() || apiKey.isEmpty()) {
            _customKeyStatus.value = ValidationStatus.Error("All fields are required")
            return
        }

        _customKeyStatus.value = ValidationStatus.Loading
        viewModelScope.launch {
            val providerId = displayName.lowercase().replace(" ", "_")
            val result = validateApiKeyUseCase(providerId, apiKey, baseUrl, customHeaders)
            if (result.isSuccess) {
                apiKeyRepository.saveApiKey(
                    providerId = providerId,
                    apiKey = apiKey,
                    displayName = displayName,
                    baseUrl = baseUrl,
                    isCustom = true,
                    customHeaders = customHeaders
                )
                _customKeyStatus.value = ValidationStatus.Success
            } else {
                _customKeyStatus.value = ValidationStatus.Error(result.exceptionOrNull()?.localizedMessage ?: "Validation failed")
            }
        }
    }

    fun deleteApiKey(providerId: String) {
        viewModelScope.launch {
            apiKeyRepository.deleteApiKey(providerId)
            if (providerId == "nvidia_nim") {
                _nvidiaKeyStatus.value = ValidationStatus.Idle
                _availableModels.value = emptyList()
            }
        }
    }

    private fun fetchModelsForProvider(providerId: String) {
        viewModelScope.launch {
            val apiKey = apiKeyRepository.getDecryptedApiKey(providerId) ?: return@launch
            val provider = if (providerId == "nvidia_nim") {
                com.example.data.remote.NvidiaNimProvider()
            } else {
                val ref = apiKeyRepository.getApiKeyRef(providerId) ?: return@launch
                com.example.data.remote.OpenAiCompatProvider(ref.providerId, ref.displayName, ref.baseUrl, ref.customHeaders)
            }
            provider.listModels(apiKey).onSuccess { models ->
                _availableModels.value = models
            }.onFailure {
                _availableModels.value = emptyList()
            }
        }
    }

    fun addManualMemory(content: String, scope: String, projectId: String? = null) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val words = content.lowercase().split(Regex("[^a-zA-Z0-9]+")).filter { it.length > 2 }
            val dummyEmbedding = FloatArray(16) { i ->
                if (i < words.size) words[i].hashCode().toFloat() else 0.0f
            }
            memoryRepository.insertMemory(
                Memory(
                    id = UUID.randomUUID().toString(),
                    scope = scope,
                    content = content.trim(),
                    createdAt = System.currentTimeMillis(),
                    projectId = projectId,
                    embedding = dummyEmbedding
                )
            )
        }
    }

    fun updateMemory(memory: Memory) {
        if (memory.content.isBlank()) return
        viewModelScope.launch {
            memoryRepository.insertMemory(memory)
        }
    }

    fun deleteMemory(id: String) {
        viewModelScope.launch {
            memoryRepository.deleteMemoryById(id)
        }
    }

    fun wipeAllMemories() {
        viewModelScope.launch {
            memoryRepository.deleteAllMemories()
        }
    }
}

sealed class ValidationStatus {
    object Idle : ValidationStatus()
    object Loading : ValidationStatus()
    object Success : ValidationStatus()
    data class Error(val message: String) : ValidationStatus()
}
