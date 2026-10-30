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
import com.example.util.IranianPhoneUtils
import com.example.util.PersianCalendarHelper

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
    val isCurrentMonth: Boolean = false,
    val otherAmount: Long = 0L
)

/**
 * Domain Analyzer for Vehicle Dossier
 */
object VehicleAnalyzer {

    private val dateRegex = Regex("""^\d{4}/\d{2}/\d{2}$""")

    internal fun parseAndValidateJalaliDate(rawDate: String?): String? {
        if (rawDate.isNullOrBlank()) return null
        val clean = IranianPhoneUtils.convertDigitsToEnglish(rawDate.trim())
        val parts = clean.split('/', '-')
        if (parts.size == 3) {
            val p0 = parts[0].trim()
            val p1 = parts[1].trim()
            val p2 = parts[2].trim()
            if (p0.length != 4 || p1.length !in 1..2 || p2.length !in 1..2) return null
            val y = p0.toIntOrNull() ?: return null
            val m = p1.toIntOrNull() ?: return null
            val d = p2.toIntOrNull() ?: return null
            if (y !in 1300..1500 || m !in 1..12) return null
            val maxDays = PersianCalendarHelper.getDaysInMonth(y, m)
            if (d !in 1..maxDays) return null
            val padded = "%04d/%02d/%02d".format(y, m, d)
            return if (dateRegex.matches(padded)) padded else null
        }
        return null
    }

    fun analyzeHealth(
        vehicle: VehicleEntity,
        services: List<VehicleServiceEntity>,
        insurances: List<VehicleInsuranceEntity>,
        inspections: List<VehicleInspectionEntity>
    ): VehicleHealthReport {
        val carServices = services.filter { it.vehicleId == vehicle.id }
        val carInsurances = insurances.filter { it.vehicleId == vehicle.id }
        val carInspections = inspections.filter { it.vehicleId == vehicle.id }

        val currentPdt = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis())
        val today = parseAndValidateJalaliDate(currentPdt.toFormattedDate())
            ?: "%04d/%02d/%02d".format(currentPdt.year, currentPdt.month, currentPdt.day)

        fun latestService(type: ServiceType): VehicleServiceEntity? =
            carServices.filter { it.serviceType == type }
                .maxByOrNull { parseAndValidateJalaliDate(it.date) ?: "" }

        fun serviceStatus(
            title: String,
            types: List<ServiceType>,
            iconRes: Int,
            defaultIntervalKm: Int = 0
        ): VehicleHealthCheckItem {
            val records = types.mapNotNull { type -> latestService(type)?.let { type to it } }
            if (records.size < types.size) {
                return VehicleHealthCheckItem(
                    title = title,
                    statusText = "سابقه ثبت نشده",
                    isOk = false,
                    isWarning = true,
                    iconRes = iconRes
                )
            }

            var dueSoon = false
            for ((type, record) in records) {
                val nextKm = record.nextReminderMileage
                    ?: if (type == ServiceType.OIL_CHANGE && defaultIntervalKm > 0) record.mileage + defaultIntervalKm else null
                if (nextKm != null) {
                    val remaining = nextKm - vehicle.currentMileage
                    if (remaining < 0) {
                        return VehicleHealthCheckItem(title, "سررسید گذشته", false, true, iconRes)
                    }
                    if (remaining <= 500) dueSoon = true
                }

                val nextDate = record.nextReminderDate
                if (!nextDate.isNullOrBlank()) {
                    val cleanDueDate = parseAndValidateJalaliDate(nextDate)
                    if (cleanDueDate == null) {
                        return VehicleHealthCheckItem(title, "تاریخ سرویس نامعتبر", false, true, iconRes)
                    }
                    if (cleanDueDate < today) {
                        return VehicleHealthCheckItem(title, "سررسید گذشته", false, true, iconRes)
                    }
                    if (cleanDueDate <= today) dueSoon = true
                } else if (nextKm == null) {
                    // A record without a defined interval cannot be presented as confirmed healthy.
                    dueSoon = true
                }
            }

            return if (dueSoon) {
                VehicleHealthCheckItem(title, "نیاز به بررسی موعد", false, true, iconRes)
            } else {
                VehicleHealthCheckItem(title, "در موعد سرویس", true, false, iconRes)
            }
        }

        fun insuranceStatus(title: String, typeMatch: (String) -> Boolean): VehicleHealthCheckItem {
            val insurance = carInsurances.filter { typeMatch(it.type) }
                .maxByOrNull { parseAndValidateJalaliDate(it.startDate) ?: "" }
                ?: return VehicleHealthCheckItem(title, "ثبت نشده", false, true, R.drawable.img_3d_insurance)

            val expiry = parseAndValidateJalaliDate(insurance.endDate)
            return if (expiry != null && expiry >= today) {
                VehicleHealthCheckItem(title, "دارای اعتبار", true, false, R.drawable.img_3d_insurance)
            } else {
                VehicleHealthCheckItem(title, "فاقد اعتبار", false, true, R.drawable.img_3d_insurance)
            }
        }

