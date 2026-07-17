package com.example.data.repository

import android.content.Context
import com.example.data.database.*
import com.example.data.indexing.DocumentVectorDb
import com.example.security.CryptoHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ChatRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val chatDao = db.chatDao()
    private val messageDao = db.messageDao()
    private val documentDao = db.documentDao()
    private val credentialDao = db.credentialDao()
    private val userDao = db.userDao()

    val allChats: Flow<List<ChatEntity>> = chatDao.getAllChatsFlow()

    suspend fun createChat(id: String, title: String, provider: String, model: String) {
        withContext(Dispatchers.IO) {
            chatDao.insertChat(ChatEntity(id, title, provider, model))
        }
    }

    suspend fun updateChatTitle(id: String, newTitle: String) {
        withContext(Dispatchers.IO) {
            val chat = chatDao.getChatById(id)
            if (chat != null) {
                chatDao.updateChat(chat.copy(title = newTitle))
            }
        }
    }

    suspend fun deleteChat(id: String) {
        withContext(Dispatchers.IO) {
            chatDao.deleteChatById(id)
            messageDao.deleteMessagesForChat(id)
        }
    }

    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>> = 
        messageDao.getMessagesForChatFlow(chatId)

    suspend fun insertMessage(message: MessageEntity) {
        withContext(Dispatchers.IO) {
            messageDao.insertMessage(message)
        }
    }

    val allDocuments: Flow<List<DocumentEntity>> = documentDao.getAllDocumentsFlow()

    suspend fun addDocument(id: String, name: String, content: String, mimeType: String, size: Long) {
        withContext(Dispatchers.IO) {
            val vector = DocumentVectorDb.buildVector(content)
            val serializedVector = DocumentVectorDb.serializeVector(vector)
            val doc = DocumentEntity(
                id = id,
                name = name,
                content = content,
                mimeType = mimeType,
                size = size,
                vectorJson = serializedVector
            )
            documentDao.insertDocument(doc)
        }
    }

    suspend fun deleteDocument(id: String) {
        withContext(Dispatchers.IO) {
            documentDao.deleteDocumentById(id)
        }
    }

    suspend fun searchSimilarDocuments(query: String, limit: Int = 3): List<Pair<DocumentEntity, Double>> {
        return withContext(Dispatchers.IO) {
            val queryVector = DocumentVectorDb.buildVector(query)
            val allDocs = documentDao.getAllDocuments()
            val results = allDocs.map { doc ->
                val docVector = DocumentVectorDb.deserializeVector(doc.vectorJson)
                val similarity = DocumentVectorDb.computeCosineSimilarity(queryVector, docVector)
                Pair(doc, similarity)
            }
            results.filter { it.second > 0.05 }
                .sortedByDescending { it.second }
                .take(limit)
        }
    }

    suspend fun saveCredential(providerId: String, apiKey: String) {
        withContext(Dispatchers.IO) {
            val (encrypted, iv) = CryptoHelper.encrypt(apiKey)
            val cred = CredentialEntity(providerId, encrypted, iv)
            credentialDao.insertCredential(cred)
        }
    }

    suspend fun getDecryptedCredential(providerId: String): String? {
        return withContext(Dispatchers.IO) {
            val cred = credentialDao.getCredentialByProvider(providerId) ?: return@withContext null
            CryptoHelper.decrypt(cred.encryptedKey, cred.iv)
        }
    }

    suspend fun deleteCredential(providerId: String) {
        withContext(Dispatchers.IO) {
            credentialDao.deleteCredentialByProvider(providerId)
        }
    }

    suspend fun getSavedProviders(): List<String> {
        return withContext(Dispatchers.IO) {
            credentialDao.getAllCredentials().map { it.providerId }
        }
    }

    val activeUser: Flow<UserEntity?> = userDao.getActiveUserFlow()

    suspend fun getActiveUserSync(): UserEntity? = withContext(Dispatchers.IO) {
        userDao.getActiveUser()
    }

    suspend fun registerOrLoginUser(id: String, email: String, displayName: String, photoUrl: String? = null) {
        withContext(Dispatchers.IO) {
            val existing = userDao.getActiveUser()
            val mfaSec = existing?.mfaSecret
            val mfaEn = existing?.isMfaEnabled ?: false
            val user = UserEntity(
                id = id,
                email = email,
                displayName = displayName,
                photoUrl = photoUrl,
                mfaSecret = mfaSec,
                isMfaEnabled = mfaEn,
                isLoggedIn = true
            )
            userDao.insertUser(user)
        }
    }

    suspend fun updateMfaSecret(userId: String, secret: String?) {
        withContext(Dispatchers.IO) {
            userDao.updateMfaStatus(userId, secret != null, secret)
        }
    }

    suspend fun logout() {
        withContext(Dispatchers.IO) {
            userDao.clearUsers()
        }
    }
}
