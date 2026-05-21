package com.portfolio.financetracker.data.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
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

    override val currentUser: Flow<UserProfile?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }.combine(dataStore.userName) { firebaseUser, cachedName ->
        firebaseUser?.let { user ->
            if (user.isEmailVerified) {
                UserProfile(
                    uid      = user.uid,
                    email    = user.email ?: "",
                    username = cachedName.ifBlank { user.displayName ?: user.email ?: "User" }
                )
            } else null
        }
    }

    override val isLoggedIn: Boolean
        get() = firebaseAuth.currentUser != null &&
                firebaseAuth.currentUser?.isEmailVerified == true

    override suspend fun signIn(email: String, password: String): Result<UserProfile> =
        runCatching {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user   = result.user ?: error("Sign-in succeeded but user is null.")
            if (!user.isEmailVerified) {
                firebaseAuth.signOut()
                throw EmailNotVerifiedException("Please verify your email before logging in.")
            }
            val cached  = dataStore.userName.first().ifBlank { user.displayName ?: email }
            val profile = UserProfile(uid = user.uid, email = user.email ?: email, username = cached)
            dataStore.saveUserProfile(profile.uid, profile.email, profile.username)
            profile
        }.mapFirebaseErrors()

    override suspend fun register(
        email: String,
        password: String,
        username: String
    ): Result<UserProfile> = runCatching {
        // createUserWithEmailAndPassword already throws FirebaseAuthUserCollisionException
        // if the email is taken — no need for a separate fetchSignInMethodsForEmail call
        // (that API is deprecated and unreliable in newer Firebase SDKs).
        val result  = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val user    = result.user ?: error("Registration succeeded but user is null.")
        user.sendEmailVerification().await()
        val profile = UserProfile(uid = user.uid, email = user.email ?: email, username = username)
        dataStore.saveUserProfile(profile.uid, profile.email, profile.username)
        firebaseAuth.signOut()
        profile
    }.mapFirebaseErrors()

    override suspend fun signOut() {
        firebaseAuth.signOut()
        dataStore.clearUserProfile()
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        // fetchSignInMethodsForEmail is deprecated and unreliable in newer Firebase SDKs.
        // Instead, just send the reset email directly. Firebase will silently succeed
        // even for non-existent emails (by design, to prevent email enumeration attacks).
        // We surface a user-friendly message on success regardless.
        firebaseAuth.sendPasswordResetEmail(email).await()
        Unit
    }.mapFirebaseErrors()

    override suspend fun resendVerificationEmail(): Result<Unit> = runCatching {
        val user = firebaseAuth.currentUser ?: throw Exception("No user is currently signed in.")
        user.sendEmailVerification().await()
        Unit  // Task<Void>.await() returns Void? — explicitly return Unit
    }.mapFirebaseErrors()

    override suspend fun reloadAndCheckVerification(): Boolean {
        return try {
            firebaseAuth.currentUser?.reload()?.await()
            firebaseAuth.currentUser?.isEmailVerified == true
        } catch (e: Exception) { false }
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

    private fun <T> Result<T>.mapFirebaseErrors(): Result<T> {
        val ex = exceptionOrNull() ?: return this
        val friendly = when (ex) {
            is EmailNotVerifiedException               -> ex.message ?: "Email not verified."
            is FirebaseAuthInvalidCredentialsException -> "Invalid email or password. Please try again."
            is FirebaseAuthInvalidUserException        -> when (ex.errorCode) {
                "ERROR_USER_NOT_FOUND" -> "No account found with this email. Please register."
                "ERROR_USER_DISABLED"  -> "This account has been disabled. Contact support."
                else                   -> "Account error. Please try again."
            }
            is FirebaseAuthUserCollisionException      -> "An account with this email already exists. Please sign in."
            is FirebaseAuthWeakPasswordException       -> "Password is too weak. Use at least 8 characters with uppercase, number, and symbol."
            is FirebaseNetworkException                -> "No internet connection. Please check your network."
            else -> when {
                ex.message?.contains("too-many-requests", ignoreCase = true) == true ->
                    "Too many attempts. Please wait a few minutes and try again."
                ex.message?.contains("No account found", ignoreCase = true) == true -> ex.message!!
                else -> ex.message ?: "Something went wrong. Please try again."
            }
        }
        return Result.failure(Exception(friendly))
    }
}

class EmailNotVerifiedException(message: String) : Exception(message)
