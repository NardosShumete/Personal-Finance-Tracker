package com.portfolio.financetracker.domain.repository

import com.portfolio.financetracker.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    fun getAllReminders(): Flow<List<ReminderEntity>>
    suspend fun insertReminder(reminder: ReminderEntity)
    suspend fun deleteReminder(reminder: ReminderEntity)
    suspend fun updateCompletionStatus(id: Int, completed: Boolean)
    suspend fun getPendingReminders(currentTime: Long): List<ReminderEntity>
}
