package com.example.data.repository

import com.example.data.local.SkillDao
import com.example.data.local.toEntity
import com.example.domain.model.Skill
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SkillRepository @Inject constructor(
    private val skillDao: SkillDao
) {
    val allSkills: Flow<List<Skill>> = skillDao.getAllSkills()
        .map { list -> list.map { it.toDomain() } }

    val enabledSkills: Flow<List<Skill>> = skillDao.getEnabledSkills()
        .map { list -> list.map { it.toDomain() } }

    suspend fun getSkillById(id: String): Skill? = withContext(Dispatchers.IO) {
        skillDao.getSkillById(id)?.toDomain()
    }

    suspend fun insertSkill(skill: Skill) = withContext(Dispatchers.IO) {
        skillDao.insertSkill(skill.toEntity())
    }

    suspend fun deleteSkillById(id: String) = withContext(Dispatchers.IO) {
        skillDao.deleteSkillById(id)
    }
}
