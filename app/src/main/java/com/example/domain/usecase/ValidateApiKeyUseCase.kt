package com.example.domain.usecase

import com.example.data.remote.OpenAiCompatProvider
import com.example.data.repository.ModelProviderRegistry
import javax.inject.Inject

class ValidateApiKeyUseCase @Inject constructor(
    private val providerRegistry: ModelProviderRegistry
) {
    suspend operator fun invoke(
        providerId: String,
        apiKey: String,
        baseUrl: String,
        customHeaders: String? = null
    ): Result<Boolean> {
        if (apiKey.isEmpty()) {
            return Result.failure(Exception("API Key cannot be empty"))
        }

        // Standard client-side format checks
        if (providerId == "nvidia_nim" && !apiKey.startsWith("nvapi-")) {
            return Result.failure(Exception("NVIDIA NIM API keys must start with 'nvapi-'"))
        }

        return try {
            val provider = if (providerId == "nvidia_nim") {
                providerRegistry.getProvider(providerId) ?: com.example.data.remote.NvidiaNimProvider()
            } else {
                OpenAiCompatProvider(providerId, "Test Connection", baseUrl, customHeaders)
            }

            val result = provider.listModels(apiKey)
            if (result.isSuccess) {
                Result.success(true)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Failed to connect or authorize API key"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
