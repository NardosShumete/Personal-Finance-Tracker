package com.portfolio.financetracker.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.portfolio.financetracker.domain.model.AuthResult
import com.portfolio.financetracker.domain.model.UserProfile
import com.portfolio.financetracker.domain.use_case.auth.AuthUseCases
import com.portfolio.financetracker.domain.util.InputValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UI State ──────────────────────────────────────────────────────────────────

data class AuthUiState(
    val isLoading: Boolean     = false,
    val errorMessage: String?  = null,
    val authResult: AuthResult = AuthResult.Idle,
    val isLoginMode: Boolean   = true,

    // Per-field real-time validation errors (null = no error shown yet)
    val emailError:    String? = null,
    val passwordError: String? = null,
    val usernameError: String? = null,

    // True only when ALL visible fields are non-empty AND pass validation
    val isFormValid: Boolean = false
)

data class PasswordResetState(
    val isLoading: Boolean    = false,
    val isSuccess: Boolean    = false,
    val errorMessage: String? = null
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authUseCases: AuthUseCases,
    // Injected directly so we can cheaply check Firebase session state
    // without a suspend call (used only for splash routing, not auth logic).
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _resetState = MutableStateFlow(PasswordResetState())
    val resetState: StateFlow<PasswordResetState> = _resetState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow: SharedFlow<UiEvent> = _eventFlow.asSharedFlow()

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
        object NavigateToHome : UiEvent()
        object NavigateToVerifyEmail : UiEvent()
    }

    /** Emits a non-null [UserProfile] only when signed in AND email verified. */
    val currentUser: StateFlow<UserProfile?> = authUseCases.getCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ── Splash helper ─────────────────────────────────────────────────────────

    /**
     * Returns true if there is a Firebase session for a user whose email has
     * NOT yet been verified. Used by SplashScreen to route to VerifyEmailScreen
     * instead of LoginScreen without making a network call.
     */
    fun hasUnverifiedSession(): Boolean {
        val user = firebaseAuth.currentUser ?: return false
        return !user.isEmailVerified
    }

    // ── Real-time validation (called on every keystroke) ──────────────────────

    /**
     * Validates the email field as the user types.
     * Clears the error when the field is empty (user hasn't committed yet).
     */
    fun onEmailChanged(email: String) {
        val errorMsg = if (email.isNotEmpty())
            InputValidator.validateEmail(email).errorMessage
        else null

        _uiState.update { state ->
            val updated = state.copy(emailError = errorMsg)
            updated.copy(isFormValid = computeFormValid(updated, emailValue = email))
        }
    }

    /**
     * Validates the password field as the user types.
     * Uses strong rules in register mode, relaxed rules in login mode.
     */
    fun onPasswordChanged(password: String, isLoginMode: Boolean) {
        val errorMsg = when {
            password.isEmpty() -> null
            isLoginMode -> InputValidator.validatePasswordLogin(password).errorMessage
            else -> InputValidator.validatePasswordStrong(password).errorMessage
        }

        _uiState.update { state ->
            val updated = state.copy(passwordError = errorMsg)
            updated.copy(isFormValid = computeFormValid(updated, passwordValue = password))
        }
    }

    fun onUsernameChanged(username: String) {
        val errorMsg = if (username.isNotEmpty())
            InputValidator.validateUsername(username).errorMessage
        else null

        _uiState.update { state ->
            val updated = state.copy(usernameError = errorMsg)
            updated.copy(isFormValid = computeFormValid(updated, usernameValue = username))
        }
    }

    /**
     * A form is valid when:
     * - All error fields are null (no validation failures)
     * - All required fields are non-empty
     *
     * We track field emptiness via the caller passing the latest value.
     * Null means "unchanged from current state" — we don't re-check it.
     */
    private fun computeFormValid(
        state: AuthUiState,
        emailValue: String?    = null,
        passwordValue: String? = null,
        usernameValue: String? = null
    ): Boolean {
        // No errors allowed
        if (state.emailError != null || state.passwordError != null) return false
        if (!state.isLoginMode && state.usernameError != null) return false

        // All required fields must be non-empty.
        // We use the passed value when available; otherwise we can't know the
        // current field content from state alone, so we conservatively return
        // false only if we know a field is empty.
        val emailOk    = emailValue?.isNotEmpty() ?: true
        val passwordOk = passwordValue?.isNotEmpty() ?: true
        val usernameOk = state.isLoginMode || (usernameValue?.isNotEmpty() ?: true)

        return emailOk && passwordOk && usernameOk
    }

    // ── Auth actions ──────────────────────────────────────────────────────────

    fun signIn(email: String, password: String) {
        // Run full validation before touching Firebase
        val validation = authUseCases.validateAuthInput.validateLogin(email, password)
        if (!validation.isValid) {
            _uiState.update {
                it.copy(
                    emailError    = validation.emailResult.errorMessage,
                    passwordError = validation.passwordResult.errorMessage,
                    isFormValid   = false
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, authResult = AuthResult.Loading) }
            authUseCases.signIn(validation.sanitizedEmail, validation.sanitizedPassword)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, authResult = AuthResult.Success) }
                    _eventFlow.emit(UiEvent.NavigateToHome)
                }
                .onFailure { e ->
                    val message = e.message ?: "Login failed. Please try again."
                    when {
                        // AuthRepositoryImpl throws EmailNotVerifiedException with this phrase
                        message.contains("verify your email", ignoreCase = true) -> {
                            _uiState.update {
                                it.copy(isLoading = false, authResult = AuthResult.EmailNotVerified)
                            }
                            _eventFlow.emit(UiEvent.NavigateToVerifyEmail)
                        }
                        message.contains("No account found", ignoreCase = true) -> {
                            _uiState.update {
                                it.copy(
                                    isLoading    = false,
                                    errorMessage = message,
                                    authResult   = AuthResult.UserNotFound
                                )
                            }
                        }
                        else -> {
                            _uiState.update {
                                it.copy(
                                    isLoading    = false,
                                    errorMessage = message,
                                    authResult   = AuthResult.Error(message)
                                )
                            }
                        }
                    }
                }
        }
    }

    fun register(email: String, password: String, username: String) {
        val validation = authUseCases.validateAuthInput.validateRegister(email, password, username)
        if (!validation.isValid) {
            _uiState.update {
                it.copy(
                    emailError    = validation.emailResult.errorMessage,
                    passwordError = validation.passwordResult.errorMessage,
                    usernameError = validation.usernameResult.errorMessage,
                    isFormValid   = false
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, authResult = AuthResult.Loading) }
            authUseCases.register(
                validation.sanitizedEmail,
                validation.sanitizedPassword,
                validation.sanitizedUsername
            )
                .onSuccess {
                    // Registration succeeded — user is signed out and must verify email
                    _uiState.update { it.copy(isLoading = false, authResult = AuthResult.EmailNotVerified) }
                    _eventFlow.emit(UiEvent.NavigateToVerifyEmail)
                }
                .onFailure { e ->
                    val message = e.message ?: "Registration failed. Please try again."
                    _uiState.update {
                        it.copy(
                            isLoading    = false,
                            errorMessage = message,
                            authResult   = AuthResult.Error(message)
                        )
                    }
                }
        }
    }

    fun signOut() {
        viewModelScope.launch { authUseCases.signOut() }
    }

    /** Reloads the Firebase user and returns true if email is now verified. */
    suspend fun checkEmailVerification(): Boolean =
        authUseCases.reloadAndCheckVerification()

    fun resendVerificationEmail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            authUseCases.resendVerificationEmail()
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    _eventFlow.emit(UiEvent.ShowSnackbar("Verification email sent! Check your inbox."))
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false) }
                    val msg = e.message ?: "Failed to resend verification email."
                    _eventFlow.emit(UiEvent.ShowSnackbar(msg))
                }
        }
    }

    // ── Password reset ────────────────────────────────────────────────────────

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _resetState.update { it.copy(isLoading = true, errorMessage = null, isSuccess = false) }
            authUseCases.sendPasswordReset(InputValidator.sanitizeEmail(email))
                .onSuccess {
                    _resetState.update { it.copy(isLoading = false, isSuccess = true) }
                }
                .onFailure { e ->
                    _resetState.update {
                        it.copy(isLoading = false, errorMessage = e.message ?: "Reset failed. Please try again.")
                    }
                }
        }
    }

    fun consumeResetState() {
        _resetState.update { PasswordResetState() }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    fun toggleMode() {
        _uiState.update {
            it.copy(
                isLoginMode   = !it.isLoginMode,
                errorMessage  = null,
                emailError    = null,
                passwordError = null,
                usernameError = null,
                isFormValid   = false,
                authResult    = AuthResult.Idle
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
