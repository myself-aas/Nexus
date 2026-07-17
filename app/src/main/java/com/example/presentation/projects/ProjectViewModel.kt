package com.example.presentation.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ProjectRepository
import com.example.domain.model.Project
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ProjectViewModel @Inject constructor(
    private val projectRepository: ProjectRepository
) : ViewModel() {
    val allProjects = projectRepository.allProjects.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun createProject(name: String, description: String, instructions: String) {
        viewModelScope.launch {
            projectRepository.insertProject(
                Project(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    description = description,
                    customInstructions = instructions,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }
}
