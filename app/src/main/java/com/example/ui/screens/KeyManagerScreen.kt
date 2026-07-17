package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.PortalViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyManagerScreen(
    viewModel: PortalViewModel,
    onBack: () -> Unit
) {
    val savedKeys by viewModel.savedKeys.collectAsState()
    
    val providers = listOf(
        ProviderConfig("anthropic", "Anthropic Claude", Icons.Default.Cloud, "Claude 3.5 Sonnet / Haiku"),
        ProviderConfig("openai", "OpenAI GPT-4", Icons.Default.SmartToy, "GPT-4o, GPT-4o-mini"),
        ProviderConfig("nvidia", "Nvidia Cloud API", Icons.Default.GraphicEq, "Llama-3.1-405b-instruct, Mixtral"),
        ProviderConfig("gemini", "Google Gemini Core", Icons.Default.AutoAwesome, "Gemini 3.5 Flash, 3.1 Pro")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "API CREDENTIALS MANAGER",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("keys_back_btn")) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark,
                    titleContentColor = TextLight,
                    navigationIconContentColor = TextLight
                )
            )
        },
        containerColor = BackgroundDark
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Cryptographic warning card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2218)),
                    border = BorderStroke(1.dp, Color(0xFF5E4527)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = PrimaryGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "PRIVACY & SECURITY WARNING",
                                color = PrimaryGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Security Warning: I have included your API keys in the generated APK file for this prototype. Please be aware that Android APKs can be easily decompiled, and these keys can be extracted by anyone who has access to the file. Do not share this APK file publicly or with unauthorized individuals to prevent potential misuse.\n\nAll credentials inputted below are encrypted end-to-end using the hardware-backed Android Keystore with AES-GCM (256-bit). Stored keys remain inaccessible directly to non-authorized apps on your device.",
                            color = TextLight.copy(alpha = 0.9f),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            item {
                // Local memory privacy note card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2F23)),
                    border = BorderStroke(1.dp, Color(0xFF3B5E43)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Shield",
                                tint = Color(0xFF81C784),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "100% OFFLINE LOCAL MEMORY",
                                color = Color(0xFF81C784),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Nexus AI includes a fully client-side memory system. Durable facts and preferences extracted from your conversations are stored strictly on-device in your encrypted database, and are never transmitted or synced to any Nexus-owned servers.",
                            color = TextLight.copy(alpha = 0.9f),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            items(providers) { provider ->
                val isSaved = savedKeys[provider.id] == true
                ProviderKeyCard(
                    config = provider,
                    isSaved = isSaved,
                    onSave = { key -> viewModel.saveApiKey(provider.id, key) },
                    onDelete = { viewModel.deleteApiKey(provider.id) }
                )
            }
        }
    }
}

@Composable
fun ProviderKeyCard(
    config: ProviderConfig,
    isSaved: Boolean,
    onSave: (String) -> Unit,
    onDelete: () -> Unit
) {
    var keyInput by remember { mutableStateOf("") }
    var keyVisible by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(!isSaved) }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderDark),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(CardDark, RoundedCornerShape(8.dp))
                            .border(1.dp, BorderDark, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = config.icon,
                            contentDescription = config.displayName,
                            tint = if (isSaved) PrimaryGold else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = config.displayName.uppercase(),
                            color = TextLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = config.models,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                // Lock state Badge
                Box(
                    modifier = Modifier
                        .background(
                            if (isSaved) Color(0xFF1B5E20).copy(alpha = 0.15f) else Color(0xFF424242).copy(alpha = 0.15f),
                            RoundedCornerShape(6.dp)
                        )
                        .border(
                            1.dp,
                            if (isSaved) Color(0xFF2E7D32) else Color(0xFF616161),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Lock State",
                            tint = if (isSaved) Color(0xFF81C784) else Color(0xFFE0E0E0),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isSaved) "AES ENCRYPTED" else "NOT STORED",
                            color = if (isSaved) Color(0xFF81C784) else Color(0xFFE0E0E0),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            AnimatedVisibility(visible = isEditing) {
                Column {
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text("Secret API Key") },
                        singleLine = true,
                        visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGold,
                            unfocusedBorderColor = BorderDark,
                            focusedLabelColor = PrimaryGold,
                            unfocusedLabelColor = TextMuted,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        trailingIcon = {
                            IconButton(onClick = { keyVisible = !keyVisible }) {
                                Icon(
                                    imageVector = if (keyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle Visibility",
                                    tint = TextMuted
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("key_input_${config.id}")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (isSaved) {
                            TextButton(
                                onClick = { isEditing = false },
                                modifier = Modifier.testTag("cancel_edit_${config.id}")
                            ) {
                                Text("Cancel", color = TextMuted)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Button(
                            onClick = {
                                if (keyInput.trim().isNotEmpty()) {
                                    onSave(keyInput)
                                    keyInput = ""
                                    isEditing = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.testTag("save_key_${config.id}")
                        ) {
                            Text("Encrypt & Save", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            AnimatedVisibility(visible = !isEditing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "••••••••••••••••••••••••••••••••",
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )

                    Row {
                        IconButton(
                            onClick = { isEditing = true },
                            modifier = Modifier.testTag("edit_key_${config.id}")
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Key", tint = TextMuted)
                        }
                        IconButton(
                            onClick = { onDelete() },
                            modifier = Modifier.testTag("delete_key_${config.id}")
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Key", tint = Color(0xFFEF5350))
                        }
                    }
                }
            }
        }
    }
}

data class ProviderConfig(
    val id: String,
    val displayName: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val models: String
)
