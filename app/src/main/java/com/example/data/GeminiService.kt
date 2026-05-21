package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// --- Gemini REST API Request & Response Models ---

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null,
    val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponsePart(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponseContent(
    val parts: List<GeminiResponsePart>
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiResponseContent
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

// --- Domain-Specific Suggestions Models ---

@JsonClass(generateAdapter = true)
data class AisTaskSuggestion(
    val priority: String, // "LOW" | "MEDIUM" | "HIGH"
    val priorityReason: String?,
    val category: String,
    val optimalScheduleTime: Long?, // MS Timestamp
    val schedulingReason: String?,
    val suggestedSubtasks: List<String>?
)

@JsonClass(generateAdapter = true)
data class AisBulkTaskSuggestion(
    val taskId: Long,
    val priority: String,
    val optimalScheduleTime: Long?,
    val aiPriorityReason: String?,
    val aiSchedulingReason: String?
)

@JsonClass(generateAdapter = true)
data class AisBulkPlanSuggestions(
    val plannedTasks: List<AisBulkTaskSuggestion>
)

// --- Retrofit API Service ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiService: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }

    suspend fun getSingleTaskOptimization(
        title: String,
        description: String,
        deadlineMs: Long?,
        recurrenceTypeStr: String
    ): AisTaskSuggestion? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e("GeminiCore", "Gemini API Key is blank or placeholder.")
            return@withContext null
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val currentDateStr = dateFormat.format(Date())
        val deadlineStr = if (deadlineMs != null) dateFormat.format(Date(deadlineMs)) else "No deadline"

        val prompt = """
            Analyze the following task and suggest:
            1. An appropriate priority (LOW, MEDIUM, HIGH) based on the task description and deadline.
            2. A brief, 1-sentence reason for this priority.
            3. A category (e.g., "Work", "Personal", "Health", "Study", "Finance").
            4. An optimal scheduling time (within the next 7 days, as a Unix epoch millisecond timestamp) and a brief reason.
            5. A list of 3 to 5 clear, actionable subtasks to help break this task down.

            Task Details:
            - Title: "$title"
            - Description: "$description"
            - Deadline: "$deadlineStr"
            - Recurrence Frequency: "$recurrenceTypeStr"
            - Current Context Time: "$currentDateStr"

            You MUST strictly return a valid JSON object in your response text conforming EXACTLY to this schema. DO NOT wrap in Markdown code blocks like ```json ... ```:
            {
              "priority": "HIGH",
              "priorityReason": "The deadline is coming up fast, making this highly urgent.",
              "category": "Work",
              "optimalScheduleTime": 1716345600000,
              "schedulingReason": "This is scheduled on Monday morning to give you enough prep time before the deadline.",
              "suggestedSubtasks": ["Verify details of the draft document", "Incorporate feedback from team members", "Finalize submission"]
            }
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = prompt)))
            ),
            generationConfig = GeminiGenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.7f
            )
        )

        try {
            val response = apiService.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                // Parse suggesting json text using Moshi
                Log.d("GeminiCore", "Raw Single Task Suggestion response: $jsonText")
                val cleanJson = cleanJsonResponse(jsonText)
                val adapter = moshi.adapter(AisTaskSuggestion::class.java)
                adapter.fromJson(cleanJson)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("GeminiCore", "Error suggestions for task", e)
            null
        }
    }

    suspend fun getBulkTaskOptimization(
        incompleteTasks: List<Task>
    ): AisBulkPlanSuggestions? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e("GeminiCore", "Gemini API Key is blank or placeholder.")
            return@withContext null
        }

        if (incompleteTasks.isEmpty()) return@withContext null

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val currentDateStr = dateFormat.format(Date())

        val tasksListStr = incompleteTasks.joinToString("\n") { task ->
            val dStr = if (task.deadline != null) dateFormat.format(Date(task.deadline)) else "None"
            val subsStr = task.subtasks.joinToString(", ") { it.title }
            "- [ID:${task.id}] Title: \"${task.title}\", Desc: \"${task.description}\", Deadline: $dStr, Priority: ${task.priority}, Recurrence: ${task.recurrenceType}, Subtasks: [$subsStr]"
        }

        val prompt = """
            You are a productivity expert. Analyze the following list of active tasks and coordinate an optimized schedule plan. For each task, suggest:
            1. Its optimal priority (LOW, MEDIUM, HIGH) to streamline work and balance load.
            2. An ideal schedule time (as a Unix epoch millisecond timestamp) matching busy times, deadlines, and dependencies.
            3. A brief reason for the proposed priority and scheduling.

            Current Context Time: "$currentDateStr"

            System Tasks List:
            $tasksListStr

            You MUST strictly return a valid JSON object in your response text conforming EXACTLY to this schema. DO NOT wrap in Markdown code blocks like ```json ... ```:
            {
              "plannedTasks": [
                {
                  "taskId": 1,
                  "priority": "HIGH",
                  "optimalScheduleTime": 1716385200000,
                  "aiPriorityReason": "Due tomorrow morning and critical helper tasks depend on this.",
                  "aiSchedulingReason": "Scheduled for early this evening to align with your focus hours before dinner."
                }
              ]
            }
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = prompt)))
            ),
            generationConfig = GeminiGenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.5f
            )
        )

        try {
            val response = apiService.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                Log.d("GeminiCore", "Raw Bulk Plan response: $jsonText")
                val cleanJson = cleanJsonResponse(jsonText)
                val adapter = moshi.adapter(AisBulkPlanSuggestions::class.java)
                adapter.fromJson(cleanJson)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("GeminiCore", "Error in bulk schedules", e)
            null
        }
    }

    private fun cleanJsonResponse(raw: String): String {
        // Sometimes LLMs still wrap the JSON in markdown code blocks even when asked not to
        var cleaned = raw.trim()
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.removePrefix("```json")
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.removeSuffix("```")
            }
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```")
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.removeSuffix("```")
            }
        }
        return cleaned.trim()
    }
}
