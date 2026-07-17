package com.example.data.repository

import com.example.data.local.AgentDao
import com.example.data.local.toEntity
import com.example.domain.model.Agent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentRepository @Inject constructor(
    private val agentDao: AgentDao
) {
    val allAgents: Flow<List<Agent>> = agentDao.getAllAgents()
        .map { list -> list.map { it.toDomain() } }

    suspend fun getAgentById(id: String): Agent? = withContext(Dispatchers.IO) {
        agentDao.getAgentById(id)?.toDomain()
    }

    suspend fun insertAgent(agent: Agent) = withContext(Dispatchers.IO) {
        agentDao.insertAgent(agent.toEntity())
    }

    suspend fun deleteAgentById(id: String) = withContext(Dispatchers.IO) {
        agentDao.deleteAgentById(id)
    }
}
