package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.*
import com.example.ui.TaskViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TaskEditorDialog(
    viewModel: TaskViewModel,
    onDismiss: () -> Unit
) {
    val editingTask by viewModel.editingTask.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()
    val aiError by viewModel.aiError.collectAsState()
    val aiSuggestion by viewModel.singleTaskAiSuggestion.collectAsState()

    // Ensure we have a task to manipulate, if null default to a blank task
    val currentTask = editingTask ?: Task(title = "")

    var title by remember { mutableStateOf(currentTask.title) }
    var description by remember { mutableStateOf(currentTask.description) }
    var deadline by remember { mutableStateOf(currentTask.deadline) }
    var recurrenceType by remember { mutableStateOf(currentTask.recurrenceType) }
    var priority by remember { mutableStateOf(currentTask.priority) }
    var category by remember { mutableStateOf(currentTask.category) }
    
    // Subtasks being edited locally
    var subtasksList by remember { mutableStateOf(currentTask.subtasks) }
    var newSubtaskTitle by remember { mutableStateOf("") }

    val context = LocalContext.current
    val sdf = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())

    // If AI Suggestion is applied, update local state fields
    LaunchedEffect(aiSuggestion) {
        aiSuggestion?.let { sug ->
            priority = when (sug.priority.uppercase()) {
                "HIGH" -> Priority.HIGH
                "LOW" -> Priority.LOW
                else -> Priority.MEDIUM
            }
            category = sug.category
            sug.optimalScheduleTime?.let {
                deadline = it
            }
            sug.suggestedSubtasks?.forEach { subtaskText ->
                if (subtasksList.none { it.title.equals(subtaskText, ignoreCase = true) }) {
                    subtasksList = subtasksList + Subtask(
                        id = UUID.randomUUID().toString(),
                        title = subtaskText,
                        isCompleted = false
                    )
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_editor_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel")
                    }
                    Text(
                        text = if (currentTask.id == 0L) "New Task" else "Edit Task",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Button(
                        onClick = {
                            if (title.indigoTrim().isNotEmpty()) {
                                val finalizedTask = currentTask.copy(
                                    title = title.indigoTrim(),
                                    description = description.indigoTrim(),
                                    deadline = deadline,
                                    recurrenceType = recurrenceType,
                                    priority = priority,
                                    category = category.indigoTrim(),
                                    subtasks = subtasksList,
                                    optimalScheduleTime = deadline // defaults or syncs optimal schedule with selected deadline
                                )
                                viewModel.saveTask(finalizedTask)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("save_task_button"),
                        enabled = title.indigoTrim().isNotEmpty()
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title *") },
                    placeholder = { Text("What needs to be done?") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description Input
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("Add task details or remarks") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .testTag("task_description_input"),
                    colors = OutlinedTextFieldDefaults.colors()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Priority Selection
                Text("Priority Level", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Priority.values().forEach { prio ->
                        val isSelected = priority == prio
                        val color = when (prio) {
                            Priority.LOW -> MaterialTheme.colorScheme.secondaryContainer
                            Priority.MEDIUM -> MaterialTheme.colorScheme.tertiaryContainer
                            Priority.HIGH -> MaterialTheme.colorScheme.errorContainer
                        }
                        val onColor = when (prio) {
                            Priority.LOW -> MaterialTheme.colorScheme.onSecondaryContainer
                            Priority.MEDIUM -> MaterialTheme.colorScheme.onTertiaryContainer
                            Priority.HIGH -> MaterialTheme.colorScheme.onErrorContainer
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) color else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { priority = prio }
                                .padding(vertical = 12.dp)
                                .testTag("priority_${prio.name.lowercase()}_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = prio.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) onColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Deadline Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Event, contentDescription = "", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Deadline & Schedule", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (deadline != null) sdf.format(Date(deadline!!)) else "No deadline set",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (deadline != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (deadline != null) {
                        IconButton(onClick = { deadline = null }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear Deadline")
                        }
                    }
                    Button(
                        onClick = {
                            val calendar = Calendar.getInstance()
                            if (deadline != null) {
                                calendar.timeInMillis = deadline!!
                            }
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    calendar.set(Calendar.YEAR, year)
                                    calendar.set(Calendar.MONTH, month)
                                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                    TimePickerDialog(
                                        context,
                                        { _, hour, min ->
                                            calendar.set(Calendar.HOUR_OF_DAY, hour)
                                            calendar.set(Calendar.MINUTE, min)
                                            calendar.set(Calendar.SECOND, 0)
                                            calendar.set(Calendar.MILLISECOND, 0)
                                            deadline = calendar.timeInMillis
                                        },
                                        calendar.get(Calendar.HOUR_OF_DAY),
                                        calendar.get(Calendar.MINUTE),
                                        false
                                    ).show()
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(),
                        modifier = Modifier.testTag("set_deadline_button")
                    ) {
                        Text("Set Due Date")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Recurrence Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.RotateRight, contentDescription = "", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Recurring Task", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = when (recurrenceType) {
                                RecurrenceType.NONE -> "One-time action"
                                RecurrenceType.DAILY -> "Repeats Daily"
                                RecurrenceType.WEEKLY -> "Repeats Weekly"
                                RecurrenceType.MONTHLY -> "Repeats Monthly"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RecurrenceType.values().forEach { rec ->
                        val isSelected = recurrenceType == rec
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { recurrenceType = rec }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = rec.name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Category & Tag Input
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (e.g. Work, Personal)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_category_input"),
                    singleLine = true,
                    leadingIcon = { Icon(imageVector = Icons.Default.Label, contentDescription = "") },
                    colors = OutlinedTextFieldDefaults.colors()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Subtasks Section
                Text("Subtasks (${subtasksList.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                // Add Subtask Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newSubtaskTitle,
                        onValueChange = { newSubtaskTitle = it },
                        placeholder = { Text("Add secondary step...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("subtask_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors()
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newSubtaskTitle.indigoTrim().isNotEmpty()) {
                                subtasksList = subtasksList + Subtask(
                                    id = UUID.randomUUID().toString(),
                                    title = newSubtaskTitle.indigoTrim(),
                                    isCompleted = false
                                )
                                newSubtaskTitle = ""
                            }
                        },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                            .testTag("add_subtask_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Subtask", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // List of existing subtasks in Editor
                subtasksList.forEach { sub ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (sub.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = "",
                                tint = if (sub.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = sub.title, style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(
                            onClick = {
                                subtasksList = subtasksList.filter { it.id != sub.id }
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove subtask", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- AI INTEGRATION CORNER ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Action",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Gemini AI Helper",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Get real-time AI workload recommendation to dynamically prioritize, schedule, and flesh out your task with detailed subtask steps.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (aiLoading) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Gemini is analyzing...", style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (title.indigoTrim().isNotEmpty()) {
                                        viewModel.fetchSingleTaskAiImprovement(
                                            title = title.indigoTrim(),
                                            description = description.indigoTrim(),
                                            deadlineMs = deadline,
                                            recurrenceTypeStr = recurrenceType.name
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ai_suggest_button"),
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primary),
                                enabled = title.indigoTrim().isNotEmpty()
                            ) {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ask AI for Scheduling & Steps", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (aiError != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = aiError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Button(
                                onClick = { viewModel.dismissAiError() },
                                colors = ButtonDefaults.filledTonalButtonColors(),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text("Dismiss")
                            }
                        }

                        aiSuggestion?.let { sug ->
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(12.dp))

                            Text("AI Suggested Parameters:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))

                            Text("Priority Score: ${sug.priority}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            sug.priorityReason?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp))
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text("Recommended Category: ${sug.category}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)

                            Spacer(modifier = Modifier.height(6.dp))

                            Text("Optimal Scheduling Time:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            val suggTimeText = if (sug.optimalScheduleTime != null) sdf.format(Date(sug.optimalScheduleTime)) else "Flexible"
                            Text(suggTimeText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            sug.schedulingReason?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp))
                            }

                            sug.suggestedSubtasks?.let { list ->
                                if (list.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Suggested Action Steps:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    list.forEach { step ->
                                        Row(
                                            modifier = Modifier.padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowRight,
                                                contentDescription = "",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Text(step, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    viewModel.saveSingleSuggestionToEditingTask(sug)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("apply_ai_suggestion"),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = "")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Apply AI Recommendations", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Inline helper for trimming
fun String.indigoTrim(): String {
    return this.trim()
}
