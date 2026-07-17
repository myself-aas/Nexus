package com.example.presentation.agents

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.domain.model.Agent
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassChip
import com.example.ui.theme.GlassPillButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentsScreen(
    modifier: Modifier = Modifier,
    viewModel: AgentsViewModel = hiltViewModel()
) {
    val agents by viewModel.allAgents.collectAsState()

    var showEditorDialog by remember { mutableStateOf(false) }
    var editingAgent by remember { mutableStateOf<Agent?>(null) }

    var agentName by remember { mutableStateOf("") }
    var agentDescription by remember { mutableStateOf("") }
    var agentPrompt by remember { mutableStateOf("") }
    var agentIcon by remember { mutableStateOf("Face") }
    var agentTemperature by remember { mutableFloatStateOf(0.7f) }
    var agentSkills by remember { mutableStateOf(setOf<String>()) }
    
    val allSkills by viewModel.allSkills.collectAsState()

    val iconsList = listOf("Face", "Person", "Build", "SmartToy", "Star", "Info", "Settings")

    fun getAgentIcon(name: String): ImageVector {
        return when (name) {
            "Face" -> Icons.Default.Face
            "Person" -> Icons.Default.Person
            "Build" -> Icons.Default.Build
            "SmartToy" -> Icons.Default.AccountBox
            "Star" -> Icons.Default.Star
            "Info" -> Icons.Default.Info
            "Settings" -> Icons.Default.Settings
            else -> Icons.Default.Face
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Nexus AI Agents", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingAgent = null
                    agentName = ""
                    agentDescription = ""
                    agentPrompt = ""
                    agentIcon = "Face"
                    agentTemperature = 0.7f
                    agentSkills = emptySet()
                    showEditorDialog = true
                },
                modifier = Modifier.padding(bottom = 76.dp), // Safe from overlapping bottom bar
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Agent")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Custom agents combine system prompts, models, and skills. Start a new conversation as an agent to load its configuration.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (agents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.0f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Face,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No Custom Agents",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Create your first agent to streamline your workflow.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(agents) { agent ->
                        GlassSurface(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(10.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getAgentIcon(agent.icon),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1.0f)) {
                                        Text(
                                            text = agent.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = agent.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (agent.systemPrompt.length > 50) "${agent.systemPrompt.take(50)}..." else agent.systemPrompt,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (agent.enabledSkillIds.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row {
                                                GlassChip(
                                                    selected = true,
                                                    onClick = {},
                                                    label = "${agent.enabledSkillIds.size} Skill(s)",
                                                    activeColor = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                editingAgent = agent
                                                agentName = agent.name
                                                agentDescription = agent.description
                                                agentPrompt = agent.systemPrompt
                                                agentIcon = agent.icon
                                                agentTemperature = agent.temperature
                                                agentSkills = agent.enabledSkillIds.toSet()
                                                showEditorDialog = true
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Edit agent",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteAgent(agent.id) }
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete agent",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showEditorDialog) {
            AlertDialog(
                onDismissRequest = { showEditorDialog = false },
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                title = {
                    Text(
                        text = if (editingAgent == null) "Create Agent" else "Edit Agent",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = agentName,
                            onValueChange = { agentName = it },
                            label = { Text("Agent Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            )
                        )
                        OutlinedTextField(
                            value = agentDescription,
                            onValueChange = { agentDescription = it },
                            label = { Text("Short Description") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            )
                        )
                        OutlinedTextField(
                            value = agentPrompt,
                            onValueChange = { agentPrompt = it },
                            label = { Text("System Prompt") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            maxLines = 8,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            )
                        )
                        Text(
                            "Temperature: ${"%.1f".format(agentTemperature)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Slider(
                            value = agentTemperature,
                            onValueChange = { agentTemperature = it },
                            valueRange = 0f..2f,
                            steps = 19,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            "Select Icon",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(iconsList) { name ->
                                val isSelected = agentIcon == name
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clickable { agentIcon = name }
                                ) {
                                    Surface(
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = getAgentIcon(name),
                                                contentDescription = name,
                                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (allSkills.isNotEmpty()) {
                            Text(
                                "Enabled Skills",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                allSkills.forEach { skill ->
                                    val isChecked = agentSkills.contains(skill.id)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                agentSkills = if (isChecked) agentSkills - skill.id else agentSkills + skill.id
                                            }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = null,
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(skill.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    GlassPillButton(
                        onClick = {
                            if (agentName.isNotEmpty()) {
                                val currentAgent = editingAgent
                                if (currentAgent == null) {
                                    viewModel.createAgent(
                                        name = agentName,
                                        description = agentDescription,
                                        systemPrompt = agentPrompt,
                                        icon = agentIcon,
                                        temperature = agentTemperature,
                                        enabledSkillIds = agentSkills.toList()
                                    )
                                } else {
                                    viewModel.updateAgent(
                                        currentAgent.copy(
                                            name = agentName,
                                            description = agentDescription,
                                            systemPrompt = agentPrompt,
                                            icon = agentIcon,
                                            temperature = agentTemperature,
                                            enabledSkillIds = agentSkills.toList()
                                        )
                                    )
                                }
                                showEditorDialog = false
                            }
                        }
                    ) {
                        Text(if (editingAgent == null) "Create" else "Save", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditorDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    }
}
