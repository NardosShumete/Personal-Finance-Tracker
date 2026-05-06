package com.portfolio.financetracker.domain.use_case.auth

import android.util.Patterns
import com.portfolio.financetracker.domain.repository.AuthRepository
import javax.inject.Inject

class SendPasswordResetUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        // Validate before hitting the network
        if (email.isBlank())
            return Result.failure(IllegalArgumentException("Please enter your email address."))
        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches())
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))

        return repository.sendPasswordReset(email.trim())
    }
}
