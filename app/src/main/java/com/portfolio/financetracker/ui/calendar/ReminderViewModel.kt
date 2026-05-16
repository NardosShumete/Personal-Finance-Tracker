package com.portfolio.financetracker.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.financetracker.data.local.entity.ReminderEntity
import com.portfolio.financetracker.domain.repository.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val repository: ReminderRepository
) : ViewModel() {

    val reminders: StateFlow<List<ReminderEntity>> = repository.getAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addReminder(
        title: String, 
        amount: Double, 
        date: Long, 
        type: String, 
        category: String,
        repeatInterval: String,
        autoGenerate: Boolean,
        syncToCalendar: Boolean
    ) {
        viewModelScope.launch {
            repository.insertReminder(
                ReminderEntity(
                    title = title,
                    amount = amount,
                    date = date,
                    type = type,
                    category = category,
                    repeatInterval = repeatInterval,
                    autoGenerateExpense = autoGenerate,
                    syncToGoogleCalendar = syncToCalendar
                )
            )
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
        }
    }

    fun toggleCompletion(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.updateCompletionStatus(reminder.id, !reminder.isCompleted)
        }
    }
}
