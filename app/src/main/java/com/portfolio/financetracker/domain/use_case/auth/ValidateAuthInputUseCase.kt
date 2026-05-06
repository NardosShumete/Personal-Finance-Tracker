package com.portfolio.financetracker.domain.use_case.auth

import com.portfolio.financetracker.domain.model.ValidationResult
import com.portfolio.financetracker.domain.util.InputValidator
import javax.inject.Inject

/**
 * Validates and sanitizes all auth form fields before any Firebase call.
 *
 * Returns [AuthValidationResult] which bundles per-field results so the
 * ViewModel can map each one to its own error label in the UI.
 */
class ValidateAuthInputUseCase @Inject constructor() {

    data class AuthValidationResult(
        val emailResult:    ValidationResult,
        val passwordResult: ValidationResult,
        val usernameResult: ValidationResult = ValidationResult.Success,
        // Sanitized values ready to pass to Firebase — use these, not the raw inputs
        val sanitizedEmail:    String = "",
        val sanitizedPassword: String = "",
        val sanitizedUsername: String = ""
    ) {
        val isValid: Boolean
            get() = emailResult.isSuccess &&
                    passwordResult.isSuccess &&
                    usernameResult.isSuccess
    }

    /** Called for LOGIN — relaxed password check. */
    fun validateLogin(email: String, password: String): AuthValidationResult {
        val emailResult    = InputValidator.validateEmail(email)
        val passwordResult = InputValidator.validatePasswordLogin(password)
        return AuthValidationResult(
            emailResult       = emailResult,
            passwordResult    = passwordResult,
            sanitizedEmail    = InputValidator.sanitizeEmail(email),
            sanitizedPassword = InputValidator.sanitizePassword(password)
        )
    }

    /** Called for REGISTRATION — full strength password check. */
    fun validateRegister(
        email: String,
        password: String,
        username: String
    ): AuthValidationResult {
        val emailResult    = InputValidator.validateEmail(email)
        val passwordResult = InputValidator.validatePasswordStrong(password)
        val usernameResult = InputValidator.validateUsername(username)
        return AuthValidationResult(
            emailResult       = emailResult,
            passwordResult    = passwordResult,
            usernameResult    = usernameResult,
            sanitizedEmail    = InputValidator.sanitizeEmail(email),
            sanitizedPassword = InputValidator.sanitizePassword(password),
            sanitizedUsername = InputValidator.sanitizeUsername(username)
        )
    }
}
