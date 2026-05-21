package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.ui.TaskViewModel

@Composable
fun AiHubScreen(
    viewModel: TaskViewModel
) {
    val tasks by viewModel.tasks.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()
    val aiError by viewModel.aiError.collectAsState()

    val uncompletedTasks = remember(tasks) { tasks.filter { !it.isCompleted } }
    val totalTasksCount = tasks.size
    val activeTasksCount = uncompletedTasks.size
    val completedTasksCount = tasks.count { it.isCompleted }

    val highPriorityCount = uncompletedTasks.count { it.priority == Priority.HIGH }
    val recurringCount = uncompletedTasks.count { it.recurrenceType != RecurrenceType.NONE }
    val taskWithDeadlineCount = uncompletedTasks.count { it.deadline != null }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // AI Scheduler Core Action Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_balancing_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "AI Whole-Schedule Balancer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Let Gemini analyze your entire workload of active tasks. The AI will coordinate due dates, priority weightings, and recurrence frequencies to update your items with optimal, balanced schedule times and detailed prioritization reasoning.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (aiLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Gemini is planning your schedule...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Button(
                        onClick = { viewModel.optimizeFullScheduleWithAi() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bulk_optimize_schedule_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        enabled = uncompletedTasks.isNotEmpty()
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Auto-Structure My Entire Week", fontWeight = FontWeight.Bold)
                    }
                }

                if (aiError != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = aiError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Button(
                        onClick = { viewModel.dismissAiError() },
                        colors = ButtonDefaults.filledTonalButtonColors(),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text("Dismiss")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Workload Health Check Metrics
        Text(
            text = "Workload Health Check",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Double column metric grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(
                title = "Total Active",
                value = activeTasksCount.toString(),
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Assignment,
                subText = "$completedTasksCount tasks completed",
                containerColor = MaterialTheme.colorScheme.surface
            )
            MetricCard(
                title = "High Urgency",
                value = highPriorityCount.toString(),
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Warning,
                subText = "Action required soon",
                containerColor = if (highPriorityCount > 2) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(
                title = "Recurring Items",
                value = recurringCount.toString(),
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Sync,
                subText = "Refreshes automatically",
                containerColor = MaterialTheme.colorScheme.surface
            )
            MetricCard(
                title = "Has Deadlines",
                value = taskWithDeadlineCount.toString(),
                modifier = Modifier.weight(1f),
                icon = Icons.Default.HourglassBottom,
                subText = "Due targets locked",
                containerColor = MaterialTheme.colorScheme.surface
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // AI Balance & Scheduling Advice Section
        Text(
            text = "AI Scheduling Insights",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(10.dp))

        InsightTipRow(
            icon = Icons.Default.CalendarToday,
            text = "Bulk optimization calculates a load-balanced index to prevent scheduling bottlenecks on a single work night.",
            title = "Weekly Context Routing"
        )
        InsightTipRow(
            icon = Icons.Default.AccessAlarms,
            text = "Deadlines set within 24 hours automatically score higher priority, which feeds into the AI's urgency matrices.",
            title = "Proactive Urgency Triggers"
        )
        InsightTipRow(
            icon = Icons.Default.TipsAndUpdates,
            text = "Recurring tasks (Daily, Weekly, Monthly) automatically advance their target deadlines to keep routines moving.",
            title = "Automated Habit Anchors"
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clip(RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(imageVector = icon, contentDescription = "", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun InsightTipRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
