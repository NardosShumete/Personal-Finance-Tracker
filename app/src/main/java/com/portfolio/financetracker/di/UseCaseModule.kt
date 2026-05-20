package com.portfolio.financetracker.di

import com.portfolio.financetracker.domain.repository.*
import com.portfolio.financetracker.domain.use_case.*
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
            addTransaction       = AddTransactionUseCase(repository)
        )
    }

    @Provides
    @Singleton
    fun provideGoalUseCases(repository: GoalRepository): GoalUseCases {
        return GoalUseCases(
            getGoal = GetGoalUseCase(repository),
            saveGoal = SaveGoalUseCase(repository)
        )
    }

    @Provides
    @Singleton
    fun provideSavingsGoalUseCases(repository: SavingsGoalRepository): SavingsGoalUseCases {
        return SavingsGoalUseCases(
            getSavingsGoals = GetSavingsGoals(repository),
            getSavingsGoalById = GetSavingsGoalById(repository),
            addSavingsGoal = AddSavingsGoal(repository),
            updateSavingsGoal = UpdateSavingsGoal(repository),
            deleteSavingsGoal = DeleteSavingsGoal(repository),
            addMoneyToGoal = AddMoneyToGoal(repository),
            withdrawMoneyFromGoal = WithdrawMoneyFromGoal(repository),
            updateGoalStatus = UpdateGoalStatus(repository),
            getTotalSavings = GetTotalSavings(repository)
        )
    }
}
