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

    // Helper to build a minimal valid TransactionEntity
    private fun makeTransaction(
        id: Int,
        amount: Double,
        category: String,
        type: String,
        note: String = "",
        date: Long = 0L
    ) = TransactionEntity(
        id               = id,
        amount           = amount,
        category         = category,
        date             = date,
        type             = type,
        note             = note,
        receiptPath      = null,
        recurringPeriod  = "NONE",   // non-null default
        source           = "MANUAL", // non-null default
        rawSms           = null,
        smsBalance       = null,
        smsHash          = null,
        smsId            = null,
        isPending        = false,
        bankName         = null
    )

    @Test
    fun insertAndGetTransactionById() = runBlocking {
        val transaction = makeTransaction(id = 1, amount = 100.0, category = "Food", type = "EXPENSE", note = "Lunch")

        transactionDao.insertTransaction(transaction)

        val retrieved = transactionDao.getTransactionById(1)
        assertNotNull(retrieved)
        assertEquals(100.0, retrieved?.amount)
        assertEquals("EXPENSE", retrieved?.type)
    }

    @Test
    fun deleteTransaction() = runBlocking {
        val transaction = makeTransaction(id = 1, amount = 100.0, category = "Food", type = "EXPENSE")

        transactionDao.insertTransaction(transaction)
        transactionDao.deleteTransaction(transaction)

        val retrieved = transactionDao.getTransactionById(1)
        assertNull(retrieved)
    }

    @Test
    fun getTransactionsByType() = runBlocking {
        val t1 = makeTransaction(id = 1, amount = 10.0,  category = "Food",      type = "EXPENSE")
        val t2 = makeTransaction(id = 2, amount = 20.0,  category = "Salary",    type = "INCOME")
        val t3 = makeTransaction(id = 3, amount = 30.0,  category = "Transport", type = "EXPENSE")

        transactionDao.insertTransaction(t1)
        transactionDao.insertTransaction(t2)
        transactionDao.insertTransaction(t3)

        val expenses = transactionDao.getTransactionsByType("EXPENSE").first()
        assertEquals(2, expenses.size)

        val incomes = transactionDao.getTransactionsByType("INCOME").first()
        assertEquals(1, incomes.size)
        assertEquals(20.0, incomes[0].amount, 0.0)
    }
}
