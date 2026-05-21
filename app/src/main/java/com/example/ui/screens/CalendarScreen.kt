package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.TaskViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarScreen(
    viewModel: TaskViewModel
) {
    val tasks by viewModel.tasks.collectAsState()
    val selectedMs by viewModel.selectedCalendarDate.collectAsState()

    // Setup working calendars for operations
    val selectedCal = remember(selectedMs) {
        Calendar.getInstance().apply { timeInMillis = selectedMs }
    }

    var viewingYear by remember(selectedMs) { mutableStateOf(selectedCal.get(Calendar.YEAR)) }
    var viewingMonth by remember(selectedMs) { mutableStateOf(selectedCal.get(Calendar.MONTH)) } // 0-Indexed

    val currentTodayCal = remember { Calendar.getInstance() }

    val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val dayHeaderFormat = remember { SimpleDateFormat("E", Locale.getDefault()) }

    // Represent the base calendar for viewing Month
    val monthCal = remember(viewingYear, viewingMonth) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, viewingYear)
            set(Calendar.MONTH, viewingMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    val totalDaysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val startDayOfWeek = monthCal.get(Calendar.DAY_OF_WEEK) // 1 = Sun, 7 = Sat

    // Compile list of date cells in month
    val daysInGrid = remember(viewingYear, viewingMonth, totalDaysInMonth, startDayOfWeek) {
        val list = mutableListOf<Long?>()
        
        // Pad previous empty cells (Sun start offset)
        for (i in 1 until startDayOfWeek) {
            list.add(null)
        }
        
        // Add actual day epochs
        for (day in 1..totalDaysInMonth) {
            val cellCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, viewingYear)
                set(Calendar.MONTH, viewingMonth)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            list.add(cellCal.timeInMillis)
        }
        
        // Pad tail cells
        while (list.size % 7 != 0) {
            list.add(null)
        }
        list
    }

    val weekRows = daysInGrid.chunked(7)

    // Filter tasks due or scheduled on the SELECTED date
    val selectedDayTasks = remember(tasks, selectedMs) {
        tasks.filter { task ->
            val datesToCheck = listOf(task.deadline, task.optimalScheduleTime).filterNotNull()
            datesToCheck.any { isSameDay(it, selectedMs) }
        }
    }

    val scheduleSdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val detailSdf = SimpleDateFormat("MMM EEE dd, yyyy - hh:mm a", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Calendar Title / Selector header
        Card(
            modifier = Modifier.fillMaxWidth().testTag("calendar_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Selector Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (viewingMonth == 0) {
                                viewingMonth = 11
                                viewingYear -= 1
                            } else {
                                viewingMonth -= 1
                            }
                        },
                        modifier = Modifier.testTag("prev_month_button")
                    ) {
                        Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                    }

                    Text(
                        text = monthFormat.format(monthCal.time),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )

                    IconButton(
                        onClick = {
                            if (viewingMonth == 11) {
                                viewingMonth = 0
                                viewingYear += 1
                            } else {
                                viewingMonth += 1
                            }
                        },
                        modifier = Modifier.testTag("next_month_button")
                    ) {
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next Month")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Days of week Headers Row (S M T W T F S)
                Row(modifier = Modifier.fillMaxWidth()) {
                    val daysHeaderCal = Calendar.getInstance()
                    for (i in 1..7) {
                        daysHeaderCal.set(Calendar.DAY_OF_WEEK, i)
                        val dayName = dayHeaderFormat.format(daysHeaderCal.time).take(2)
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Days Grid Rows
                weekRows.forEach { week ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        week.forEach { cellMs ->
                            if (cellMs != null) {
                                val cellCal = Calendar.getInstance().apply { timeInMillis = cellMs }
                                val isSelected = isSameDay(cellMs, selectedMs)
                                val isToday = isSameDay(cellMs, currentTodayCal.timeInMillis)
                                val dayNum = cellCal.get(Calendar.DAY_OF_MONTH)

                                // Check elements due on this day
                                val dayTasks = tasks.filter { task ->
                                    val times = listOf(task.deadline, task.optimalScheduleTime).filterNotNull()
                                    times.any { isSameDay(it, cellMs) }
                                }
                                val maxPriority = dayTasks.filter { !it.isCompleted }.map { it.priority }.maxWithOrNull(compareBy { it.ordinal })

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                        )
                                        .border(
                                            width = if (isToday) 1.5.dp else 0.dp,
                                            color = if (isToday) MaterialTheme.colorScheme.outline else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            viewModel.selectCalendarDate(cellMs)
                                        }
                                        .testTag("calendar_day_$dayNum"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = dayNum.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 15.sp,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimary
                                            } else if (isToday) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                        
                                        // Priority DOT display below number
                                        if (dayTasks.isNotEmpty()) {
                                            val dotColor = when (maxPriority) {
                                                null -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) // completed only
                                                Priority.HIGH -> MaterialTheme.colorScheme.error
                                                Priority.MEDIUM -> MaterialTheme.colorScheme.tertiary
                                                Priority.LOW -> MaterialTheme.colorScheme.secondary
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else dotColor)
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Empty spacer cell
                                Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected Date Label Header
        val selectedHeaderStr = remember(selectedMs) {
            val sdfHeader = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
            sdfHeader.format(Date(selectedMs))
        }

        Text(
            text = "Due & Scheduled: $selectedHeaderStr",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Scrollable List of Due Tasks
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("calendar_tasks_list"),
            contentPadding = PaddingValues(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (selectedDayTasks.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Relax! No tasks scheduled today",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(selectedDayTasks, key = { it.id }) { task ->
                    TaskItemCard(
                        task = task,
                        viewModel = viewModel,
                        sdf = detailSdf
                    )
                }
            }
        }
    }
}

// Check calendar helper which compares year, month, and day-of-month ignoring hour/min/seconds
private fun isSameDay(ms1: Long, ms2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = ms1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = ms2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
