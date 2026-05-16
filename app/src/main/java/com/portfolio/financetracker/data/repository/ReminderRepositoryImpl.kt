package com.portfolio.financetracker.data.repository

import com.portfolio.financetracker.data.local.dao.ReminderDao
import com.portfolio.financetracker.data.local.entity.ReminderEntity
import com.portfolio.financetracker.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepositoryImpl @Inject constructor(
    private val reminderDao: ReminderDao
) : ReminderRepository {

    override fun getAllReminders(): Flow<List<ReminderEntity>> {
        return reminderDao.getAllReminders()
    }

    override suspend fun insertReminder(reminder: ReminderEntity) {
        reminderDao.insertReminder(reminder)
    }

    override suspend fun deleteReminder(reminder: ReminderEntity) {
        reminderDao.deleteReminder(reminder)
    }

    override suspend fun updateCompletionStatus(id: Int, completed: Boolean) {
        reminderDao.updateCompletionStatus(id, completed)
    }

    override suspend fun getPendingReminders(currentTime: Long): List<ReminderEntity> {
        return reminderDao.getPendingReminders(currentTime)
    }
}
