package com.example.ui.screens.finance.domain

import com.example.ui.screens.finance.model.RecurringTransaction
import com.example.ui.screens.finance.model.SavingsGoal
import com.example.ui.screens.finance.model.TransactionItemData
import com.example.ui.screens.finance.model.TransactionType
import com.example.util.MoneyAmount
import com.example.ui.screens.settings.model.AppCurrency
import java.math.BigInteger

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
                total = Math.addExact(total, MoneyAmount.of(tx.amount, tx.amountUnit).asRialsLongExact())
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
                total = Math.addExact(total, MoneyAmount.of(tx.amount, tx.amountUnit).asRialsLongExact())
            }
        }
        return total
    }

    /**
     * Calculates net balance (Income - Expense).
     */
    fun calculateMonthlyBalance(income: Long, expense: Long): Long {
        return Math.subtractExact(income, expense)
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

        val saved = Math.subtractExact(income, expense)
        val rate = BigInteger.valueOf(saved).multiply(BigInteger.valueOf(100L))
            .divide(BigInteger.valueOf(income)).toInt()
        return rate.coerceIn(0, 100)
    }

    /**
     * Aggregates expenses by category ID.
     */
    fun calculateExpenseByCategory(transactions: List<TransactionItemData>): Map<String, Long> {
        val map = mutableMapOf<String, Long>()
        for (tx in transactions) {
            if (tx.type == TransactionType.EXPENSE) {
                val current = map.getOrDefault(tx.categoryId, 0L)
                map[tx.categoryId] = Math.addExact(current, MoneyAmount.of(tx.amount, tx.amountUnit).asRialsLongExact())
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
                total = Math.addExact(total, MoneyAmount.of(item.amount, item.amountUnit).asRialsLongExact())
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
        val percent = BigInteger.valueOf(currentAmount).multiply(BigInteger.valueOf(100L))
            .divide(BigInteger.valueOf(targetAmount)).toInt()
        return percent.coerceIn(0, 100)
    }

    /**
     * Calculates remaining amount for a savings goal.
     */
    fun calculateSavingsGoalRemaining(goal: SavingsGoal): Long {
        val remaining = Math.subtractExact(goal.targetAmount, goal.currentAmount)
        return if (remaining > 0L) remaining else 0L
    }
}
