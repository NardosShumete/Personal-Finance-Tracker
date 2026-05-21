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
            id               = 1,
            shortName        = "CBE",
            fullName         = "Commercial Bank of Ethiopia",
            smsSenderId      = "CBE",
            colorHex         = "#0055A4",
            isConnected      = false,
            totalIncome      = 500.0,
            totalExpense     = 100.0,
            transactionCount = 2
        )

        bankAccountDao.insertBankAccount(account)

        val accounts = bankAccountDao.getAllBankAccounts().first()
        assertEquals(1, accounts.size)
        assertEquals("CBE", accounts[0].shortName)
    }

    @Test
    fun getBankAccountByShortName_and_SenderId() = runBlocking {
        val account = BankAccountEntity(
            id               = 2,
            shortName        = "Awash",
            fullName         = "Awash Bank",
            smsSenderId      = "AWASH_SMS",
            colorHex         = "#FF6600",
            isConnected      = false,
            totalIncome      = 0.0,
            totalExpense     = 0.0,
            transactionCount = 0
        )
        bankAccountDao.insertBankAccount(account)

        val byShortName = bankAccountDao.getBankAccountByShortName("Awash")
        assertNotNull(byShortName)
        assertEquals("Awash Bank", byShortName?.fullName)

        val bySenderId = bankAccountDao.getBankAccountBySenderId("AWASH_SMS")
        assertNotNull(bySenderId)
        assertEquals("Awash Bank", bySenderId?.fullName)
    }

    @Test
    fun updateTotals_byId() = runBlocking {
        val account = BankAccountEntity(
            id               = 3,
            shortName        = "Dashen",
            fullName         = "Dashen Bank",
            smsSenderId      = "DashenBank",
            colorHex         = "#800000",
            isConnected      = false,
            totalIncome      = 1000.0,
            totalExpense     = 200.0,
            transactionCount = 5
        )
        bankAccountDao.insertBankAccount(account)

        // Full recalculate via id-based updateTotals
        bankAccountDao.updateTotals(id = 3, income = 1500.0, expense = 300.0, count = 6, lastKnownBalance = null)

        val updated = bankAccountDao.getBankAccountByShortName("Dashen")
        assertNotNull(updated)
        assertEquals(1500.0, updated?.totalIncome)
        assertEquals(300.0, updated?.totalExpense)
        assertEquals(6, updated?.transactionCount)
    }
}
