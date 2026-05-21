package com.portfolio.financetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.portfolio.financetracker.data.local.entity.SavingsGoalTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalTransactionDao {
    @Insert
    suspend fun insertTransaction(transaction: SavingsGoalTransactionEntity)

    @Query("SELECT * FROM goal_transactions WHERE goalId = :goalId ORDER BY timestamp DESC")
    fun getTransactionsByGoal(goalId: Int): Flow<List<SavingsGoalTransactionEntity>>

    @Query("SELECT * FROM goal_transactions ORDER BY timestamp DESC")
    fun getAllGoalTransactions(): Flow<List<SavingsGoalTransactionEntity>>
}
