package com.example.financial_health.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "financial_health_profile")
data class FinancialHealthEntity(
    @PrimaryKey val id: String = "default_user_profile",
    val monthlyIncome: Long = 30_000_000L,
    val monthlyInstallments: Long = 8_000_000L,
    val fixedExpenses: Long = 4_000_000L,
    val financialPressure: Float = 40.0f,
    val healthStatus: String = "MEDIUM", // "SAFE", "MEDIUM", "HIGH"
    val updatedAt: Long = System.currentTimeMillis()
)
