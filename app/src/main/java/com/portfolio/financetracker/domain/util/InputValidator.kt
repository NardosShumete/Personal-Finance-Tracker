package com.portfolio.financetracker.domain.util

import android.util.Patterns
import com.portfolio.financetracker.domain.model.ValidationResult

/**
 * Pure domain-layer validator — no Android UI imports, fully unit-testable.
 *
 * Sanitization rules applied before every check:
 *   - trim()        → removes leading/trailing whitespace
 *   - lowercase()   → normalises email casing (email is case-insensitive)
 *
 * Password rules (register mode):
 *   ✔ Minimum 8 characters
 *   ✔ At least 1 uppercase letter  [A-Z]
 *   ✔ At least 1 lowercase letter  [a-z]
 *   ✔ At least 1 digit             [0-9]
 *   ✔ At least 1 special character [!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]
 *
 * Login mode uses a relaxed password check (just non-empty) because the
 * user may have an old account with a weaker password — we don't want to
 * block them from signing in.
 */
object InputValidator {

    // ── Sanitization ──────────────────────────────────────────────────────────

    /** Trims whitespace and lowercases — call before storing or sending. */
    fun sanitizeEmail(raw: String): String = raw.trim().lowercase()

    /** Only trims — passwords are case-sensitive, never lowercased. */
    fun sanitizePassword(raw: String): String = raw.trim()

    fun sanitizeUsername(raw: String): String = raw.trim()

    // ── Email validation ──────────────────────────────────────────────────────

    fun validateEmail(raw: String): ValidationResult {
        val email = sanitizeEmail(raw)
        return when {
            email.isEmpty() ->
                ValidationResult.Error("Email address is required.")
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                ValidationResult.Error("Please enter a valid email address.")
            else -> ValidationResult.Success
        }
    }

    // ── Password validation ───────────────────────────────────────────────────

    /**
     * Full strength check — used during REGISTRATION.
     * Returns the first failing rule so the user fixes one thing at a time.
     */
    fun validatePasswordStrong(raw: String): ValidationResult {
        val password = sanitizePassword(raw)
        return when {
            password.isEmpty() ->
                ValidationResult.Error("Password is required.")
            password.length < 8 ->
                ValidationResult.Error("Password must be at least 8 characters.")
            !password.any { it.isUpperCase() } ->
                ValidationResult.Error("Password must contain at least one uppercase letter.")
            !password.any { it.isLowerCase() } ->
                ValidationResult.Error("Password must contain at least one lowercase letter.")
            !password.any { it.isDigit() } ->
                ValidationResult.Error("Password must contain at least one number.")
            !password.any { it in SPECIAL_CHARS } ->
                ValidationResult.Error("Password must contain at least one special character (!@#\$%^&*).")
            else -> ValidationResult.Success
        }
    }

    /**
     * Relaxed check — used during LOGIN.
     * We only verify the field is non-empty; Firebase handles the rest.
     */
    fun validatePasswordLogin(raw: String): ValidationResult {
        val password = sanitizePassword(raw)
        return if (password.isEmpty())
            ValidationResult.Error("Password is required.")
        else
            ValidationResult.Success
    }

    // ── Username validation ───────────────────────────────────────────────────

    fun validateUsername(raw: String): ValidationResult {
        val username = sanitizeUsername(raw)
        return when {
            username.isEmpty() ->
                ValidationResult.Error("Username is required.")
            username.length < 2 ->
                ValidationResult.Error("Username must be at least 2 characters.")
            username.length > 30 ->
                ValidationResult.Error("Username must be 30 characters or fewer.")
            else -> ValidationResult.Success
        }
    }

    // ── Constants ─────────────────────────────────────────────────────────────

    private const val SPECIAL_CHARS = "!@#\$%^&*()-_=+[]{}|;:'\",.<>?/\\`~"
}
