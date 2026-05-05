package com.portfolio.financetracker.domain.model

/**
 * Pure domain model — no Firebase or Android imports.
 * Represents the signed-in user's profile.
 */
data class UserProfile(
    val uid: String,
    val email: String,
    val username: String
)
