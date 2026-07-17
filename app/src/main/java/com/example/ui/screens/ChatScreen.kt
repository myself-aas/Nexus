package com.example.ui.screens

import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.database.ChatEntity
import com.example.data.database.DocumentEntity
import com.example.data.database.MessageEntity
import com.example.ui.PortalViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: PortalViewModel,
    onNavigateToKeys: () -> Unit,
    onNavigateToDocuments: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val chats by viewModel.allChats.collectAsState()
    val selectedChatId by viewModel.selectedChatId.collectAsState()
    val messages by viewModel.currentChatMessages.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val documents by viewModel.allDocuments.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var selectedProvider by remember { mutableStateOf("gemini") }
    var selectedModel by remember { mutableStateOf("gemini-3.5-flash") }
    var showExportMenu by remember { mutableStateOf(false) }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var selectedAttachments = remember { mutableStateListOf<DocumentEntity>() }

    // Auto-scroll to bottom of chat when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val currentChat = chats.find { it.id == selectedChatId }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = SurfaceDark,
                drawerContentColor = TextLight,
                modifier = Modifier.width(300.dp)
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                
                // Title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Forum, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "CONVERSATIONS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Serif,
                        letterSpacing = 1.sp,
                        color = TextLight
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // New Chat Button
                Button(
                    onClick = {
                        viewModel.createChat(selectedProvider, selectedModel)
                        coroutineScope.launch { drawerState.close() }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(44.dp)
                        .testTag("drawer_new_chat")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = BackgroundDark, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Thread", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Divider(color = BorderDark, modifier = Modifier.padding(horizontal = 20.dp))

                // Chats List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 8.dp)
                ) {
                    items(chats) { chat ->
                        val isSelected = chat.id == selectedChatId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CardDark else Color.Transparent)
                                .clickable {
                                    viewModel.selectChat(chat.id)
                                    coroutineScope.launch { drawerState.close() }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("drawer_chat_item_${chat.id}"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = if (isSelected) PrimaryGold else TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = chat.title,
                                    color = if (isSelected) TextLight else TextMuted,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            IconButton(
                                onClick = { viewModel.deleteChat(chat.id) },
                                modifier = Modifier.size(24.dp).testTag("drawer_delete_chat_${chat.id}")
                            ) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                Divider(color = BorderDark, modifier = Modifier.padding(horizontal = 20.dp))

                // Drawer Footers
                Column(modifier = Modifier.padding(16.dp)) {
                    DrawerItem(
                        icon = Icons.Default.Security,
                        label = "API Key Storage",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            onNavigateToKeys()
                        }
                    )
                    DrawerItem(
                        icon = Icons.Default.FolderOpen,
                        label = "Document Database",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            onNavigateToDocuments()
                        }
                    )
                    DrawerItem(
                        icon = Icons.Default.Logout,
                        label = "Disconnect Profile",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            viewModel.logout()
                            onLogout()
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = currentChat?.title ?: "CLAUDE",
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextLight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (currentChat != null) {
                                Text(
                                    text = "${currentChat.provider.uppercase()} (${currentChat.model})",
                                    fontSize = 11.sp,
                                    color = PrimaryGold
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Drawer", tint = TextLight)
                        }
                    },
                    actions = {
                        // Model configuration quick actions
                        if (currentChat != null) {
                            IconButton(onClick = { showExportMenu = true }, modifier = Modifier.testTag("export_btn")) {
                                Icon(imageVector = Icons.Default.FileDownload, contentDescription = "Export History", tint = TextLight)
                            }
                            
                            // Native Export Dropdown Menu
                            DropdownMenu(
                                expanded = showExportMenu,
                                onDismissRequest = { showExportMenu = false },
                                modifier = Modifier.background(SurfaceDark).border(1.dp, BorderDark)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Export as PDF Report", color = TextLight, fontSize = 13.sp) },
                                    leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = PrimaryGold) },
                                    onClick = {
                                        showExportMenu = false
                                        val file = viewModel.exportChat(currentChat.id, "PDF")
                                        if (file != null) {
                                            Toast.makeText(context, "Exported PDF to: ${file.name}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export as JSON File", color = TextLight, fontSize = 13.sp) },
                                    leadingIcon = { Icon(Icons.Default.Code, contentDescription = null, tint = PrimaryGold) },
                                    onClick = {
                                        showExportMenu = false
                                        val file = viewModel.exportChat(currentChat.id, "JSON")
                                        if (file != null) {
                                            Toast.makeText(context, "Exported JSON to: ${file.name}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
                )
            },
            containerColor = BackgroundDark,
            bottomBar = {
                // Interactive Compose Chat Input Panel
                if (currentChat != null) {
                    Column(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // Selected inline documents indicator
                        if (selectedAttachments.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                selectedAttachments.forEach { doc ->
                                    Box(
                                        modifier = Modifier
                                            .background(CardDark, RoundedCornerShape(6.dp))
                                            .border(1.dp, BorderDark, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(doc.name, color = TextLight, fontSize = 11.sp, maxLines = 1)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = TextMuted,
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clickable { selectedAttachments.remove(doc) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Document Context Attachment Button
                            IconButton(
                                onClick = { showAttachmentMenu = true },
                                modifier = Modifier.testTag("attach_btn")
                            ) {
                                Icon(imageVector = Icons.Default.AttachFile, contentDescription = "Attach Documents", tint = TextMuted)
                            }

                            // Model Quick Selector dropdown
                            DropdownMenu(
                                expanded = showAttachmentMenu,
                                onDismissRequest = { showAttachmentMenu = false },
                                modifier = Modifier.background(SurfaceDark).border(1.dp, BorderDark)
                            ) {
                                if (documents.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No documents in vector database", color = TextMuted, fontSize = 12.sp) },
                                        onClick = { showAttachmentMenu = false }
                                    )
                                } else {
                                    documents.forEach { doc ->
                                        DropdownMenuItem(
                                            text = { Text(doc.name, color = TextLight, fontSize = 12.sp) },
                                            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = PrimaryGold) },
                                            onClick = {
                                                showAttachmentMenu = false
                                                if (!selectedAttachments.contains(doc)) {
                                                    selectedAttachments.add(doc)
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            // Primary Text Input
                            OutlinedTextField(
                                value = textInput,
                                onValueChange = { textInput = it },
                                placeholder = { Text("Ask Claude anything...", color = TextMuted) },
                                maxLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGold,
                                    unfocusedBorderColor = BorderDark,
                                    focusedTextColor = TextLight,
                                    unfocusedTextColor = TextLight
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_input")
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // Send Message
                            IconButton(
                                onClick = {
                                    if (textInput.trim().isNotEmpty() && !isStreaming) {
                                        val prompt = textInput.trim()
                                        textInput = ""
                                        
                                        // Handle generative media triggers or standard message
                                        if (prompt.startsWith("/image ")) {
                                            viewModel.generateMultimedia(currentChat.id, prompt.substring(7), "IMAGE")
                                        } else if (prompt.startsWith("/audio ")) {
                                            viewModel.generateMultimedia(currentChat.id, prompt.substring(7), "AUDIO")
                                        } else if (prompt.startsWith("/video ")) {
                                            viewModel.generateMultimedia(currentChat.id, prompt.substring(7), "VIDEO")
                                        } else {
                                            viewModel.sendMessage(currentChat.id, prompt)
                                        }
                                        selectedAttachments.clear()
                                    }
                                },
                                enabled = textInput.trim().isNotEmpty() && !isStreaming,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(if (textInput.trim().isNotEmpty() && !isStreaming) PrimaryGold else CardDark)
                                    .testTag("send_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = if (textInput.trim().isNotEmpty() && !isStreaming) BackgroundDark else TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            if (currentChat == null) {
                // First Boot state - choose Model Provider to start
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "CHOOSE MODEL PROVIDER",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = TextLight,
                            fontSize = 18.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            "Select an AI model to begin secure conversational analysis:",
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp, bottom = 24.dp)
                        )

                        // Selector Cards
                        val providerList = listOf(
                            Pair("gemini", "Google Gemini 3.5"),
                            Pair("openai", "OpenAI GPT-4o"),
                            Pair("anthropic", "Claude 3.5 Sonnet"),
                            Pair("nvidia", "Nvidia Llama-3.1")
                        )

                        providerList.forEach { (prov, label) ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                border = BorderStroke(1.dp, if (selectedProvider == prov) PrimaryGold else BorderDark),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        selectedProvider = prov
                                        selectedModel = when (prov) {
                                            "gemini" -> "gemini-3.5-flash"
                                            "openai" -> "gpt-4o"
                                            "anthropic" -> "claude-3-5-sonnet-20241022"
                                            else -> "meta/llama-3.1-405b-instruct"
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(label, color = TextLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    RadioButton(
                                        selected = selectedProvider == prov,
                                        onClick = {
                                            selectedProvider = prov
                                            selectedModel = when (prov) {
                                                "gemini" -> "gemini-3.5-flash"
                                                "openai" -> "gpt-4o"
                                                "anthropic" -> "claude-3-5-sonnet-20241022"
                                                else -> "meta/llama-3.1-405b-instruct"
                                            }
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = PrimaryGold)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { viewModel.createChat(selectedProvider, selectedModel) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Initialize Conversation Thread", color = BackgroundDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Messages Conversation Window
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Chat commands info ribbon
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardDark)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Commands: ", color = PrimaryGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("/image <prompt>", color = TextMuted, fontSize = 10.sp)
                        Text("/audio <prompt>", color = TextMuted, fontSize = 10.sp)
                        Text("/video <prompt>", color = TextMuted, fontSize = 10.sp)
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp)
                    ) {
                        items(messages) { message ->
                            MessageBubble(message)
                        }
                        
                        if (isStreaming) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Start)
                                        .background(CardDark, RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            color = PrimaryGold,
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Streaming AI weights...", color = TextMuted, fontSize = 12.sp)
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

@Composable
fun DrawerItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = TextMuted, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(label, color = TextLight, fontSize = 13.sp)
    }
}

@Composable
fun MessageBubble(msg: MessageEntity) {
    val isUser = msg.role == "user"
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 310.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (isUser) 12.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 12.dp
                    )
                )
                .background(if (isUser) CardDark else SurfaceDark)
                .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Column {
                if (msg.type.uppercase() == "IMAGE" && msg.mediaUrl != null) {
                    // Coil Image Loader Card
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .padding(bottom = 8.dp)
                    ) {
                        AsyncImage(
                            model = File(msg.mediaUrl),
                            contentDescription = "Generated Multimedia",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else if (msg.type.uppercase() == "AUDIO" && msg.mediaUrl != null) {
                    AudioMessagePlayer(filePath = msg.mediaUrl)
                    Spacer(modifier = Modifier.height(6.dp))
                } else if (msg.type.uppercase() == "VIDEO" && msg.mediaUrl != null) {
                    // Video player mockup card with active play visual states
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .padding(bottom = 8.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(imageVector = Icons.Default.PlayCircleFilled, contentDescription = "Play Video", tint = PrimaryGold, modifier = Modifier.size(48.dp))
                            Text("SIMULATED VEO VIDEO CLIP", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
                        }
                    }
                }

                // Check for Code Block formatting
                val content = msg.content
                if (content.contains("```")) {
                    val parts = content.split("```")
                    parts.forEachIndexed { idx, part ->
                        if (idx % 2 == 1) {
                            // Monospace syntax container
                            val cleanCode = part.substringAfter("\n").trim()
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CodeBg)
                                    .border(1.dp, BorderDark, RoundedCornerShape(6.dp))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("SOURCE CODE", color = PrimaryGold, fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy",
                                            tint = TextMuted,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable {
                                                    clipboardManager.setText(AnnotatedString(cleanCode))
                                                    Toast.makeText(context, "Copied code", Toast.LENGTH_SHORT).show()
                                                }
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    SelectionContainer {
                                        Text(
                                            text = cleanCode,
                                            color = Color(0xFFA5D6A7), // Green editor syntax
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(text = part, color = TextLight, fontSize = 13.sp)
                        }
                    }
                } else {
                    SelectionContainer {
                        Text(
                            text = content,
                            color = TextLight,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(2.dp))
        
        Text(
            text = if (isUser) "You" else "Claude",
            color = TextMuted,
            fontSize = 9.sp,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
    }
}

@Composable
fun AudioMessagePlayer(filePath: String) {
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(filePath) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CodeBg, RoundedCornerShape(8.dp))
            .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                if (isPlaying) {
                    mediaPlayer?.pause()
                    isPlaying = false
                } else {
                    try {
                        if (mediaPlayer == null) {
                            mediaPlayer = MediaPlayer().apply {
                                setDataSource(filePath)
                                prepare()
                                setOnCompletionListener {
                                    isPlaying = false
                                }
                            }
                        }
                        mediaPlayer?.start()
                        isPlaying = true
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = PrimaryGold,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text("AI Voice Synthesizer", color = TextLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = if (isPlaying) 0.5f else 0.0f,
                color = PrimaryGold,
                trackColor = BorderDark,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp))
            )
        }
    }
}
