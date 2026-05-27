package com.portfolio.financetracker.di

import com.portfolio.financetracker.domain.repository.TransactionRepository
import com.portfolio.financetracker.domain.use_case.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

import com.portfolio.financetracker.domain.repository.GoalRepository
import com.portfolio.financetracker.domain.repository.AiRepository

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    @Provides
    @Singleton
    fun provideGetAiInsightsUseCase(repository: AiRepository): GetAiInsightsUseCase {
        return GetAiInsightsUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideGetAiChatResponseUseCase(repository: AiRepository): GetAiChatResponseUseCase {
        return GetAiChatResponseUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideTransactionUseCases(repository: TransactionRepository): TransactionUseCases {
        return TransactionUseCases(
            getTransactions      = GetTransactionsUseCase(repository),
            getPagedTransactions = GetPagedTransactionsUseCase(repository),
            getTransaction       = GetTransactionUseCase(repository),
            deleteTransaction    = DeleteTransactionUseCase(repository),
            deleteAllTransactions = DeleteAllTransactionsUseCase(repository),
            addTransaction       = AddTransactionUseCase(repository),
            getPendingCount      = GetPendingCountUseCase(repository)
        )
    }

    @Provides
    @Singleton
    fun provideGoalUseCases(repository: GoalRepository): GoalUseCases {
        return GoalUseCases(
            getGoal = GetGoalUseCase(repository),
            saveGoal = SaveGoalUseCase(repository),
            getCategoryBudgets = GetCategoryBudgetsUseCase(repository),
            saveCategoryBudget = SaveCategoryBudgetUseCase(repository),
            clearBudgetsForMonth = ClearBudgetsForMonthUseCase(repository)
        )
    }
}
