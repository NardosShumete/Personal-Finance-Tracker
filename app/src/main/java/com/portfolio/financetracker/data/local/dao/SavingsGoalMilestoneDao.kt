package com.portfolio.financetracker.data.local.dao

import androidx.room.*
import com.portfolio.financetracker.data.local.entity.SavingsGoalMilestoneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalMilestoneDao {
    @Insert
    suspend fun insertMilestone(milestone: SavingsGoalMilestoneEntity)

    @Query("SELECT * FROM goal_milestones WHERE goalId = :goalId ORDER BY targetAmount ASC")
    fun getMilestonesByGoal(goalId: Int): Flow<List<SavingsGoalMilestoneEntity>>

    @Update
    suspend fun updateMilestone(milestone: SavingsGoalMilestoneEntity)

    @Query("UPDATE goal_milestones SET isReached = 1, reachedAt = :timestamp WHERE id = :id")
    suspend fun markMilestoneReached(id: Int, timestamp: Long)
}
