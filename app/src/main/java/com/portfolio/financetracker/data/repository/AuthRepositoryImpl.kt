package com.portfolio.financetracker.data.repository

import com.portfolio.financetracker.data.local.DataStoreManager
import com.portfolio.financetracker.domain.model.UserProfile
import com.portfolio.financetracker.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val dataStore: DataStoreManager
) : AuthRepository {

    override val currentUser: Flow<UserProfile?> = combine(
        dataStore.userUid,
        dataStore.userEmail,
        dataStore.userName
    ) { uid, email, name ->
        if (uid.isNotBlank()) {
            UserProfile(uid = uid, email = email, username = name)
        } else null
    }

    override val isLoggedIn: Boolean
        get() = runBlocking { dataStore.userUid.first().isNotBlank() }

    override suspend fun signIn(email: String, password: String): Result<UserProfile> =
        runCatching {
            // Local mock sign-in: check if email matches cached email
            val cachedEmail = dataStore.userEmail.first()
            if (cachedEmail.isNotBlank() && cachedEmail == email) {
                val profile = loadCachedProfile() ?: error("User profile data missing")
                profile
            } else {
                // Enforce registration: do not allow login if email doesn't match or is empty
                throw Exception("Account not found. Please register first.")
            }
        }

    override suspend fun register(
        email: String,
        password: String,
        username: String
    ): Result<UserProfile> = runCatching {
        val profile = UserProfile(uid = java.util.UUID.randomUUID().toString(), email = email, username = username)
        saveUserProfile(profile)
        profile
    }

    override suspend fun signOut() {
        dataStore.clearUserProfile()
    }

    override suspend fun saveUserProfile(profile: UserProfile) {
        dataStore.saveUserProfile(profile.uid, profile.email, profile.username)
    }

    override suspend fun loadCachedProfile(): UserProfile? {
        val uid   = dataStore.userUid.first()
        val email = dataStore.userEmail.first()
        val name  = dataStore.userName.first()
        return if (uid.isNotBlank()) UserProfile(uid, email, name) else null
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = Result.success(Unit)
}
