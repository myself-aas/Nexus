package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val title: String,
    val provider: String,
    val model: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val role: String, // "user" or "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val mimeType: String? = null,
    val filePath: String? = null,
    val type: String = "TEXT", // "TEXT", "CODE", "MARKDOWN", "IMAGE", "AUDIO", "VIDEO"
    val mediaUrl: String? = null // For generated media (images, audio, video)
)

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val content: String,
    val mimeType: String,
    val size: Long,
    val indexedAt: Long = System.currentTimeMillis(),
    val vectorJson: String? = null // TF-IDF term frequency vector map as JSON
)

@Entity(tableName = "credentials")
data class CredentialEntity(
    @PrimaryKey val providerId: String, // "nvidia", "openai", "anthropic", "gemini"
    val encryptedKey: String,
    val iv: String, // Initialisation vector for GCM
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val mfaSecret: String? = null,
    val isMfaEnabled: Boolean = false,
    val isLoggedIn: Boolean = false
)
