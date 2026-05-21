package com.portfolio.financetracker.ui.auth

import com.portfolio.financetracker.domain.use_case.auth.AuthUseCases
import com.portfolio.financetracker.domain.use_case.auth.ValidateAuthInput
import com.portfolio.financetracker.domain.util.ValidationResult
import io.mockk.coEvery
import io.mockk.every
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private lateinit var viewModel: AuthViewModel
    private val authUseCases = mockk<AuthUseCases>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        every { authUseCases.getCurrentUser() } returns flowOf(null)
        
        val mockValidator = mockk<ValidateAuthInput>(relaxed = true)
        // Mock valid login
        every { mockValidator.validateLogin(any(), any()) } returns ValidateAuthInput.ValidationData(
            isValid = true,
            sanitizedEmail = "test@test.com",
            sanitizedPassword = "password",
            emailResult = ValidationResult(isValid = true),
            passwordResult = ValidationResult(isValid = true)
        )
        
        every { authUseCases.validateAuthInput } returns mockValidator
        
        viewModel = AuthViewModel(authUseCases)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `toggleMode switches between login and register`() {
        assertTrue(viewModel.uiState.value.isLoginMode)
        viewModel.toggleMode()
        assertFalse(viewModel.uiState.value.isLoginMode)
        viewModel.toggleMode()
        assertTrue(viewModel.uiState.value.isLoginMode)
    }

    @Test
    fun `signIn success updates state`() = runTest(testDispatcher) {
        coEvery { authUseCases.signIn(any(), any()) } returns Result.success(Unit)

        viewModel.signIn("test@test.com", "password")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isSuccess)
    }

    @Test
    fun `signIn failure updates error message`() = runTest(testDispatcher) {
        coEvery { authUseCases.signIn(any(), any()) } returns Result.failure(Exception("Network error"))

        viewModel.signIn("test@test.com", "password")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
        assertEquals("Network error", state.errorMessage)
    }
}
