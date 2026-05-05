package com.portfolio.financetracker.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.portfolio.financetracker.data.local.DataStoreManager
import com.portfolio.financetracker.domain.model.UserProfile
import com.portfolio.financetracker.domain.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val dataStore: DataStoreManager
) : AuthRepository {

    /**
     * Emits the current [UserProfile] by combining:
     * - Firebase Auth state listener (live session changes)
     * - DataStore cached username (survives offline)
     *
     * Emits null when signed out.
     */
    override val currentUser: Flow<UserProfile?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }.combine(dataStore.userName) { firebaseUser, cachedName ->
        firebaseUser?.let { user ->
            UserProfile(
                uid      = user.uid,
                email    = user.email ?: "",
                username = cachedName.ifBlank { user.displayName ?: user.email ?: "User" }
            )
        }
    }

    override val isLoggedIn: Boolean
        get() = firebaseAuth.currentUser != null

    override suspend fun signIn(email: String, password: String): Result<UserProfile> =
        runCatching {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user   = result.user ?: error("Sign-in succeeded but user is null.")
            val cached = dataStore.userName.first().ifBlank { user.displayName ?: email }
            val profile = UserProfile(uid = user.uid, email = user.email ?: email, username = cached)
            dataStore.saveUserProfile(profile.uid, profile.email, profile.username)
            profile
        }

    override suspend fun register(
        email: String,
        password: String,
        username: String
    ): Result<UserProfile> = runCatching {
        val result  = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val user    = result.user ?: error("Registration succeeded but user is null.")
        val profile = UserProfile(uid = user.uid, email = user.email ?: email, username = username)
        dataStore.saveUserProfile(profile.uid, profile.email, profile.username)
        profile
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
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
}
