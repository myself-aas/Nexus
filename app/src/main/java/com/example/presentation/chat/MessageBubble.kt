package com.example.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider as MDHorizontalDivider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import android.widget.Toast
import com.example.domain.model.Message
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.Sizing

sealed class MarkdownPart {
    data class Header(val level: Int, val text: String) : MarkdownPart()
    data class Paragraph(val annotatedText: AnnotatedString) : MarkdownPart()
    data class ListElement(val isOrdered: Boolean, val index: Int?, val annotatedText: AnnotatedString) : MarkdownPart()
    data class BlockQuote(val annotatedText: AnnotatedString) : MarkdownPart()
    data class Table(val headers: List<AnnotatedString>, val rows: List<List<AnnotatedString>>) : MarkdownPart()
    data class CodeBlock(val code: String, val language: String) : MarkdownPart()
}

@Composable
fun MessageBubble(
    message: Message,
    onEditAndResend: (String, String) -> Unit = { _, _ -> },
    onRegenerate: (String) -> Unit = {},
    onDelete: (String) -> Unit = {},
    onBranch: (String) -> Unit = {},
    onPreviewCode: ((String, String) -> Unit)? = null
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val isUser = message.role == "user"
    var showEditDialog by remember { mutableStateOf(false) }
    var editPromptText by remember { mutableStateOf(message.content) }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Message") },
            text = {
                OutlinedTextField(
                    value = editPromptText,
                    onValueChange = { editPromptText = it },
                    modifier = Modifier.fillMaxWidth().testTag("edit_message_input"),
                    label = { Text("Your Message") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEditDialog = false
                        onEditAndResend(message.id, editPromptText)
                    },
                    modifier = Modifier.testTag("confirm_edit_resend_button")
                ) {
                    Text("Resend")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .animateContentSize(),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            if (message.role == "tool") {
                var toolExpanded by remember { mutableStateOf(false) }
                GlassSurface(
                    cornerRadius = 12.dp,
                    blurRadius = 8.dp,
                    tintAlpha = 0.05f,
                    tintColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().testTag("tool_card_${message.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { toolExpanded = !toolExpanded },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Build,
                                    contentDescription = "Tool used in response",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Used tool: ${message.toolName ?: "unknown"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                if (toolExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (toolExpanded) "Hide tool details" else "Show tool details",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (toolExpanded) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            SelectionContainer {
                                Text(
                                    message.content,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 16.sp
                                    )
                                )
                            }
                        }
                    }
                }
            } else {
                // Bubble card using Glassmorphism
                val tintColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                val tintAlpha = if (isUser) 0.16f else 0.08f
                val borderColor = if (isUser) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                } else {
                    if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f)
                }
                GlassSurface(
                    cornerRadius = 16.dp,
                    blurRadius = if (isUser) 12.dp else 16.dp,
                    tintAlpha = tintAlpha,
                    tintColor = tintColor,
                    borderColor = borderColor,
                    modifier = Modifier.testTag("message_bubble_${message.id}")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (message.attachments.isNotEmpty()) {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(message.attachments) { attachment ->
                                    AssistChip(
                                        onClick = { },
                                        label = {
                                            Text(attachment.fileName ?: "Attachment", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        },
                                        leadingIcon = {
                                            Icon(
                                                if (attachment.type == "image") Icons.Default.Image else Icons.Default.InsertDriveFile,
                                                contentDescription = if (attachment.type == "image") "Image attachment" else "File attachment",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                            labelColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    )
                                }
                            }
                        }

                        // Render deep reasoning segment if present (collapsible/exploratory format, collapsed by default)
                        message.reasoningContent?.let { reasoning ->
                            var reasoningVisible by remember { mutableStateOf(false) }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { reasoningVisible = !reasoningVisible }
                                        .padding(8.dp)
                                        .testTag("toggle_thinking_${message.id}"),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.SmartToy,
                                            contentDescription = "AI thinking process",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Thinking Process",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Icon(
                                        if (reasoningVisible) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (reasoningVisible) "Hide thinking process" else "Show thinking process",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                if (reasoningVisible) {
                                    SelectionContainer {
                                        Text(
                                            text = reasoning,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                lineHeight = 16.sp
                                            ),
                                            modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Render custom Markdown content
                        SelectionContainer {
                            MarkdownRenderer(content = message.content, isUser = isUser, onPreviewCode = onPreviewCode)
                        }
                    }
                }

            }
            // Small toolbar beneath the message (only show for saved messages, i.e., not live streaming message)
            if (message.id != "live_message") {
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp, start = if (isUser) 0.dp else 4.dp, end = if (isUser) 4.dp else 0.dp)
                        .align(if (isUser) Alignment.End else Alignment.Start),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(message.content))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp).testTag("action_copy_${message.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy message",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    if (isUser) {
                        IconButton(
                            onClick = { showEditDialog = true },
                            modifier = Modifier.size(28.dp).testTag("action_edit_${message.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit and resend",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { onRegenerate(message.id) },
                            modifier = Modifier.size(28.dp).testTag("action_regenerate_${message.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Regenerate",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { onBranch(message.id) },
                        modifier = Modifier.size(28.dp).testTag("action_branch_${message.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallSplit,
                            contentDescription = "Branch conversation",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    IconButton(
                        onClick = { onDelete(message.id) },
                        modifier = Modifier.size(28.dp).testTag("action_delete_${message.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete message",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MarkdownRenderer(
    content: String,
    isUser: Boolean,
    onPreviewCode: ((String, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = isSystemInDarkTheme()
    val parsedParts = remember(content, isDarkTheme) { parseMarkdown(content, isDarkTheme) }
    Column(modifier = modifier) {
        parsedParts.forEach { part ->
            when (part) {
                is MarkdownPart.Header -> {
                    val textStyle = when (part.level) {
                        1 -> MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        2 -> MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        3 -> MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        else -> MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = part.text,
                        style = textStyle,
                        color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                is MarkdownPart.Paragraph -> {
                    Text(
                        text = part.annotatedText,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                        color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                is MarkdownPart.ListElement -> {
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = if (part.isOrdered) "${part.index}." else "•",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = part.annotatedText,
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                            color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                is MarkdownPart.BlockQuote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isUser) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .drawBehind {
                                val strokeWidth = 4.dp.toPx()
                                drawLine(
                                    color = if (isUser) Color.White else Color.Gray,
                                    start = Offset(strokeWidth / 2, 0f),
                                    end = Offset(strokeWidth / 2, size.height),
                                    strokeWidth = strokeWidth
                                )
                            }
                            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 8.dp)
                    ) {
                        Text(
                            text = part.annotatedText,
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                is MarkdownPart.Table -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        // Headers
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(10.dp)
                        ) {
                            part.headers.forEach { header ->
                                Text(
                                    text = header,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                                )
                            }
                        }
                        
                        // Rows
                        part.rows.forEachIndexed { rowIndex, row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (rowIndex % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .padding(10.dp)
                            ) {
                                row.forEach { cell ->
                                    Text(
                                        text = cell,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                is MarkdownPart.CodeBlock -> {
                    val clipboardManager = LocalClipboardManager.current
                    val context = LocalContext.current
                    val isDarkTheme = isSystemInDarkTheme()

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(8.dp),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    part.language.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val supportedPreviewLangs = listOf("html", "jsx", "react", "svg", "mermaid", "markdown")
                                    val isPreviewSupported = supportedPreviewLangs.contains(part.language.lowercase())
                                    if (isPreviewSupported && onPreviewCode != null) {
                                        TextButton(onClick = { onPreviewCode(part.code, part.language.lowercase()) }, modifier = Modifier.height(24.dp)) {
                                            Text("Preview", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(part.code))
                                            Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = "Copy code",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                            SelectionContainer {
                                Text(
                                    text = highlightCode(part.code, part.language, isDarkTheme),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun parseMarkdown(content: String, isDarkTheme: Boolean = false): List<MarkdownPart> {
    val parts = mutableListOf<MarkdownPart>()
    val lines = content.split("\n")
    var index = 0
    
    while (index < lines.size) {
        val line = lines[index]
        val trimmedLine = line.trim()
        
        // 1. Fenced Code Block
        if (trimmedLine.startsWith("```")) {
            val lang = trimmedLine.removePrefix("```").trim().ifEmpty { "code" }
            val codeBuilder = StringBuilder()
            index++
            while (index < lines.size) {
                val nextLine = lines[index]
                if (nextLine.trim().startsWith("```")) {
                    break
                }
                codeBuilder.append(nextLine).append("\n")
                index++
            }
            parts.add(MarkdownPart.CodeBlock(codeBuilder.toString().trimEnd(), lang))
            index++
            continue
        }
        
        // 2. BlockQuote
        if (trimmedLine.startsWith(">")) {
            val quoteContent = trimmedLine.removePrefix(">").trim()
            parts.add(MarkdownPart.BlockQuote(parseInlineFormatting(quoteContent)))
            index++
            continue
        }
        
        // 3. Header
        if (trimmedLine.startsWith("#")) {
            val level = trimmedLine.takeWhile { it == '#' }.length
            if (level in 1..6 && trimmedLine.getOrNull(level) == ' ') {
                val headerText = trimmedLine.substring(level).trim()
                parts.add(MarkdownPart.Header(level, headerText))
                index++
                continue
            }
        }
        
        // 4. Unordered List
        if (trimmedLine.startsWith("* ") || trimmedLine.startsWith("- ")) {
            val listText = trimmedLine.substring(2).trim()
            parts.add(MarkdownPart.ListElement(isOrdered = false, index = null, annotatedText = parseInlineFormatting(listText)))
            index++
            continue
        }
        
        // 5. Ordered List
        val orderedMatch = "^(\\d+)\\.\\s+(.*)$".toRegex().find(trimmedLine)
        if (orderedMatch != null) {
            val num = orderedMatch.groupValues[1].toInt()
            val listText = orderedMatch.groupValues[2].trim()
            parts.add(MarkdownPart.ListElement(isOrdered = true, index = num, annotatedText = parseInlineFormatting(listText)))
            index++
            continue
        }
        
        // 6. Table
        if (trimmedLine.startsWith("|") && trimmedLine.endsWith("|") && trimmedLine.length > 2) {
            val tableHeaders = trimmedLine.split("|").map { it.trim() }.filterIndexed { idx, _ -> idx != 0 && idx != trimmedLine.split("|").lastIndex }
            if (index + 1 < lines.size && lines[index + 1].trim().startsWith("|") && lines[index + 1].trim().contains("-")) {
                val headers = tableHeaders.map { parseInlineFormatting(it) }
                val rows = mutableListOf<List<AnnotatedString>>()
                index += 2 // Skip header line and separator line
                while (index < lines.size) {
                    val rowLine = lines[index].trim()
                    if (rowLine.startsWith("|") && rowLine.endsWith("|") && rowLine.length > 2) {
                        val rowCells = rowLine.split("|").map { it.trim() }.filterIndexed { idx, _ -> idx != 0 && idx != rowLine.split("|").lastIndex }
                        rows.add(rowCells.map { parseInlineFormatting(it) })
                        index++
                    } else {
                        break
                    }
                }
                parts.add(MarkdownPart.Table(headers, rows))
                continue
            }
        }
        
        // 7. Regular paragraph
        if (trimmedLine.isNotEmpty()) {
            parts.add(MarkdownPart.Paragraph(parseInlineFormatting(line)))
        }
        index++
    }
    
    return parts
}

fun parseInlineFormatting(text: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    var i = 0
    while (i < text.length) {
        // Bold **
        if (i + 1 < text.length && text[i] == '*' && text[i + 1] == '*') {
            val endIdx = text.indexOf("**", i + 2)
            if (endIdx != -1) {
                builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                builder.append(text.substring(i + 2, endIdx))
                builder.pop()
                i = endIdx + 2
                continue
            }
        }
        // Italic *
        if (text[i] == '*') {
            val endIdx = text.indexOf('*', i + 1)
            if (endIdx != -1) {
                builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                builder.append(text.substring(i + 1, endIdx))
                builder.pop()
                i = endIdx + 1
                continue
            }
        }
        // Inline code `
        if (text[i] == '`') {
            val endIdx = text.indexOf('`', i + 1)
            if (endIdx != -1) {
                builder.pushStyle(SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = Color(0x20E91E63), // Material pink with 12.5% opacity for better contrast
                    color = Color(0xFFC2185B) // Strong pink for better contrast
                ))
                builder.append(text.substring(i + 1, endIdx))
                builder.pop()
                i = endIdx + 1
                continue
            }
        }
        // Fallback standard char
        builder.append(text[i])
        i++
    }
    return builder.toAnnotatedString()
}

fun highlightCode(code: String, language: String, isDarkTheme: Boolean): AnnotatedString {
    val builder = AnnotatedString.Builder()
    
    val keywordColor = if (isDarkTheme) Color(0xFFE5C07B) else Color(0xFF0056B3)
    val stringColor = if (isDarkTheme) Color(0xFF98C379) else Color(0xFF228B22)
    val commentColor = if (isDarkTheme) Color(0xFF5C6370) else Color(0xFF808080)
    val numberColor = if (isDarkTheme) Color(0xFFD19A66) else Color(0xFFD2691E)
    val standardColor = if (isDarkTheme) Color(0xFFABB2BF) else Color(0xFF000000)

    val keywords = setOf(
        "fun", "class", "interface", "val", "var", "import", "package", "return", "if", "else", "when", "for", "while",
        "private", "public", "protected", "override", "suspend", "constructor", "inject", "this", "super", "null", "true", "false",
        "const", "object", "sealed", "data", "companion", "as", "is", "in", "break", "continue",
        "try", "catch", "finally", "throw", "typealias", "inline", "noinline", "crossinline", "init", "get", "set",
        "function", "let", "extends", "implements", "new", "void", "int", "double", "float", "long", "short", "byte", "char", "boolean", "static", "final"
    )

    val combinedRegex = "(\"[^\"]*\"|'[^']*')|(//.*|/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/)|(\\b\\d+\\b)|(\\b[a-zA-Z_][a-zA-Z0-9_]*\\b)|(\\s+)|(.)".toRegex()
    val matches = combinedRegex.findAll(code)
    
    for (match in matches) {
        val token = match.value
        val groupValues = match.groupValues
        
        when {
            groupValues[2].isNotEmpty() -> {
                builder.pushStyle(SpanStyle(color = commentColor, fontStyle = FontStyle.Italic))
                builder.append(token)
                builder.pop()
            }
            groupValues[1].isNotEmpty() -> {
                builder.pushStyle(SpanStyle(color = stringColor))
                builder.append(token)
                builder.pop()
            }
            groupValues[3].isNotEmpty() -> {
                builder.pushStyle(SpanStyle(color = numberColor))
                builder.append(token)
                builder.pop()
            }
            groupValues[4].isNotEmpty() -> {
                if (keywords.contains(token)) {
                    builder.pushStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold))
                    builder.append(token)
                    builder.pop()
                } else {
                    builder.pushStyle(SpanStyle(color = standardColor))
                    builder.append(token)
                    builder.pop()
                }
            }
            else -> {
                builder.pushStyle(SpanStyle(color = standardColor))
                builder.append(token)
                builder.pop()
            }
        }
    }
    
    return builder.toAnnotatedString()
}
