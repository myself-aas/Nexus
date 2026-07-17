package com.example.data.repository

import com.example.data.local.*
import com.example.domain.model.Attachment
import com.example.domain.model.Conversation
import com.example.domain.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepository @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val attachmentDao: AttachmentDao
) {
    val allConversations: Flow<List<Conversation>> = conversationDao.getAllConversations()
        .map { list -> list.map { it.toDomain() } }

    val activeConversations: Flow<List<Conversation>> = conversationDao.getActiveConversations()
        .map { list -> list.map { it.toDomain() } }

    val archivedConversations: Flow<List<Conversation>> = conversationDao.getArchivedConversations()
        .map { list -> list.map { it.toDomain() } }

    suspend fun getConversationById(id: String): Conversation? = withContext(Dispatchers.IO) {
        conversationDao.getConversationById(id)?.toDomain()
    }

    suspend fun insertConversation(conversation: Conversation) = withContext(Dispatchers.IO) {
        conversationDao.insertConversation(conversation.toEntity())
    }

    suspend fun updateConversation(conversation: Conversation) = withContext(Dispatchers.IO) {
        conversationDao.updateConversation(conversation.toEntity())
    }

    suspend fun updateConversationTitle(id: String, newTitle: String) = withContext(Dispatchers.IO) {
        val convo = conversationDao.getConversationById(id)
        if (convo != null) {
            conversationDao.updateConversation(convo.copy(title = newTitle))
        }
    }

    suspend fun deleteConversationById(id: String) = withContext(Dispatchers.IO) {
        conversationDao.deleteConversationById(id)
        messageDao.deleteMessagesForConversation(id)
    }

    suspend fun deleteAllConversations() = withContext(Dispatchers.IO) {
        conversationDao.deleteAllConversations()
    }

    suspend fun getMessagesForConversationList(conversationId: String): List<Message> = withContext(Dispatchers.IO) {
        messageDao.getMessagesForConversation(conversationId).map { list -> list.map { it.toDomain() } }.first()
    }

    suspend fun getMessageById(id: String): Message? = withContext(Dispatchers.IO) {
        messageDao.getMessageById(id)?.toDomain()
    }

    suspend fun deleteMessageById(id: String) = withContext(Dispatchers.IO) {
        messageDao.deleteMessageById(id)
    }

    fun getMessagesForConversation(conversationId: String): Flow<List<Message>> {
        return messageDao.getMessagesForConversation(conversationId)
            .map { list -> list.map { it.toDomain() } }
    }

    suspend fun insertMessage(message: Message) = withContext(Dispatchers.IO) {
        messageDao.insertMessage(message.toEntity())
    }

    suspend fun getAttachmentsForMessage(messageId: String): List<Attachment> = withContext(Dispatchers.IO) {
        attachmentDao.getAttachmentsForMessage(messageId).map { it.toDomain() }
    }

    suspend fun insertAttachment(attachment: Attachment) = withContext(Dispatchers.IO) {
        attachmentDao.insertAttachment(attachment.toEntity())
    }
}
