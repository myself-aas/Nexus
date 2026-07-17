package com.example.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileExtractionUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    init {
        PDFBoxResourceLoader.init(context)
    }

    suspend fun extractTextOrBase64(uri: Uri, mimeType: String): String? = withContext(Dispatchers.IO) {
        try {
            when {
                mimeType.startsWith("image/") -> {
                    // Downscale and encode to base64
                    val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeStream(inputStream, null, options)
                    inputStream.close()
                    
                    var inSampleSize = 1
                    val reqSize = 1024
                    if (options.outHeight > reqSize || options.outWidth > reqSize) {
                        val halfHeight = options.outHeight / 2
                        val halfWidth = options.outWidth / 2
                        while ((halfHeight / inSampleSize) >= reqSize && (halfWidth / inSampleSize) >= reqSize) {
                            inSampleSize *= 2
                        }
                    }
                    
                    val options2 = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
                    val is2 = context.contentResolver.openInputStream(uri) ?: return@withContext null
                    val bitmap = BitmapFactory.decodeStream(is2, null, options2)
                    is2.close()
                    
                    if (bitmap != null) {
                        val outputStream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                        val bytes = outputStream.toByteArray()
                        return@withContext Base64.encodeToString(bytes, Base64.NO_WRAP)
                    }
                    null
                }
                mimeType == "application/pdf" -> {
                    var document: PDDocument? = null
                    try {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        document = PDDocument.load(inputStream)
                        val stripper = PDFTextStripper()
                        stripper.getText(document)
                    } finally {
                        document?.close()
                    }
                }
                mimeType.startsWith("text/") || mimeType == "application/json" -> {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }
                else -> null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getFileName(uri: Uri): String {
        var name = "Unknown File"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }
}
