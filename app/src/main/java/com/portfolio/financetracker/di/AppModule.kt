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
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideTransactionDao(db: FinanceDatabase): TransactionDao = db.transactionDao

    @Provides
    fun provideMonthlyGoalDao(db: FinanceDatabase): MonthlyGoalDao = db.monthlyGoalDao

    @Provides
    fun provideCustomBankDao(db: FinanceDatabase): CustomBankDao = db.customBankDao

    @Provides
    fun provideBankAccountDao(db: FinanceDatabase): BankAccountDao = db.bankAccountDao

    @Provides
    fun provideReminderDao(db: FinanceDatabase): ReminderDao = db.reminderDao

    @Provides
    fun provideSavingsGoalDao(db: FinanceDatabase): SavingsGoalDao = db.savingsGoalDao
    
    @Provides
    fun provideSavingsGoalTransactionDao(db: FinanceDatabase): SavingsGoalTransactionDao = db.savingsGoalTransactionDao
    
    @Provides
    fun provideSavingsGoalMilestoneDao(db: FinanceDatabase): SavingsGoalMilestoneDao = db.savingsGoalMilestoneDao

    @Provides
    @Singleton
    fun provideTransactionRepository(
        transactionDao: TransactionDao,
        bankAccountDao: BankAccountDao,
        dataStoreManager: com.portfolio.financetracker.data.local.DataStoreManager
    ): TransactionRepository {
        return TransactionRepositoryImpl(transactionDao, bankAccountDao, dataStoreManager)
    }

    @Provides
    @Singleton
    fun provideGoalRepository(dao: MonthlyGoalDao): GoalRepository {
        return GoalRepositoryImpl(dao)
    }

    @Provides
    @Singleton
    fun provideReminderRepository(dao: ReminderDao): ReminderRepository {
        return ReminderRepositoryImpl(dao)
    }

    @Provides
    @Singleton
    fun provideBankAccountRepository(dao: BankAccountDao): BankAccountRepository {
        return BankAccountRepositoryImpl(dao)
    }

    @Provides
    @Singleton
    fun provideCustomBankRepository(dao: CustomBankDao): CustomBankRepository {
        return CustomBankRepositoryImpl(dao)
    }

    @Provides
    @Singleton
    fun provideSavingsGoalRepository(
        dao: SavingsGoalDao,
        transactionDao: SavingsGoalTransactionDao
    ): SavingsGoalRepository {
        return SavingsGoalRepositoryImpl(dao, transactionDao)
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
