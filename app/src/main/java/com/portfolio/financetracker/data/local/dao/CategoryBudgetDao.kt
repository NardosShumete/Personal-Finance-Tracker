package com.portfolio.financetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.portfolio.financetracker.data.local.entity.CategoryBudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryBudgetDao {
    @Query("SELECT * FROM category_budget_table WHERE monthYear = :monthYear")
    fun getCategoryBudgets(monthYear: String): Flow<List<CategoryBudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoryBudget(budget: CategoryBudgetEntity)

    @Query("DELETE FROM category_budget_table WHERE monthYear = :monthYear")
    suspend fun clearBudgetsForMonth(monthYear: String)
}
