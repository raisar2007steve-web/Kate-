package com.example

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screens.AssistantDashboard
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AssistantViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DashboardRenderingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testDashboardInflation() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = AssistantViewModel(application)

        composeTestRule.setContent {
            MyApplicationTheme {
                AssistantDashboard(viewModel = viewModel)
            }
        }

        // Wait for composition to settle down
        composeTestRule.waitForIdle()
    }
}
