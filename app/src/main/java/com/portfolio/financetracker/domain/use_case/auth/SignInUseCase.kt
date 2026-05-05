package com.portfolio.financetracker.domain.use_case.auth

import com.portfolio.financetracker.domain.model.UserProfile
import com.portfolio.financetracker.domain.repository.AuthRepository
import javax.inject.Inject

class SignInUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<UserProfile> {
        if (email.isBlank() || password.isBlank())
            return Result.failure(IllegalArgumentException("Email and password must not be empty."))
        return repository.signIn(email.trim(), password)
    }
}
