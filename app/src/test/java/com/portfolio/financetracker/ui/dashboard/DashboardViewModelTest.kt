package com.portfolio.financetracker.ui.dashboard

import com.portfolio.financetracker.domain.model.MonthlyGoal
import com.portfolio.financetracker.domain.model.RecurringPeriod
import com.portfolio.financetracker.domain.model.Transaction
import com.portfolio.financetracker.domain.model.TransactionSource
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
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private lateinit var viewModel: DashboardViewModel
    private val transactionUseCases = mockk<TransactionUseCases>(relaxed = true)
    private val goalUseCases        = mockk<GoalUseCases>(relaxed = true)
    private val testDispatcher      = StandardTestDispatcher()

    // Helper to build a minimal valid Transaction
    private fun makeTransaction(
        id: Int,
        amount: Double,
        category: String,
        type: TransactionType,
        bankName: String? = null,
        date: Long = 0L,
        isPending: Boolean = false
    ) = Transaction(
        id              = id,
        amount          = amount,
        category        = category,
        date            = date,
        type            = type,
        note            = "",
        receiptPath     = null,
        recurringPeriod = RecurringPeriod.NONE,
        source          = TransactionSource.MANUAL,
        rawSms          = null,
        smsBalance      = null,
        smsHash         = null,
        smsId           = null,
        isPending       = isPending,
        bankName        = bankName
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val sampleTransactions = listOf(
            makeTransaction(1, 100.0, "Food",   TransactionType.EXPENSE, bankName = "CBE"),
            makeTransaction(2, 500.0, "Salary", TransactionType.INCOME,  bankName = "CBE")
        )

        val sampleGoal = MonthlyGoal(
            monthYear    = "05-2026",
            incomeGoal   = 2000.0,
            expenseLimit = 1000.0
        )

        coEvery { transactionUseCases.getTransactions() } returns flowOf(sampleTransactions)
        coEvery { transactionUseCases.getPagedTransactions() } returns flowOf()
        coEvery { goalUseCases.getGoal(any()) } returns flowOf(sampleGoal)

        viewModel = DashboardViewModel(transactionUseCases, goalUseCases)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `dashboard state calculates all-time totals correctly`() = runTest(testDispatcher) {
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(400.0, state.totalBalance, 0.0)
        assertEquals(500.0, state.totalIncome,  0.0)
        assertEquals(100.0, state.totalExpense, 0.0)
    }

    @Test
    fun `bank balances are grouped correctly`() = runTest(testDispatcher) {
        advanceUntilIdle()

        val bankBalance = viewModel.state.value.bankBalances["CBE"]
        assertNotNull(bankBalance)
        assertEquals(400.0, bankBalance!!.balance, 0.0)
        assertEquals(500.0, bankBalance.income,    0.0)
        assertEquals(100.0, bankBalance.expense,   0.0)
    }

    @Test
    fun `search query updates state but does not change totals`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.onEvent(DashboardEvent.OnSearchQueryChanged("Food"))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Food", state.searchQuery)
        // Totals are always from ALL confirmed transactions, not the filtered list
        assertEquals(500.0, state.totalIncome,  0.0)
        assertEquals(100.0, state.totalExpense, 0.0)
    }

    @Test
    fun `period toggle changes selected period`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.onEvent(DashboardEvent.OnPeriodChanged(SummaryPeriod.TODAY))
        advanceUntilIdle()

        assertEquals(SummaryPeriod.TODAY, viewModel.state.value.selectedPeriod)
    }

    @Test
    fun `pending transactions are excluded from totals`() = runTest(testDispatcher) {
        val transactions = listOf(
            makeTransaction(1, 100.0, "Food",   TransactionType.EXPENSE, isPending = false),
            makeTransaction(2, 500.0, "Salary", TransactionType.INCOME,  isPending = false),
            makeTransaction(3, 999.0, "Pending SMS", TransactionType.EXPENSE, isPending = true)
        )
        coEvery { transactionUseCases.getTransactions() } returns flowOf(transactions)
        coEvery { transactionUseCases.getPagedTransactions() } returns flowOf()

        viewModel = DashboardViewModel(transactionUseCases, goalUseCases)
        advanceUntilIdle()

        val state = viewModel.state.value
        // Pending transaction (999.0) must NOT be included
        assertEquals(100.0, state.totalExpense, 0.0)
        assertEquals(400.0, state.totalBalance, 0.0)
    }

    @Test
    fun `delete transaction calls use case`() = runTest(testDispatcher) {
        val transaction = makeTransaction(3, 50.0, "Misc", TransactionType.EXPENSE)
        viewModel.onEvent(DashboardEvent.DeleteTransaction(transaction))
        advanceUntilIdle()

        coVerify { transactionUseCases.deleteTransaction(transaction) }
    }
}
