package com.portfolio.financetracker.di

import android.app.Application
import androidx.room.Room
import com.portfolio.financetracker.data.local.DataStoreManager
import com.portfolio.financetracker.data.local.FinanceDatabase
import com.portfolio.financetracker.data.local.MIGRATION_5_6
import com.portfolio.financetracker.data.local.MIGRATION_6_7
import com.portfolio.financetracker.data.local.MIGRATION_7_8
import com.portfolio.financetracker.data.repository.GoalRepositoryImpl
import com.portfolio.financetracker.data.repository.ReminderRepositoryImpl
import com.portfolio.financetracker.data.repository.TransactionRepositoryImpl
import com.portfolio.financetracker.domain.repository.GoalRepository
import com.portfolio.financetracker.domain.repository.ReminderRepository
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
        )
            .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
            // Safety net: if device has a schema version we don't have a migration for
            // (e.g. version 1-4 from early development), wipe and recreate cleanly.
            // Remove this before production release and write proper migrations instead.
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideTransactionRepository(
        db: FinanceDatabase,
        dataStoreManager: DataStoreManager
    ): TransactionRepository = TransactionRepositoryImpl(
        db.transactionDao,
        db.bankAccountDao,
        dataStoreManager
    )

    @Provides
    @Singleton
    fun provideGoalRepository(db: FinanceDatabase): GoalRepository =
        GoalRepositoryImpl(db.monthlyGoalDao)

    @Provides
    @Singleton
    fun provideReminderRepository(db: FinanceDatabase): ReminderRepository =
        ReminderRepositoryImpl(db.reminderDao)

    @Provides
    @Singleton
    fun provideDataStoreManager(app: Application): DataStoreManager =
        DataStoreManager(app)

    @Provides
    @Singleton
    fun provideCustomBankDao(db: FinanceDatabase) = db.customBankDao

    @Provides
    @Singleton
    fun provideBankAccountDao(db: FinanceDatabase) = db.bankAccountDao

    @Provides
    @Singleton
    fun provideReminderDao(db: FinanceDatabase) = db.reminderDao
}
