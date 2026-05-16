package com.portfolio.financetracker.di

import android.app.Application
import androidx.room.Room
import com.portfolio.financetracker.data.local.FinanceDatabase
import com.portfolio.financetracker.data.local.MIGRATION_6_7
import com.portfolio.financetracker.data.repository.TransactionRepositoryImpl
import com.portfolio.financetracker.domain.repository.TransactionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFinanceDatabase(app: Application): FinanceDatabase {
        return Room.databaseBuilder(
            app,
            FinanceDatabase::class.java,
            FinanceDatabase.DATABASE_NAME
        ).addMigrations(MIGRATION_6_7).build()
    }

    @Provides
    @Singleton
    fun provideTransactionRepository(
        db: FinanceDatabase,
        dataStoreManager: com.portfolio.financetracker.data.local.DataStoreManager
    ): TransactionRepository {
        return TransactionRepositoryImpl(db.transactionDao, db.bankAccountDao, dataStoreManager)
    }

    @Provides
    @Singleton
    fun provideGoalRepository(db: FinanceDatabase): com.portfolio.financetracker.domain.repository.GoalRepository {
        return com.portfolio.financetracker.data.repository.GoalRepositoryImpl(db.monthlyGoalDao)
    }

    @Provides
    @Singleton
    fun provideDataStoreManager(app: Application): com.portfolio.financetracker.data.local.DataStoreManager {
        return com.portfolio.financetracker.data.local.DataStoreManager(app)
    }
}
