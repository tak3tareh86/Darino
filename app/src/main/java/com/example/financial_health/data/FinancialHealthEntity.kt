package com.example.financial_health.data

import androidx.room.Entity

@Entity(
    tableName = "financial_health_profile",
    primaryKeys = ["id", "userId"]
)
data class FinancialHealthEntity(
    val id: String,
    val userId: String,
    val monthlyIncome: Long = 0L,
    val monthlyInstallments: Long = 0L,
    val fixedExpenses: Long = 0L,
    val financialPressure: Float = 0.0f,
    val healthStatus: String = "MEDIUM", // "SAFE", "MEDIUM", "HIGH"
    val updatedAt: Long = System.currentTimeMillis()
)
