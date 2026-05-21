package com.portfolio.financetracker.ui.dashboard

import com.portfolio.financetracker.domain.model.MonthlyGoal
import com.portfolio.financetracker.domain.model.Transaction
import com.portfolio.financetracker.domain.model.TransactionType
import com.portfolio.financetracker.domain.use_case.GoalUseCases
import com.portfolio.financetracker.domain.use_case.TransactionUseCases
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
class DashboardViewModelTest {

    private lateinit var viewModel: DashboardViewModel
    private val transactionUseCases = mockk<TransactionUseCases>(relaxed = true)
    private val goalUseCases = mockk<GoalUseCases>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val sampleTransactions = listOf(
            Transaction(1, 100.0, "Food", "Lunch", 0L, TransactionType.EXPENSE, receiptPath = null, bankName = "Bank A", recurringPeriod = null),
            Transaction(2, 500.0, "Salary", "Work", 0L, TransactionType.INCOME, receiptPath = null, bankName = "Bank A", recurringPeriod = null)
        )

        val sampleGoal = MonthlyGoal(monthYear = "05-2026", incomeGoal = 2000.0, expenseLimit = 1000.0)

        coEvery { transactionUseCases.getTransactions() } returns flowOf(sampleTransactions)
        coEvery { goalUseCases.getGoal(any()) } returns flowOf(sampleGoal)

        viewModel = DashboardViewModel(transactionUseCases, goalUseCases)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `dashboard state calculates total balance correctly`() = runTest(testDispatcher) {
        advanceUntilIdle() // let stateIn combine flows

        val state = viewModel.state.value
        assertEquals(400.0, state.totalBalance, 0.0)
        assertEquals(500.0, state.totalIncome, 0.0)
        assertEquals(100.0, state.totalExpense, 0.0)
        
        // Check bank balance
        val bankABalance = state.bankBalances["Bank A"]
        assertEquals(400.0, bankABalance?.balance)
    }

    @Test
    fun `search query filters transactions`() = runTest(testDispatcher) {
        advanceUntilIdle()
        
        viewModel.onEvent(DashboardEvent.OnSearchQueryChanged("Food"))
        advanceUntilIdle()

        // With "Food" search, the income transaction is filtered out
        val state = viewModel.state.value
        assertEquals("Food", state.searchQuery)
        // Note: total income/expense are calculated from the FILTERED list
        assertEquals(0.0, state.totalIncome, 0.0)
        assertEquals(100.0, state.totalExpense, 0.0)
    }

    @Test
    fun `delete transaction calls use case`() = runTest(testDispatcher) {
        val transaction = Transaction(3, 50.0, "Misc", "", 0L, TransactionType.EXPENSE, null, null, null)
        viewModel.onEvent(DashboardEvent.DeleteTransaction(transaction))
        
        advanceUntilIdle()
        coVerify { transactionUseCases.deleteTransaction(transaction) }
    }
}
