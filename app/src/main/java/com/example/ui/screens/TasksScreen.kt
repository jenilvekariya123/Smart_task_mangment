package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.ui.TaskViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: TaskViewModel,
    onAddTaskClick: () -> Unit
) {
    val tasks by viewModel.tasks.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedPriorityFilter by remember { mutableStateOf<Priority?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var showCompletedOnly by remember { mutableStateOf<Boolean?>(null) } // null = All, false = Active, true = Completed

    val sdf = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())

    // Derive unique categories for filter chips
    val categories = remember(tasks) {
        tasks.map { it.category }.filter { it.isNotBlank() }.distinct()
    }

    // Filter tasks based on selected configurations
    val filteredTasks = remember(tasks, searchQuery, selectedPriorityFilter, selectedCategoryFilter, showCompletedOnly) {
        tasks.filter { task ->
            val matchesSearch = task.title.contains(searchQuery, ignoreCase = true) || 
                                task.description.contains(searchQuery, ignoreCase = true)
            val matchesPriority = selectedPriorityFilter == null || task.priority == selectedPriorityFilter
            val matchesCategory = selectedCategoryFilter == null || task.category.equals(selectedCategoryFilter, ignoreCase = true)
            val matchesCompletion = when (showCompletedOnly) {
                true -> task.isCompleted
                false -> !task.isCompleted
                null -> true
            }
            matchesSearch && matchesPriority && matchesCategory && matchesCompletion
        }
    }

    // Divide filtered tasks into Active and Completed
    val activeTasks = filteredTasks.filter { !it.isCompleted }
    val completedTasks = filteredTasks.filter { it.isCompleted }

    // Progress metrics
    val totalCount = tasks.size
    val completedCount = tasks.count { it.isCompleted }
    val progressFraction = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Welcome & Workload progress Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("progress_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Your Productivity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (totalCount == 0) {
                            "No tasks available. Tap + to begin!"
                        } else {
                            "Completed $completedCount of $totalCount tasks (${(progressFraction * 100).toInt()}% done)"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .testTag("weekly_progress_indicator"),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search tasks...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_search_field"),
                singleLine = true,
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "")
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Pills Section (Horizontal Scrollable Row)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Task state Filter Chip
                Button(
                    onClick = {
                        showCompletedOnly = when (showCompletedOnly) {
                            null -> false     // Go to Active
                            false -> true     // Go to Completed
                            true -> null      // Go to All
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showCompletedOnly != null) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (showCompletedOnly != null) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("completion_filter_chip")
                ) {
                    Icon(
                        imageVector = when (showCompletedOnly) {
                            false -> Icons.Default.CheckCircleOutline
                            true -> Icons.Default.CheckCircle
                            null -> Icons.Default.FormatListBulleted
                        },
                        contentDescription = "",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when (showCompletedOnly) {
                            false -> "Active Only"
                            true -> "Completed Only"
                            null -> "All Progress"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Priority Filtering toggle
                Button(
                    onClick = {
                        selectedPriorityFilter = when (selectedPriorityFilter) {
                            null -> Priority.HIGH
                            Priority.HIGH -> Priority.MEDIUM
                            Priority.MEDIUM -> Priority.LOW
                            Priority.LOW -> null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedPriorityFilter != null) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selectedPriorityFilter != null) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("priority_filter_chip")
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (selectedPriorityFilter != null) "${selectedPriorityFilter} Priority" else "Priority (All)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scrollable List of Tasks
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("tasks_lazy_column"),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filteredTasks.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assignment,
                                contentDescription = "",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No corresponding tasks found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Add a new task or adjust the filters above.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Active Tasks Title Header & List
                if (activeTasks.isNotEmpty()) {
                    item {
                        Text(
                            text = "ACTIVE TASKS (${activeTasks.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                    items(activeTasks, key = { it.id }) { task ->
                        TaskItemCard(
                            task = task,
                            viewModel = viewModel,
                            sdf = sdf
                        )
                    }
                }

                // Completed Tasks Title Header & List
                if (completedTasks.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "COMPLETED WORK (${completedTasks.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                    items(completedTasks, key = { it.id }) { task ->
                        TaskItemCard(
                            task = task,
                            viewModel = viewModel,
                            sdf = sdf
                        )
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onAddTaskClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_task_fab"),
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Task")
        }
    }
}

@Composable
fun TaskItemCard(
    task: Task,
    viewModel: TaskViewModel,
    sdf: SimpleDateFormat
) {
    var expanded by remember { mutableStateOf(false) }

    val priorityColor = when (task.priority) {
        Priority.LOW -> MaterialTheme.colorScheme.secondary
        Priority.MEDIUM -> MaterialTheme.colorScheme.tertiary
        Priority.HIGH -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { expanded = !expanded }
            .animateContentSize()
            .testTag("task_card_${task.id}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (expanded) 1.5.dp else 1.dp,
            color = if (expanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Main Line Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Completion Checkbox
                IconButton(
                    onClick = { viewModel.toggleTaskCompletion(task) },
                    modifier = Modifier.testTag("task_checkbox_${task.id}")
                ) {
                    Icon(
                        imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Toggle completion",
                        tint = if (task.isCompleted) MaterialTheme.colorScheme.primary else priorityColor,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Title, Tag & Badges
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                        )
                        if (task.recurrenceType != RecurrenceType.NONE) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Recurring",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Category Label Case
                        if (task.category.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = task.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Priority Label Case
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(priorityColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = task.priority.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = priorityColor,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Is there a schedule or deadline? Show mini-tag
                        if (task.deadline != null) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // AI Spark badge if task has elements optimized by AI
                if (task.aiSchedulingReason != null || task.aiPriorityReason != null) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Optimized by AI",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 4.dp)
                    )
                }

                // Down / Up Arrow indicator
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            // Visible Subtasks checklist (Compact peek, only first 2 incomplete if collapsed and has subtasks)
            val pendingSubs = task.subtasks.filter { !it.isCompleted }
            if (!expanded && pendingSubs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.padding(start = 38.dp)) {
                    Text(
                        text = "Next steps (${task.subtasks.count { it.isCompleted }}/${task.subtasks.size}):",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    pendingSubs.take(2).forEach { sub ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                            Icon(
                                imageVector = Icons.Default.RadioButtonUnchecked,
                                contentDescription = "",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = sub.title,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (pendingSubs.size > 2) {
                        Text(
                            "+ ${pendingSubs.size - 2} more steps...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            // Fully Expanded Panel
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, start = 8.dp, end = 8.dp)
                ) {
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Description text block
                    if (task.description.isNotBlank()) {
                        Text(
                            text = "Description:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Deadline info
                    if (task.deadline != null) {
                        Text(
                            text = "Deadline Due Date:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = "",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = sdf.format(Date(task.deadline)),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // AI Insights in Expandable
                    if (task.aiPriorityReason != null || task.aiSchedulingReason != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "AI Productivity Rationale",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                task.aiPriorityReason?.let {
                                    Text(
                                        text = "• Priority reasoning: $it",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 4.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                task.aiSchedulingReason?.let {
                                    Text(
                                        text = "• Scheduling suggestion: $it",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 4.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Complete Checklist of Subtasks
                    if (task.subtasks.isNotEmpty()) {
                        Text(
                            text = "Interactive Steps:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        task.subtasks.forEach { sub ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleSubtaskCompletion(task, sub.id) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (sub.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = "",
                                    tint = if (sub.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = sub.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textDecoration = if (sub.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                    color = if (sub.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Action buttons: Edit, Delete
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = { viewModel.startEditingTask(task) },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("edit_task_${task.id}")
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit")
                        }

                        Button(
                            onClick = { viewModel.deleteTask(task) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            modifier = Modifier.testTag("delete_task_${task.id}")
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}
