package com.example.ui.screens.home.domain

data class BankSmsSuggestion(
    val id: String,
    val bankName: String,
    val amount: Long,
    val formattedAmount: String,
    val isExpense: Boolean, // true = برداشت (خرید), false = واریز
    val smsText: String,
    val dateText: String,
    val category: String = "سایر"
)
