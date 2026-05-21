package com.portfolio.financetracker.domain.model

sealed class AuthResult {
    object Idle : AuthResult()
    object Loading : AuthResult()
    object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
    object EmailNotVerified : AuthResult()
    object UserNotFound : AuthResult()
}
