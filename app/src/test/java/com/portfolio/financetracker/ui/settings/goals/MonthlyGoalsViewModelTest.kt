package com.portfolio.financetracker.ui.settings.goals

import app.cash.turbine.test
import com.portfolio.financetracker.data.local.DataStoreManager
import com.portfolio.financetracker.domain.model.CategoryBudget
import com.portfolio.financetracker.domain.model.MonthlyGoal
import com.portfolio.financetracker.domain.use_case.GoalUseCases
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MonthlyGoalsViewModelTest {

    private lateinit var viewModel: MonthlyGoalsViewModel
    private val goalUseCases = mockk<GoalUseCases>(relaxed = true)
    private val dataStoreManager = mockk<DataStoreManager>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        coEvery { goalUseCases.getGoal(any()) } returns flowOf(null)
        coEvery { goalUseCases.getCategoryBudgets(any()) } returns flowOf(emptyList())
        coEvery { goalUseCases.saveGoal(any()) } returns Unit
        coEvery { goalUseCases.saveCategoryBudget(any()) } returns Unit
        coEvery { dataStoreManager.isBudgetAlertsEnabled } returns flowOf(false)
        
        viewModel = MonthlyGoalsViewModel(goalUseCases, dataStoreManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load sets default categories if none exist`() = runTest(testDispatcher) {
        advanceUntilIdle() // let init block finish
        
        val state = viewModel.state.value
        assertEquals(false, state.isLoading)
        assertEquals(6, state.categoryBudgets.size)
        assertEquals("Food", state.categoryBudgets[0].category)
    }

    @Test
    fun `entering income goal updates state`() = runTest(testDispatcher) {
        viewModel.onEvent(MonthlyGoalsEvent.EnteredIncomeGoal("5000"))
        assertEquals("5000", viewModel.state.value.incomeGoal)
    }

    @Test
    fun `saving goals calls use cases and emits success`() = runTest(testDispatcher) {
        viewModel.onEvent(MonthlyGoalsEvent.EnteredIncomeGoal("6000"))
        viewModel.onEvent(MonthlyGoalsEvent.EnteredExpenseLimit("2000"))

        viewModel.eventFlow.test {
            viewModel.onEvent(MonthlyGoalsEvent.SaveGoals)
            val event = awaitItem()
            
            assertEquals(MonthlyGoalsViewModel.UiEvent.SaveSuccess, event)
            coVerify { goalUseCases.saveGoal(any()) }
            coVerify(exactly = 6) { goalUseCases.saveCategoryBudget(any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }
}
