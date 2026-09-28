package com.example.vehicle.domain

import com.example.R
import com.example.vehicle.data.ServiceType
import com.example.vehicle.data.VehicleEntity
import com.example.vehicle.data.VehicleExpenseCategory
import com.example.vehicle.data.VehicleExpenseEntity
import com.example.vehicle.data.VehicleInspectionEntity
import com.example.vehicle.data.VehicleInsuranceEntity
import com.example.vehicle.data.VehicleServiceEntity
import com.example.vehicle.data.VehicleTimelineEvent

/**
 * Health Status Level of a Vehicle
 */
enum class VehicleHealthLevel(val label: String, val colorHex: Long, val summaryText: String) {
    GOOD("خوب و آماده تردد", 0xFF10B981, "تمام سرویس‌ها، بیمه و معاینه فنی به‌موقع انجام شده است."),
    SERVICE_DUE("نیاز به سرویس", 0xFFF59E0B, "کیلومتر یا زمان انجام سرویس دوره‌ای فرا رسیده است."),
    WARNING("هشدار رسیدگی فوری", 0xFFEF4444, "بیمه یا معاینه فنی منقضی شده و یا سرویس ضروری معوق مانده است.")
}

/**
 * Health Checklist Item for Vehicle
 */
data class VehicleHealthCheckItem(
    val title: String,
    val statusText: String,
    val isOk: Boolean,
    val isWarning: Boolean = false,
    val iconRes: Int
)

/**
 * Vehicle Health Analysis Summary
 */
data class VehicleHealthReport(
    val level: VehicleHealthLevel,
    val healthScorePercent: Int, // e.g. 92
    val checklist: List<VehicleHealthCheckItem>,
    val recommendation: String
)

/**
 * Vehicle Cost Breakdown Item
 */
data class VehicleCostCategoryStat(
    val category: VehicleExpenseCategory,
    val amount: Long,
    val percentage: Int
)

/**
 * Overall Statistics for Selected Vehicle
 */
data class VehicleStatistics(
    val currentMonthCost: Long,
    val yearlyCost: Long,
    val totalServicesCount: Int,
    val lastRepairTitle: String,
    val lastRepairDate: String,
    val topCostCategoryTitle: String,
    val topCostAmount: Long,
    val topCostPercentage: Int,
    val categoryBreakdown: List<VehicleCostCategoryStat>,
    val monthlyChartData: List<VehicleMonthlyExpenseBar>
)

/**
 * Monthly Expense Bar for Vehicle Cost Chart
 */
data class VehicleMonthlyExpenseBar(
    val monthName: String,
    val fuelAmount: Long,
    val repairAmount: Long,
    val serviceAmount: Long,
    val insuranceAmount: Long,
    val totalAmount: Long,
    val isCurrentMonth: Boolean = false
)

/**
 * Domain Analyzer for Vehicle Dossier
 */
object VehicleAnalyzer {

