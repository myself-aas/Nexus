package com.example.domain.paging

import androidx.paging.LoadParams
import androidx.paging.LoadResult
import androidx.paging.PagingSource
import com.example.data.local.MessageDao
import com.example.domain.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * PagingSource for loading messages in pages from the database.
 * Loads pages in reverse chronological order (newest first) but returns each page in ascending order (oldest first).
 * This allows the list to display oldest at the top and newest at the bottom.
 */
class MessagePagingSource(
    private val messageDao: MessageDao,
    private val conversationId: String
) : PagingSource<Int, Message>() {

    companion object {
        private const val PAGE_SIZE = 20
    }

    // Mutable state for total count, updated in load()
    private var totalCount = 0
        private set

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Message> {
        // Update total count from the database (this is a suspend call)
        totalCount = messageDao.getMessageCountForConversation(conversationId)

        val page = params.key ?: 0 // page 0 is the newest page
        val offset = calculateOffset(page, totalCount)
        val limit = calculateLimit(page, totalCount, PAGE_SIZE)

        // Load the page from the database
        val items = withContext(Dispatchers.IO) {
            messageDao.getMessagesForConversationPage(conversationId, limit, offset)
                .map { it.toDomain() } // Convert to domain model
        }

        // Calculate previous and next keys
        val prevKey = if (offset > 0) page + 1 else null
        val nextKey = if (offset + limit < totalCount) page - 1 else null

        return LoadResult.Page(
            data = items,
            prevKey = prevKey,
            nextKey = nextKey
        )
    }

    override fun getRefreshKey(state: PagingState<Int, Message>): Int? {
        // Use the last known totalCount to estimate the page
        return state.anchorPosition?.let { anchorPosition ->
            val approximatePage = anchorPosition / PAGE_SIZE
            // Ensure the page is within valid bounds [0, totalPages-1]
            val totalPages = if (totalCount == 0) 0 else (totalCount + PAGE_SIZE - 1) / PAGE_SIZE
            when {
                approximatePage < 0 -> 0
                approximatePage >= totalPages -> totalPages - 1
                else -> approximatePage
            }
        }
    }

    private fun calculateOffset(page: Int, totalCount: Int): Int {
        // Calculate offset from the end: page 0 is the last page
        val rawOffset = totalCount - (page + 1) * PAGE_SIZE
        return if (rawOffset < 0) 0 else rawOffset
    }

    private fun calculateLimit(page: Int, totalCount: Int, pageSize: Int): Int {
        val offset = calculateOffset(page, totalCount)
        val remaining = totalCount - offset
        return if (remaining > 0) {
            if (remaining >= pageSize) pageSize else remaining
        } else {
            0
        }
    }
}