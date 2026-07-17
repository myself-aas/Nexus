package com.example.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("nexus_ai_global_settings", Context.MODE_PRIVATE)

    fun getDefaultProviderId(): String {
        return prefs.getString("default_provider_id", "nvidia_nim") ?: "nvidia_nim"
    }

    fun setDefaultProviderId(providerId: String) {
        prefs.edit().putString("default_provider_id", providerId).apply()
    }

    fun getDefaultModelId(): String {
        return prefs.getString("default_model_id", "meta/llama-3.3-70b-instruct") ?: "meta/llama-3.3-70b-instruct"
    }

    fun setDefaultModelId(modelId: String) {
        prefs.edit().putString("default_model_id", modelId).apply()
    }
}
