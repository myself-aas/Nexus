package com.example.data.repository

import com.example.data.local.MemoryDao
import com.example.data.local.toEntity
import com.example.domain.model.Memory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryRepository @Inject constructor(
    private val memoryDao: MemoryDao
) {
    val allMemories: Flow<List<Memory>> = memoryDao.getAllMemories()
        .map { list -> list.map { it.toDomain() } }

    fun getMemoriesByScope(scope: String): Flow<List<Memory>> {
        return memoryDao.getMemoriesByScope(scope)
            .map { list -> list.map { it.toDomain() } }
    }

    suspend fun getMemoryById(id: String): Memory? = withContext(Dispatchers.IO) {
        memoryDao.getMemoryById(id)?.toDomain()
    }

    suspend fun insertMemory(memory: Memory) = withContext(Dispatchers.IO) {
        memoryDao.insertMemory(memory.toEntity())
    }

    suspend fun deleteMemoryById(id: String) = withContext(Dispatchers.IO) {
        memoryDao.deleteMemoryById(id)
    }

    suspend fun deleteAllMemories() = withContext(Dispatchers.IO) {
        memoryDao.deleteAllMemories()
    }

    suspend fun getAllMemoriesSync(): List<Memory> = withContext(Dispatchers.IO) {
        memoryDao.getAllMemoriesSync().map { it.toDomain() }
    }

    suspend fun getMemoriesForProjectSync(projectId: String): List<Memory> = withContext(Dispatchers.IO) {
        memoryDao.getMemoriesForProjectSync(projectId).map { it.toDomain() }
    }

    suspend fun getGlobalMemoriesSync(): List<Memory> = withContext(Dispatchers.IO) {
        memoryDao.getGlobalMemoriesSync().map { it.toDomain() }
    }
}
