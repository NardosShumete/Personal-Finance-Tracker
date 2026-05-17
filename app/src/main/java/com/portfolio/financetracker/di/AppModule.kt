package com.portfolio.financetracker.di

import android.app.Application
import androidx.room.Room
import com.portfolio.financetracker.data.local.FinanceDatabase
import com.portfolio.financetracker.data.repository.TransactionRepositoryImpl
import com.portfolio.financetracker.domain.repository.TransactionRepository
import com.portfolio.financetracker.data.repository.ReminderRepositoryImpl
import com.portfolio.financetracker.domain.repository.ReminderRepository
import com.portfolio.financetracker.data.repository.BankAccountRepositoryImpl
import com.portfolio.financetracker.domain.repository.BankAccountRepository
import com.portfolio.financetracker.data.repository.CustomBankRepositoryImpl
import com.portfolio.financetracker.domain.repository.CustomBankRepository
import com.portfolio.financetracker.data.remote.groq.GroqApi
import com.portfolio.financetracker.data.repository.AiRepositoryImpl
import com.portfolio.financetracker.domain.repository.AiRepository
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
