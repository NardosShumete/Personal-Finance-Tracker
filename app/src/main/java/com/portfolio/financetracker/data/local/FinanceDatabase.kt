package com.portfolio.financetracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.portfolio.financetracker.data.local.dao.TransactionDao
import com.portfolio.financetracker.data.local.dao.MonthlyGoalDao
import com.portfolio.financetracker.data.local.dao.ReminderDao
import com.portfolio.financetracker.data.local.entity.MonthlyGoalEntity
import com.portfolio.financetracker.data.local.entity.TransactionEntity
import com.portfolio.financetracker.data.local.entity.ReminderEntity

@Database(
    entities = [TransactionEntity::class, MonthlyGoalEntity::class, ReminderEntity::class],
    version = 8,
    exportSchema = false
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract val transactionDao: TransactionDao
    abstract val monthlyGoalDao: MonthlyGoalDao
    abstract val reminderDao: ReminderDao

    companion object {
        const val DATABASE_NAME = "finance_db"
    }
}
