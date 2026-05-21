package com.portfolio.financetracker.data.repository

import com.portfolio.financetracker.data.local.dao.BankAccountDao
import com.portfolio.financetracker.data.local.entity.BankAccountEntity
import com.portfolio.financetracker.domain.repository.BankAccountRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class BankAccountRepositoryImpl @Inject constructor(
    private val dao: BankAccountDao
) : BankAccountRepository {
    override fun getAllBankAccounts(): Flow<List<BankAccountEntity>> = dao.getAllBankAccounts()
    override suspend fun getBankAccountByShortName(shortName: String): BankAccountEntity? = dao.getBankAccountByShortName(shortName)
    override suspend fun getBankAccountBySenderId(senderId: String): BankAccountEntity? = dao.getBankAccountBySenderId(senderId)
    override suspend fun insertBankAccount(bankAccount: BankAccountEntity) = dao.insertBankAccount(bankAccount)
    override suspend fun updateBankAccount(bankAccount: BankAccountEntity) = dao.updateBankAccount(bankAccount)
    override suspend fun updateTotals(id: Int, income: Double, expense: Double, count: Int, lastKnownBalance: Double?) = dao.updateTotals(id, income, expense, count, lastKnownBalance)
    override suspend fun deleteBankAccount(id: Int) = dao.deleteBankAccount(id)
}
