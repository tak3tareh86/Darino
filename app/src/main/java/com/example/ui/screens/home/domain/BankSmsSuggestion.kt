package com.example.ui.screens.home.domain

import com.example.ui.screens.finance.model.TransactionType
import com.example.ui.screens.settings.model.AppCurrency

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
    val timestampMillis: Long = System.currentTimeMillis(),
    val balanceAmount: Long? = null,
    val trackingCode: String? = null,
    val patternId: String = "",
    // Actual inbox receipt time; distinct from the transaction date parsed from the SMS body.
    val receivedAtMillis: Long = timestampMillis,
    // All distinct, strictly grouped numeric amounts found in the original SMS.
    val amountCandidates: List<Long> = emptyList(),
    val amountUnit: AppCurrency = AppCurrency.TOMAN,
    // Balance values can use a different unit from the transaction in the same SMS.
    val balanceAmountUnit: AppCurrency? = null
) {
    val isExpense: Boolean
        get() = type == TransactionType.EXPENSE
}