package com.portfolio.financetracker.domain.model

/**
 * Represents the outcome of a single field validation.
 *
 * Using a sealed class (not a simple Boolean) lets the ViewModel
 * carry the exact error message up to the UI without any string
 * resources or Android dependencies in the domain layer.
 */
sealed class ValidationResult {
    /** Input passed all rules. */
    object Success : ValidationResult()

    /** Input failed a rule. [message] is a plain English description. */
    data class Error(val message: String) : ValidationResult()

    // Convenience helpers
    val isSuccess: Boolean get() = this is Success
    val errorMessage: String? get() = (this as? Error)?.message
}
