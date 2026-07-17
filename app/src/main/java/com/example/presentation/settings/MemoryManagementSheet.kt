package com.example.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.model.Memory
import com.example.domain.model.Project
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassChip
import com.example.ui.theme.GlassPillButton

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MemoryManagementSheet(
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel
) {
    val memories by viewModel.allMemories.collectAsState(initial = emptyList())
    val projects by viewModel.allProjects.collectAsState(initial = emptyList())

    var newMemoryText by remember { mutableStateOf("") }
    var selectedScope by remember { mutableStateOf("global") } // "global" or "project"
    var selectedProject by remember { mutableStateOf<Project?>(null) }
    var projectDropdownExpanded by remember { mutableStateOf(false) }

    // Dialog edit state
    var memoryToEdit by remember { mutableStateOf<Memory?>(null) }
    var editText by remember { mutableStateOf("") }

    // Wipe confirmation state
    var showWipeConfirm by remember { mutableStateOf(false) }
    var wipeTextConfirmation by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize().testTag("memory_management_sheet"),
                topBar = {
                    TopAppBar(
                        title = { Text("🧠 Local Memory Engine", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    // Privacy Shield Note
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Security Shield",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "100% On-Device Privacy Shield",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "All memories are stored strictly offline in your secure client-side database. No data is ever transmitted, synced, or leaked to any Nexus-owned servers. Facts are injected into system prompts dynamically to personalize your chat turns.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = com.example.ui.theme.TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Quick Stats & Actions
                    item {
                        GlassSurface(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Active Fact Store",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${memories.size} stored user preferences / facts",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = com.example.ui.theme.TextSecondary
                                    )
                                }
                                if (memories.isNotEmpty()) {
                                    Button(
                                        onClick = { showWipeConfirm = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.Default.DeleteForever, contentDescription = "Wipe")
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Wipe All")
                                    }
                                }
                            }
                        }
                    }

                    // Add Custom Memory / Fact manually
                    item {
                        GlassSurface(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Add Custom Fact Manually",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = newMemoryText,
                                    onValueChange = { newMemoryText = it },
                                    label = { Text("What should Nexus AI remember about you?") },
                                    placeholder = { Text("e.g., The user prefers Python for backends and Kotlin for apps.") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.secondary
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Scope Selector
                                Text("Memory Scope:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                ) {
                                    GlassChip(
                                        selected = selectedScope == "global",
                                        onClick = { selectedScope = "global" },
                                        label = "Global (Eligible everywhere)",
                                        activeColor = MaterialTheme.colorScheme.secondary
                                    )
                                    GlassChip(
                                        selected = selectedScope == "project",
                                        onClick = {
                                            selectedScope = "project"
                                            if (selectedProject == null && projects.isNotEmpty()) {
                                                selectedProject = projects.first()
                                            }
                                        },
                                        label = "Project-Scoped",
                                        activeColor = MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (selectedScope == "project" && projects.isNotEmpty()) {
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedCard(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { projectDropdownExpanded = true }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = selectedProject?.name ?: "Select Project",
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select")
                                            }
                                        }

                                        DropdownMenu(
                                            expanded = projectDropdownExpanded,
                                            onDismissRequest = { projectDropdownExpanded = false }
                                        ) {
                                            projects.forEach { project ->
                                                DropdownMenuItem(
                                                    text = { Text(project.name) },
                                                    onClick = {
                                                        selectedProject = project
                                                        projectDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                } else if (selectedScope == "project" && projects.isEmpty()) {
                                    Text(
                                        text = "No projects found. Please create a project first in the conversations screen.",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                GlassPillButton(
                                    onClick = {
                                        viewModel.addManualMemory(
                                            content = newMemoryText,
                                            scope = if (selectedScope == "project") "project" else "global",
                                            projectId = if (selectedScope == "project") selectedProject?.id else null
                                        )
                                        newMemoryText = ""
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = newMemoryText.isNotBlank() && (selectedScope == "global" || selectedProject != null)
                                ) {
                                    Text("Add Memory Fact", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // List of existing memories
                    item {
                        Text(
                            text = "Stored Memories",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }

                    if (memories.isEmpty()) {
                        item {
                            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier.padding(32.dp).fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "No facts remembered yet. Chat with Nexus AI to automatically extract memories, or add some manually above!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = com.example.ui.theme.TextSecondary
                                    )
                                }
                            }
                        }
                    } else {
                        items(memories) { memory ->
                            val currentProjectName = remember(memory, projects) {
                                if (memory.scope == "project") {
                                    projects.find { it.id == memory.projectId }?.name ?: "Unknown Project"
                                } else null
                            }

                            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if (memory.scope == "project") {
                                                    GlassChip(
                                                        selected = true,
                                                        onClick = {},
                                                        label = "Project: $currentProjectName",
                                                        activeColor = MaterialTheme.colorScheme.primary
                                                    )
                                                } else {
                                                    GlassChip(
                                                        selected = true,
                                                        onClick = {},
                                                        label = "Global",
                                                        activeColor = MaterialTheme.colorScheme.secondary
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = memory.content,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = com.example.ui.theme.TextLight
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(
                                                onClick = {
                                                    memoryToEdit = memory
                                                    editText = memory.content
                                                }
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit Fact", tint = MaterialTheme.colorScheme.primary)
                                            }
                                            IconButton(onClick = { viewModel.deleteMemory(memory.id) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete Fact", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Dialog
    if (memoryToEdit != null) {
        AlertDialog(
            onDismissRequest = { memoryToEdit = null },
            title = { Text("Edit Fact", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        label = { Text("Memory Content") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val original = memoryToEdit
                        if (original != null && editText.isNotBlank()) {
                            viewModel.updateMemory(original.copy(content = editText.trim()))
                        }
                        memoryToEdit = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { memoryToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // High Friction Wipe Dialog
    if (showWipeConfirm) {
        AlertDialog(
            onDismissRequest = { showWipeConfirm = false },
            title = { Text("Wipe All Memories?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("This action will permanently erase all durable facts stored in your secure offline Room database. This cannot be undone.", color = com.example.ui.theme.TextSecondary)
                    Text("Type \"WIPE\" below to confirm:", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = wipeTextConfirmation,
                        onValueChange = { wipeTextConfirmation = it },
                        placeholder = { Text("WIPE") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.wipeAllMemories()
                        showWipeConfirm = false
                        wipeTextConfirmation = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = wipeTextConfirmation == "WIPE"
                ) {
                    Text("ERASE ALL")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
