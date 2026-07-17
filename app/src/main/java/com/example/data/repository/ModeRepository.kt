package com.example.data.repository

import com.example.data.local.CustomModeDao
import com.example.data.local.toEntity
import com.example.domain.model.Mode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModeRepository @Inject constructor(
    private val customModeDao: CustomModeDao
) {
    val builtInModes = listOf(
        Mode(
            id = "smart",
            name = "Smart",
            description = "Auto-selects best mode based on input complexity",
            targetStrategy = "smart",
            temperature = 0.5f,
            maxTokens = 2048
        ),
        Mode(
            id = "quick_reply",
            name = "Quick Reply",
            description = "Fastest, lowest-latency help for quick factual answers",
            targetStrategy = "quick_reply",
            temperature = 0.3f,
            maxTokens = 512
        ),
        Mode(
            id = "casual",
            name = "Casual",
            description = "Friendly, conversational help with creative tasks",
            targetStrategy = "casual",
            temperature = 0.7f,
            maxTokens = 2048
        ),
        Mode(
            id = "daily_tasks",
            name = "Daily Tasks",
            description = "Balanced help with writing, summaries, or organizing",
            targetStrategy = "daily_tasks",
            temperature = 0.5f,
            maxTokens = 2048
        ),
        Mode(
            id = "hard_tasks",
            name = "Hard Tasks",
            description = "Largest, high-context models for coding and math",
            targetStrategy = "hard_tasks",
            temperature = 0.3f,
            maxTokens = 4096
        ),
        Mode(
            id = "thinking",
            name = "Thinking",
            description = "Deep thinking & reasoning before output is shown",
            targetStrategy = "thinking",
            temperature = 0.6f,
            maxTokens = 4096
        ),
        Mode(
            id = "reasoning",
            name = "Reasoning",
            description = "Step-by-step structured reasoning addendum",
            targetStrategy = "reasoning",
            temperature = 0.2f,
            maxTokens = 4096
        )
    )

    fun getAllModes(): Flow<List<Mode>> {
        return customModeDao.getAllCustomModes().map { customList ->
            builtInModes + customList.map { it.toDomain() }
        }
    }

    suspend fun insertCustomMode(mode: Mode) {
        customModeDao.insertCustomMode(mode.toEntity())
    }

    suspend fun deleteCustomModeById(id: String) {
        customModeDao.deleteCustomModeById(id)
    }

    suspend fun getModeById(id: String): Mode? {
        return builtInModes.find { it.id == id } ?: customModeDao.getCustomModeById(id)?.toDomain()
    }
}
