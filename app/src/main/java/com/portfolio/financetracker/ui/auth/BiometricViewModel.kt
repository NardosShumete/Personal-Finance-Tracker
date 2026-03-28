package com.portfolio.financetracker.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.financetracker.data.local.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.portfolio.financetracker.domain.use_case.TransactionUseCases
import com.portfolio.financetracker.domain.model.Transaction
import kotlinx.coroutines.flow.first

@HiltViewModel
class BiometricViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager,
    private val transactionUseCases: TransactionUseCases
) : ViewModel() {

    val isBiometricEnabled: StateFlow<Boolean> = dataStoreManager.isBiometricEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isFirstTimeUser: StateFlow<Boolean> = dataStoreManager.isFirstTimeUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isOnboarded: StateFlow<Boolean> = dataStoreManager.isOnboarded
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isDarkModeEnabled: StateFlow<Boolean?> = dataStoreManager.isDarkModeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currencyCode: StateFlow<String> = dataStoreManager.currencyCode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "ETB")

    val languageCode: StateFlow<String> = dataStoreManager.languageCode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setBiometricEnabled(enabled)
            dataStoreManager.setFirstTimeUser(false)
        }
    }

    fun skipFirstTimeSetup() {
        viewModelScope.launch {
            dataStoreManager.setFirstTimeUser(false)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            dataStoreManager.setOnboarded(true)
        }
    }

    fun setAuthenticated(authenticated: Boolean) {
        _isAuthenticated.value = authenticated
    }

    fun setDarkModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setDarkModeEnabled(enabled)
        }
    }

    fun setCurrencyCode(code: String) {
        viewModelScope.launch {
            dataStoreManager.setCurrencyCode(code)
        }
    }

    fun setLanguageCode(code: String) {
        viewModelScope.launch {
            dataStoreManager.setLanguageCode(code)
        }
    }

    suspend fun createCsvData(): String {
        val transactions = transactionUseCases.getTransactions().first()
        val builder = java.lang.StringBuilder()
        builder.append("ID,Type,Amount,Category,Date,Note\n")
        
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        
        for (t in transactions) {
            val dateStr = dateFormat.format(java.util.Date(t.date))
            val noteSafe = t.note.replace(",", " ") // Avoid CSV breaking
            val categorySafe = t.category.replace(",", " ")
            builder.append("${t.id},${t.type.name},${t.amount},${categorySafe},${dateStr},${noteSafe}\n")
        }
        return builder.toString()
    }
}
