package com.portfolio.financetracker.data.local.dao

import androidx.room.*
import com.portfolio.financetracker.data.local.entity.SavingsGoalEntity
import com.portfolio.financetracker.domain.model.SavingsGoalStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalDao {
    @Query("SELECT * FROM savings_goals ORDER BY isPinned DESC, deadlineDate ASC")
    fun getAllGoals(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    suspend fun getGoalById(id: Int): SavingsGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: SavingsGoalEntity)

    @Update
    suspend fun updateGoal(goal: SavingsGoalEntity)

    @Delete
    suspend fun deleteGoal(goal: SavingsGoalEntity)

    @Query("UPDATE savings_goals SET currentAmount = :newAmount WHERE id = :id")
    suspend fun updateCurrentAmount(id: Int, newAmount: Double)

    @Query("UPDATE savings_goals SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Int, status: SavingsGoalStatus)

    @Query("SELECT SUM(currentAmount) FROM savings_goals")
    fun getTotalSavings(): Flow<Double?>
}
