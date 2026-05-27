package com.portfolio.financetracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.portfolio.financetracker.data.local.dao.*
import com.portfolio.financetracker.data.local.entity.*

@Database(
    entities = [
        TransactionEntity::class,
        MonthlyGoalEntity::class,
        CustomBankEntity::class,
        BankAccountEntity::class,
        ReminderEntity::class,
        CategoryBudgetEntity::class,
        FailedParseEntity::class
    ],
    version = 15,
    exportSchema = true
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract val transactionDao: TransactionDao
    abstract val monthlyGoalDao: MonthlyGoalDao
    abstract val customBankDao: CustomBankDao
    abstract val bankAccountDao: BankAccountDao
    abstract val reminderDao: ReminderDao
    abstract val categoryBudgetDao: CategoryBudgetDao
    abstract val failedParseDao: FailedParseDao

    companion object {
        const val DATABASE_NAME = "finance_db"
    }
}
