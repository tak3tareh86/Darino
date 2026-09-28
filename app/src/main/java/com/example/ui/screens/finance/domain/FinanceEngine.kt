package com.example.ui.screens.finance.domain

import com.example.ui.screens.finance.model.RecurringTransaction
import com.example.ui.screens.finance.model.SavingsGoal
import com.example.ui.screens.finance.model.TransactionItemData
import com.example.ui.screens.finance.model.TransactionType

/**
 * Pure Domain calculation engine for Finance module.
 * Strictly uses Long / Int arithmetic with zero Float/Double conversions for financial values.
 */
object FinanceEngine {

    /**
     * Calculates total income from transaction list.
     */
    fun calculateMonthlyIncome(transactions: List<TransactionItemData>): Long {
        var total = 0L
        for (tx in transactions) {
            if (tx.type == TransactionType.INCOME) {
                total += tx.amount
            }
        }
        return total
    }

    /**
     * Calculates total expense from transaction list.
     */
    fun calculateMonthlyExpense(transactions: List<TransactionItemData>): Long {
        var total = 0L
        for (tx in transactions) {
            if (tx.type == TransactionType.EXPENSE) {
                total += tx.amount
            }
        }
        return total
    }

    /**
     * Calculates net balance (Income - Expense).
     */
    fun calculateMonthlyBalance(income: Long, expense: Long): Long {
        return income - expense
    }

    /**
     * Calculates savings rate as a percentage [0..100].
     * If income is zero or negative, returns 0.
     * If expense exceeds income, returns 0.
     */
    fun calculateSavingsRate(income: Long, expense: Long): Int {
        if (income <= 0L) return 0
        if (expense >= income) return 0
        if (expense <= 0L) return 100

        val saved = income - expense
        val rate = (saved * 100L) / income
        return rate.toInt().coerceIn(0, 100)
    }

    /**
     * Aggregates expenses by category ID.
     */
    fun calculateExpenseByCategory(transactions: List<TransactionItemData>): Map<String, Long> {
        val map = mutableMapOf<String, Long>()
        for (tx in transactions) {
            if (tx.type == TransactionType.EXPENSE) {
                val current = map.getOrDefault(tx.categoryId, 0L)
                map[tx.categoryId] = current + tx.amount
            }
        }
        return map
    }

    /**
     * Calculates total recurring commitments amount.
     */
    fun calculateTotalRecurring(recurring: List<RecurringTransaction>): Long {
        var total = 0L
        for (item in recurring) {
            if (item.enabled) {
                total += item.amount
            }
        }
        return total
    }

    /**
     * Calculates progress percentage for a savings goal [0..100].
     */
    fun calculateSavingsGoalProgress(currentAmount: Long, targetAmount: Long): Int {
        if (targetAmount <= 0L) {
            return if (currentAmount >= 0L) 100 else 0
        }
        if (currentAmount <= 0L) return 0
        val percent = (currentAmount * 100L) / targetAmount
        return percent.toInt().coerceIn(0, 100)
    }

    /**
     * Calculates remaining amount for a savings goal.
     */
    fun calculateSavingsGoalRemaining(goal: SavingsGoal): Long {
        val remaining = goal.targetAmount - goal.currentAmount
        return if (remaining > 0L) remaining else 0L
    }
}
