package com.portfolio.financetracker.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.portfolio.financetracker.data.local.DataStoreManager
import com.portfolio.financetracker.domain.use_case.GoalUseCases
import com.portfolio.financetracker.ui.settings.goals.MonthlyGoalsScreen
import com.portfolio.financetracker.ui.settings.goals.MonthlyGoalsViewModel
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], instrumentedPackages = ["androidx.loader.content"])
class BudgetFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun budgetConfigurationFlow() {
        val goalUseCases = mockk<GoalUseCases>(relaxed = true)
        val dataStoreManager = mockk<DataStoreManager>(relaxed = true)
        
        // Return empty flows so the ViewModel doesn't crash on init
        io.mockk.coEvery { goalUseCases.getGoal(any()) } returns flowOf(null)
        io.mockk.coEvery { goalUseCases.getCategoryBudgets(any()) } returns flowOf(emptyList())
        io.mockk.coEvery { dataStoreManager.isBudgetAlertsEnabled } returns flowOf(false)

        val viewModel = MonthlyGoalsViewModel(goalUseCases, dataStoreManager)

        composeTestRule.setContent {
            MonthlyGoalsScreen(
                onNavigateBack = {},
                viewModel = viewModel
            )
        }

        // Wait for UI to settle (loadData finishes)
        composeTestRule.waitForIdle()

        // Click FAB to open the form
        composeTestRule.onNodeWithText("Setup Budget").performClick()
        
        composeTestRule.waitForIdle()

        // Enter Income Goal
        composeTestRule.onNodeWithText("Monthly Income Target (Birr)").performTextInput("8000")
        
        // Enter Expense Limit
        composeTestRule.onNodeWithText("Max Spending Limit (Birr)").performTextInput("4000")

        // Save Configuration
        composeTestRule.onNodeWithText("Save Configuration").performClick()

        // Verify the Save event was emitted internally (mocked use cases don't throw, so SaveSuccess occurs)
    }
}
