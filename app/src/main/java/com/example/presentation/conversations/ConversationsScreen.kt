package com.example.presentation.conversations

import android.text.format.DateUtils
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.domain.model.Conversation
import com.example.presentation.chat.ChatViewModel
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassBottomSheet
import com.example.ui.theme.GlassChip
import com.example.ui.theme.GlassPillButton
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val conversations by viewModel.allConversations.collectAsState()
    val selectedId by viewModel.selectedConversationId.collectAsState()
    val agents by viewModel.allAgents.collectAsState()
    val recentExports by viewModel.recentExports.collectAsState()

    var activeTab by remember { mutableStateOf("chats") } // "chats" or "exports"

    var showRenameDialog by remember { mutableStateOf<Conversation?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var showAgentPicker by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.testTag("conversations_screen"),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Nexus AI Library", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            if (activeTab == "chats") {
                FloatingActionButton(
                    onClick = {
                        if (agents.isNotEmpty()) {
                            showAgentPicker = true
                        } else {
                            viewModel.createNewConversation()
                        }
                    },
                    modifier = Modifier
                        .testTag("create_conversation_fab")
                        .padding(bottom = 76.dp), // Keep it above floating nav bar!
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Conversation")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Tabs Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassChip(
                    selected = activeTab == "chats",
                    onClick = { activeTab = "chats" },
                    label = "Chats",
                    activeColor = MaterialTheme.colorScheme.primary,
                    inactiveColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                )
                GlassChip(
                    selected = activeTab == "exports",
                    onClick = { activeTab = "exports" },
                    label = "Recent Exports",
                    activeColor = MaterialTheme.colorScheme.primary,
                    inactiveColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (activeTab == "chats") {
                if (conversations.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
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
                                    Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No Past Conversations",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Start a new discussion inside the Chat tab.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp) // extra padding to avoid overlapping fab/nav
                    ) {
                        items(conversations) { convo ->
                            val isSelected = convo.id == selectedId
                            GlassSurface(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { viewModel.selectConversation(convo.id) },
                                tintAlpha = if (isSelected) 0.16f else 0.06f,
                                borderColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                } else {
                                    Color.White.copy(alpha = 0.12f)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1.0f)) {
                                        Text(
                                            text = convo.title,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            GlassChip(
                                                selected = isSelected,
                                                onClick = {},
                                                label = convo.modelId.split("/").last(),
                                                activeColor = MaterialTheme.colorScheme.primary,
                                                inactiveColor = MaterialTheme.colorScheme.surface
                                            )
                                            Text(
                                                text = DateUtils.getRelativeTimeSpanString(convo.updatedAt).toString(),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                renameInput = convo.title
                                                showRenameDialog = convo
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Rename Conversation",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(onClick = { viewModel.deleteConversation(convo.id) }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete Conversation",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Recent Exports Tab
                if (recentExports.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
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
                                    Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No Recent Exports",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Export code or text preview blocks to see files here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)
                    ) {
                        items(recentExports) { export ->
                            val context = LocalContext.current
                            GlassSurface(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1.0f)) {
                                        Text(
                                            text = export.fileName,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Path: Downloads/Nexus AI/${export.fileName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            GlassChip(
                                                selected = false,
                                                onClick = {},
                                                label = export.language.toUpperCase(),
                                                activeColor = MaterialTheme.colorScheme.primary,
                                                inactiveColor = MaterialTheme.colorScheme.surface
                                            )
                                            Text(
                                                text = Formatter.formatFileSize(context, export.fileSize),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = DateUtils.getRelativeTimeSpanString(export.timestamp).toString(),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                val file = File(export.filePath)
                                                if (file.exists()) {
                                                    try {
                                                        val sharedFile = File(context.cacheDir, file.name)
                                                        file.inputStream().use { input ->
                                                            sharedFile.outputStream().use { output ->
                                                                input.copyTo(output)
                                                            }
                                                        }
                                                        val fileUri = androidx.core.content.FileProvider.getUriForFile(
                                                            context,
                                                            "${context.packageName}.fileprovider",
                                                            sharedFile
                                                        )
                                                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                            type = export.mimeType
                                                            putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
                                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                        }
                                                        context.startActivity(android.content.Intent.createChooser(intent, "Share Exported File"))
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Sharing failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                                    }
                                                } else {
                                                    Toast.makeText(context, "Exported file does not exist on disk anymore.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.Share,
                                                contentDescription = "Share File",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(onClick = { viewModel.deleteRecentExport(export.id) }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete Export Record",
                                                tint = MaterialTheme.colorScheme.error
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

        // Rename dialog with elegant frosted look
        showRenameDialog?.let { convo ->
            AlertDialog(
                onDismissRequest = { showRenameDialog = null },
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                title = { Text("Rename Chat Title", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = renameInput,
                            onValueChange = { renameInput = it },
                            label = { Text("Conversation Title") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            )
                        )
                    }
                },
                confirmButton = {
                    GlassPillButton(
                        onClick = {
                            if (renameInput.isNotEmpty()) {
                                viewModel.renameConversation(convo.id, renameInput)
                                showRenameDialog = null
                            }
                        }
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = null }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }

        if (showAgentPicker) {
            GlassBottomSheet(
                onDismissRequest = { showAgentPicker = false }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Start New Conversation",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    ListItem(
                        headlineContent = { Text("Start Blank", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                        supportingContent = { Text("A standard assistant chat", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        leadingContent = {
                            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .clickable {
                                showAgentPicker = false
                                viewModel.createNewConversation()
                            }
                    )
                    
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(agents) { agent ->
                            ListItem(
                                headlineContent = { Text("Chat as ${agent.name}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                                supportingContent = { Text(agent.description, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                leadingContent = {
                                    Icon(
                                        when (agent.icon) {
                                            "Face" -> Icons.Default.Face
                                            "Person" -> Icons.Default.Person
                                            "Build" -> Icons.Default.Build
                                            "SmartToy" -> Icons.Default.AccountBox
                                            "Star" -> Icons.Default.Star
                                            "Info" -> Icons.Default.Info
                                            "Settings" -> Icons.Default.Settings
                                            else -> Icons.Default.Face
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier
                                    .clickable {
                                        showAgentPicker = false
                                        viewModel.createNewConversation(agentId = agent.id)
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}
