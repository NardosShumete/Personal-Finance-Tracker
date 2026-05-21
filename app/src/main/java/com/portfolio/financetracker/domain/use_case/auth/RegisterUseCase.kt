package com.portfolio.financetracker.domain.use_case.auth

import com.portfolio.financetracker.domain.model.UserProfile
import com.portfolio.financetracker.domain.repository.AuthRepository
import com.portfolio.financetracker.domain.util.InputValidator
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        username: String
    ): Result<UserProfile> {
        val emailValidation = InputValidator.validateEmail(email)
        if (!emailValidation.isSuccess) {
            return Result.failure(Exception(emailValidation.errorMessage))
        }

        val passwordValidation = InputValidator.validatePasswordStrong(password)
        if (!passwordValidation.isSuccess) {
            return Result.failure(Exception(passwordValidation.errorMessage))
        }

        val usernameValidation = InputValidator.validateUsername(username)
        if (!usernameValidation.isSuccess) {
            return Result.failure(Exception(usernameValidation.errorMessage))
        }

        return repository.register(
            InputValidator.sanitizeEmail(email),
            InputValidator.sanitizePassword(password),
            InputValidator.sanitizeUsername(username)
        )
    }
}
