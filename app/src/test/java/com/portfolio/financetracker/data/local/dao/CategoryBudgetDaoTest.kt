package com.portfolio.financetracker.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.portfolio.financetracker.data.local.FinanceDatabase
import com.portfolio.financetracker.data.local.entity.CategoryBudgetEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CategoryBudgetDaoTest {

    private lateinit var database: FinanceDatabase
    private lateinit var categoryBudgetDao: CategoryBudgetDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FinanceDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        categoryBudgetDao = database.categoryBudgetDao
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetCategoryBudgets() = runBlocking {
        val budget1 = CategoryBudgetEntity(
            monthYear = "06-2026",
            category = "Food",
            limitAmount = 1500.0
        )
        val budget2 = CategoryBudgetEntity(
            monthYear = "06-2026",
            category = "Transport",
            limitAmount = 800.0
        )

        categoryBudgetDao.insertCategoryBudget(budget1)
        categoryBudgetDao.insertCategoryBudget(budget2)

        val retrieved = categoryBudgetDao.getCategoryBudgets("06-2026").first()
        assertEquals(2, retrieved.size)
        assertTrue(retrieved.any { it.category == "Food" && it.limitAmount == 1500.0 })
    }

    @Test
    fun clearBudgetsForMonth() = runBlocking {
        val budget = CategoryBudgetEntity(monthYear = "06-2026", category = "Food", limitAmount = 1500.0)
        categoryBudgetDao.insertCategoryBudget(budget)

        categoryBudgetDao.clearBudgetsForMonth("06-2026")

        val retrieved = categoryBudgetDao.getCategoryBudgets("06-2026").first()
        assertTrue(retrieved.isEmpty())
    }
}
