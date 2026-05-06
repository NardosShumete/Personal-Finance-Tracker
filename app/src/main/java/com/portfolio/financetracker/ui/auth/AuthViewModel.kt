package com.portfolio.financetracker.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.financetracker.domain.model.UserProfile
import com.portfolio.financetracker.domain.use_case.auth.AuthUseCases
import com.portfolio.financetracker.domain.util.InputValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UI State ──────────────────────────────────────────────────────────────────

data class AuthUiState(
    val isLoading: Boolean    = false,
    val errorMessage: String? = null,   // Firebase / network level error
    val isSuccess: Boolean    = false,
    val isLoginMode: Boolean  = true,

    // Per-field real-time validation errors (null = no error shown yet)
    val emailError:    String? = null,
    val passwordError: String? = null,
    val usernameError: String? = null,

    // True only when ALL visible fields pass validation
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
    private val authUseCases: AuthUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _resetState = MutableStateFlow(PasswordResetState())
    val resetState: StateFlow<PasswordResetState> = _resetState.asStateFlow()

    val currentUser: StateFlow<UserProfile?> = authUseCases.getCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ── Real-time validation (called on every keystroke) ──────────────────────

    /**
     * Validates the email field as the user types.
     * Only shows an error after the field has been touched (non-empty input).
     */
    fun onEmailChanged(email: String) {
        val result = InputValidator.validateEmail(email)
        _uiState.update { state ->
            state.copy(
                // Show error only if user has typed something
                emailError = if (email.isNotEmpty()) result.errorMessage else null,
                isFormValid = computeFormValid(
                    state.copy(emailError = result.errorMessage),
                    email    = email,
                    password = null,
                    username = null
                )
            )
        }
    }

    /**
     * Validates the password field as the user types.
     * Uses strong rules in register mode, relaxed rules in login mode.
     */
    fun onPasswordChanged(password: String, isLoginMode: Boolean) {
        val result = if (isLoginMode)
            InputValidator.validatePasswordLogin(password)
        else
            InputValidator.validatePasswordStrong(password)

        _uiState.update { state ->
            state.copy(
                passwordError = if (password.isNotEmpty()) result.errorMessage else null,
                isFormValid   = computeFormValid(
                    state.copy(passwordError = result.errorMessage),
                    email    = null,
                    password = password,
                    username = null
                )
            )
        }
    }

    fun onUsernameChanged(username: String) {
        val result = InputValidator.validateUsername(username)
        _uiState.update { state ->
            state.copy(
                usernameError = if (username.isNotEmpty()) result.errorMessage else null,
                isFormValid   = computeFormValid(
                    state.copy(usernameError = result.errorMessage),
                    email    = null,
                    password = null,
                    username = username
                )
            )
        }
    }

    /**
     * Computes whether the form is submittable.
     * We check the latest error state — null error = field is valid.
     */
    private fun computeFormValid(
        state: AuthUiState,
        email: String?,
        password: String?,
        username: String?
    ): Boolean {
        // A field is "valid" if its error is null AND it's non-empty
        // We can't check emptiness here without the actual field values,
        // so we rely on the error being null as the proxy for validity.
        return state.emailError == null &&
               state.passwordError == null &&
               (state.isLoginMode || state.usernameError == null)
    }

    // ── Auth actions ──────────────────────────────────────────────────────────

    fun signIn(email: String, password: String) {
        // Run full validation + sanitization before touching Firebase
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
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            // Use sanitized values — trimmed + lowercased email
            authUseCases.signIn(validation.sanitizedEmail, validation.sanitizedPassword)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = friendlyMessage(e))
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
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authUseCases.register(
                validation.sanitizedEmail,
                validation.sanitizedPassword,
                validation.sanitizedUsername
            )
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = friendlyMessage(e))
                    }
                }
        }
    }

    fun signOut() {
        viewModelScope.launch { authUseCases.signOut() }
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
                        it.copy(isLoading = false, errorMessage = friendlyMessage(e))
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
                isFormValid   = false
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun friendlyMessage(e: Throwable): String {
        val msg = e.message ?: return "Something went wrong. Please try again."
        return when {
            msg.contains("no user record",       ignoreCase = true) -> "No account found with that email."
            msg.contains("badly formatted",      ignoreCase = true) -> "Please enter a valid email address."
            msg.contains("invalid-email",        ignoreCase = true) -> "Please enter a valid email address."
            msg.contains("user-not-found",       ignoreCase = true) -> "No account found with that email."
            msg.contains("wrong-password",       ignoreCase = true) -> "Incorrect password. Please try again."
            msg.contains("email-already-in-use", ignoreCase = true) -> "An account with this email already exists."
            msg.contains("weak-password",        ignoreCase = true) -> "Password must be at least 8 characters."
            msg.contains("network",              ignoreCase = true) -> "Network error. Check your connection."
            msg.contains("too-many-requests",    ignoreCase = true) -> "Too many attempts. Please try again later."
            else -> msg
        }
    }
}
