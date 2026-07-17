package com.example.data.indexing

import kotlin.math.sqrt

object DocumentVectorDb {
    
    fun buildVector(content: String): Map<String, Double> {
        val words = tokenize(content)
        if (words.isEmpty()) return emptyMap()
        
        val freqMap = mutableMapOf<String, Int>()
        for (w in words) {
            freqMap[w] = freqMap.getOrDefault(w, 0) + 1
        }
        
        val maxFreq = freqMap.values.maxOrNull()?.toDouble() ?: 1.0
        val vector = mutableMapOf<String, Double>()
        
        for ((word, count) in freqMap) {
            vector[word] = count.toDouble() / maxFreq
        }
        
        val magnitude = sqrt(vector.values.sumOf { it * it })
        if (magnitude > 0) {
            for (word in vector.keys) {
                vector[word] = vector[word]!! / magnitude
            }
        }
        
        return vector
    }
    
    fun serializeVector(vector: Map<String, Double>): String {
        return vector.entries.joinToString(";") { "${it.key}:${it.value}" }
    }
    
    fun deserializeVector(serialized: String?): Map<String, Double> {
        if (serialized.isNullOrEmpty()) return emptyMap()
        val vector = mutableMapOf<String, Double>()
        try {
            val parts = serialized.split(";")
            for (part in parts) {
                val kv = part.split(":")
                if (kv.size == 2) {
                    val k = kv[0]
                    val v = kv[1].toDoubleOrNull() ?: 0.0
                    vector[k] = v
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return vector
    }
    
    fun computeCosineSimilarity(vecA: Map<String, Double>, vecB: Map<String, Double>): Double {
        if (vecA.isEmpty() || vecB.isEmpty()) return 0.0
        var dotProduct = 0.0
        for ((word, weightA) in vecA) {
            val weightB = vecB[word] ?: 0.0
            dotProduct += weightA * weightB
        }
        
        val magA = sqrt(vecA.values.sumOf { it * it })
        val magB = sqrt(vecB.values.sumOf { it * it })
        
        if (magA == 0.0 || magB == 0.0) return 0.0
        return dotProduct / (magA * magB)
    }
    
    private fun tokenize(text: String): List<String> {
        return text.lowercase()
            .replace("[^a-zA-Z0-9\\s]".toRegex(), " ")
            .split("\\s+".toRegex())
            .filter { it.length > 2 }
    }
}
