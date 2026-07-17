package com.example.presentation.util

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.domain.model.Message
import com.example.data.repository.ConversationRepository

/**
 * PagingSource for efficiently loading messages in conversations.
 * Supports bidirectional loading for chat interfaces.
 */
class MessagePagingSource(
    private val conversationId: String,
    private val conversationRepository: ConversationRepository
) : PagingSource<Int, Message>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Message> {
        return try {
            val pageIndex = params.key ?: 0
            val pageSize = params.loadSize.coerceAtMost(50) // Cap at 50 to avoid memory issues
            val offset = pageIndex * pageSize

            // Fetch messages for conversation with pagination
            val allMessages = conversationRepository.getMessagesForConversationList(conversationId)
            val sortedMessages = allMessages.sortedBy { it.createdAt }
            
            val pagedMessages = sortedMessages
                .drop(offset)
                .take(pageSize)

            val prevKey = if (pageIndex > 0) pageIndex - 1 else null
            val nextKey = if (pagedMessages.size >= pageSize) pageIndex + 1 else null

            LoadResult.Page(
                data = pagedMessages,
                prevKey = prevKey,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Message>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val closestPage = state.closestPageToPosition(anchorPosition)
            closestPage?.prevKey?.let { it + 1 } ?: closestPage?.nextKey?.let { it - 1 }
        }
    }
}
