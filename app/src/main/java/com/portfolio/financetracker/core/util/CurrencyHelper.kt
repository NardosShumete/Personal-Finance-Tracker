package com.portfolio.financetracker.core.util

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object CurrencyHelper {

    fun formatAmount(amount: Double, currencyCode: String): String {
        return try {
            val format = NumberFormat.getCurrencyInstance()
            format.currency = Currency.getInstance(currencyCode)
            format.format(amount)
        } catch (e: Exception) {
            // Fallback if currency code is somehow completely invalid
            val defaultFormat = NumberFormat.getCurrencyInstance(Locale("en", "ET"))
            defaultFormat.format(amount)
        }
    }

    val supportedCurrencies = listOf(
        Pair("ETB", "Ethiopian Birr (Br)"),
        Pair("USD", "US Dollar (\$)"),
        Pair("EUR", "Euro (€)"),
        Pair("GBP", "British Pound (£)"),
        Pair("JPY", "Japanese Yen (¥)"),
        Pair("CAD", "Canadian Dollar (C\$)"),
        Pair("AUD", "Australian Dollar (A\$)")
    )
}

val LocalCurrencyCode = androidx.compose.runtime.compositionLocalOf { "ETB" }
