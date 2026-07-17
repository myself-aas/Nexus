package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.DocumentEntity
import com.example.ui.PortalViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScreen(
    viewModel: PortalViewModel,
    onBack: () -> Unit
) {
    val documents by viewModel.allDocuments.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var similarityResults by remember { mutableStateOf<List<Pair<DocumentEntity, Double>>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    // Android SAF File Selector
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val resolver = context.contentResolver
                    val cursor = resolver.query(uri, null, null, null, null)
                    var displayName = "imported_document_${System.currentTimeMillis()}.txt"
                    var size = 0L
                    
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                            if (nameIndex != -1) displayName = it.getString(nameIndex)
                            if (sizeIndex != -1) size = it.getLong(sizeIndex)
                        }
                    }

                    // Read content
                    val inputStream = resolver.openInputStream(uri)
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val contentBuilder = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        contentBuilder.append(line).append("\n")
                    }
                    reader.close()
                    inputStream?.close()

                    val finalContent = contentBuilder.toString()
                    val finalSize = if (size == 0L) finalContent.toByteArray(Charsets.UTF_8).size.toLong() else size
                    val mimeType = resolver.getType(uri) ?: "text/plain"

                    viewModel.importDocument(displayName, finalContent, mimeType, finalSize)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "VECTOR DOCUMENT DATABASE",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 17.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("doc_back_btn")) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.testTag("import_doc_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Import", tint = PrimaryGold)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Interactive local vector search console
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "OFFLINE VECTOR COSIM MATCHING TERMINAL",
                        color = PrimaryGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Query local term weights...", color = TextMuted, fontSize = 13.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryGold,
                                unfocusedBorderColor = BorderDark,
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("vector_search_input")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (searchQuery.trim().isNotEmpty()) {
                                    isSearching = true
                                    coroutineScope.launch {
                                        // Fetch similarity vectors
                                        val results = viewModel.exportChat("", "") // dummy call if needed but we have repo access in VM
                                        // Wait, the repository is encapsulated inside PortalViewModel, let's compute similarity
                                        // We can do searchSimilarDocuments from Repo, let's invoke similarity directly
                                        val repo = com.example.data.repository.ChatRepository(context)
                                        similarityResults = repo.searchSimilarDocuments(searchQuery.trim())
                                        isSearching = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("vector_search_submit")
                        ) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Query", tint = BackgroundDark)
                        }
                    }

                    // Vector results terminal display
                    if (searchQuery.isNotEmpty() && similarityResults.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("SIMILARITY MATRIX RESULTS:", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        similarityResults.forEach { (doc, score) ->
                            val scorePercentage = String.format("%.2f", score)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(doc.name, color = TextLight, fontSize = 12.sp, maxLines = 1)
                                }
                                Text("CosSim: $scorePercentage", color = PrimaryGold, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    } else if (searchQuery.isNotEmpty() && !isSearching && similarityResults.isEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No vector overlap detected locally. Try other terms.", color = TextMuted, fontSize = 11.sp)
                    }
                }
            }

            Text(
                text = "LOCAL INDEXED FILES (${documents.size})",
                color = TextMuted,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (documents.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Empty",
                            tint = BorderDark,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No documents loaded into local vector DB", color = TextMuted, fontSize = 13.sp)
                        Text("Tap + above to load TXT/CSV/JSON files", color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(documents) { doc ->
                        DocumentItemRow(
                            doc = doc,
                            onDelete = { viewModel.deleteDocument(doc.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentItemRow(
    doc: DocumentEntity,
    onDelete: () -> Unit
) {
    val sizeKb = doc.size / 1024.0
    val formattedSize = if (sizeKb < 1.0) "${doc.size} B" else String.format("%.1f KB", sizeKb)

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderDark),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(CardDark, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Description, contentDescription = "Doc", tint = PrimaryGold, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(doc.name, color = TextLight, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                    Text("$formattedSize • ${doc.mimeType}", color = TextMuted, fontSize = 11.sp)
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_doc_${doc.id}")) {
                Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFEF5350))
            }
        }
    }
}
