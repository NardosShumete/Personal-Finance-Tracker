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
        SavingsGoalEntity::class,
        SavingsGoalTransactionEntity::class,
        SavingsGoalMilestoneEntity::class
    ],
    version = 14,
        CategoryBudgetEntity::class
    ],
    version = 12,
    exportSchema = false
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract val transactionDao: TransactionDao
    abstract val monthlyGoalDao: MonthlyGoalDao
    abstract val customBankDao: CustomBankDao
    abstract val bankAccountDao: BankAccountDao
    abstract val reminderDao: ReminderDao
    abstract val savingsGoalDao: SavingsGoalDao
    abstract val savingsGoalTransactionDao: SavingsGoalTransactionDao
    abstract val savingsGoalMilestoneDao: SavingsGoalMilestoneDao
    abstract val categoryBudgetDao: CategoryBudgetDao

    companion object {
        const val DATABASE_NAME = "finance_db"
    }
}
