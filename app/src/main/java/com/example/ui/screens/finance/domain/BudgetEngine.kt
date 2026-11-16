package com.example.ui.screens.finance.domain

import com.example.ui.screens.finance.model.Budget
import com.example.ui.screens.finance.model.BudgetStatus
import com.example.ui.screens.finance.model.FinanceFilterPeriod
import com.example.ui.screens.finance.model.TransactionItemData
import com.example.ui.screens.finance.model.TransactionType
import com.example.util.MoneyAmount
import com.example.ui.screens.settings.model.AppCurrency
import java.math.BigInteger

/** Calculates budget usage from expenses in the current Jalali month. */
object BudgetEngine {

    private fun currentMonthExpenses(
        transactions: List<TransactionItemData>,
        nowMillis: Long
    ): List<TransactionItemData> {
        val range = FinanceTimeUtils.getTimeRangeForPeriod(FinanceFilterPeriod.THIS_MONTH, nowMillis)
        return transactions.filter { tx ->
            tx.type == TransactionType.EXPENSE &&
                tx.dateMillis >= range.startMillis &&
                tx.dateMillis < range.endMillis
        }
    }

    fun calculateBudgetUsage(
        budget: Budget,
        transactions: List<TransactionItemData>,
        nowMillis: Long = System.currentTimeMillis()
    ): Budget {
        val spent = currentMonthExpenses(transactions, nowMillis)
            .filter { tx -> budget.categoryId == null || tx.categoryId == budget.categoryId }
            .fold(0L) { total, tx ->
                Math.addExact(total, MoneyAmount.of(tx.amount, tx.amountUnit).asRialsLongExact())
            }

        val budgetInRials = MoneyAmount.of(budget.amount, budget.amountUnit).asRialsLongExact()
        val remaining = Math.subtractExact(budgetInRials, spent)

        return budget.copy(
            spentAmount = spent,
            spentAmountUnit = AppCurrency.RIAL,
            remainingAmount = remaining,
            remainingAmountUnit = AppCurrency.RIAL,
            usagePercentage = percentage(spent, budgetInRials),
            status = determineBudgetStatus(spent, budgetInRials)
        )
    }

    private fun percentage(spent: Long, budget: Long): Int {
        if (budget <= 0L) return if (spent > 0L) 100 else 0
        return BigInteger.valueOf(spent)
            .multiply(BigInteger.valueOf(100L))
            .divide(BigInteger.valueOf(budget))
            .min(BigInteger.valueOf(Int.MAX_VALUE.toLong()))
            .toInt()
    }

    fun determineBudgetStatus(spentAmount: Long, totalBudget: Long): BudgetStatus {
        if (totalBudget <= 0L) return if (spentAmount > 0L) BudgetStatus.EXCEEDED else BudgetStatus.SAFE
        if (spentAmount >= totalBudget) return BudgetStatus.EXCEEDED
        val percent = BigInteger.valueOf(spentAmount)
            .multiply(BigInteger.valueOf(100L))
            .divide(BigInteger.valueOf(totalBudget))
        return if (percent >= BigInteger.valueOf(75L)) BudgetStatus.WARNING else BudgetStatus.SAFE
    }

    /**
     * Overall dashboard rules:
     * - Global budget(s) govern all expenses and are not added to category caps.
     * - Without a global budget, active category caps are added and each expense
     *   is counted once, even if duplicate budgets target the same category.
     */
    fun calculateTotalMonthlyBudgetUsage(
        budgets: List<Budget>,
        transactions: List<TransactionItemData>,
        nowMillis: Long = System.currentTimeMillis()
    ): OverallBudgetSummary {
        val activeBudgets = budgets.filter { it.isEnabled }
        val globalBudgets = activeBudgets.filter { it.categoryId == null }
        val categoryBudgets = activeBudgets.filter { it.categoryId != null }
        val hasGlobalBudget = globalBudgets.isNotEmpty()
        val budgetsForTotal = if (hasGlobalBudget) globalBudgets else categoryBudgets

        val totalBudget = budgetsForTotal.fold(0L) { total, budget ->
            Math.addExact(total, MoneyAmount.of(budget.amount, budget.amountUnit).asRialsLongExact())
        }
        val expenses = currentMonthExpenses(transactions, nowMillis).filter { tx ->
            hasGlobalBudget || categoryBudgets.any { it.categoryId == tx.categoryId }
        }
        val totalSpent = expenses.fold(0L) { total, tx ->
            Math.addExact(total, MoneyAmount.of(tx.amount, tx.amountUnit).asRialsLongExact())
        }
        val remaining = Math.subtractExact(totalBudget, totalSpent)

        return OverallBudgetSummary(
            totalBudget = totalBudget,
            totalSpent = totalSpent,
            remainingBudget = remaining,
            usagePercentage = percentage(totalSpent, totalBudget),
            status = determineBudgetStatus(totalSpent, totalBudget)
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
