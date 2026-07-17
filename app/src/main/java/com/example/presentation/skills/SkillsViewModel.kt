package com.example.presentation.skills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.SkillRepository
import com.example.domain.model.Skill
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SkillsViewModel @Inject constructor(
    private val skillRepository: SkillRepository
) : ViewModel() {

    val allSkills: StateFlow<List<Skill>> = skillRepository.allSkills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createSkill(
        name: String,
        description: String,
        instructions: String,
        triggerType: String = "manual",
        icon: String = "Extension"
    ) {
        viewModelScope.launch {
            val skill = Skill(
                id = UUID.randomUUID().toString(),
                name = name,
                description = description,
                instructions = instructions,
                triggerType = triggerType,
                icon = icon,
                isEnabled = false
            )
            skillRepository.insertSkill(skill)
        }
    }

    fun updateSkill(
        id: String,
        name: String,
        description: String,
        instructions: String,
        triggerType: String,
        icon: String,
        isEnabled: Boolean
    ) {
        viewModelScope.launch {
            val skill = Skill(
                id = id,
                name = name,
                description = description,
                instructions = instructions,
                triggerType = triggerType,
                icon = icon,
                isEnabled = isEnabled
            )
            skillRepository.insertSkill(skill)
        }
    }

    fun toggleSkill(skill: Skill) {
        viewModelScope.launch {
            skillRepository.insertSkill(skill.copy(isEnabled = !skill.isEnabled))
        }
    }

    fun deleteSkill(id: String) {
        viewModelScope.launch {
            skillRepository.deleteSkillById(id)
        }
    }

    fun duplicateSkill(skill: Skill) {
        viewModelScope.launch {
            val duplicated = skill.copy(
                id = UUID.randomUUID().toString(),
                name = "Copy of ${skill.name}",
                isEnabled = false
            )
            skillRepository.insertSkill(duplicated)
        }
    }

    fun importSkillFromJson(jsonString: String): Result<Unit> {
        return try {
            val imported = Json.decodeFromString<Skill>(jsonString)
            // Ensure a unique ID to avoid overwriting existing ones
            val sanitized = imported.copy(
                id = UUID.randomUUID().toString(),
                isEnabled = false
            )
            viewModelScope.launch {
                skillRepository.insertSkill(sanitized)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportSkillToJson(skill: Skill): String {
        return Json.encodeToString(Skill.serializer(), skill)
    }
}
