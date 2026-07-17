package com.example.domain.usecase

import com.example.domain.model.Tool
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolRegistry @Inject constructor(
    webSearchTool: WebSearchTool
) {
    private val tools = mapOf(
        webSearchTool.id to webSearchTool
    )

    fun getTool(id: String): Tool? = tools[id]
    
    fun getAllTools(): List<Tool> = tools.values.toList()
}
