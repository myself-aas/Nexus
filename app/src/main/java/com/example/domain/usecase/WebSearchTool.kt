package com.example.domain.usecase

import com.example.domain.model.Tool
import com.example.data.repository.ApiKeyRepository
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class WebSearchTool @Inject constructor(
    private val apiKeyRepository: ApiKeyRepository
) : Tool {
    override val id: String = "web_search"
    override val name: String = "web_search"
    override val description: String = "Search the web for current events, news, or factual information not in your training data."
    override val jsonSchema: String = """
        {
          "type": "object",
          "properties": {
            "query": {
              "type": "string",
              "description": "The search query."
            }
          },
          "required": ["query"]
        }
    """.trimIndent()

    override suspend fun execute(arguments: String): String = withContext(Dispatchers.IO) {
        try {
            val argsJson = JSONObject(arguments)
            val query = argsJson.optString("query")
            if (query.isEmpty()) return@withContext "Error: No query provided."

            val apiKey = apiKeyRepository.getDecryptedApiKey("tool_web_search") 
            if (apiKey.isNullOrEmpty()) {
                return@withContext "Error: Web Search API key not configured. Please configure it in Settings (Add a provider named 'tool_web_search' for Brave Search API)."
            }

            val url = URL("https://api.search.brave.com/res/v1/web/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}&count=3")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("X-Subscription-Token", apiKey)
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode in 200..299) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val web = json.optJSONObject("web")
                val results = web?.optJSONArray("results")
                if (results != null && results.length() > 0) {
                    val sb = java.lang.StringBuilder()
                    for (i in 0 until results.length()) {
                        val item = results.getJSONObject(i)
                        sb.append("Title: ${item.optString("title")}\n")
                        sb.append("URL: ${item.optString("url")}\n")
                        sb.append("Description: ${item.optString("description")}\n\n")
                    }
                    return@withContext sb.toString()
                } else {
                    return@withContext "No results found for query: $query"
                }
            } else {
                return@withContext "Search API returned error code: ${connection.responseCode}"
            }

        } catch (e: Exception) {
            return@withContext "Error executing search: ${e.message}"
        }
    }
}
