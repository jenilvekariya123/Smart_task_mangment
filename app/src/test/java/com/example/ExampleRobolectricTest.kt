package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.ui.TaskViewModel
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Smart Task Manager", appName)
  }

  @Test
  fun testViewModelInitialization() {
    val context = ApplicationProvider.getApplicationContext<Application>()
    val db = AppDatabase.getDatabase(context)
    val dao = db.taskDao()
    assertNotNull(dao)
    
    val viewModel = TaskViewModel(context)
    assertNotNull(viewModel)
    assertNotNull(viewModel.tasks)
  }

  @Test
  fun testMainContentCompose() {
    val context = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = TaskViewModel(context)
    composeTestRule.setContent {
      MyApplicationTheme {
        MainContent(viewModel = viewModel)
      }
    }
  }
}
