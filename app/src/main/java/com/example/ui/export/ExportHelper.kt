package com.example.ui.export

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.data.database.ChatEntity
import com.example.data.database.MessageEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

object ExportHelper {

    fun exportToPdf(context: Context, chat: ChatEntity, messages: List<MessageEntity>): File? {
        try {
            val pdf = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            var pageNumber = 1
            
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdf.startPage(pageInfo)
            var canvas = page.canvas
            
            val paint = Paint()
            paint.isAntiAlias = true
            
            // Header
            paint.textSize = 20f
            paint.isFakeBoldText = true
            paint.color = android.graphics.Color.BLACK
            canvas.drawText("Claude AI Chat History", 40f, 60f, paint)
            
            paint.textSize = 12f
            paint.isFakeBoldText = false
            paint.color = android.graphics.Color.GRAY
            canvas.drawText("Chat: ${chat.title}", 40f, 85f, paint)
            canvas.drawText("Model: ${chat.provider.uppercase()} (${chat.model})", 40f, 105f, paint)
            
            paint.color = android.graphics.Color.LTGRAY
            canvas.drawLine(40f, 120f, (pageWidth - 40).toFloat(), 120f, paint)
            
            var y = 145f
            paint.color = android.graphics.Color.BLACK
            
            for (msg in messages) {
                if (y > pageHeight - 80) {
                    pdf.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdf.startPage(pageInfo)
                    canvas = page.canvas
                    y = 50f
                }
                
                // Sender Identifier
                paint.textSize = 11f
                paint.isFakeBoldText = true
                paint.color = if (msg.role == "user") {
                    android.graphics.Color.parseColor("#1B5E20") // clean user accent
                } else {
                    android.graphics.Color.parseColor("#3E2723") // clean claude accent
                }
                val sender = if (msg.role == "user") "USER:" else "CLAUDE:"
                canvas.drawText(sender, 40f, y, paint)
                y += 18f
                
                // Content with text wrapping
                paint.textSize = 10f
                paint.isFakeBoldText = false
                paint.color = android.graphics.Color.BLACK
                
                val lines = wrapText(msg.content, paint, (pageWidth - 80).toFloat())
                for (line in lines) {
                    if (y > pageHeight - 50) {
                        pdf.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                        page = pdf.startPage(pageInfo)
                        canvas = page.canvas
                        y = 50f
                    }
                    canvas.drawText(line, 40f, y, paint)
                    y += 15f
                }
                y += 15f
            }
            
            pdf.finishPage(page)
            
            val exportDir = File(context.getExternalFilesDir(null), "exports")
            if (!exportDir.exists()) exportDir.mkdirs()
            val file = File(exportDir, "chat_${chat.id}_${System.currentTimeMillis()}.pdf")
            
            pdf.writeTo(FileOutputStream(file))
            pdf.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    fun exportToJson(context: Context, chat: ChatEntity, messages: List<MessageEntity>): File? {
        try {
            val root = JSONObject()
            root.put("id", chat.id)
            root.put("title", chat.title)
            root.put("provider", chat.provider)
            root.put("model", chat.model)
            root.put("createdAt", chat.createdAt)
            
            val msgsArray = JSONArray()
            for (msg in messages) {
                val m = JSONObject()
                m.put("role", msg.role)
                m.put("content", msg.content)
                m.put("timestamp", msg.timestamp)
                m.put("type", msg.type)
                m.put("mediaUrl", msg.mediaUrl ?: "")
                msgsArray.put(m)
            }
            root.put("messages", msgsArray)
            
            val exportDir = File(context.getExternalFilesDir(null), "exports")
            if (!exportDir.exists()) exportDir.mkdirs()
            val file = File(exportDir, "chat_${chat.id}_${System.currentTimeMillis()}.json")
            
            FileOutputStream(file).use { out ->
                out.write(root.toString(4).toByteArray(Charsets.UTF_8))
            }
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val list = mutableListOf<String>()
        val paragraphs = text.split("\n")
        for (p in paragraphs) {
            if (p.isEmpty()) {
                list.add("")
                continue
            }
            val words = p.split(" ")
            var line = ""
            for (word in words) {
                val testLine = if (line.isEmpty()) word else "$line $word"
                val width = paint.measureText(testLine)
                if (width <= maxWidth) {
                    line = testLine
                } else {
                    list.add(line)
                    line = word
                }
            }
            if (line.isNotEmpty()) {
                list.add(line)
            }
        }
        return list
    }
}
