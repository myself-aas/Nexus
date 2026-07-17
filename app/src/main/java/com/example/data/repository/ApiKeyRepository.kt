package com.example.data.repository

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.data.local.ApiKeyRefDao
import com.example.data.local.toEntity
import com.example.domain.model.ApiKeyRef
import com.example.security.CryptoHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiKeyRepository @Inject constructor(
    private val apiKeyRefDao: ApiKeyRefDao,
    @ApplicationContext private val context: Context
) {
    private val isEncryptedPref by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "nexus_api_secure_keys_encrypted",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    private val prefs by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "nexus_api_secure_keys_encrypted",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences("nexus_api_secure_keys_fallback", Context.MODE_PRIVATE)
        }
    }

    val allApiKeyRefs: Flow<List<ApiKeyRef>> = apiKeyRefDao.getAllApiKeyRefs()
        .map { list -> list.map { it.toDomain() } }

    suspend fun getApiKeyRef(providerId: String): ApiKeyRef? = withContext(Dispatchers.IO) {
        apiKeyRefDao.getApiKeyRef(providerId)?.toDomain()
    }

    suspend fun saveApiKey(
        providerId: String,
        apiKey: String,
        displayName: String,
        baseUrl: String,
        isCustom: Boolean = false,
        customHeaders: String? = null
    ) = withContext(Dispatchers.IO) {
        if (isEncryptedPref) {
            prefs.edit().putString(providerId, apiKey).apply()
        } else {
            val (encrypted, iv) = CryptoHelper.encrypt(apiKey)
            if (encrypted.isNotEmpty() && iv.isNotEmpty()) {
                prefs.edit().putString(providerId, "$encrypted:$iv").apply()
            }
        }

        val ref = ApiKeyRef(
            providerId = providerId,
            keyAlias = "secure_alias_$providerId",
            displayName = displayName,
            baseUrl = baseUrl,
            isCustom = isCustom,
            customHeaders = customHeaders
        )
        apiKeyRefDao.insertApiKeyRef(ref.toEntity())
    }

    suspend fun getDecryptedApiKey(providerId: String): String? = withContext(Dispatchers.IO) {
        val value = prefs.getString(providerId, null) ?: return@withContext null
        if (isEncryptedPref) {
            value
        } else {
            val parts = value.split(":")
            if (parts.size == 2) {
                val decrypted = CryptoHelper.decrypt(parts[0], parts[1])
                if (decrypted.isNotEmpty()) decrypted else null
            } else {
                null
            }
        }
    }

    suspend fun deleteApiKey(providerId: String) = withContext(Dispatchers.IO) {
        prefs.edit().remove(providerId).apply()
        apiKeyRefDao.deleteApiKeyRef(providerId)
    }
}
