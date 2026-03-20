package com.portfolio.financetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.portfolio.financetracker.data.local.entity.MonthlyGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlyGoalDao {
    @Query("SELECT * FROM monthly_goal_table WHERE monthYear = :monthYear LIMIT 1")
    fun getGoalByMonth(monthYear: String): Flow<MonthlyGoalEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: MonthlyGoalEntity)

    @Query("SELECT * FROM monthly_goal_table ORDER BY monthYear DESC")
    fun getAllGoals(): Flow<List<MonthlyGoalEntity>>
}
