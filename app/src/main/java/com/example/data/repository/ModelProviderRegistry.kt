package com.example.data.repository

import com.example.data.remote.NvidiaNimProvider
import com.example.data.remote.OpenAiCompatProvider
import com.example.domain.model.ModelProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelProviderRegistry @Inject constructor(
    private val nvidiaNimProvider: NvidiaNimProvider,
    private val apiKeyRepository: ApiKeyRepository
) {
    suspend fun getProvider(providerId: String): ModelProvider? {
        if (providerId == "nvidia_nim") {
            return nvidiaNimProvider
        }
        
        // Fetch from API Key references in the local database
        val keyRef = apiKeyRepository.getApiKeyRef(providerId) ?: return null
        return OpenAiCompatProvider(
            id = keyRef.providerId,
            displayName = keyRef.displayName,
            baseUrl = keyRef.baseUrl,
            customHeaders = keyRef.customHeaders
        )
    }
}
