package com.portfolio.financetracker.domain.use_case.auth

import com.portfolio.financetracker.domain.model.UserProfile
import com.portfolio.financetracker.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        username: String
    ): Result<UserProfile> {
        if (email.isBlank() || password.isBlank() || username.isBlank())
            return Result.failure(IllegalArgumentException("All fields are required."))
        if (password.length < 6)
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        return repository.register(email.trim(), password, username.trim())
    }
}
