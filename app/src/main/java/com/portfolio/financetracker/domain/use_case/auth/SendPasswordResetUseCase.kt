package com.portfolio.financetracker.domain.use_case.auth

import com.portfolio.financetracker.domain.repository.AuthRepository
import com.portfolio.financetracker.domain.util.InputValidator
import javax.inject.Inject

class SendPasswordResetUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        // Use domain-layer InputValidator — no Android framework dependency
        val validation = InputValidator.validateEmail(email)
        if (!validation.isSuccess) {
            return Result.failure(IllegalArgumentException(validation.errorMessage))
        }
        return repository.sendPasswordReset(InputValidator.sanitizeEmail(email))
    }
}