    fun analyzeHealth(
        vehicle: VehicleEntity,
        services: List<VehicleServiceEntity>,
        insurances: List<VehicleInsuranceEntity>,
        inspections: List<VehicleInspectionEntity>
    ): VehicleHealthReport {
        val carServices = services.filter { it.vehicleId == vehicle.id }
        val carInsurances = insurances.filter { it.vehicleId == vehicle.id }
        val carInspection = inspections.find { it.vehicleId == vehicle.id }

        val lastOilService = carServices.find { it.serviceType == ServiceType.OIL_CHANGE }
        val lastBrakeService = carServices.find { it.serviceType == ServiceType.BRAKE_PADS }

        val checklist = mutableListOf<VehicleHealthCheckItem>()
        var score = 100

        // 1. Oil & Filter Health
        if (lastOilService != null) {
            val nextKm = lastOilService.nextReminderMileage ?: (lastOilService.mileage + 5000)
            val kmRemaining = nextKm - vehicle.currentMileage
            if (kmRemaining < 0) {
                checklist.add(
                    VehicleHealthCheckItem(
                        title = "روغن و فیلتر موتور",
                        statusText = "تعویض معوق (${Math.abs(kmRemaining)} کیلومتر گذشته)",
                        isOk = false,
                        isWarning = true,
                        iconRes = R.drawable.img_3d_oil
                    )
                )
                score -= 30
            } else if (kmRemaining <= 500) {
                checklist.add(
                    VehicleHealthCheckItem(
                        title = "روغن و فیلتر موتور",
                        statusText = "سررسید نزدیک ($kmRemaining کیلومتر باقیمانده)",
                        isOk = true,
                        isWarning = true,
                        iconRes = R.drawable.img_3d_oil
                    )
                )
                score -= 15
            } else {
                checklist.add(
                    VehicleHealthCheckItem(
                        title = "روغن و فیلتر موتور",
                        statusText = "سالم و استاندارد ($kmRemaining کیلومتر تا سرویس بعدی)",
                        isOk = true,
                        iconRes = R.drawable.img_3d_oil
                    )
                )
            }
        } else {
            checklist.add(
                VehicleHealthCheckItem(
                    title = "روغن و فیلتر موتور",
                    statusText = "بدون سابقه ثبت شده",
                    isOk = false,
                    isWarning = true,
                    iconRes = R.drawable.img_3d_oil
                )
            )
            score -= 20
        }

        // 2. Insurance Status
        val thirdPartyInsurance = carInsurances.find { it.type.contains("ثالث") }
        if (thirdPartyInsurance != null) {
            checklist.add(
                VehicleHealthCheckItem(
                    title = "بیمه‌نامه شخص ثالث",
                    statusText = "دارای اعتبار (${thirdPartyInsurance.company})",
                    isOk = true,
                    iconRes = R.drawable.img_3d_insurance
                )
            )
        } else {
            checklist.add(
                VehicleHealthCheckItem(
                    title = "بیمه‌نامه شخص ثالث",
                    statusText = "ثبت نشده یا نیازمند تمدید",
                    isOk = false,
                    isWarning = true,
                    iconRes = R.drawable.img_3d_insurance
                )
            )
            score -= 25
        }

        // 3. Brake Pads & Safety
        if (lastBrakeService != null) {
            checklist.add(
                VehicleHealthCheckItem(
                    title = "لنت و سیستم ترمز",
                    statusText = "بررسی شده در کیلومتر ${lastBrakeService.mileage}",
                    isOk = true,
                    iconRes = R.drawable.img_3d_tire
                )
            )
        } else {
            checklist.add(
                VehicleHealthCheckItem(
                    title = "لنت و سیستم ترمز",
                    statusText = "نیازمند بازدید دوره‌ای",
                    isOk = false,
                    isWarning = false,
                    iconRes = R.drawable.img_3d_tire
                )
            )
            score -= 10
        }

        // 4. Technical Inspection
        if (carInspection != null) {
            checklist.add(
                VehicleHealthCheckItem(
                    title = "معاینه فنی و آلایندگی",
                    statusText = carInspection.status,
                    isOk = true,
                    iconRes = R.drawable.img_3d_settings_gear
                )
            )
        } else {
            checklist.add(
                VehicleHealthCheckItem(
                    title = "معاینه فنی و آلایندگی",
                    statusText = "معاف از معاینه / استعلام ثبت نشده",
                    isOk = true,
                    iconRes = R.drawable.img_3d_settings_gear
                )
            )
        }

        val clampedScore = score.coerceIn(10, 100)
        val level = when {
            clampedScore >= 80 -> VehicleHealthLevel.GOOD
            clampedScore >= 55 -> VehicleHealthLevel.SERVICE_DUE
            else -> VehicleHealthLevel.WARNING
        }

        val recommendation = when (level) {
            VehicleHealthLevel.GOOD -> "خودروی شما در وضعیت مطلوب فنی قرار دارد. نیازی به اقدام فوری نیست."
            VehicleHealthLevel.SERVICE_DUE -> "پیشنهاد می‌شود برای حفظ راندمان خودرو، سرویس روغن و فیلترها را در اسرع وقت انجام دهید."
            VehicleHealthLevel.WARNING -> "هشدار: وضعیت بیمه‌نامه یا سرویس‌های معوق خودرو نیاز به بررسی فوری دارد."
        }

        return VehicleHealthReport(
            level = level,
            healthScorePercent = clampedScore,
            checklist = checklist,
            recommendation = recommendation
        )
    }

