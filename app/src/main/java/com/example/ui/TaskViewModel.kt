package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

enum class Tab {
    TASKS, CALENDAR, AI_HUB
}

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = TaskRepository(database.taskDao())

    // UI state flows
    val tasks: StateFlow<List<Task>> = repository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentTab = MutableStateFlow(Tab.TASKS)
    val currentTab: StateFlow<Tab> = _currentTab.asStateFlow()

    private val _selectedCalendarDate = MutableStateFlow(System.currentTimeMillis())
    val selectedCalendarDate: StateFlow<Long> = _selectedCalendarDate.asStateFlow()

    private val _editingTask = MutableStateFlow<Task?>(null)
    val editingTask: StateFlow<Task?> = _editingTask.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    private val _singleTaskAiSuggestion = MutableStateFlow<AisTaskSuggestion?>(null)
    val singleTaskAiSuggestion: StateFlow<AisTaskSuggestion?> = _singleTaskAiSuggestion.asStateFlow()

    fun selectTab(tab: Tab) {
        _currentTab.value = tab
    }

    fun selectCalendarDate(timeMs: Long) {
        _selectedCalendarDate.value = timeMs
    }

    fun startEditingTask(task: Task?) {
        _editingTask.value = task
        _singleTaskAiSuggestion.value = null // reset suggestion for new task
    }

    fun stopEditingTask() {
        _editingTask.value = null
        _singleTaskAiSuggestion.value = null
    }

    // Task operations
    fun saveTask(task: Task) {
        viewModelScope.launch {
            repository.upsert(task)
            stopEditingTask()
        }
    }

    fun saveSingleSuggestionToEditingTask(suggestion: AisTaskSuggestion) {
        val current = _editingTask.value ?: return
        val currentSubtasks = suggestion.suggestedSubtasks?.mapIndexed { index, title ->
            Subtask(id = "${System.currentTimeMillis()}_$index", title = title, isCompleted = false)
        } ?: emptyList()

        _editingTask.value = current.copy(
            priority = when (suggestion.priority.uppercase()) {
                "HIGH" -> Priority.HIGH
                "LOW" -> Priority.LOW
                else -> Priority.MEDIUM
            },
            category = suggestion.category,
            optimalScheduleTime = suggestion.optimalScheduleTime,
            aiSchedulingReason = suggestion.schedulingReason,
            aiPriorityReason = suggestion.priorityReason,
            subtasks = current.subtasks + currentSubtasks // append sugered subtasks to user entered list
        )
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.delete(task)
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            if (!task.isCompleted) {
                // Completing the task
                if (task.recurrenceType != RecurrenceType.NONE) {
                    // It is a recurring task! Shift the deadline AND optimal schedule time forward, Keep as uncompleted, reset subtasks.
                    val shiftedDeadline = shiftDeadline(task.deadline, task.recurrenceType)
                    val shiftedOptimal = shiftDeadline(task.optimalScheduleTime, task.recurrenceType)
                    
                    val updatedSubtasks = task.subtasks.map { it.copy(isCompleted = false) }
                    
                    val updatedTask = task.copy(
                        deadline = shiftedDeadline,
                        optimalScheduleTime = shiftedOptimal,
                        subtasks = updatedSubtasks,
                        isCompleted = false // stays uncompleted for next round
                    )
                    repository.upsert(updatedTask)
                } else {
                    // Ordinary task, normal complete
                    val updatedTask = task.copy(
                        isCompleted = true,
                        subtasks = task.subtasks.map { it.copy(isCompleted = true) } // Complete all subtasks
                    )
                    repository.upsert(updatedTask)
                }
            } else {
                // Uncompleting a pre-completed task
                val updatedTask = task.copy(isCompleted = false)
                repository.upsert(updatedTask)
            }
        }
    }

    fun toggleSubtaskCompletion(task: Task, subtaskId: String) {
        viewModelScope.launch {
            val updatedSubtasks = task.subtasks.map { subtask ->
                if (subtask.id == subtaskId) {
                    subtask.copy(isCompleted = !subtask.isCompleted)
                } else {
                    subtask
                }
            }
            
            // Check if all subtasks are complete. Usually we don't auto-complete parent but we can
            val updatedTask = task.copy(subtasks = updatedSubtasks)
            repository.upsert(updatedTask)
        }
    }

    // AI Helpers
    fun fetchSingleTaskAiImprovement(title: String, description: String, deadlineMs: Long?, recurrenceTypeStr: String) {
        _aiLoading.value = true
        _aiError.value = null
        viewModelScope.launch {
            try {
                val suggestion = GeminiClient.getSingleTaskOptimization(
                    title = title,
                    description = description,
                    deadlineMs = deadlineMs,
                    recurrenceTypeStr = recurrenceTypeStr
                )
                if (suggestion != null) {
                    _singleTaskAiSuggestion.value = suggestion
                } else {
                    _aiError.value = "Failed to fetch suggestions. Please verify your API Key in the Secrets panel."
                }
            } catch (e: Exception) {
                _aiError.value = "Error connecting to AI: ${e.message}"
            } finally {
                _aiLoading.value = false
            }
        }
    }

    fun optimizeFullScheduleWithAi() {
        val currentTasks = tasks.value.filter { !it.isCompleted }
        if (currentTasks.isEmpty()) {
            _aiError.value = "No incomplete tasks found to schedule!"
            return
        }

        _aiLoading.value = true
        _aiError.value = null

        viewModelScope.launch {
            try {
                val plan = GeminiClient.getBulkTaskOptimization(currentTasks)
                if (plan != null && plan.plannedTasks.isNotEmpty()) {
                    // Update all tasks in database with recommendations
                    for (suggestion in plan.plannedTasks) {
                        val matchingTask = currentTasks.find { it.id == suggestion.taskId }
                        if (matchingTask != null) {
                            val updatedPriority = when (suggestion.priority.uppercase()) {
                                "HIGH" -> Priority.HIGH
                                "LOW" -> Priority.LOW
                                else -> Priority.MEDIUM
                            }
                            val updatedTask = matchingTask.copy(
                                priority = updatedPriority,
                                optimalScheduleTime = suggestion.optimalScheduleTime,
                                aiPriorityReason = suggestion.aiPriorityReason,
                                aiSchedulingReason = suggestion.aiSchedulingReason
                            )
                            repository.upsert(updatedTask)
                        }
                    }
                } else {
                    _aiError.value = "AI scheduling failed. Make sure your GEMINI_API_KEY is configured."
                }
            } catch (e: Exception) {
                _aiError.value = "Error during planning optimization: ${e.message}"
            } finally {
                _aiLoading.value = false
            }
        }
    }

    fun dismissAiError() {
        _aiError.value = null
    }

    private fun shiftDeadline(currentMs: Long?, recurrence: RecurrenceType): Long? {
        if (currentMs == null) return null
        val calendar = Calendar.getInstance().apply { timeInMillis = currentMs }
        when (recurrence) {
            RecurrenceType.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            RecurrenceType.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            RecurrenceType.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            RecurrenceType.NONE -> {}
        }
        return calendar.timeInMillis
    }
}
