package com.portfolio.financetracker.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.portfolio.financetracker.data.local.FinanceDatabase
import com.portfolio.financetracker.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TransactionDaoTest {

    private lateinit var database: FinanceDatabase
    private lateinit var transactionDao: TransactionDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FinanceDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        transactionDao = database.transactionDao
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetTransactionById() = runBlocking {
        val transaction = TransactionEntity(
            id = 1,
            amount = 100.0,
            category = "Food",
            date = System.currentTimeMillis(),
            type = "Expense",
            note = "Lunch",
            bankName = null,
            smsHash = null,
            smsId = null,
            source = "Manual",
            isPending = false,
            receiptPath = null,
            recurringPeriod = null
        )

        transactionDao.insertTransaction(transaction)

        val retrieved = transactionDao.getTransactionById(1)
        assertNotNull(retrieved)
        assertEquals(100.0, retrieved?.amount)
        assertEquals("Expense", retrieved?.type)
    }

    @Test
    fun deleteTransaction() = runBlocking {
        val transaction = TransactionEntity(
            id = 1,
            amount = 100.0,
            category = "Food",
            date = System.currentTimeMillis(),
            type = "Expense",
            note = "",
            bankName = null,
            smsHash = null,
            smsId = null,
            source = "Manual",
            isPending = false,
            receiptPath = null,
            recurringPeriod = null
        )

        transactionDao.insertTransaction(transaction)
        transactionDao.deleteTransaction(transaction)

        val retrieved = transactionDao.getTransactionById(1)
        assertNull(retrieved)
    }

    @Test
    fun getTransactionsByType() = runBlocking {
        val t1 = TransactionEntity(id = 1, amount = 10.0, category = "Food", date = 0L, type = "Expense", note = "", bankName = null, smsHash = null, smsId = null, source = "Manual", isPending = false, receiptPath = null, recurringPeriod = null)
        val t2 = TransactionEntity(id = 2, amount = 20.0, category = "Salary", date = 0L, type = "Income", note = "", bankName = null, smsHash = null, smsId = null, source = "Manual", isPending = false, receiptPath = null, recurringPeriod = null)
        val t3 = TransactionEntity(id = 3, amount = 30.0, category = "Transport", date = 0L, type = "Expense", note = "", bankName = null, smsHash = null, smsId = null, source = "Manual", isPending = false, receiptPath = null, recurringPeriod = null)

        transactionDao.insertTransaction(t1)
        transactionDao.insertTransaction(t2)
        transactionDao.insertTransaction(t3)

        val expenses = transactionDao.getTransactionsByType("Expense").first()
        assertEquals(2, expenses.size)
        
        val incomes = transactionDao.getTransactionsByType("Income").first()
        assertEquals(1, incomes.size)
        assertEquals(20.0, incomes[0].amount)
    }
}