        val checklist = listOf(
            insuranceStatus("بیمه‌نامه شخص ثالث") { it.contains("ثالث") },
            insuranceStatus("بیمه‌نامه بدنه") { it.contains("بدنه") },
            run {
                val inspection = carInspections.maxByOrNull { parseAndValidateJalaliDate(it.lastInspectionDate) ?: "" }
                if (inspection == null) {
                    VehicleHealthCheckItem("معاینه فنی", "ثبت نشده", false, true, R.drawable.img_3d_settings_gear)
                } else {
                    val expiry = parseAndValidateJalaliDate(inspection.expiryDate)
                    if (expiry != null && expiry >= today) {
                        VehicleHealthCheckItem("معاینه فنی", "دارای اعتبار", true, false, R.drawable.img_3d_settings_gear)
                    } else {
                        VehicleHealthCheckItem("معاینه فنی", "فاقد اعتبار", false, true, R.drawable.img_3d_settings_gear)
                    }
                }
            },
            serviceStatus(
                "روغن موتور",
                listOf(ServiceType.OIL_CHANGE),
                R.drawable.img_3d_oil,
                defaultIntervalKm = 5000
            ),
            serviceStatus(
                "فیلترها",
                listOf(ServiceType.FILTERS),
                R.drawable.img_3d_oil
            ),
            serviceStatus(
                "روغن گیربکس",
                listOf(ServiceType.TRANSMISSION_OIL_CHANGE),
                R.drawable.img_3d_oil
            ),
            serviceStatus(
                "تسمه دینام",
                listOf(ServiceType.ALTERNATOR_BELT_CHANGE),
                R.drawable.img_3d_settings_gear
            ),
            serviceStatus(
                "تسمه تایم",
                listOf(ServiceType.TIMING_BELT_CHANGE),
                R.drawable.img_3d_settings_gear
            )
        )

        val penalties = checklist.sumOf { item ->
            when {
                item.isWarning -> 15
                !item.isOk -> 15
                else -> 0
            }
        }
        val score = (100 - penalties).coerceIn(10, 100)
        val level = when {
            score >= 80 -> VehicleHealthLevel.GOOD
            score >= 55 -> VehicleHealthLevel.SERVICE_DUE
            else -> VehicleHealthLevel.WARNING
        }
        val recommendation = when {
            checklist.any { (it.title.startsWith("بیمه‌نامه") || it.title == "معاینه فنی") && !it.isOk } ->
                "اعتبار بیمه‌نامه‌ها و معاینه فنی را بررسی و موارد فاقد اعتبار را تمدید کنید."
            checklist.any { !it.isOk || it.isWarning } ->
                "سوابق سرویس و موعد تعویض قطعات را بررسی کنید؛ برای موارد ثبت‌نشده، اطلاعات سرویس را وارد کنید."
            else -> "موارد ثبت‌شده در موعد هستند؛ سرویس‌های بعدی را طبق دفترچه خودروی خود انجام دهید."
        }

        return VehicleHealthReport(
            level = level,
            healthScorePercent = score,
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

        val currentPdt = com.example.util.PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis())
        val currentYear = currentPdt.year
        val currentMonth = currentPdt.month
        val currentYearMonth = "%04d/%02d".format(currentYear, currentMonth)
        val currentYearStr = "%04d".format(currentYear)

        fun normalizeExpenseDate(rawDate: String): String? {
            return parseAndValidateJalaliDate(rawDate)
        }

        val currentMonthCost = carExpenses.filter { 
            val normalized = normalizeExpenseDate(it.date)
            normalized != null && normalized.startsWith(currentYearMonth)
        }.sumOf { it.amount }
        val yearlyCost = carExpenses.filter { 
            val normalized = normalizeExpenseDate(it.date)
            normalized != null && normalized.startsWith(currentYearStr)
        }.sumOf { it.amount }

        val totalAll = carExpenses.sumOf { it.amount }
        val categoryTotals = VehicleExpenseCategory.values().map { cat ->
            val total = carExpenses.filter { it.category == cat }.sumOf { it.amount }
            val percentage = if (totalAll > 0) ((total.toDouble() / totalAll.toDouble()) * 100).toInt() else 0
            VehicleCostCategoryStat(cat, total, percentage)
        }.filter { it.amount > 0 }.sortedByDescending { it.amount }

