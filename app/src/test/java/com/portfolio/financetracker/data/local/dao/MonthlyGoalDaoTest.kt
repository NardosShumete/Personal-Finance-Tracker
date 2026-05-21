package com.portfolio.financetracker.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.portfolio.financetracker.data.local.FinanceDatabase
import com.portfolio.financetracker.data.local.entity.MonthlyGoalEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MonthlyGoalDaoTest {

    private lateinit var database: FinanceDatabase
    private lateinit var monthlyGoalDao: MonthlyGoalDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FinanceDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        monthlyGoalDao = database.monthlyGoalDao
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetGoalByMonth() = runBlocking {
        val goal = MonthlyGoalEntity(
            monthYear = "05-2026",
            incomeGoal = 5000.0,
            expenseLimit = 3000.0
        )

        monthlyGoalDao.insertGoal(goal)

        val retrieved = monthlyGoalDao.getGoalByMonth("05-2026").first()
        assertNotNull(retrieved)
        assertEquals(5000.0, retrieved?.incomeGoal)
        assertEquals(3000.0, retrieved?.expenseLimit)
    }

    @Test
    fun replaceGoalUpdatesExistingRecord() = runBlocking {
        val goal1 = MonthlyGoalEntity(monthYear = "05-2026", incomeGoal = 5000.0, expenseLimit = 3000.0)
        val goal2 = MonthlyGoalEntity(monthYear = "05-2026", incomeGoal = 6000.0, expenseLimit = 3500.0)

        monthlyGoalDao.insertGoal(goal1)
        monthlyGoalDao.insertGoal(goal2)

        val allGoals = monthlyGoalDao.getAllGoals().first()
        assertEquals(1, allGoals.size)
        assertEquals(6000.0, allGoals[0].incomeGoal, 0.0)  // delta required for Double comparison
    }
}
