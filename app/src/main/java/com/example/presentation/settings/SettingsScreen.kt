package com.example.presentation.settings

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.domain.model.ApiKeyRef
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassChip
import com.example.ui.theme.GlassPillButton

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val apiKeyRefs by viewModel.apiKeyRefs.collectAsState(initial = emptyList())
    val nvidiaStatus by viewModel.nvidiaKeyStatus.collectAsState()
    val customStatus by viewModel.customKeyStatus.collectAsState()
    val testStatus by viewModel.testStatus.collectAsState()
    val availableModels by viewModel.availableModels.collectAsState()

    // Global default configuration states
    val defaultProviderId by viewModel.defaultProviderId.collectAsState()
    val defaultModelId by viewModel.defaultModelId.collectAsState()

    var nvidiaKeyInput by remember { mutableStateOf("") }
    var showCustomDialog by remember { mutableStateOf(false) }
    var showMemoryManagement by remember { mutableStateOf(false) }

    var customProviderName by remember { mutableStateOf("") }
    var customBaseUrl by remember { mutableStateOf("") }
    var customApiKey by remember { mutableStateOf("") }
    var customHeaders by remember { mutableStateOf("") }

    val presets = listOf(
        PresetProvider("OpenAI", "https://api.openai.com/v1"),
        PresetProvider("Groq", "https://api.groq.com/openai/v1"),
        PresetProvider("OpenRouter", "https://openrouter.ai/api/v1"),
        PresetProvider("Together AI", "https://api.together.xyz/v1"),
        PresetProvider("Fireworks", "https://api.fireworks.ai/inference/v1"),
        PresetProvider("Mistral", "https://api.mistral.ai/v1"),
        PresetProvider("DeepSeek", "https://api.deepseek.com/v1"),
        PresetProvider("Local Ollama", "http://10.0.2.2:11434/v1") // 10.0.2.2 points to localhost from Android Emulator
    )

    LaunchedEffect(showCustomDialog) {
        if (showCustomDialog) {
            viewModel.resetTestStatus()
        }
    }

    Scaffold(
        modifier = modifier.testTag("settings_screen"),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Nexus AI Settings", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
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
            // Section 1: NVIDIA NIM (Primary / Default Provider)
            item {
                GlassSurface(
                    modifier = Modifier.fillMaxWidth().testTag("nvidia_nim_card"),
                    tintAlpha = 0.12f,
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "NVIDIA NIM (Primary Provider)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://build.nvidia.com"))
                                    context.startActivity(intent)
                                }
                            ) {
                                Icon(Icons.Default.Link, contentDescription = "Get Key from build.nvidia.com", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Text(
                            text = "Obtain free developer credits on build.nvidia.com. NVIDIA NIM offers extremely high throughput and reasoning capabilities.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        Text(
                            text = "Get free API Key",
                            style = MaterialTheme.typography.bodySmall.copy(
                                textDecoration = TextDecoration.Underline,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier
                                .clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://build.nvidia.com"))
                                    context.startActivity(intent)
                                }
                                .padding(bottom = 12.dp)
                        )

                        val isSaved = apiKeyRefs.any { it.providerId == "nvidia_nim" }

                        if (isSaved) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Active",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("NVIDIA NIM Key Active", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("${availableModels.size} models loaded successfully", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Button(
                                    onClick = { viewModel.deleteApiKey("nvidia_nim") },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Disconnect")
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Discovered Models",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            if (availableModels.isEmpty()) {
                                Text(
                                    text = "No models available or loading...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    availableModels.forEach { model ->
                                        GlassSurface(
                                            modifier = Modifier.fillMaxWidth(),
                                            tintAlpha = 0.06f
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = model.label,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = model.id,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    if (model.supportsVision) {
                                                        Icon(
                                                            imageVector = Icons.Default.Visibility,
                                                            contentDescription = "Vision supported",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                    if (model.supportsTools) {
                                                        Icon(
                                                            imageVector = Icons.Default.Build,
                                                            contentDescription = "Tools supported",
                                                            tint = MaterialTheme.colorScheme.secondary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                    if (model.isFree) {
                                                        GlassChip(
                                                            selected = true,
                                                            onClick = {},
                                                            label = "Free",
                                                            activeColor = MaterialTheme.colorScheme.secondary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = nvidiaKeyInput,
                                onValueChange = { nvidiaKeyInput = it },
                                label = { Text("Nvidia API Key (nvapi-...)") },
                                modifier = Modifier.fillMaxWidth().testTag("nvidia_key_input"),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            GlassPillButton(
                                onClick = { viewModel.validateAndSaveNvidiaKey(nvidiaKeyInput) },
                                modifier = Modifier.fillMaxWidth().testTag("validate_nvidia_key_button"),
                                enabled = nvidiaKeyInput.startsWith("nvapi-")
                            ) {
                                Text("Connect & Validate", color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            AnimatedVisibility(visible = nvidiaStatus is ValidationStatus.Loading) {
                                Row(
                                    modifier = Modifier.padding(top = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Validating key and mapping models inline...", color = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            AnimatedVisibility(visible = nvidiaStatus is ValidationStatus.Error) {
                                val errorMsg = (nvidiaStatus as? ValidationStatus.Error)?.message ?: ""
                                Row(
                                    modifier = Modifier.padding(top = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(errorMsg, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            // Section 1.5: Global Default Configuration Settings
            item {
                GlassSurface(
                    modifier = Modifier.fillMaxWidth().testTag("global_defaults_card"),
                    tintAlpha = 0.08f,
                    borderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Global Fallback Pipeline",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Configure the global default provider and model pipeline for new sessions.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        var defaultProviderInput by remember(defaultProviderId) { mutableStateOf(defaultProviderId) }
                        var defaultModelInput by remember(defaultModelId) { mutableStateOf(defaultModelId) }

                        OutlinedTextField(
                            value = defaultProviderInput,
                            onValueChange = { 
                                defaultProviderInput = it
                                viewModel.setDefaultProviderId(it)
                            },
                            label = { Text("Default Provider ID (e.g., nvidia_nim)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = defaultModelInput,
                            onValueChange = { 
                                defaultModelInput = it
                                viewModel.setDefaultModelId(it)
                            },
                            label = { Text("Default Model ID") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }

            // Section 1.8: On-Device Memory management card
            item {
                GlassSurface(
                    modifier = Modifier.fillMaxWidth().testTag("memory_settings_card"),
                    tintAlpha = 0.08f,
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = "Memory",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "On-Device Memory",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "View and manage durable preferences and facts Nexus AI has remembered about you offline in your encrypted database. Note: Memory is 100% client-side, stored locally, and never synced to any server.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        GlassPillButton(
                            onClick = { showMemoryManagement = true },
                            modifier = Modifier.fillMaxWidth().testTag("manage_memories_button")
                        ) {
                            Text("Manage Local Memories", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Section 2: Presets & Custom OpenAI-Compatible Providers
            item {
                Text(
                    text = "Presets & Custom Providers",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Text(
                    text = "One-tap add standard AI providers, or configure a fully custom OpenAI-compatible endpoint.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Render Presets
            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        val isConfigured = apiKeyRefs.any { it.displayName == preset.name }
                        GlassChip(
                            selected = isConfigured,
                            onClick = {
                                customProviderName = preset.name
                                customBaseUrl = preset.baseUrl
                                customApiKey = ""
                                customHeaders = ""
                                showCustomDialog = true
                            },
                            label = preset.name,
                            activeColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                        )
                    }
                }
            }

            // Section 3: Stored Providers list
            item {
                Text(
                    text = "Configured Providers",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            val otherRefs = apiKeyRefs.filter { it.providerId != "nvidia_nim" }

            if (otherRefs.isEmpty()) {
                item {
                    Text(
                        text = "No custom providers configured yet. Click any preset above to add.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(otherRefs) { ref ->
                    GlassSurface(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ref.displayName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text(ref.baseUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { viewModel.deleteApiKey(ref.providerId) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete key", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            if (!ref.customHeaders.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row {
                                    GlassChip(
                                        selected = true,
                                        onClick = {},
                                        label = "Headers: ${ref.customHeaders}",
                                        activeColor = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 4: Panic Wipe & General Controls
            item {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        apiKeyRefs.forEach { viewModel.deleteApiKey(it.providerId) }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().testTag("panic_wipe_button")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Panic Wipe All Keys", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Custom Provider Dialog
        if (showCustomDialog) {
            AlertDialog(
                onDismissRequest = { showCustomDialog = false },
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                title = { Text("Configure $customProviderName", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = customProviderName,
                            onValueChange = { customProviderName = it },
                            label = { Text("Provider Name") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            )
                        )
                        OutlinedTextField(
                            value = customBaseUrl,
                            onValueChange = { customBaseUrl = it },
                            label = { Text("Base URL") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            )
                        )
                        OutlinedTextField(
                            value = customApiKey,
                            onValueChange = { customApiKey = it },
                            label = { Text("API Key") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            )
                        )
                        OutlinedTextField(
                            value = customHeaders,
                            onValueChange = { customHeaders = it },
                            label = { Text("Custom Headers (Optional, e.g. X-Header:Value, line-separated)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            )
                        )

                        // Test Connection Button
                        GlassPillButton(
                            onClick = {
                                viewModel.testConnection(
                                    displayName = customProviderName,
                                    baseUrl = customBaseUrl,
                                    apiKey = customApiKey,
                                    customHeaders = customHeaders.ifEmpty { null }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = customApiKey.isNotEmpty()
                        ) {
                            Text("Test Connection", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        // Connection Test feedback
                        AnimatedVisibility(visible = testStatus is ValidationStatus.Loading) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Testing endpoint...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        AnimatedVisibility(visible = testStatus is ValidationStatus.Success) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Connection Successful!", color = Color(0xFF4CAF50), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }

                        AnimatedVisibility(visible = testStatus is ValidationStatus.Error) {
                            val errorMsg = (testStatus as? ValidationStatus.Error)?.message ?: ""
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(errorMsg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        // Validation on Save feedback
                        AnimatedVisibility(visible = customStatus is ValidationStatus.Loading) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Saving provider...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        AnimatedVisibility(visible = customStatus is ValidationStatus.Error) {
                            val errorMsg = (customStatus as? ValidationStatus.Error)?.message ?: ""
                            Text(errorMsg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                confirmButton = {
                    GlassPillButton(
                        onClick = {
                            viewModel.saveCustomProvider(
                                displayName = customProviderName,
                                baseUrl = customBaseUrl,
                                apiKey = customApiKey,
                                customHeaders = customHeaders.ifEmpty { null }
                            )
                            showCustomDialog = false
                        },
                        enabled = customApiKey.isNotEmpty() && customProviderName.isNotEmpty() && customBaseUrl.isNotEmpty()
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCustomDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }

        if (showMemoryManagement) {
            MemoryManagementSheet(
                onDismiss = { showMemoryManagement = false },
                viewModel = viewModel
            )
        }
    }
}

data class PresetProvider(val name: String, val baseUrl: String)
