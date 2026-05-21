package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.Task
import com.example.ui.Tab
import com.example.ui.TaskViewModel
import com.example.ui.screens.AiHubScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.TaskEditorDialog
import com.example.ui.screens.TasksScreen
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainContent(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(viewModel: TaskViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val editingTask by viewModel.editingTask.collectAsState()

    val todayDateStr = remember {
        val sdfHeader = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault())
        sdfHeader.format(Date())
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = when (currentTab) {
                                Tab.TASKS -> "Priority Flow"
                                Tab.CALENDAR -> "Calendar Schedule"
                                Tab.AI_HUB -> "AI Planner"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = todayDateStr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.30f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Column {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    thickness = 1.dp
                )
                NavigationBar(
                    modifier = Modifier.testTag("app_bottom_bar"),
                    containerColor = MaterialTheme.colorScheme.background,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == Tab.TASKS,
                        onClick = { viewModel.selectTab(Tab.TASKS) },
                        icon = { Icon(imageVector = Icons.Default.FormatListBulleted, contentDescription = "Tasks") },
                        label = { Text("Tasks") },
                        modifier = Modifier.testTag("nav_item_tasks")
                    )
                    NavigationBarItem(
                        selected = currentTab == Tab.CALENDAR,
                        onClick = { viewModel.selectTab(Tab.CALENDAR) },
                        icon = { Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Calendar") },
                        label = { Text("Calendar") },
                        modifier = Modifier.testTag("nav_item_calendar")
                    )
                    NavigationBarItem(
                        selected = currentTab == Tab.AI_HUB,
                        onClick = { viewModel.selectTab(Tab.AI_HUB) },
                        icon = { Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI Hub") },
                        label = { Text("AI Hub") },
                        modifier = Modifier.testTag("nav_item_ai_hub")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                Tab.TASKS -> TasksScreen(
                    viewModel = viewModel,
                    onAddTaskClick = { viewModel.startEditingTask(Task(title = "")) }
                )
                Tab.CALENDAR -> CalendarScreen(
                    viewModel = viewModel
                )
                Tab.AI_HUB -> AiHubScreen(
                    viewModel = viewModel
                )
            }
        }

        // Display Fullscreen / Overlay Task Editor Screen
        if (editingTask != null) {
            TaskEditorDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.stopEditingTask() }
            )
        }
    }
}
