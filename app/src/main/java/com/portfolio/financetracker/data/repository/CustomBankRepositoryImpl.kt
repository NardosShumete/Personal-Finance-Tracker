package com.portfolio.financetracker.data.repository

import com.portfolio.financetracker.data.local.dao.CustomBankDao
import com.portfolio.financetracker.data.local.entity.CustomBankEntity
import com.portfolio.financetracker.domain.repository.CustomBankRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CustomBankRepositoryImpl @Inject constructor(
    private val dao: CustomBankDao
) : CustomBankRepository {
    override fun getAllCustomBanks(): Flow<List<CustomBankEntity>> = dao.getAllCustomBanks()
    override fun getEnabledCustomBanks(): Flow<List<CustomBankEntity>> = dao.getEnabledCustomBanks()
    override suspend fun getEnabledCustomBanksSync(): List<CustomBankEntity> = dao.getEnabledCustomBanksSync()
    override suspend fun insertCustomBank(customBank: CustomBankEntity) = dao.insertCustomBank(customBank)
    override suspend fun updateCustomBank(customBank: CustomBankEntity) = dao.updateCustomBank(customBank)
    override suspend fun deleteCustomBank(customBank: CustomBankEntity) = dao.deleteCustomBank(customBank)
}
