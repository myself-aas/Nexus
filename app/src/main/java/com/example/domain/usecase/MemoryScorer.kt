package com.example.domain.usecase

import com.example.domain.model.Memory
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ln

interface MemoryScorer {
    /**
     * Ranks memories against the query text and returns ranked memories with their score.
     */
    fun rank(memories: List<Memory>, query: String): List<Pair<Memory, Double>>
}

@Singleton
class TfIdfMemoryScorer @Inject constructor() : MemoryScorer {

    private val stopWords = setOf(
        "the", "a", "an", "and", "or", "but", "is", "are", "was", "were", "to", "for", 
        "in", "of", "on", "at", "by", "with", "about", "as", "that", "this", "these", 
        "those", "i", "you", "he", "she", "it", "we", "they", "my", "your", "his", 
        "her", "its", "our", "their"
    )

    override fun rank(memories: List<Memory>, query: String): List<Pair<Memory, Double>> {
        // TODO: Swap in a real on-device TFLite sentence-embedding model here in the future.
        // For now, we use this highly performant, fully local TF-IDF keyword overlap ranking algorithm.
        
        val queryTokens = tokenize(query)
        if (queryTokens.isEmpty() || memories.isEmpty()) {
            return memories.map { it to 0.0 }
        }

        // 1. Calculate Document Frequency (DF) for each query term across all memories
        val df = mutableMapOf<String, Int>()
        memories.forEach { memory ->
            val memoryTokens = tokenize(memory.content).toSet()
            memoryTokens.forEach { token ->
                df[token] = df.getOrDefault(token, 0) + 1
            }
        }

        val totalDocs = memories.size.toDouble()

        // 2. Score each memory based on query TF-IDF
        return memories.map { memory ->
            val memoryTokens = tokenize(memory.content)
            val memoryTf = mutableMapOf<String, Int>()
            memoryTokens.forEach { token ->
                memoryTf[token] = memoryTf.getOrDefault(token, 0) + 1
            }

            var score = 0.0
            queryTokens.forEach { queryToken ->
                val tf = memoryTf.getOrDefault(queryToken, 0)
                if (tf > 0) {
                    val docFreq = df.getOrDefault(queryToken, 0)
                    // Inverse Document Frequency (IDF) with smoothing
                    val idf = ln((1.0 + totalDocs) / (1.0 + docFreq)) + 1.0
                    score += tf * idf
                }
            }
            memory to score
        }.sortedByDescending { it.second }
    }

    private fun tokenize(text: String): List<String> {
        return text.lowercase()
            .split(Regex("[^a-zA-Z0-9]+"))
            .filter { it.length > 2 && !stopWords.contains(it) }
    }
}
