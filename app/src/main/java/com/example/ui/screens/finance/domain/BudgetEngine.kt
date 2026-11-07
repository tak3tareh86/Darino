package com.example.ui.screens.finance.domain

import com.example.ui.screens.finance.model.Budget
import com.example.ui.screens.finance.model.BudgetStatus
import com.example.ui.screens.finance.model.TransactionItemData
import com.example.ui.screens.finance.model.TransactionType
import com.example.util.MoneyAmount
import com.example.ui.screens.settings.model.AppCurrency
import java.math.BigInteger

/**
 * Budget Calculation Engine.
 * Provides safe Long/Int calculations for budget progress, remaining funds, and status threshold alerts.
 */
object BudgetEngine {

    /**
     * Recalculates budget spent amount, remaining amount, usage percentage, and status
     * based on the actual transactions.
     */
    fun calculateBudgetUsage(
        budget: Budget,
        transactions: List<TransactionItemData>
    ): Budget {
        val spent = transactions
            .filter { tx ->
                tx.type == TransactionType.EXPENSE &&
                        (budget.categoryId == null || tx.categoryId == budget.categoryId)
            }
            .fold(0L) { total, tx -> Math.addExact(total, MoneyAmount.of(tx.amount, tx.amountUnit).asRialsLongExact()) }

        val budgetInRials = MoneyAmount.of(budget.amount, budget.amountUnit).asRialsLongExact()
        val remaining = Math.subtractExact(budgetInRials, spent)

        val usagePercent: Int = if (budgetInRials <= 0L) {
            if (spent > 0L) 100 else 0
        } else {
            BigInteger.valueOf(spent).multiply(BigInteger.valueOf(100L))
                .divide(BigInteger.valueOf(budgetInRials))
                .min(BigInteger.valueOf(Int.MAX_VALUE.toLong()))
                .toInt()
        }

        val status = determineBudgetStatus(spent, budgetInRials)

        return budget.copy(
            spentAmount = spent,
            spentAmountUnit = AppCurrency.RIAL,
            remainingAmount = remaining,
            remainingAmountUnit = AppCurrency.RIAL,
            usagePercentage = usagePercent,
            status = status
        )
    }

    /**
     * Determines the status of a budget:
     * - SAFE (مصرف مطلوب): usage < 75%
     * - WARNING (نزدیک به سقف): 75% <= usage < 100%
     * - EXCEEDED (فراتر از بودجه): usage >= 100%
     */
    fun determineBudgetStatus(spentAmount: Long, totalBudget: Long): BudgetStatus {
        if (totalBudget <= 0L) {
            return if (spentAmount > 0L) BudgetStatus.EXCEEDED else BudgetStatus.SAFE
        }
        if (spentAmount >= totalBudget) {
            return BudgetStatus.EXCEEDED
        }
        val percent = BigInteger.valueOf(spentAmount).multiply(BigInteger.valueOf(100L)).divide(BigInteger.valueOf(totalBudget))
        return if (percent >= BigInteger.valueOf(75L)) {
            BudgetStatus.WARNING
        } else {
            BudgetStatus.SAFE
        }
    }

    /**
     * Aggregates total monthly budget vs total expense across active budgets.
     * Only expenses matching an enabled budget's category are counted.
     */
    fun calculateTotalMonthlyBudgetUsage(
        budgets: List<Budget>,
        transactions: List<TransactionItemData>
    ): OverallBudgetSummary {
        val activeBudgets = budgets.filter { it.isEnabled }
        val totalBudget = activeBudgets.fold(0L) { total, budget -> Math.addExact(total, MoneyAmount.of(budget.amount, budget.amountUnit).asRialsLongExact()) }
        
        // Only count expenses that belong to one of the active budget categories
        val activeBudgetCategoryIds = activeBudgets.mapNotNull { it.categoryId }.toSet()
        
        val totalSpent = transactions
            .filter { tx -> 
                tx.type == TransactionType.EXPENSE && 
                activeBudgetCategoryIds.contains(tx.categoryId)
            }
            .fold(0L) { total, tx -> Math.addExact(total, MoneyAmount.of(tx.amount, tx.amountUnit).asRialsLongExact()) }
            
        val remaining = Math.subtractExact(totalBudget, totalSpent)

        val usagePercentage = if (totalBudget <= 0L) {
            if (totalSpent > 0L) 100 else 0
        } else {
            BigInteger.valueOf(totalSpent).multiply(BigInteger.valueOf(100L)).divide(BigInteger.valueOf(totalBudget))
                .min(BigInteger.valueOf(Int.MAX_VALUE.toLong()))
                .toInt()
        }

        val status = determineBudgetStatus(totalSpent, totalBudget)

        return OverallBudgetSummary(
            totalBudget = totalBudget,
            totalSpent = totalSpent,
            remainingBudget = remaining,
            usagePercentage = usagePercentage,
            status = status
        )
    }

    data class OverallBudgetSummary(
        val totalBudget: Long,
        val totalSpent: Long,
        val remainingBudget: Long,
        val usagePercentage: Int,
        val status: BudgetStatus
    )
}
