package com.example.presentation.chat

import android.annotation.SuppressLint
import android.os.Environment
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File
import java.io.FileOutputStream
import android.util.Base64
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtifactsPreviewSheet(
    code: String,
    language: String,
    viewModel: ChatViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showFormatPicker by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    com.example.ui.theme.GlassBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Preview (${language.toUpperCase()})",
                    style = MaterialTheme.typography.titleMedium
                )
                Row {
                    IconButton(onClick = {
                        showFormatPicker = true
                    }) {
                        Icon(Icons.Default.Download, contentDescription = "Export and share")
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val htmlContent = remember(code, language) {
                when (language) {
                    "html" -> code
                    "svg" -> "<html><body style='margin:0;display:flex;justify-content:center;align-items:center;min-height:100vh;'>$code</body></html>"
                    "jsx", "react" -> getReactHtml(code)
                    "mermaid" -> getMermaidHtml(code)
                    "markdown" -> getMarkdownHtml(code)
                    else -> "<html><body>Unsupported preview language: $language</body></html>"
                }
            }

            com.example.ui.theme.GlassSurface(
                modifier = Modifier.fillMaxSize(),
                tintAlpha = 0.05f
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            webViewClient = WebViewClient()
                            webChromeClient = WebChromeClient()
                            webViewRef = this
                            val encodedHtml = Base64.encodeToString(htmlContent.toByteArray(), Base64.NO_PADDING)
                            loadData(encodedHtml, "text/html", "base64")
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { webView ->
                        webViewRef = webView
                        val encodedHtml = Base64.encodeToString(htmlContent.toByteArray(), Base64.NO_PADDING)
                        webView.loadData(encodedHtml, "text/html", "base64")
                    }
                )
            }
        }
    }

    if (showFormatPicker) {
        com.example.ui.theme.GlassBottomSheet(
            onDismissRequest = { showFormatPicker = false }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Export Artifact",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = com.example.ui.theme.TextLight
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Select format to export and share:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = com.example.ui.theme.TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                val formats = getAvailableFormats(language)
                val chunkedFormats = formats.chunked(3)
                
                chunkedFormats.forEach { rowFormats ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowFormats.forEach { format ->
                            com.example.ui.theme.GlassChip(
                                selected = false,
                                onClick = {
                                    showFormatPicker = false
                                    performExport(context, format, code, language, webViewRef, viewModel)
                                },
                                label = format.toUpperCase(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Fill empty spaces in row
                        for (i in 0 until (3 - rowFormats.size)) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                com.example.ui.theme.GlassPillButton(
                    onClick = { showFormatPicker = false },
                    modifier = Modifier.fillMaxWidth(),
                    tintColor = MaterialTheme.colorScheme.error
                ) {
                    Text("Cancel", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

private fun getAvailableFormats(language: String): List<String> {
    val lang = language.lowercase()
    return when (lang) {
        "html" -> listOf("html", "txt", "pdf", "png", "md", "docx")
        "svg" -> listOf("svg", "png", "pdf")
        "mermaid" -> listOf("png", "pdf", "html", "md", "txt")
        "markdown", "md" -> listOf("md", "pdf", "docx", "html", "txt")
        "py", "python" -> listOf("py", "txt", "pdf")
        "js", "javascript", "ts", "typescript", "jsx", "react" -> listOf(lang, "txt", "pdf", "png")
        else -> listOf("txt", "pdf", "docx", "md")
    }
}

private fun performExport(
    context: android.content.Context,
    format: String,
    code: String,
    language: String,
    webView: WebView?,
    viewModel: ChatViewModel
) {
    try {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val nexusDir = File(downloadsDir, "Nexus AI")
        if (!nexusDir.exists()) {
            nexusDir.mkdirs()
        }
        
        val timestamp = System.currentTimeMillis()
        val extension = format.lowercase()
        val fileName = "export_$timestamp.$extension"
        val file = File(nexusDir, fileName)
        
        val mimeType = when (extension) {
            "pdf" -> "application/pdf"
            "png" -> "image/png"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "html" -> "text/html"
            "md" -> "text/markdown"
            "py" -> "text/plain"
            "svg" -> "image/svg+xml"
            else -> "text/plain"
        }
        
        when (extension) {
            "txt", "md", "py", "html", "svg", "js", "ts", "jsx", "react" -> {
                val contentToWrite = if (language == "mermaid" && extension == "html") {
                    getMermaidHtml(code)
                } else {
                    code
                }
                FileOutputStream(file).use {
                    it.write(contentToWrite.toByteArray())
                }
            }
            "pdf" -> {
                if ((language == "html" || language == "svg" || language == "mermaid" || language == "react" || language == "jsx") && webView != null) {
                    val width = webView.width.coerceAtLeast(1)
                    val height = webView.height.coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    webView.draw(canvas)
                    
                    val pdfDocument = android.graphics.pdf.PdfDocument()
                    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(width, height, 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    pdfDocument.finishPage(page)
                    FileOutputStream(file).use { pdfDocument.writeTo(it) }
                    pdfDocument.close()
                    bitmap.recycle()
                } else {
                    writeTextToPdf(code, file)
                }
            }
            "png" -> {
                if (webView != null) {
                    val width = webView.width.coerceAtLeast(1)
                    val height = webView.height.coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    webView.draw(canvas)
                    
                    FileOutputStream(file).use {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                    }
                    bitmap.recycle()
                } else {
                    throw Exception("Visual preview not fully initialized yet.")
                }
            }
            "docx" -> {
                generateDocx(code, file)
            }
            else -> {
                FileOutputStream(file).use {
                    it.write(code.toByteArray())
                }
            }
        }
        
        val fileSize = if (file.exists()) file.length() else 0L
        viewModel.saveRecentExport(
            fileName = fileName,
            filePath = file.absolutePath,
            mimeType = mimeType,
            language = language,
            fileSize = fileSize
        )
        
        Toast.makeText(context, "Saved to Downloads/Nexus AI/$fileName", Toast.LENGTH_LONG).show()
        
        shareFile(context, file, mimeType)
        
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to export: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

private fun shareFile(context: android.content.Context, file: File, mimeType: String) {
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
            type = mimeType
            putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Share Exported File"))
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to share: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

private fun writeTextToPdf(text: String, outputFile: File) {
    val pdfDocument = android.graphics.pdf.PdfDocument()
    val paint = android.graphics.Paint().apply {
        textSize = 12f
        color = android.graphics.Color.BLACK
    }
    val titlePaint = android.graphics.Paint().apply {
        textSize = 16f
        isFakeBoldText = true
        color = android.graphics.Color.BLACK
    }
    
    val pageWidth = 595
    val pageHeight = 842
    val margin = 54f
    val contentWidth = pageWidth - 2 * margin
    
    val lines = mutableListOf<String>()
    text.split("\n").forEach { paragraph ->
        if (paragraph.isEmpty()) {
            lines.add("")
        } else {
            var currentLine = ""
            paragraph.split(" ").forEach { word ->
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                val width = paint.measureText(testLine)
                if (width <= contentWidth) {
                    currentLine = testLine
                } else {
                    lines.add(currentLine)
                    currentLine = word
                }
            }
            if (currentLine.isNotEmpty()) {
                lines.add(currentLine)
            }
        }
    }
    
    var lineIndex = 0
    var pageNumber = 1
    val lineHeight = paint.fontSpacing + 4f
    
    while (lineIndex < lines.size) {
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        
        var y = margin + paint.fontSpacing
        
        if (pageNumber == 1) {
            canvas.drawText("Nexus AI Exported Document", margin, y, titlePaint)
            y += titlePaint.fontSpacing + 12f
            canvas.drawLine(margin, y - 6f, pageWidth - margin, y - 6f, paint)
        }
        
        while (lineIndex < lines.size && y < pageHeight - margin) {
            val line = lines[lineIndex]
            canvas.drawText(line, margin, y, paint)
            y += lineHeight
            lineIndex++
        }
        
        pdfDocument.finishPage(page)
        pageNumber++
    }
    
    FileOutputStream(outputFile).use { pdfDocument.writeTo(it) }
    pdfDocument.close()
}

private fun parseLineToParagraphXml(line: String): String {
    val cleanLine = escapeXml(line)
    return when {
        cleanLine.startsWith("# ") -> {
            val text = cleanLine.substring(2)
            "<w:p><w:pPr><w:pStyle w:val=\"Heading1\"/></w:pPr><w:r><w:rPr><w:b/><w:sz w:val=\"32\"/><w:rFonts w:ascii=\"Arial\" w:hAnsi=\"Arial\"/></w:rPr><w:t>$text</w:t></w:r></w:p>"
        }
        cleanLine.startsWith("## ") -> {
            val text = cleanLine.substring(3)
            "<w:p><w:r><w:rPr><w:b/><w:sz w:val=\"28\"/><w:rFonts w:ascii=\"Arial\" w:hAnsi=\"Arial\"/></w:rPr><w:t>$text</w:t></w:r></w:p>"
        }
        cleanLine.startsWith("### ") -> {
            val text = cleanLine.substring(4)
            "<w:p><w:r><w:rPr><w:b/><w:sz w:val=\"24\"/><w:rFonts w:ascii=\"Arial\" w:hAnsi=\"Arial\"/></w:rPr><w:t>$text</w:t></w:r></w:p>"
        }
        cleanLine.startsWith("- ") || cleanLine.startsWith("* ") -> {
            val text = cleanLine.substring(2)
            "<w:p><w:r><w:rPr><w:rFonts w:ascii=\"Arial\" w:hAnsi=\"Arial\"/></w:rPr><w:t>•   $text</w:t></w:r></w:p>"
        }
        cleanLine.startsWith("    ") || cleanLine.startsWith("\t") -> {
            "<w:p><w:r><w:rPr><w:rFonts w:ascii=\"Courier New\" w:hAnsi=\"Courier New\"/><w:sz w:val=\"20\"/></w:rPr><w:t>$cleanLine</w:t></w:r></w:p>"
        }
        else -> {
            val parts = cleanLine.split("**")
            val runs = java.lang.StringBuilder()
            for (i in parts.indices) {
                val isBold = i % 2 == 1
                val text = parts[i]
                if (text.isNotEmpty()) {
                    runs.append("<w:r>")
                    runs.append("<w:rPr>")
                    if (isBold) {
                        runs.append("<w:b/>")
                    }
                    runs.append("<w:rFonts w:ascii=\"Arial\" w:hAnsi=\"Arial\"/>")
                    runs.append("</w:rPr>")
                    runs.append("<w:t>$text</w:t>")
                    runs.append("</w:r>")
                }
            }
            "<w:p>$runs</w:p>"
        }
    }
}

private fun escapeXml(text: String): String {
    return text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}

private fun generateDocx(text: String, file: File) {
    val contentTypesXml = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
          <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
          <Default Extension="xml" ContentType="application/xml"/>
          <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
        </Types>
    """.trimIndent()
    
    val relsXml = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
        </Relationships>
    """.trimIndent()
    
    val paragraphs = text.split("\n").joinToString("\n") { line ->
        parseLineToParagraphXml(line)
    }
    
    val documentXml = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
          <w:body>
            $paragraphs
            <w:sectPr>
              <w:pgSz w:w="12240" w:h="15840"/>
              <w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440" w:header="720" w:footer="720" w:gutter="0"/>
            </w:sectPr>
          </w:body>
        </w:document>
    """.trimIndent()
    
    java.util.zip.ZipOutputStream(FileOutputStream(file)).use { zos ->
        zos.putNextEntry(java.util.zip.ZipEntry("[Content_Types].xml"))
        zos.write(contentTypesXml.toByteArray())
        zos.closeEntry()
        
        zos.putNextEntry(java.util.zip.ZipEntry("_rels/.rels"))
        zos.write(relsXml.toByteArray())
        zos.closeEntry()
        
        zos.putNextEntry(java.util.zip.ZipEntry("word/document.xml"))
        zos.write(documentXml.toByteArray())
        zos.closeEntry()
    }
}

private fun getReactHtml(jsxCode: String): String {
    var processedCode = jsxCode
        .replace(Regex("import .* from .*;\n?"), "")
        .replace(Regex("export default function (\\w+)"), "function $1")
        .replace(Regex("export default (\\w+);?"), "const Default = $1;")
        .replace(Regex("export function (\\w+)"), "function $1")

    val escapedCode = processedCode
        .replace("\\", "\\\\")
        .replace("`", "\\`")
        .replace("\$", "\\$")
    
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1" />
            <script src="https://unpkg.com/react@18/umd/react.development.js" crossorigin></script>
            <script src="https://unpkg.com/react-dom@18/umd/react-dom.development.js" crossorigin></script>
            <script src="https://unpkg.com/@babel/standalone/babel.min.js"></script>
            <script src="https://cdn.tailwindcss.com"></script>
        </head>
        <body>
            <div id="root"></div>
            <script type="text/babel">
                try {
                    $escapedCode
                    
                    let ComponentToRender = null;
                    if (typeof App !== 'undefined') {
                        ComponentToRender = App;
                    } else if (typeof Default !== 'undefined') {
                        ComponentToRender = Default;
                    } else if (typeof Main !== 'undefined') {
                        ComponentToRender = Main;
                    }
                    
                    if (ComponentToRender) {
                        const root = ReactDOM.createRoot(document.getElementById('root'));
                        root.render(<ComponentToRender />);
                    } else {
                        document.getElementById('root').innerHTML = "Could not find an 'App' or 'Default' component to render.";
                    }
                } catch(e) {
                    document.getElementById('root').innerHTML = "<pre style='color:red'>" + e.toString() + "</pre>";
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}

private fun getMermaidHtml(mermaidCode: String): String {
    val escapedCode = mermaidCode
        .replace("\\", "\\\\")
        .replace("`", "\\`")
        .replace("\$", "\\$")
        
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
        </head>
        <body>
            <div class="mermaid">
                $escapedCode
            </div>
            <script type="module">
                import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.esm.min.mjs';
                mermaid.initialize({ startOnLoad: true });
            </script>
        </body>
        </html>
    """.trimIndent()
}

private fun getMarkdownHtml(markdownCode: String): String {
    val escapedCode = markdownCode
        .replace("\\", "\\\\")
        .replace("`", "\\`")
        .replace("\$", "\\$")
        
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
        </head>
        <body style="font-family: sans-serif; padding: 16px;">
            <div id="content"></div>
            <script>
                document.getElementById('content').innerHTML = marked.parse(`$escapedCode`);
            </script>
        </body>
        </html>
    """.trimIndent()
}
