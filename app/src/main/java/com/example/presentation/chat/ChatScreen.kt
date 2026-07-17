package com.example.presentation.chat

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.domain.model.Message
import com.example.domain.model.ModelInfo
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassChip
import com.example.ui.theme.GlassPillButton
import com.example.ui.theme.GlassBottomSheet
import kotlinx.coroutines.launch
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

private fun getModeIcon(modeId: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (modeId) {
        "quick_reply" -> Icons.Default.Bolt
        "casual" -> Icons.Default.ChatBubble
        "daily_tasks" -> Icons.Default.CheckCircle
        "hard_tasks" -> Icons.Default.Build
        "thinking" -> Icons.Default.Lightbulb
        "reasoning" -> Icons.Default.Star
        "smart" -> Icons.Default.Star
        else -> Icons.Default.Settings
    }
}

@Composable
fun AnimatedPlaceholder() {
    val prompts = listOf("Ask about...", "Summarize...", "Analyze...", "Message Copilot...")
    var promptIndex by remember { mutableStateOf(0) }
    var charIndex by remember { mutableStateOf(0) }
    var isDeleting by remember { mutableStateOf(false) }

    LaunchedEffect(promptIndex, isDeleting) {
        val currentPrompt = prompts[promptIndex]
        if (!isDeleting) {
            if (charIndex < currentPrompt.length) {
                kotlinx.coroutines.delay(100)
                charIndex++
            } else {
                kotlinx.coroutines.delay(2000)
                isDeleting = true
            }
        } else {
            if (charIndex > 0) {
                kotlinx.coroutines.delay(50)
                charIndex--
            } else {
                isDeleting = false
                promptIndex = (promptIndex + 1) % prompts.size
            }
        }
    }

    Text(
        text = prompts[promptIndex].take(charIndex) + "|",
        color = com.example.ui.theme.TextSecondary,
        style = MaterialTheme.typography.bodyLarge
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val conversations by viewModel.allConversations.collectAsState()
    val selectedId by viewModel.selectedConversationId.collectAsState()
    val messages by viewModel.currentMessages.collectAsState()
    val activeConvo by viewModel.activeConversation.collectAsState()

    val isStreaming by viewModel.isStreaming.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val liveResponse by viewModel.streamedResponse.collectAsState()
    val liveThinking by viewModel.streamedThinking.collectAsState()
    val errorText by viewModel.errorText.collectAsState()

    val selectedProvider by viewModel.selectedProviderId.collectAsState()
    val selectedModel by viewModel.selectedModelId.collectAsState()
    val models by viewModel.availableModels.collectAsState()

    val configuredProviders by viewModel.configuredProviders.collectAsState()
    val sheetSelectedProviderId by viewModel.sheetSelectedProviderId.collectAsState()
    val sheetAvailableModels by viewModel.sheetAvailableModels.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var showModelSheet by remember { mutableStateOf(false) }
    var showNewModeDialog by remember { mutableStateOf(false) }
    val allModes by viewModel.allModes.collectAsState()
    val isRefreshingModels by viewModel.isRefreshingModels.collectAsState()
    val modelDiscoveryError by viewModel.modelDiscoveryError.collectAsState()
    val selectedModeId by viewModel.selectedModeId.collectAsState()
    val currentMode = allModes.find { it.id == selectedModeId } ?: allModes.find { it.id == "smart" } ?: allModes.firstOrNull() ?: com.example.domain.model.Mode("smart", "Smart", "Auto-selects best mode", "smart", 0.5f)
    
    var showArtifactsPreview by remember { mutableStateOf(false) }
    var previewCodeContent by remember { mutableStateOf("") }
    var previewCodeLanguage by remember { mutableStateOf("") }

    val skills by viewModel.allSkills.collectAsState()
    val manuallyEnabledSkillIds by viewModel.manuallyEnabledSkillIds.collectAsState()
    
    val stagedAttachments by viewModel.stagedAttachments.collectAsState()
    
    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val mimeType = context.contentResolver.getType(uri)
            viewModel.processFileAttachment(uri, mimeType)
        }
    }
    
    var tempCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraUri?.let { viewModel.processFileAttachment(it, "image/jpeg") }
        }
    }

    LaunchedEffect(showModelSheet) {
        if (showModelSheet) {
            viewModel.openModelSwitcher()
        }
    }

    // Auto scroll to bottom when a new chunk or message arrives
    LaunchedEffect(messages.size, liveResponse.length) {
        if (messages.isNotEmpty() || liveResponse.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem((messages.size + (if (isStreaming) 1 else 0)).coerceAtLeast(0))
            }
        }
    }

    Scaffold(
        modifier = modifier.testTag("chat_screen"),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = activeConvo?.title ?: "Nexus AI Workspace",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        GlassChip(
                            selected = true,
                            onClick = {
                                viewModel.openModelSwitcher()
                                showModelSheet = true
                            },
                            label = currentMode.name,
                            icon = getModeIcon(currentMode.id),
                            modifier = Modifier.testTag("mode_selector_chip")
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.createNewConversation() }) {
                        Icon(Icons.Default.Add, contentDescription = "New Conversation")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedVisibility(visible = !isConnected) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Offline - Cannot send messages",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            // Main conversation board
            if (messages.isEmpty() && liveResponse.isEmpty() && !isStreaming) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1.0f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Chat,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Start a discussion with Nexus AI",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Plug in your keys to stream reasoning models directly on your device. Zero cloud sync, 100% private.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.0f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(messages) { message ->
                        MessageBubble(
                            message = message,
                            onEditAndResend = { msgId, newText -> viewModel.editAndResendMessage(msgId, newText) },
                            onRegenerate = { msgId -> viewModel.regenerateMessage(msgId) },
                            onDelete = { msgId -> viewModel.deleteMessage(msgId) },
                            onBranch = { msgId -> viewModel.branchConversation(msgId) },
                            onPreviewCode = { code, lang ->
                                previewCodeContent = code
                                previewCodeLanguage = lang
                                showArtifactsPreview = true
                            }
                        )
                    }

                    if (isStreaming && liveResponse.isEmpty() && liveThinking.isEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    ),
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = 0.dp,
                                        bottomEnd = 16.dp
                                    ),
                                    modifier = Modifier.padding(end = 24.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp).testTag("thinking_loading_indicator"),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Nexus AI is thinking...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isStreaming && (liveResponse.isNotEmpty() || liveThinking.isNotEmpty())) {
                        item {
                            MessageBubble(
                                message = Message(
                                    id = "live_message",
                                    conversationId = selectedId ?: "",
                                    role = "assistant",
                                    content = liveResponse,
                                    reasoningContent = liveThinking.ifEmpty { null },
                                    createdAt = System.currentTimeMillis()
                                ),
                                onPreviewCode = { code, lang ->
                                    previewCodeContent = code
                                    previewCodeLanguage = lang
                                    showArtifactsPreview = true
                                }
                            )
                        }
                    }
                }
            }

            // Error notifications
            AnimatedVisibility(visible = errorText != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(errorText ?: "", color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1.0f))
                        }
                        TextButton(
                            onClick = { viewModel.clearError() },
                            modifier = Modifier.testTag("dismiss_error_button")
                        ) {
                            Text("Dismiss", color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            // Input suggestions
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val suggestions = listOf("Design a tattoo", "Invent a product", "Break the ice", "Plan a trip")
                items(suggestions) { suggestion ->
                    com.example.ui.theme.GlassChip(
                        selected = false,
                        onClick = { textInput = suggestion },
                        label = suggestion
                    )
                }
            }

            // Input bar
            var isFocused by remember { mutableStateOf(false) }
            val inputScale by androidx.compose.animation.core.animateFloatAsState(targetValue = if (isFocused) 1.02f else 1.0f)
            val inputElevation by androidx.compose.animation.core.animateDpAsState(targetValue = if (isFocused) 8.dp else 0.dp)
            val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

            GlassSurface(
                cornerRadius = 24.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .scale(inputScale)
                    .shadow(inputElevation, RoundedCornerShape(24.dp))
                    .animateContentSize()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    // 1. Active Manual Skills Chips Row (shown if any manual skill is toggled on for the conversation)
                    val activeManualSkills = skills.filter { it.id in manuallyEnabledSkillIds }
                    if (activeManualSkills.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(activeManualSkills) { skill ->
                                InputChip(
                                    selected = true,
                                    onClick = { viewModel.toggleManualSkill(skill.id) },
                                    label = { Text(skill.name, style = MaterialTheme.typography.labelSmall) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove",
                                            modifier = Modifier.size(12.dp)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Extension,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // 2. Slash-command Autocomplete Dropdown List
                    val isSlashCommand = textInput.startsWith("/")
                    val slashQuery = if (isSlashCommand) textInput.removePrefix("/") else ""
                    val filteredSkills = if (isSlashCommand) {
                        skills.filter { it.name.contains(slashQuery, ignoreCase = true) }
                    } else {
                        emptyList()
                    }

                    if (isSlashCommand && filteredSkills.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .heightIn(max = 200.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(filteredSkills) { skill ->
                                    val isCurrentlyEnabled = skill.id in manuallyEnabledSkillIds
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.toggleManualSkill(skill.id)
                                                textInput = "" // Clear input so they don't send "/"
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Extension,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = "/${skill.name}",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = skill.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        if (isCurrentlyEnabled) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Active",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }

                    // Staged Attachments Row
                    val currentModelInfo = models.find { it.id == selectedModel }
                    val supportsVision = currentModelInfo?.supportsVision ?: true
                    val hasImages = stagedAttachments.any { it.type == "image" }
                    val canSend = !hasImages || supportsVision

                    if (stagedAttachments.isNotEmpty()) {
                        if (hasImages && !supportsVision) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = "Warning", tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Selected model doesn't support images.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }

                        androidx.compose.foundation.lazy.LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(stagedAttachments) { attachment ->
                                Box(
                                    modifier = Modifier
                                        .height(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (attachment.type == "image") {
                                            coil.compose.AsyncImage(
                                                model = attachment.uri,
                                                contentDescription = null,
                                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                            )
                                        } else {
                                            Box(modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(attachment.fileName ?: "Attachment", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 100.dp), style = MaterialTheme.typography.bodySmall)
                                        IconButton(onClick = { viewModel.removeStagedAttachment(attachment.uri) }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Main row of the composer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left side: '+' icon
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            galleryLauncher.launch("*/*")
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Add attachment", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }

                        // Center: TextField
                        Box(
                            modifier = Modifier
                                .weight(1.0f)
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (textInput.isEmpty() && !isFocused) {
                                AnimatedPlaceholder()
                            }
                            androidx.compose.foundation.text.BasicTextField(
                                value = textInput,
                                onValueChange = { textInput = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { isFocused = it.isFocused }
                                    .testTag("prompt_text_field"),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = com.example.ui.theme.TextLight),
                                maxLines = 4,
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
                            )
                        }

                        // Right side: Vision and Voice icons
                        if (isStreaming) {
                            IconButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.stopGenerating()
                            }, modifier = Modifier.testTag("stop_generating_button")) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop", tint = MaterialTheme.colorScheme.error)
                            }
                        } else if (textInput.trim().isNotEmpty() || stagedAttachments.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    if (canSend) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.sendMessage(textInput)
                                        textInput = ""
                                    }
                                },
                                modifier = Modifier.testTag("send_prompt_button")
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Send", tint = if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            IconButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val file = java.io.File(context.cacheDir, "camera_capture_${System.currentTimeMillis()}.jpg")
                                val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                tempCameraUri = uri
                                cameraLauncher.launch(uri)
                            }) {
                                Icon(Icons.Outlined.Visibility, contentDescription = "Vision", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        }

        // Model selection picker bottom sheet
        if (showModelSheet) {
            GlassBottomSheet(
                onDismissRequest = { showModelSheet = false },
                modifier = Modifier.fillMaxHeight(0.85f)
            ) {
                var searchQuery by remember { mutableStateOf("") }
                var browseModelsExpanded by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    if (isRefreshingModels) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    }

                    if (modelDiscoveryError != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { viewModel.refreshModelsForProvider(sheetSelectedProviderId) },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Warning",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Using cached model list",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "Tap to retry updating list.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        // Section 1: Modes
                        item {
                            Text(
                                text = "Modes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        // Modes list
                        items(allModes) { mode ->
                            val isSelected = mode.id == selectedModeId
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(CircleShape)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                                    .clickable {
                                        viewModel.selectMode(mode.id)
                                        showModelSheet = false
                                    }
                                    .border(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else if (isSystemInDarkTheme()) Color(0xFF2E3B5E) else Color.Black.copy(alpha = 0.05f),
                                        CircleShape
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = getModeIcon(mode.id),
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else com.example.ui.theme.TextLight,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mode.name,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else com.example.ui.theme.TextLight
                                        )
                                        Text(
                                            text = mode.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = com.example.ui.theme.TextSecondary
                                        )
                                    }
                                    if (mode.isCustom) {
                                        IconButton(
                                            onClick = { viewModel.deleteCustomMode(mode.id) }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Custom Mode",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            OutlinedButton(
                                onClick = { showNewModeDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = CircleShape,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = com.example.ui.theme.TextLight
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSystemInDarkTheme()) Color(0xFF2E3B5E) else Color.Black.copy(alpha = 0.1f)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("New Mode", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Section 2: Browse all models
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { browseModelsExpanded = !browseModelsExpanded }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Browse all models",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isRefreshingModels) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                    } else {
                                        IconButton(
                                            onClick = { viewModel.refreshModelsForProvider(sheetSelectedProviderId) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Refresh models",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Icon(
                                        imageVector = if (browseModelsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (browseModelsExpanded) "Collapse" else "Expand",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                    if (browseModelsExpanded) {
                        item {
                            // Provider selection
                            if (configuredProviders.size > 1) {
                                ScrollableTabRow(
                                    selectedTabIndex = configuredProviders.indexOfFirst { it.providerId == sheetSelectedProviderId }.coerceAtLeast(0),
                                    edgePadding = 0.dp,
                                    modifier = Modifier.padding(bottom = 8.dp),
                                    containerColor = Color.Transparent
                                ) {
                                    configuredProviders.forEach { provider ->
                                        val isSelected = provider.providerId == sheetSelectedProviderId
                                        Tab(
                                            selected = isSelected,
                                            onClick = { viewModel.updateSheetProvider(provider.providerId) },
                                            text = {
                                                Text(
                                                    text = provider.displayName,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        )
                                    }
                                }
                            } else {
                                val providerName = configuredProviders.firstOrNull()?.displayName ?: "NVIDIA NIM"
                                Text(
                                    text = "Provider: $providerName",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }

                        item {
                            // Search Bar
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search models...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        val filteredModels = sheetAvailableModels.filter {
                            it.label.contains(searchQuery, ignoreCase = true) || it.id.contains(searchQuery, ignoreCase = true)
                        }

                        val groupedModels = filteredModels.groupBy { model ->
                            val id = model.id.lowercase()
                            when {
                                id.contains("reason") || id.contains("thinking") || id.contains("deepseek-r") || id.contains("-r1") || id.contains("qwq") -> "Reasoning"
                                id.contains("code") || id.contains("coder") || id.contains("starcoder") -> "Code"
                                model.supportsVision || id.contains("vision") || id.contains("vl") || id.contains("vila") || id.contains("llava") -> "Vision"
                                model.isFree -> "Free-tier"
                                else -> "Multilingual"
                            }
                        }

                        if (filteredModels.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("No models found matching \"$searchQuery\"", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        } else {
                            groupedModels.forEach { (category, categoryModels) ->
                                item {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                    ) {
                                        Text(
                                            text = category,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }

                                items(categoryModels) { model ->
                                    val isSelected = model.id == selectedModel && sheetSelectedProviderId == selectedProvider
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else Color.Transparent
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.selectModel(sheetSelectedProviderId, model.id)
                                                showModelSheet = false
                                            },
                                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1.0f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = model.label,
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                    if (model.isFree) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Surface(
                                                            color = MaterialTheme.colorScheme.tertiaryContainer,
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text(
                                                                text = "Free",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = model.id,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            Icons.Default.Menu,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(12.dp),
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "Ctx: ${model.contextWindow ?: 4096}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    if (model.supportsTools) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(
                                                                Icons.Default.Build,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(12.dp),
                                                                tint = MaterialTheme.colorScheme.primary
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = "Tools",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    }
                                                    if (model.supportsVision) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(
                                                                Icons.Default.Visibility,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(12.dp),
                                                                tint = MaterialTheme.colorScheme.secondary
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = "Vision",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.secondary
                                                            )
                                                        }
                                                    }
                                                    val supportsReasoning = model.id.lowercase().contains("reason") || model.id.lowercase().contains("thinking") || model.id.lowercase().contains("deepseek-r1") || model.id.lowercase().contains("-r1")
                                                    if (supportsReasoning) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(
                                                                Icons.Default.Lightbulb,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(12.dp),
                                                                tint = MaterialTheme.colorScheme.tertiary
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = "Reasoning",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.tertiary
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            if (isSelected) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = "Selected",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(24.dp)
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
            }
        }
    }

    if (showNewModeDialog) {
        var modeName by remember { mutableStateOf("") }
        var modeDescription by remember { mutableStateOf("") }
        var temp by remember { mutableFloatStateOf(0.5f) }
        var selectedStrategy by remember { mutableStateOf("smart") }
        var customMaxTokens by remember { mutableStateOf("2048") }
        var customTopP by remember { mutableStateOf("0.9") }
        var expandedStrategyDropdown by remember { mutableStateOf(false) }

        val strategyOptions = listOf(
            "smart" to "Smart (Auto-select)",
            "quick_reply" to "Quick Reply (Fast & concise)",
            "casual" to "Casual (Conversational)",
            "daily_tasks" to "Daily Tasks (Balanced)",
            "hard_tasks" to "Hard Tasks (Advanced coding/math)",
            "thinking" to "Thinking (Deep analysis)",
            "reasoning" to "Reasoning (Step-by-step instructions)"
        ) + models.map { it.id to "Pinned Model: ${it.label}" }

        AlertDialog(
            onDismissRequest = { showNewModeDialog = false },
            title = {
                Text(
                    text = "Create Custom Mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = modeName,
                        onValueChange = { modeName = it },
                        label = { Text("Name") },
                        placeholder = { Text("e.g., Code Reviewer") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = modeDescription,
                        onValueChange = { modeDescription = it },
                        label = { Text("Description") },
                        placeholder = { Text("e.g., Optimizes code and adds explanations") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 2,
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Strategy dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val currentOptionLabel = strategyOptions.find { it.first == selectedStrategy }?.second ?: selectedStrategy
                        OutlinedTextField(
                            value = currentOptionLabel,
                            onValueChange = {},
                            label = { Text("Target Strategy or Pinned Model") },
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { expandedStrategyDropdown = !expandedStrategyDropdown }) {
                                    Icon(
                                        imageVector = if (expandedStrategyDropdown) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        DropdownMenu(
                            expanded = expandedStrategyDropdown,
                            onDismissRequest = { expandedStrategyDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            strategyOptions.forEach { (strategy, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        selectedStrategy = strategy
                                        expandedStrategyDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Temperature Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Temperature: ${String.format("%.2f", temp)}", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = when {
                                    temp < 0.3f -> "Precise"
                                    temp < 0.7f -> "Balanced"
                                    else -> "Creative"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = temp,
                            onValueChange = { temp = it },
                            valueRange = 0f..1f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Max Tokens field
                    OutlinedTextField(
                        value = customMaxTokens,
                        onValueChange = { customMaxTokens = it },
                        label = { Text("Max Output Tokens") },
                        placeholder = { Text("e.g., 2048") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )

                    // Top P field
                    OutlinedTextField(
                        value = customTopP,
                        onValueChange = { customTopP = it },
                        label = { Text("Top P (Optional)") },
                        placeholder = { Text("e.g., 0.9") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (modeName.isNotBlank()) {
                            viewModel.createCustomMode(
                                name = modeName,
                                description = modeDescription,
                                targetStrategy = selectedStrategy,
                                temperature = temp,
                                maxTokens = customMaxTokens.toIntOrNull(),
                                topP = customTopP.toFloatOrNull()
                            )
                            showNewModeDialog = false
                        } else {
                            Toast.makeText(context, "Please enter a name", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewModeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    if (showArtifactsPreview) {
        ArtifactsPreviewSheet(
            code = previewCodeContent,
            language = previewCodeLanguage,
            viewModel = viewModel,
            onDismiss = { showArtifactsPreview = false }
        )
    }
}


