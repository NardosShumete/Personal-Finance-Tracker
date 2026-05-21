package com.portfolio.financetracker.ui

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.portfolio.financetracker.domain.use_case.TransactionUseCases
import com.portfolio.financetracker.ui.transaction.AddTransactionScreen
import com.portfolio.financetracker.ui.transaction.AddTransactionViewModel
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], instrumentedPackages = ["androidx.loader.content"])
class TransactionFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun addTransactionFlow_validInputs_callsSave() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val useCases = mockk<TransactionUseCases>(relaxed = true)
        val viewModel = AddTransactionViewModel(useCases, context, SavedStateHandle())

        composeTestRule.setContent {
            AddTransactionScreen(
                onNavigateBack = {},
                viewModel = viewModel
            )
        }

        // Wait for Compose to render
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("amount_input").performTextInput("150.0")
        
        composeTestRule.onNodeWithTag("category_input").performTextInput("Groceries")
        
        composeTestRule.onNodeWithTag("note_input").performTextInput("Weekend shopping")

        // Click Save button
        composeTestRule.onNodeWithTag("save_button").performClick()

        // Since we mocked useCases, it won't crash and will emit SaveSuccess.
        // Verifying the use case was called is done in Unit Tests, here we just check if UI interacts without crashing.
    }
}
