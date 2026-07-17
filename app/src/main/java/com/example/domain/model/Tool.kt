package com.example.domain.model

interface Tool {
    val id: String
    val name: String
    val description: String
    val jsonSchema: String // The JSON schema for the arguments

    suspend fun execute(arguments: String): String
}
