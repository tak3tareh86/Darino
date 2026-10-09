package com.example.ui.screens.home.domain

import com.example.ui.screens.finance.model.TransactionType

data class BankSmsSuggestion(
    val id: String,
    val bankName: String,
    val amount: Long,
    val formattedAmount: String,
    val isAmountValid: Boolean = (amount > 0L),
    val type: TransactionType? = null,
    val isTypeUncertain: Boolean = false,
    val smsText: String,
    val dateText: String,
    val timeText: String = "",
    val category: String = "سایر",
    val sourceAccount: String? = null,
    val destinationAccount: String? = null,
    val rawSender: String? = null,
    val parseError: String? = null,
    val timestampMillis: Long = System.currentTimeMillis()
) {
    val isExpense: Boolean
        get() = type == TransactionType.EXPENSE
}
