package com.portfolio.financetracker.ui.transaction

import com.portfolio.financetracker.domain.model.RecurringPeriod
import com.portfolio.financetracker.domain.model.TransactionType

data class AddTransactionState(
    val id: Int? = null,
    val amount: String = "",
    val category: String = "",
    val note: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val date: Long = System.currentTimeMillis(),
    val receiptPath: String? = null,
    val recurringPeriod: RecurringPeriod = RecurringPeriod.NONE,
    // True while the image is being copied to permanent storage
    val isUploadingReceipt: Boolean = false
)
