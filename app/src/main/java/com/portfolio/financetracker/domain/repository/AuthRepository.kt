package com.portfolio.financetracker.domain.repository

import com.portfolio.financetracker.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * Auth contract — domain layer knows nothing about Firebase.
 * All results are wrapped in [Result] so the UI can handle
 * success/failure without try-catch in Composables.
 */
interface AuthRepository {

    /** Currently cached user profile, or null if not signed in. */
    val currentUser: Flow<UserProfile?>

    /** True if a Firebase session already exists (persisted across restarts). */
    val isLoggedIn: Boolean

    suspend fun signIn(email: String, password: String): Result<UserProfile>

    suspend fun register(email: String, password: String, username: String): Result<UserProfile>

    suspend fun signOut()

    /** Saves username locally so it survives offline use. */
    suspend fun saveUserProfile(profile: UserProfile)

    /** Loads the locally cached profile (DataStore). */
    suspend fun loadCachedProfile(): UserProfile?

    /**
     * Sends a Firebase password-reset email to [email].
     * Returns [Result.success] if the email was dispatched,
     * [Result.failure] with a user-friendly message otherwise.
     */
    suspend fun sendPasswordReset(email: String): Result<Unit>
}
