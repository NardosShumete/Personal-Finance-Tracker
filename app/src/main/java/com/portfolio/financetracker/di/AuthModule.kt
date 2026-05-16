package com.portfolio.financetracker.di

import com.portfolio.financetracker.data.local.DataStoreManager
import com.portfolio.financetracker.data.repository.AuthRepositoryImpl
import com.portfolio.financetracker.domain.repository.AuthRepository
import com.portfolio.financetracker.domain.use_case.auth.AuthUseCases
import com.portfolio.financetracker.domain.use_case.auth.GetCurrentUserUseCase
import com.portfolio.financetracker.domain.use_case.auth.RegisterUseCase
import com.portfolio.financetracker.domain.use_case.auth.SignInUseCase
import com.portfolio.financetracker.domain.use_case.auth.SignOutUseCase
import com.portfolio.financetracker.domain.use_case.auth.SendPasswordResetUseCase
import com.portfolio.financetracker.domain.use_case.auth.ValidateAuthInputUseCase
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
    fun provideAuthRepository(
        dataStore: DataStoreManager
    ): AuthRepository = AuthRepositoryImpl(dataStore)

    @Provides
    @Singleton
    fun provideAuthUseCases(repository: AuthRepository): AuthUseCases = AuthUseCases(
        signIn             = SignInUseCase(repository),
        register           = RegisterUseCase(repository),
        signOut            = SignOutUseCase(repository),
        getCurrentUser     = GetCurrentUserUseCase(repository),
        sendPasswordReset  = SendPasswordResetUseCase(repository),
        validateAuthInput  = ValidateAuthInputUseCase()
    )
}
