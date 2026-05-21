package com.portfolio.financetracker.ui.transaction

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.portfolio.financetracker.domain.model.Transaction
import com.portfolio.financetracker.domain.model.TransactionType
import com.portfolio.financetracker.domain.use_case.InvalidTransactionException
import com.portfolio.financetracker.domain.use_case.TransactionUseCases
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddTransactionViewModelTest {

    private lateinit var viewModel: AddTransactionViewModel
    private val transactionUseCases = mockk<TransactionUseCases>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val savedStateHandle = SavedStateHandle()
        viewModel = AddTransactionViewModel(transactionUseCases, context, savedStateHandle)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `entering amount updates state correctly`() {
        viewModel.onEvent(AddTransactionEvent.EnteredAmount("50.0"))
        assertEquals("50.0", viewModel.state.value.amount)
    }

    @Test
    fun `saving valid transaction emits SaveSuccess`() = runTest(testDispatcher) {
        viewModel.onEvent(AddTransactionEvent.EnteredAmount("100.0"))
        viewModel.onEvent(AddTransactionEvent.EnteredCategory("Food"))
        viewModel.onEvent(AddTransactionEvent.EnteredNote("Lunch"))

        viewModel.eventFlow.test {
            viewModel.onEvent(AddTransactionEvent.SaveTransaction)
            
            val emission = awaitItem()
            assertEquals(AddTransactionViewModel.UiEvent.SaveSuccess, emission)
            
            coVerify { transactionUseCases.addTransaction(any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `saving invalid transaction emits ShowSnackbar`() = runTest(testDispatcher) {
        coEvery { transactionUseCases.addTransaction(any()) } throws InvalidTransactionException("Amount cannot be empty")
        
        viewModel.eventFlow.test {
            viewModel.onEvent(AddTransactionEvent.SaveTransaction)
            
            val emission = awaitItem()
            assert(emission is AddTransactionViewModel.UiEvent.ShowSnackbar)
            assertEquals("Amount cannot be empty", (emission as AddTransactionViewModel.UiEvent.ShowSnackbar).message)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
