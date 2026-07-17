package com.example.presentation.projects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.domain.model.Project

@Composable
fun ProjectListScreen(
    viewModel: ProjectViewModel = hiltViewModel(),
    onProjectClick: (String) -> Unit,
    onAddProject: () -> Unit
) {
    val projects by viewModel.allProjects.collectAsState(initial = emptyList())

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddProject) {
                Icon(Icons.Default.Add, contentDescription = "Add Project")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(projects) { project ->
                ListItem(
                    headlineContent = { Text(project.name) },
                    supportingContent = { Text(project.description) },
                    modifier = Modifier.clickable { onProjectClick(project.id) }
                )
            }
        }
    }
}
