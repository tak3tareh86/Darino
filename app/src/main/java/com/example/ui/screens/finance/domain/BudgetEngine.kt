package com.example.ui.screens.finance.domain

import com.example.ui.screens.finance.model.Budget
import com.example.ui.screens.finance.model.BudgetStatus
import com.example.ui.screens.finance.model.TransactionItemData
import com.example.ui.screens.finance.model.TransactionType

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
            .sumOf { it.amount }

        val remaining = budget.amount - spent

        val usagePercent: Int = if (budget.amount <= 0L) {
            if (spent > 0L) 100 else 0
        } else {
            ((spent * 100L) / budget.amount).toInt()
        }

        val status = determineBudgetStatus(spent, budget.amount)

        return budget.copy(
            spentAmount = spent,
            remainingAmount = remaining,
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
        val percent = (spentAmount * 100L) / totalBudget
        return if (percent >= 75L) {
            BudgetStatus.WARNING
        } else {
            BudgetStatus.SAFE
        }
    }

    /**
     * Aggregates total monthly budget vs total expense across active budgets.
     */
    fun calculateTotalMonthlyBudgetUsage(
        budgets: List<Budget>,
        transactions: List<TransactionItemData>
    ): OverallBudgetSummary {
        val totalBudget = budgets.filter { it.isEnabled }.sumOf { it.amount }
        val totalSpent = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val remaining = totalBudget - totalSpent

        val usagePercentage = if (totalBudget <= 0L) {
            if (totalSpent > 0L) 100 else 0
        } else {
            ((totalSpent * 100L) / totalBudget).toInt()
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
