package com.example.presentation.skills

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.domain.model.Skill
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassChip
import com.example.ui.theme.GlassPillButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillsScreen(
    modifier: Modifier = Modifier,
    viewModel: SkillsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val skills by viewModel.allSkills.collectAsState()

    var showEditorDialog by remember { mutableStateOf(false) }
    var editingSkill by remember { mutableStateOf<Skill?>(null) }

    var skillName by remember { mutableStateOf("") }
    var skillDescription by remember { mutableStateOf("") }
    var skillInstructions by remember { mutableStateOf("") }
    var skillTrigger by remember { mutableStateOf("manual") }
    var skillIcon by remember { mutableStateOf("Extension") }

    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }

    var showExportDialog by remember { mutableStateOf(false) }
    var exportingSkillJson by remember { mutableStateOf("") }
    var exportingSkillName by remember { mutableStateOf("") }

    val iconsList = listOf("Extension", "Build", "Star", "Info", "Settings", "Play", "Face")

    fun getSkillIcon(name: String): ImageVector {
        return when (name) {
            "Build" -> Icons.Default.Build
            "Star" -> Icons.Default.Star
            "Info" -> Icons.Default.Info
            "Settings" -> Icons.Default.Settings
            "Play" -> Icons.Default.PlayArrow
            "Face" -> Icons.Default.Face
            else -> Icons.Default.Extension
        }
    }

    Scaffold(
        modifier = modifier.testTag("skills_screen"),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Nexus AI Skills", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                actions = {
                    IconButton(
                        onClick = {
                            importJsonText = ""
                            showImportDialog = true
                        },
                        modifier = Modifier.testTag("import_skill_button")
                    ) {
                        Icon(Icons.Default.Input, contentDescription = "Import Skill JSON", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingSkill = null
                    skillName = ""
                    skillDescription = ""
                    skillInstructions = ""
                    skillTrigger = "manual"
                    skillIcon = "Extension"
                    showEditorDialog = true
                },
                modifier = Modifier
                    .testTag("add_skill_fab")
                    .padding(bottom = 76.dp), // Keep it above floating nav bar!
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Custom Skill")
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
                text = "Custom prompt bundles injected dynamically into your session's system context. Toggled via slash commands in chat.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (skills.isEmpty()) {
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
                                Icons.Default.Extension,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No Custom Skills",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Create your own prompt block, import a JSON skill, or load one of our quick presets to get started:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            GlassPillButton(
                                onClick = {
                                    viewModel.createSkill(
                                        "JSON Formatter",
                                        "Forces the assistant to always respond in valid structured JSON.",
                                        "You must strictly output responses in structured JSON format. No markdown fences, no text explanation outside the JSON. All JSON fields must match standard syntax."
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("JSON Formatter", style = MaterialTheme.typography.labelMedium)
                            }

                            GlassPillButton(
                                onClick = {
                                    viewModel.createSkill(
                                        "Socratic Guide",
                                        "Instructs the assistant to tutor you using the Socratic method.",
                                        "Do not give the user direct answers. Instead, ask guided questions to help them uncover the logic and build the knowledge themselves."
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Socratic Guide", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp) // Leave padding for the bottom bar
                ) {
                    items(skills) { skill ->
                        val isEnabled = skill.isEnabled
                        GlassSurface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("skill_card_${skill.id}"),
                            tintAlpha = if (isEnabled) 0.14f else 0.06f,
                            borderColor = if (isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Custom visual icon inside high-contrast glass circle
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
                                            imageVector = getSkillIcon(skill.icon),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1.0f)) {
                                        Text(
                                            text = skill.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = skill.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Switch(
                                        checked = skill.isEnabled,
                                        onCheckedChange = { viewModel.toggleSkill(skill) },
                                        modifier = Modifier.testTag("skill_toggle_${skill.id}")
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val isManual = skill.triggerType == "manual"
                                    GlassChip(
                                        selected = isManual,
                                        onClick = {},
                                        label = if (isManual) "Manual Toggle" else "Always On",
                                        activeColor = MaterialTheme.colorScheme.secondary,
                                        inactiveColor = MaterialTheme.colorScheme.surface
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (skill.instructions.length > 50) "${skill.instructions.take(50)}..." else skill.instructions,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )

                                    // Action Toolbar
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                editingSkill = skill
                                                skillName = skill.name
                                                skillDescription = skill.description
                                                skillInstructions = skill.instructions
                                                skillTrigger = skill.triggerType
                                                skillIcon = skill.icon
                                                showEditorDialog = true
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Edit skill",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.duplicateSkill(skill) }
                                        ) {
                                            Icon(
                                                Icons.Default.ContentCopy,
                                                contentDescription = "Duplicate skill",
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                exportingSkillName = skill.name
                                                exportingSkillJson = viewModel.exportSkillToJson(skill)
                                                showExportDialog = true
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.Share,
                                                contentDescription = "Export JSON",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.deleteSkill(skill.id) }
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete skill",
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

        // Skill Creator / Editor Dialog
        if (showEditorDialog) {
            AlertDialog(
                onDismissRequest = { showEditorDialog = false },
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                title = {
                    Text(
                        text = if (editingSkill == null) "Create Custom Skill" else "Edit Skill",
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
                            value = skillName,
                            onValueChange = { skillName = it },
                            label = { Text("Skill Name (e.g. Code Refactorer)") },
                            modifier = Modifier.fillMaxWidth().testTag("skill_editor_name"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            )
                        )
                        OutlinedTextField(
                            value = skillDescription,
                            onValueChange = { skillDescription = it },
                            label = { Text("Short Description") },
                            modifier = Modifier.fillMaxWidth().testTag("skill_editor_desc"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            )
                        )
                        OutlinedTextField(
                            value = skillInstructions,
                            onValueChange = { skillInstructions = it },
                            label = { Text("Context System Instructions") },
                            modifier = Modifier.fillMaxWidth().testTag("skill_editor_instructions"),
                            minLines = 4,
                            maxLines = 8,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            )
                        )

                        Text(
                            "Trigger Type",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            GlassChip(
                                selected = skillTrigger == "manual",
                                onClick = { skillTrigger = "manual" },
                                label = "Manual Toggle",
                                activeColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            GlassChip(
                                selected = skillTrigger == "auto",
                                onClick = { skillTrigger = "auto" },
                                label = "Always On",
                                activeColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                        }

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
                                val isSelected = skillIcon == name
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clickable { skillIcon = name }
                                ) {
                                    Surface(
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = getSkillIcon(name),
                                                contentDescription = name,
                                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    GlassPillButton(
                        onClick = {
                            if (skillName.isNotEmpty() && skillInstructions.isNotEmpty()) {
                                val currentSkill = editingSkill
                                if (currentSkill == null) {
                                    viewModel.createSkill(
                                        name = skillName,
                                        description = skillDescription,
                                        instructions = skillInstructions,
                                        triggerType = skillTrigger,
                                        icon = skillIcon
                                    )
                                } else {
                                    viewModel.updateSkill(
                                        id = currentSkill.id,
                                        name = skillName,
                                        description = skillDescription,
                                        instructions = skillInstructions,
                                        triggerType = skillTrigger,
                                        icon = skillIcon,
                                        isEnabled = currentSkill.isEnabled
                                    )
                                }
                                showEditorDialog = false
                            } else {
                                Toast.makeText(context, "Name and Instructions are required", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("save_skill_button")
                    ) {
                        Text(if (editingSkill == null) "Create" else "Save", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditorDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }

        // Import Dialog
        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                title = { Text("Import Skill JSON", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Paste a shared skill's JSON configuration below to import it.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = importJsonText,
                            onValueChange = { importJsonText = it },
                            label = { Text("JSON Code") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .testTag("import_json_input"),
                            minLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            )
                        )
                    }
                },
                confirmButton = {
                    GlassPillButton(
                        onClick = {
                            if (importJsonText.isNotEmpty()) {
                                val result = viewModel.importSkillFromJson(importJsonText)
                                if (result.isSuccess) {
                                    Toast.makeText(context, "Skill imported successfully!", Toast.LENGTH_SHORT).show()
                                    showImportDialog = false
                                } else {
                                    Toast.makeText(context, "Failed to import: ${result.exceptionOrNull()?.localizedMessage ?: "Invalid JSON"}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.testTag("confirm_import_button")
                    ) {
                        Text("Import", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }

        // Export/Share Dialog
        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                title = { Text("Share Skill: $exportingSkillName", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Copy the JSON representation of this skill or send it to another app.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = exportingSkillJson,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Exported JSON") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            minLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            )
                        )
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassPillButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(exportingSkillJson))
                                Toast.makeText(context, "Copied JSON to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        GlassPillButton(
                            onClick = {
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, exportingSkillJson)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Share Skill JSON")
                                context.startActivity(shareIntent)
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("Close", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    }
}