        val topCategory = categoryTotals.firstOrNull() ?: VehicleCostCategoryStat(VehicleExpenseCategory.SERVICE, 0L, 0)

        val lastRepair = carExpenses.filter { it.category == VehicleExpenseCategory.REPAIRS }.firstOrNull()
        val lastRepairTitle = lastRepair?.title ?: "بدون تعمیرات سنگین"
        val lastRepairDate = lastRepair?.date ?: "—"

        // Dynamic 6-Month Chart Data
        val monthNames = listOf("فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند")
        val monthlyBars = mutableListOf<VehicleMonthlyExpenseBar>()

        for (i in 5 downTo 0) {
            var m = currentMonth - i
            var y = currentYear
            while (m <= 0) {
                m += 12
                y -= 1
            }
            val prefix = "%04d/%02d".format(y, m)
            val monthExpenses = carExpenses.filter { 
                val normalized = normalizeExpenseDate(it.date)
                normalized != null && normalized.startsWith(prefix)
            }
            val fuel = monthExpenses.filter { it.category == VehicleExpenseCategory.FUEL }.sumOf { it.amount }
            val repair = monthExpenses.filter { it.category == VehicleExpenseCategory.REPAIRS || it.category == VehicleExpenseCategory.PARTS }.sumOf { it.amount }
            val service = monthExpenses.filter { it.category == VehicleExpenseCategory.SERVICE }.sumOf { it.amount }
            val insurance = monthExpenses.filter { it.category == VehicleExpenseCategory.INSURANCE }.sumOf { it.amount }
            val total = monthExpenses.sumOf { it.amount }
            val name = monthNames.getOrElse(m - 1) { "ماه $m" }

            monthlyBars.add(
                VehicleMonthlyExpenseBar(
                    monthName = name,
                    fuelAmount = fuel,
                    repairAmount = repair,
                    serviceAmount = service,
                    insuranceAmount = insurance,
                    totalAmount = total,
                    isCurrentMonth = (i == 0)
                )
            )
        }

        return VehicleStatistics(
            currentMonthCost = currentMonthCost,
            yearlyCost = yearlyCost,
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
        insurances: List<VehicleInsuranceEntity>,
        inspections: List<VehicleInspectionEntity> = emptyList()
    ): List<VehicleTimelineEvent> {
        val timeline = mutableListOf<VehicleTimelineEvent>()

        services.filter { it.vehicleId == vehicleId }.forEach { s ->
            timeline.add(
                VehicleTimelineEvent.ServiceEvent(
                    id = s.id,
                    date = s.date,
                    title = s.title,
                    amount = s.cost,
                    subtitle = "سرویس در کیلومتر ${s.mileage}" + s.description.takeIf { it.isNotBlank() }?.let { " - $it" }.orEmpty(),
                    iconRes = s.serviceType.iconRes,
                    colorHex = 0xFF10B981,
                    mileage = s.mileage
                )
            )
        }

        // Service, insurance, and inspection costs are already represented by their
        // dedicated timeline events. Keep them in expense totals/reports, but do not
        // render their linked expense rows as a second timeline event.
        expenses.filter {
            it.vehicleId == vehicleId &&
                it.category != VehicleExpenseCategory.SERVICE &&
                it.category != VehicleExpenseCategory.INSURANCE &&
                it.category != VehicleExpenseCategory.INSPECTION
        }.forEach { e ->
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
                    subtitle = "اعتبار تا ${ins.endDate} - شماره بیمه‌نامه: ${ins.policyNumber.ifBlank { "ثبت نشده" }}",
                    iconRes = R.drawable.img_3d_insurance,
                    colorHex = 0xFFF59E0B,
                    company = ins.company
                )
            )
        }

        inspections.filter { it.vehicleId == vehicleId }.forEach { inspection ->
            timeline.add(
                VehicleTimelineEvent.InspectionEvent(
                    id = "inspection_${inspection.id}",
                    date = inspection.lastInspectionDate,
                    title = "معاینه فنی خودرو",
                    amount = inspection.cost,
                    subtitle = "اعتبار تا ${inspection.expiryDate} - ${inspection.centerName.ifBlank { "مرکز ثبت نشده" }} - ${inspection.status.ifBlank { "وضعیت ثبت نشده" }}",
                    iconRes = R.drawable.img_3d_settings_gear,
                    colorHex = 0xFF0D9488,
                    expiryDate = inspection.expiryDate
                )
            )
        }

        // Normalize and validate Jalali dates before chronological sorting.
        return timeline.sortedByDescending { parseAndValidateJalaliDate(it.date) ?: "0000/00/00" }
    }
}