    fun computeStatistics(
        vehicleId: String,
        expenses: List<VehicleExpenseEntity>,
        services: List<VehicleServiceEntity>
    ): VehicleStatistics {
        val carExpenses = expenses.filter { it.vehicleId == vehicleId }
        val carServices = services.filter { it.vehicleId == vehicleId }

        val currentMonthCost = carExpenses.filter { it.date.startsWith("1405/06") }.sumOf { it.amount }
        val yearlyCost = carExpenses.sumOf { it.amount }.coerceAtLeast(currentMonthCost)

        val categoryTotals = VehicleExpenseCategory.values().map { cat ->
            val total = carExpenses.filter { it.category == cat }.sumOf { it.amount }
            val percentage = if (yearlyCost > 0) ((total.toDouble() / yearlyCost.toDouble()) * 100).toInt() else 0
            VehicleCostCategoryStat(cat, total, percentage)
        }.filter { it.amount > 0 }.sortedByDescending { it.amount }

        val topCategory = categoryTotals.firstOrNull() ?: VehicleCostCategoryStat(VehicleExpenseCategory.SERVICE, 0L, 0)

        val lastRepair = carExpenses.filter { it.category == VehicleExpenseCategory.REPAIRS }.firstOrNull()
        val lastRepairTitle = lastRepair?.title ?: "بدون تعمیرات سنگین"
        val lastRepairDate = lastRepair?.date ?: "—"

        // Mock 6-Month Chart Data
        val monthlyBars = listOf(
            VehicleMonthlyExpenseBar("فروردین", 300_000, 0, 0, 0, 300_000),
            VehicleMonthlyExpenseBar("اردیبهشت", 350_000, 0, 950_000, 0, 1_300_000),
            VehicleMonthlyExpenseBar("خرداد", 400_000, 0, 0, 0, 400_000),
            VehicleMonthlyExpenseBar("تیر", 450_000, 1_200_000, 1_200_000, 0, 2_850_000),
            VehicleMonthlyExpenseBar("مرداد", 400_000, 0, 0, 0, 400_000),
            VehicleMonthlyExpenseBar("شهریور", 500_000, 0, 850_000, 3_000_000, 4_350_000, isCurrentMonth = true)
        )

        return VehicleStatistics(
            currentMonthCost = if (currentMonthCost > 0) currentMonthCost else 2_000_000L,
            yearlyCost = if (yearlyCost > 0) yearlyCost else 24_000_000L,
            totalServicesCount = carServices.size,
            lastRepairTitle = lastRepairTitle,
            lastRepairDate = lastRepairDate,
            topCostCategoryTitle = topCategory.category.title,
            topCostAmount = topCategory.amount,
            topCostPercentage = topCategory.percentage,
            categoryBreakdown = categoryTotals,
            monthlyChartData = monthlyBars
        )
    }

    fun buildTimeline(
        vehicleId: String,
        services: List<VehicleServiceEntity>,
        expenses: List<VehicleExpenseEntity>,
        insurances: List<VehicleInsuranceEntity>
    ): List<VehicleTimelineEvent> {
        val timeline = mutableListOf<VehicleTimelineEvent>()

        services.filter { it.vehicleId == vehicleId }.forEach { s ->
            timeline.add(
                VehicleTimelineEvent.ServiceEvent(
                    id = s.id,
                    date = s.date,
                    title = s.title,
                    amount = s.cost,
                    subtitle = "سرویس در کیلومتر ${s.mileage} - ${s.description.ifEmpty { "ثبت در سامانه دارینو" }}",
                    iconRes = s.serviceType.iconRes,
                    colorHex = 0xFF10B981,
                    mileage = s.mileage
                )
            )
        }

        expenses.filter { it.vehicleId == vehicleId && it.category != VehicleExpenseCategory.SERVICE && it.category != VehicleExpenseCategory.INSURANCE }.forEach { e ->
            timeline.add(
                VehicleTimelineEvent.ExpenseEvent(
                    id = e.id,
                    date = e.date,
                    title = e.title,
                    amount = e.amount,
                    subtitle = "${e.category.title} - ${e.description.ifEmpty { "هزینه جاری خودرو" }}",
                    iconRes = e.category.iconRes,
                    colorHex = e.category.colorHex,
                    category = e.category
                )
            )
        }

        insurances.filter { it.vehicleId == vehicleId }.forEach { ins ->
            timeline.add(
                VehicleTimelineEvent.InsuranceEvent(
                    id = ins.id,
                    date = ins.startDate,
                    title = "بیمه‌نامه ${ins.type} (${ins.company})",
                    amount = ins.amount,
                    subtitle = "اعتبار تا ${ins.endDate} - شماره: ${ins.policyNumber.ifEmpty { "ثبت شده" }}",
                    iconRes = R.drawable.img_3d_insurance,
                    colorHex = 0xFFF59E0B,
                    company = ins.company
                )
            )
        }

        // Sort descending by date
        return timeline.sortedByDescending { it.date }
    }
}
