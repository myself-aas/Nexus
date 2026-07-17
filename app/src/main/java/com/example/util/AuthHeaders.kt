package com.example.util

fun bearerAuthorizationValue(apiKey: String): String {
    val trimmed = apiKey.trim()
    val authScheme = "Bearer"
    return if (trimmed.startsWith("Bearer ", ignoreCase = true)) {
        trimmed
    } else {
        "$authScheme $trimmed"
    }
}
