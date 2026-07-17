package com.example.data.repository

import com.example.data.local.ProjectDao
import com.example.data.local.toEntity
import com.example.domain.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepository @Inject constructor(
    private val projectDao: ProjectDao
) {
    val allProjects: Flow<List<Project>> = projectDao.getAllProjects()
        .map { list -> list.map { it.toDomain() } }

    suspend fun getProjectById(id: String): Project? = withContext(Dispatchers.IO) {
        projectDao.getProjectById(id)?.toDomain()
    }

    suspend fun insertProject(project: Project) = withContext(Dispatchers.IO) {
        projectDao.insertProject(project.toEntity())
    }

    suspend fun deleteProjectById(id: String) = withContext(Dispatchers.IO) {
        projectDao.deleteProjectById(id)
    }
}
