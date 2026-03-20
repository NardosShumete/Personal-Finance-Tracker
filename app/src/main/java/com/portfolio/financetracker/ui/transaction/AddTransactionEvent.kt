package com.portfolio.financetracker.ui.transaction

import com.portfolio.financetracker.domain.model.RecurringPeriod
import com.portfolio.financetracker.domain.model.TransactionType

sealed class AddTransactionEvent {
    data class EnteredAmount(val value: String): AddTransactionEvent()
    data class EnteredCategory(val value: String): AddTransactionEvent()
    data class EnteredNote(val value: String): AddTransactionEvent()
    data class ChangedType(val type: TransactionType): AddTransactionEvent()
    data class ChangedDate(val date: Long): AddTransactionEvent()
    data class ChangedReceipt(val path: String?): AddTransactionEvent()
    data class ChangedRecurring(val period: RecurringPeriod): AddTransactionEvent()
    object SaveTransaction: AddTransactionEvent()
    object DeleteTransaction: AddTransactionEvent()
}
