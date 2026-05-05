package com.portfolio.financetracker.domain.use_case.auth

import com.portfolio.financetracker.domain.repository.AuthRepository
import javax.inject.Inject

class SignOutUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke() = repository.signOut()
}
