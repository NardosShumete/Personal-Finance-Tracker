package com.portfolio.financetracker.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.portfolio.financetracker.data.local.FinanceDatabase
import com.portfolio.financetracker.data.local.entity.BankAccountEntity
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
class BankAccountDaoTest {

    private lateinit var database: FinanceDatabase
    private lateinit var bankAccountDao: BankAccountDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FinanceDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        bankAccountDao = database.bankAccountDao
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetAllBankAccounts() = runBlocking {
        val account = BankAccountEntity(
            id = 1,
            name = "Commercial Bank of Ethiopia",
            shortName = "CBE",
            smsSenderId = "CBE",
            logoResId = 0,
            accountNumber = "1000",
            totalIncome = 500.0,
            totalExpense = 100.0,
            transactionCount = 2
        )

        bankAccountDao.insertBankAccount(account)

        val accounts = bankAccountDao.getAllBankAccounts().first()
        assertEquals(1, accounts.size)
        assertEquals("CBE", accounts[0].shortName)
    }

    @Test
    fun getBankAccountByShortName_and_SenderId() = runBlocking {
        val account = BankAccountEntity(id = 2, name = "Awash Bank", shortName = "Awash", smsSenderId = "AWASH_SMS", logoResId = 0, accountNumber = "", totalIncome = 0.0, totalExpense = 0.0, transactionCount = 0)
        bankAccountDao.insertBankAccount(account)

        val byShortName = bankAccountDao.getBankAccountByShortName("Awash")
        assertNotNull(byShortName)
        assertEquals("Awash Bank", byShortName?.name)

        val bySenderId = bankAccountDao.getBankAccountBySenderId("AWASH_SMS")
        assertNotNull(bySenderId)
        assertEquals("Awash Bank", bySenderId?.name)
    }

    @Test
    fun updateTotals_byShortName() = runBlocking {
        val account = BankAccountEntity(id = 3, name = "Dashen", shortName = "Dashen", smsSenderId = "", logoResId = 0, accountNumber = "", totalIncome = 1000.0, totalExpense = 200.0, transactionCount = 5)
        bankAccountDao.insertBankAccount(account)

        // Adding 500 income, 100 expense
        bankAccountDao.updateTotals("Dashen", 500.0, 100.0)

        val updated = bankAccountDao.getBankAccountByShortName("Dashen")
        assertNotNull(updated)
        assertEquals(1500.0, updated?.totalIncome)
        assertEquals(300.0, updated?.totalExpense)
        assertEquals(6, updated?.transactionCount)
    }
}
