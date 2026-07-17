package com.example.presentation.agents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AgentRepository
import com.example.data.repository.SkillRepository
import com.example.domain.model.Agent
import com.example.domain.model.Skill
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AgentsViewModel @Inject constructor(
    private val agentRepository: AgentRepository,
    private val skillRepository: SkillRepository
) : ViewModel() {

    val allAgents: StateFlow<List<Agent>> = agentRepository.allAgents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSkills: StateFlow<List<Skill>> = skillRepository.allSkills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createAgent(
        name: String,
        description: String,
        systemPrompt: String,
        icon: String = "Face",
        defaultProviderId: String = "nvidia_nim",
        defaultModelId: String = "nvidia/llama-3.1-nemotron-70b-instruct",
        enabledSkillIds: List<String> = emptyList(),
        temperature: Float = 0.7f
    ) {
        viewModelScope.launch {
            val agent = Agent(
                id = UUID.randomUUID().toString(),
                name = name,
                description = description,
                icon = icon,
                systemPrompt = systemPrompt,
                defaultProviderId = defaultProviderId,
                defaultModelId = defaultModelId,
                enabledSkillIds = enabledSkillIds,
                enabledToolIds = emptyList(),
                schedule = null,
                isEnabled = true,
                temperature = temperature
            )
            agentRepository.insertAgent(agent)
        }
    }

    fun updateAgent(agent: Agent) {
        viewModelScope.launch {
            agentRepository.insertAgent(agent)
        }
    }

    fun deleteAgent(id: String) {
        viewModelScope.launch {
            agentRepository.deleteAgentById(id)
        }
    }
}
