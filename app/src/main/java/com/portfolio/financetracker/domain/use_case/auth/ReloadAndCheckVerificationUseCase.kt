package com.portfolio.financetracker.domain.use_case.auth

import com.portfolio.financetracker.domain.repository.AuthRepository
import javax.inject.Inject

class ReloadAndCheckVerificationUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): Boolean {
        return repository.reloadAndCheckVerification()
    }
}
