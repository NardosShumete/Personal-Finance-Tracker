package com.portfolio.financetracker.di

import com.google.firebase.auth.FirebaseAuth
import com.portfolio.financetracker.data.local.DataStoreManager
import com.portfolio.financetracker.data.repository.AuthRepositoryImpl
import com.portfolio.financetracker.domain.repository.AuthRepository
import com.portfolio.financetracker.domain.use_case.auth.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuth,
        dataStore: DataStoreManager
    ): AuthRepository = AuthRepositoryImpl(firebaseAuth, dataStore)

    @Provides
    @Singleton
    fun provideAuthUseCases(repository: AuthRepository): AuthUseCases = AuthUseCases(
        signIn             = SignInUseCase(repository),
        register           = RegisterUseCase(repository),
        signOut            = SignOutUseCase(repository),
        getCurrentUser     = GetCurrentUserUseCase(repository),
        sendPasswordReset  = SendPasswordResetUseCase(repository),
        validateAuthInput  = ValidateAuthInputUseCase(),
        resendVerificationEmail = ResendVerificationEmailUseCase(repository),
        reloadAndCheckVerification = ReloadAndCheckVerificationUseCase(repository)
    )
}
