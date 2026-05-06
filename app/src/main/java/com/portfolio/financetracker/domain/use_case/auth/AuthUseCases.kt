package com.portfolio.financetracker.domain.use_case.auth

data class AuthUseCases(
    val signIn: SignInUseCase,
    val register: RegisterUseCase,
    val signOut: SignOutUseCase,
    val getCurrentUser: GetCurrentUserUseCase,
    val sendPasswordReset: SendPasswordResetUseCase,
    val validateAuthInput: ValidateAuthInputUseCase
)
