package com.portfolio.financetracker.ui.auth

import com.google.firebase.auth.FirebaseAuth
import com.portfolio.financetracker.domain.model.AuthResult
import com.portfolio.financetracker.domain.model.UserProfile
import com.portfolio.financetracker.domain.use_case.auth.AuthUseCases
import com.portfolio.financetracker.domain.use_case.auth.ValidateAuthInputUseCase
import com.portfolio.financetracker.domain.model.ValidationResult
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private lateinit var viewModel: AuthViewModel
    private val authUseCases   = mockk<AuthUseCases>(relaxed = true)
    private val firebaseAuth   = mockk<FirebaseAuth>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // currentUser flow returns null (not logged in)
        every { authUseCases.getCurrentUser() } returns flowOf(null)

        // firebaseAuth.currentUser returns null (no session)
        every { firebaseAuth.currentUser } returns null

        // validateAuthInput returns a valid result for any input
        val mockValidator = mockk<ValidateAuthInputUseCase>(relaxed = true)
        every { mockValidator.validateLogin(any(), any()) } returns
            ValidateAuthInputUseCase.AuthValidationResult(
                emailResult       = ValidationResult.Success,
                passwordResult    = ValidationResult.Success,
                sanitizedEmail    = "test@test.com",
                sanitizedPassword = "Password1@"
            )
        every { authUseCases.validateAuthInput } returns mockValidator

        viewModel = AuthViewModel(authUseCases, firebaseAuth)
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
    fun `signIn success emits NavigateToHome event`() = runTest(testDispatcher) {
        val profile = UserProfile(uid = "uid1", email = "test@test.com", username = "Test")
        coEvery { authUseCases.signIn(any(), any()) } returns Result.success(profile)

        viewModel.signIn("test@test.com", "Password1@")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.authResult is AuthResult.Success)
        assertNull(state.errorMessage)
    }

    @Test
    fun `signIn failure updates error message`() = runTest(testDispatcher) {
        coEvery { authUseCases.signIn(any(), any()) } returns
            Result.failure(Exception("Invalid email or password. Please try again."))

        viewModel.signIn("test@test.com", "Password1@")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.authResult is AuthResult.Error)
        assertEquals("Invalid email or password. Please try again.", state.errorMessage)
    }

    @Test
    fun `clearError resets errorMessage`() = runTest(testDispatcher) {
        coEvery { authUseCases.signIn(any(), any()) } returns
            Result.failure(Exception("Some error"))

        viewModel.signIn("test@test.com", "Password1@")
        advanceUntilIdle()

        viewModel.clearError()
        assertNull(viewModel.uiState.value.errorMessage)
    }
}
