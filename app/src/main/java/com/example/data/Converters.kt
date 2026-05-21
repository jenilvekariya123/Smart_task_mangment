package com.example.data

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
        
    private val subtaskListType = Types.newParameterizedType(List::class.java, Subtask::class.java)
    private val subtaskListAdapter = moshi.adapter<List<Subtask>>(subtaskListType)

    @TypeConverter
    fun fromSubtaskList(value: List<Subtask>?): String {
        return subtaskListAdapter.toJson(value ?: emptyList())
    }

    @TypeConverter
    fun toSubtaskList(value: String?): List<Subtask> {
        if (value.isNullOrEmpty()) return emptyList()
        return try {
            subtaskListAdapter.fromJson(value) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromPriority(priority: Priority): String {
        return priority.name
    }

    @TypeConverter
    fun toPriority(value: String): Priority {
        return try {
            Priority.valueOf(value)
        } catch (e: Exception) {
            Priority.MEDIUM
        }
    }

    @TypeConverter
    fun fromRecurrenceType(recurrenceType: RecurrenceType): String {
        return recurrenceType.name
    }

    @TypeConverter
    fun toRecurrenceType(value: String): RecurrenceType {
        return try {
            RecurrenceType.valueOf(value)
        } catch (e: Exception) {
            RecurrenceType.NONE
        }
    }
}
