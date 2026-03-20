package com.portfolio.financetracker.di

import com.portfolio.financetracker.domain.repository.TransactionRepository
import com.portfolio.financetracker.domain.use_case.AddTransactionUseCase
import com.portfolio.financetracker.domain.use_case.DeleteTransactionUseCase
import com.portfolio.financetracker.domain.use_case.GetTransactionsUseCase
import com.portfolio.financetracker.domain.use_case.TransactionUseCases
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    @Provides
    @Singleton
    fun provideTransactionUseCases(repository: TransactionRepository): TransactionUseCases {
        return TransactionUseCases(
            getTransactions = GetTransactionsUseCase(repository),
            deleteTransaction = DeleteTransactionUseCase(repository),
            addTransaction = AddTransactionUseCase(repository)
        )
    }
}
