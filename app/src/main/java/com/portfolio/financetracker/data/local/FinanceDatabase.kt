package com.portfolio.financetracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.portfolio.financetracker.data.local.dao.TransactionDao
import com.portfolio.financetracker.data.local.dao.MonthlyGoalDao
import com.portfolio.financetracker.data.local.entity.MonthlyGoalEntity
import com.portfolio.financetracker.data.local.entity.TransactionEntity

@Database(
    entities = [TransactionEntity::class, MonthlyGoalEntity::class],
    version = 4,
    exportSchema = false
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract val transactionDao: TransactionDao
    abstract val monthlyGoalDao: MonthlyGoalDao

    companion object {
        const val DATABASE_NAME = "finance_db"
    }
}
