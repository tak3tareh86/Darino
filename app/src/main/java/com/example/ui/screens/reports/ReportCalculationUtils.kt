package com.example.ui.screens.reports

import kotlin.math.abs

/**
 * Pure calculations shared by the report UI and ViewModel so date and comparison
 * rules can be verified without Android framework dependencies.
 */
internal object ReportCalculationUtils {
    fun isValidDateRange(start: ShamsiDate, end: ShamsiDate): Boolean {
        val startKey = start.year * 10_000 + start.month * 100 + start.day
        val endKey = end.year * 10_000 + end.month * 100 + end.day
        return startKey <= endKey
    }

    fun calculateExpenseChangePercent(current: Long, previous: Long): Double {
        if (previous <= 0L) return 0.0
        return abs(current - previous).toDouble() / previous.toDouble() * 100.0
    }
}
