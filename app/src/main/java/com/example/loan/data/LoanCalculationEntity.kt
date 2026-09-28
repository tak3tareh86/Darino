package com.example.loan.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.loan.domain.DurationType
import com.example.loan.domain.LoanCalculation
import com.example.loan.domain.LoanType

@Entity(tableName = "loan_calculations")
data class LoanCalculationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val loanType: String,
    val amount: Long,
    val interestRate: Double,
    val duration: Int,
    val durationType: String,
    val monthlyPayment: Long,
    val totalPayment: Long,
    val totalInterest: Long,
    val realCostPercentage: Double,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): LoanCalculation {
        val type = try {
            LoanType.valueOf(loanType)
        } catch (e: Exception) {
            LoanType.BANK_LOAN
        }
        val durType = try {
            DurationType.valueOf(durationType)
        } catch (e: Exception) {
            DurationType.MONTHS
        }
        return LoanCalculation(
            id = id,
            title = title,
            loanType = type,
            amount = amount,
            interestRate = interestRate,
            duration = duration,
            durationType = durType,
            monthlyPayment = monthlyPayment,
            totalPayment = totalPayment,
            totalInterest = totalInterest,
            realCostPercentage = realCostPercentage,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomainModel(domain: LoanCalculation): LoanCalculationEntity {
            return LoanCalculationEntity(
                id = domain.id,
                title = domain.title,
                loanType = domain.loanType.name,
                amount = domain.amount,
                interestRate = domain.interestRate,
                duration = domain.duration,
                durationType = domain.durationType.name,
                monthlyPayment = domain.monthlyPayment,
                totalPayment = domain.totalPayment,
                totalInterest = domain.totalInterest,
                realCostPercentage = domain.realCostPercentage,
                createdAt = domain.createdAt
            )
        }
    }
}
