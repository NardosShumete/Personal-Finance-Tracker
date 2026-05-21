package com.portfolio.financetracker.domain.util

import com.portfolio.financetracker.domain.model.ValidationResult

/**
 * Pure domain-layer validator — no Android UI imports, fully unit-testable.
 */
object InputValidator {

    private val EMAIL_REGEX = Regex(
        "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                "\\@" +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                "(" +
                "\\." +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                ")+"
    )

    // ── Sanitization ──────────────────────────────────────────────────────────

    fun sanitizeEmail(raw: String): String = raw.trim().lowercase()
    fun sanitizePassword(raw: String): String = raw.trim()
    fun sanitizeUsername(raw: String): String = raw.trim()

    // ── Email validation ──────────────────────────────────────────────────────

    fun validateEmail(raw: String): ValidationResult {
        val email = sanitizeEmail(raw)
        return when {
            email.isEmpty() -> ValidationResult.Error("Email address is required.")
            !EMAIL_REGEX.matches(email) -> ValidationResult.Error("Please enter a valid email address.")
            isDisposableEmail(email) -> ValidationResult.Error("Disposable emails are not allowed.")
            else -> ValidationResult.Success
        }
    }

    private fun isDisposableEmail(email: String): Boolean {
        val disposableDomains = listOf("mailinator.com", "guerrillamail.com", "10minutemail.com")
        val domain = email.substringAfterLast('@')
        return disposableDomains.contains(domain)
    }

    // ── Password validation ───────────────────────────────────────────────────

    fun validatePasswordStrong(raw: String): ValidationResult {
        val password = sanitizePassword(raw)
        return when {
            password.isEmpty() -> ValidationResult.Error("Password is required.")
            password.length < 8 -> ValidationResult.Error("Password must be at least 8 characters.")
            !password.any { it.isUpperCase() } -> ValidationResult.Error("Password must contain at least one uppercase letter.")
            !password.any { it.isDigit() } -> ValidationResult.Error("Password must contain at least one number.")
            !password.any { it in SPECIAL_CHARS } -> ValidationResult.Error("Password must contain at least one special character (@, #, $, etc.).")
            else -> ValidationResult.Success
        }
    }

    fun validatePasswordsMatch(p1: String, p2: String): ValidationResult {
        return if (p1 == p2) ValidationResult.Success
        else ValidationResult.Error("Passwords do not match.")
    }

    fun validatePasswordLogin(raw: String): ValidationResult {
        val password = sanitizePassword(raw)
        return if (password.isEmpty()) ValidationResult.Error("Password is required.")
        else ValidationResult.Success
    }

    // ── Username validation ───────────────────────────────────────────────────

    fun validateUsername(raw: String): ValidationResult {
        val username = sanitizeUsername(raw)
        return when {
            username.isEmpty() -> ValidationResult.Error("Username is required.")
            username.length < 2 -> ValidationResult.Error("Username must be at least 2 characters.")
            else -> ValidationResult.Success
        }
    }

    private const val SPECIAL_CHARS = "!@#\$%^&*()-_=+[]{}|;:'\",.<>?/\\`~"
}
