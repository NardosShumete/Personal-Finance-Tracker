package com.portfolio.financetracker.domain.use_case.auth

import com.portfolio.financetracker.domain.repository.AuthRepository
import javax.inject.Inject

class ResendVerificationEmailUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return repository.resendVerificationEmail()
    }
}
