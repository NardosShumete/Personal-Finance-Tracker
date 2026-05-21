package com.portfolio.financetracker.di

import android.app.Application
import androidx.room.Room
import com.portfolio.financetracker.data.local.FinanceDatabase
import com.portfolio.financetracker.data.local.dao.*
import com.portfolio.financetracker.data.repository.*
import com.portfolio.financetracker.domain.repository.*
import com.portfolio.financetracker.data.remote.groq.GroqApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
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
        .addMigrations(
            com.portfolio.financetracker.data.local.MIGRATION_5_6,
            com.portfolio.financetracker.data.local.MIGRATION_6_7,
            com.portfolio.financetracker.data.local.MIGRATION_7_8,
            com.portfolio.financetracker.data.local.MIGRATION_8_9,
            com.portfolio.financetracker.data.local.MIGRATION_9_10,
            com.portfolio.financetracker.data.local.MIGRATION_10_11,
            com.portfolio.financetracker.data.local.MIGRATION_11_12
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideBankAccountDao(db: FinanceDatabase): com.portfolio.financetracker.data.local.dao.BankAccountDao {
        return db.bankAccountDao
    }

    @Provides
    fun provideTransactionDao(db: FinanceDatabase): TransactionDao = db.transactionDao

    @Provides
    fun provideBankAccountDao(db: FinanceDatabase): BankAccountDao = db.bankAccountDao

    @Provides
    fun provideMonthlyGoalDao(db: FinanceDatabase): MonthlyGoalDao = db.monthlyGoalDao

    @Provides
    fun provideReminderDao(db: FinanceDatabase): ReminderDao = db.reminderDao

    @Provides
    fun provideCustomBankDao(db: FinanceDatabase): CustomBankDao = db.customBankDao
    
    @Provides
    fun provideCategoryBudgetDao(db: FinanceDatabase): CategoryBudgetDao = db.categoryBudgetDao

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
    fun provideGoalRepository(db: FinanceDatabase): GoalRepository {
        return GoalRepositoryImpl(db.monthlyGoalDao, db.categoryBudgetDao, db.transactionDao)
    }

    @Provides
    @Singleton
    fun provideReminderRepository(db: FinanceDatabase): ReminderRepository {
        return ReminderRepositoryImpl(db.reminderDao)
    }

    @Provides
    @Singleton
    fun provideBankAccountRepository(db: FinanceDatabase): BankAccountRepository {
        return BankAccountRepositoryImpl(db.bankAccountDao)
    }

    @Provides
    @Singleton
    fun provideCustomBankRepository(db: FinanceDatabase): CustomBankRepository {
        return CustomBankRepositoryImpl(db.customBankDao)
    }

    @Provides
    @Singleton
    fun provideDataStoreManager(app: Application): com.portfolio.financetracker.data.local.DataStoreManager {
        return com.portfolio.financetracker.data.local.DataStoreManager(app)
    }

    @Provides
    @Singleton
    fun provideGroqApi(): GroqApi {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val json = Json { 
            ignoreUnknownKeys = true
            encodeDefaults = true 
        }
        val contentType = "application/json".toMediaType()

        return Retrofit.Builder()
            .baseUrl("https://api.groq.com/openai/")
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(GroqApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAiRepository(api: GroqApi): AiRepository {
        return AiRepositoryImpl(api)
    }
}
