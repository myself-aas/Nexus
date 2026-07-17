package com.example.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return try {
            Json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromToolCallList(value: List<com.example.domain.model.ToolCall>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toToolCallList(value: String): List<com.example.domain.model.ToolCall> {
        return try {
            Json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromFloatArray(value: FloatArray?): String? {
        if (value == null) return null
        return value.joinToString(",")
    }

    @TypeConverter
    fun toFloatArray(value: String?): FloatArray? {
        if (value == null || value.isEmpty()) return null
        return value.split(",").mapNotNull { it.toFloatOrNull() }.toFloatArray()
    }
}
