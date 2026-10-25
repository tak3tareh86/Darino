package com.example.ui.screens.reports

import com.example.vehicle.data.VehicleExpenseEntity
import com.example.vehicle.data.VehicleInspectionEntity
import com.example.vehicle.data.VehicleInsuranceEntity
import com.example.vehicle.data.VehicleServiceEntity

/**
 * VehicleRepository mirrors service, insurance and inspection costs into the vehicle-expense ledger.
 * Prefer that ledger and only fall back to a canonical record when its mirrored expense is absent
 * (legacy/imported data), otherwise those costs are counted twice.
 */
internal object ReportVehicleExpenseCalculator {
    fun totalForRange(
        startTime: Long,
        endTime: Long,
        expenses: List<VehicleExpenseEntity>,
        services: List<VehicleServiceEntity>,
        insurances: List<VehicleInsuranceEntity>,
        inspections: List<VehicleInspectionEntity>
    ): Long {
        val allExpenseIds = expenses.mapTo(mutableSetOf()) { it.id }
        val expenseTotal = expenses
            .filter { ReportDateRangeCalculator.parsePersianDateToMillis(it.date) in startTime..endTime }
            .sumOf { it.amount }

        val unmirroredServiceTotal = services
            .filter {
                ReportDateRangeCalculator.parsePersianDateToMillis(it.date) in startTime..endTime &&
                    ("exp_svc_" + it.id) !in allExpenseIds
            }
            .sumOf { it.cost }

        val unmirroredInsuranceTotal = insurances
            .filter {
                ReportDateRangeCalculator.parsePersianDateToMillis(it.startDate) in startTime..endTime &&
                    ("exp_ins_" + it.id) !in allExpenseIds
            }
            .sumOf { it.amount }

        val unmirroredInspectionTotal = inspections
            .filter {
                ReportDateRangeCalculator.parsePersianDateToMillis(it.lastInspectionDate) in startTime..endTime &&
                    ("exp_insp_" + it.id) !in allExpenseIds
            }
            .sumOf { it.cost }

        return expenseTotal + unmirroredServiceTotal + unmirroredInsuranceTotal + unmirroredInspectionTotal
    }
}
