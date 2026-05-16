package com.portfolio.financetracker.data.local.dao

import androidx.room.*
import com.portfolio.financetracker.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminder_table ORDER BY date ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("UPDATE reminder_table SET isCompleted = :completed WHERE id = :id")
    suspend fun updateCompletionStatus(id: Int, completed: Boolean)

    @Query("SELECT * FROM reminder_table WHERE isCompleted = 0 AND date <= :currentTime")
    suspend fun getPendingReminders(currentTime: Long): List<ReminderEntity>
}
