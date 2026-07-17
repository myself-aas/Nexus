package com.example.domain.usecase

import com.example.data.repository.AgentRepository
import com.example.data.repository.ProjectRepository
import com.example.data.repository.SkillRepository
import com.example.data.repository.ConversationRepository
import com.example.data.repository.MemoryRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ComposeSystemPromptUseCase @Inject constructor(
    private val agentRepository: AgentRepository,
    private val skillRepository: SkillRepository,
    private val projectRepository: ProjectRepository,
    private val conversationRepository: ConversationRepository,
    private val memoryRepository: MemoryRepository,
    private val memoryScorer: MemoryScorer
) {
    suspend operator fun invoke(
        agentId: String? = null,
        projectId: String? = null,
        manuallyEnabledSkillIds: List<String> = emptyList(),
        conversationId: String? = null
    ): String {
        val promptBuilder = StringBuilder()

        // 1. Base Persona
        promptBuilder.append("You are Nexus AI, a powerful, offline-first multi-provider AI assistant designed to replicate a high-end chat experience. Provide thorough, precise, beautifully structured Markdown answers.\n\n")

        // 2. Custom Agent System Prompt
        if (agentId != null) {
            val agent = agentRepository.getAgentById(agentId)
            if (agent != null && agent.systemPrompt.isNotEmpty()) {
                promptBuilder.append("=== Agent Prompt: ${agent.name} ===\n")
                promptBuilder.append(agent.systemPrompt).append("\n\n")
            }
        }

        // 3. Active Skills' Instructions
        val activeSkills = skillRepository.enabledSkills.first()
        val manualSkills = skillRepository.allSkills.first().filter { it.id in manuallyEnabledSkillIds }
        
        val agentSkills = if (agentId != null) {
            val agent = agentRepository.getAgentById(agentId)
            if (agent != null) {
                skillRepository.allSkills.first().filter { it.id in agent.enabledSkillIds }
            } else emptyList()
        } else emptyList()

        val combinedSkills = (activeSkills + manualSkills + agentSkills).distinctBy { it.id }

        if (combinedSkills.isNotEmpty()) {
            promptBuilder.append("=== Active Skills Instructions ===\n")
            combinedSkills.forEach { skill ->
                promptBuilder.append("Skill [${skill.name}]: ${skill.instructions}\n\n")
            }
        }

        // 4. Project Custom Instructions (Claude-like shared context)
        if (projectId != null) {
            val project = projectRepository.getProjectById(projectId)
            if (project != null && project.customInstructions.isNotEmpty()) {
                promptBuilder.append("=== Project Shared Context: ${project.name} ===\n")
                promptBuilder.append(project.customInstructions).append("\n\n")
            }
        }

        // 5. Relevant Memories Section (On-device context injection)
        if (conversationId != null) {
            val messages = conversationRepository.getMessagesForConversation(conversationId).first()
            val recentUserMessages = messages.filter { it.role == "user" }.takeLast(3)
            val queryText = recentUserMessages.joinToString(" ") { it.content }
            
            if (queryText.isNotBlank()) {
                val candidateMemories = if (projectId != null && projectId.isNotEmpty()) {
                    memoryRepository.getMemoriesForProjectSync(projectId)
                } else {
                    memoryRepository.getGlobalMemoriesSync()
                }

                if (candidateMemories.isNotEmpty()) {
                    val rankedMemories = memoryScorer.rank(candidateMemories, queryText)
                    val relevant = rankedMemories.filter { it.second > 0.0 }.take(3).map { it.first }
                    if (relevant.isNotEmpty()) {
                        promptBuilder.append("=== Relevant Memories ===\n")
                        promptBuilder.append("Here are durable facts you remembered about the user from previous conversations. Use them to tailor your responses appropriately:\n")
                        relevant.forEach { memory ->
                            promptBuilder.append("- ").append(memory.content).append("\n")
                        }
                        promptBuilder.append("\n")
                    }
                }
            }
        }

        return promptBuilder.toString().trim()
    }
}
