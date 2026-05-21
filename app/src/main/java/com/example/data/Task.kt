package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

enum class Priority {
    LOW, MEDIUM, HIGH
}

enum class RecurrenceType {
    NONE, DAILY, WEEKLY, MONTHLY
}

@JsonClass(generateAdapter = true)
data class Subtask(
    val id: String,
    val title: String,
    val isCompleted: Boolean = false
)

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val deadline: Long? = null, // Epoch ms for deadline
    val recurrenceType: RecurrenceType = RecurrenceType.NONE,
    val priority: Priority = Priority.MEDIUM,
    val subtasks: List<Subtask> = emptyList(),
    val category: String = "General",
    val optimalScheduleTime: Long? = null, // Milliseconds suggested by AI
    val aiSchedulingReason: String? = null,
    val aiPriorityReason: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
