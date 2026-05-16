package com.portfolio.financetracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.portfolio.financetracker.data.local.dao.BankAccountDao
import com.portfolio.financetracker.data.local.dao.CustomBankDao
import com.portfolio.financetracker.data.local.dao.MonthlyGoalDao
import com.portfolio.financetracker.data.local.dao.ReminderDao
import com.portfolio.financetracker.data.local.dao.TransactionDao
import com.portfolio.financetracker.data.local.entity.BankAccountEntity
import com.portfolio.financetracker.data.local.entity.CustomBankEntity
import com.portfolio.financetracker.data.local.entity.MonthlyGoalEntity
import com.portfolio.financetracker.data.local.entity.ReminderEntity
import com.portfolio.financetracker.data.local.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        MonthlyGoalEntity::class,
        CustomBankEntity::class,
        BankAccountEntity::class,
        ReminderEntity::class
    ],
    version = 11,
    exportSchema = false
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract val transactionDao: TransactionDao
    abstract val monthlyGoalDao: MonthlyGoalDao
    abstract val customBankDao: CustomBankDao
    abstract val bankAccountDao: BankAccountDao
    abstract val reminderDao: ReminderDao

    companion object {
        const val DATABASE_NAME = "finance_db"
    }
}
