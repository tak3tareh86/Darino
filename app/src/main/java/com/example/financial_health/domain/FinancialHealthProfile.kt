package com.example.financial_health.domain

import androidx.compose.ui.graphics.Color

enum class FinancialHealthStatus(
    val title: String,
    val badgeText: String,
    val color: Color,
    val lightContainerColor: Color,
    val darkContainerColor: Color,
    val iconEmoji: String,
    val defaultMessage: String
) {
    SAFE(
        title = "مناسب",
        badgeText = "🟢 مناسب",
        color = Color(0xFF10B981),
        lightContainerColor = Color(0xFFECFDF5),
        darkContainerColor = Color(0xFF064E3B),
        iconEmoji = "🟢",
        defaultMessage = "تعهدات مالی شما در محدوده کنترل شده است."
    ),
    MEDIUM(
        title = "متوسط",
        badgeText = "🟡 متوسط",
        color = Color(0xFFF59E0B),
        lightContainerColor = Color(0xFFFFFBEB),
        darkContainerColor = Color(0xFF78350F),
        iconEmoji = "🟡",
        defaultMessage = "بخش قابل توجهی از درآمد شما صرف تعهدات مالی می‌شود."
    ),
    HIGH(
        title = "بالا",
        badgeText = "🔴 بالا",
        color = Color(0xFFEF4444),
        lightContainerColor = Color(0xFFFEF2F2),
        darkContainerColor = Color(0xFF7F1D1D),
        iconEmoji = "🔴",
        defaultMessage = "تعهدات مالی شما بخش زیادی از درآمدتان را تشکیل می‌دهد."
    )
}

enum class InsightType {
    POSITIVE,
    WARNING,
    ALERT,
    INFO
}

data class FinancialHealthInsight(
    val id: String,
    val title: String,
    val description: String,
    val type: InsightType = InsightType.INFO,
    val recommendation: String? = null
)

data class NextMonthPrediction(
    val installments: Long = 8_500_000L,
    val reminders: Long = 2_000_000L,
    val vehicleExpenses: Long = 1_500_000L
) {
    val total: Long get() = installments + reminders + vehicleExpenses
}

data class MonthlyPressureData(
    val monthName: String,
    val pressurePercentage: Float,
    val commitmentsAmount: Long,
    val incomeAmount: Long
)

data class FinancialHealthProfile(
    val id: String = "default_user_profile",
    val monthlyIncome: Long,
    val monthlyInstallments: Long,
    val fixedExpenses: Long,
    val financialPressure: Float,
    val healthStatus: FinancialHealthStatus,
    val updatedAt: Long = System.currentTimeMillis()
)
