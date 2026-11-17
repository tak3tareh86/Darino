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
import com.example.util.MoneyAmount
import java.math.BigInteger
import kotlin.math.roundToInt
import com.example.ui.screens.settings.model.AppCurrency

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

    private fun normalizeServiceSearchText(value: String): String =
        value.replace('ي', 'ی')
            .replace('ك', 'ک')
            .replace(Regex("[\\s\\u200C\\u200D]+"), "")

    /**
     * Finds the newest record that represents a service, including records written by
     * older app versions where several selected services were stored in one row.
     * Exact typed records and legacy combined rows are compared together so a newer
     * combined record is not hidden by an older exact-type record.
     */
    internal fun findLatestServiceForType(
        services: List<VehicleServiceEntity>,
        type: ServiceType
    ): VehicleServiceEntity? {
        val aliases = when (type) {
            ServiceType.OIL_FILTER_CHANGE -> listOf(type.title, "فیلتر روغن")
            ServiceType.AIR_FILTER_CHANGE -> listOf(type.title, "فیلتر هوا")
            else -> listOf(type.title)
        }.map(::normalizeServiceSearchText)

        val candidates = services.filter { record ->
            when {
                record.serviceType == type -> true
                type == ServiceType.OIL_FILTER_CHANGE || type == ServiceType.AIR_FILTER_CHANGE -> {
                    // A generic FILTERS record does not prove which filter was replaced.
                    // Accept legacy rows only when their title/description names this exact filter.
                    aliases.any { alias ->
                        val searchableText = normalizeServiceSearchText(
                            record.title + " " + record.description
                        )
                        searchableText.contains(alias)
                    }
                }
                else -> aliases.any { alias ->
                    normalizeServiceSearchText(record.title + " " + record.description).contains(alias)
                }
            }
        }.distinctBy { it.id }

        return candidates.maxByOrNull { parseAndValidateJalaliDate(it.date) ?: "" }
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
            findLatestServiceForType(carServices, type)

        fun serviceStatus(
            title: String,
            types: List<ServiceType>,
            iconRes: Int
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
            var hasUnconfiguredPendingDue = false
            var hasPendingService = false
            var hasConfiguredDue = false
            for ((type, record) in records) {
                val isCompleted = record.status.equals("COMPLETED", ignoreCase = true)
                if (!isCompleted) hasPendingService = true

                // Explicit next due values remain meaningful even after the performed
                // service is marked completed; only the fabricated default interval is removed.
                // Reminder values on legacy combined/generic rows belong to their primary
                // service only; do not incorrectly apply them to every service named in title.
                val hasTypeSpecificRecord = record.serviceType == type
                val nextKm = if (hasTypeSpecificRecord) record.nextReminderMileage else null
                val nextDate = if (hasTypeSpecificRecord) record.nextReminderDate else null
                if (nextKm != null || !nextDate.isNullOrBlank()) hasConfiguredDue = true
                if (nextKm == null && nextDate.isNullOrBlank() && !isCompleted) {
                    hasUnconfiguredPendingDue = true
                }

                if (nextKm != null) {
                    val remaining = nextKm - vehicle.currentMileage
                    if (remaining < 0) {
                        return VehicleHealthCheckItem(title, "سررسید گذشته", false, true, iconRes)
                    }
                    if (remaining <= 500) dueSoon = true
                }

                if (!nextDate.isNullOrBlank()) {
                    val cleanDueDate = parseAndValidateJalaliDate(nextDate)
                    if (cleanDueDate == null) {
                        return VehicleHealthCheckItem(title, "تاریخ سرویس نامعتبر", false, true, iconRes)
                    }
                    if (cleanDueDate < today) {
                        return VehicleHealthCheckItem(title, "سررسید گذشته", false, true, iconRes)
                    }
                    if (cleanDueDate <= today) dueSoon = true
                }
            }

            return when {
                dueSoon -> VehicleHealthCheckItem(title, "نیاز به بررسی موعد", false, true, iconRes)
                hasUnconfiguredPendingDue -> VehicleHealthCheckItem(title, "موعد بعدی مشخص نشده", false, true, iconRes)
                !hasPendingService && !hasConfiguredDue -> VehicleHealthCheckItem(title, "سرویس انجام شده", true, false, iconRes)
                else -> VehicleHealthCheckItem(title, "در موعد سرویس", true, false, iconRes)
            }
        }

        fun insuranceStatus(title: String, iconRes: Int, typeMatch: (String) -> Boolean): VehicleHealthCheckItem {
            val matchingInsurances = carInsurances.filter { typeMatch(it.type) }
            if (matchingInsurances.isEmpty()) {
                return VehicleHealthCheckItem(title, "ثبت نشده", false, true, iconRes)
            }

            // Prefer a policy that covers today; a future-dated renewal must not
            // incorrectly replace an already-active policy in the health checklist.
            val activeInsurance = matchingInsurances
                .mapNotNull { insurance ->
                    val start = parseAndValidateJalaliDate(insurance.startDate)
                    val expiry = parseAndValidateJalaliDate(insurance.endDate)
                    if (start != null && expiry != null && start <= today && expiry >= today) {
                        insurance to start
                    } else {
                        null
                    }
                }
                .maxByOrNull { it.second }
                ?.first
            val insurance = activeInsurance ?: matchingInsurances.maxByOrNull {
                parseAndValidateJalaliDate(it.startDate) ?: ""
            }!!

            val start = parseAndValidateJalaliDate(insurance.startDate)
            val expiry = parseAndValidateJalaliDate(insurance.endDate)
            return when {
                start == null || expiry == null || expiry < start ->
                    VehicleHealthCheckItem(title, "تاریخ نامعتبر یا ثبت نشده", false, true, iconRes)
                start > today ->
                    VehicleHealthCheckItem(title, "هنوز شروع نشده", false, true, iconRes)
                expiry < today ->
                    VehicleHealthCheckItem(title, "منقضی شده", false, true, iconRes)
                else ->
                    VehicleHealthCheckItem(title, "دارای اعتبار", true, false, iconRes)
            }
        }

        val fixedChecklist = listOf(
            insuranceStatus("بیمه‌نامه شخص ثالث", R.drawable.sales) { it.contains("ثالث") },
            insuranceStatus("بیمه‌نامه بدنه", R.drawable.badaneh) { it.contains("بدنه") },
            run {
                val inspection = carInspections.maxByOrNull { parseAndValidateJalaliDate(it.lastInspectionDate) ?: "" }
                if (inspection == null) {
                    VehicleHealthCheckItem("معاینه فنی", "ثبت نشده", false, true, R.drawable.fani)
                } else {
                    val lastInspection = parseAndValidateJalaliDate(inspection.lastInspectionDate)
                    val expiry = parseAndValidateJalaliDate(inspection.expiryDate)
                    val statusText = inspection.status.trim()
                    val statusApproved = statusText.isBlank() ||
                        statusText.contains("معتبر") ||
                        statusText.contains("قبول") ||
                        statusText.contains("تایید") ||
                        statusText.contains("تأیید")
                    when {
                        lastInspection == null || expiry == null || expiry < lastInspection ->
                            VehicleHealthCheckItem("معاینه فنی", "تاریخ نامعتبر یا ثبت نشده", false, true, R.drawable.fani)
                        lastInspection > today ->
                            VehicleHealthCheckItem("معاینه فنی", "تاریخ معاینه در آینده است", false, true, R.drawable.fani)
                        expiry < today ->
                            VehicleHealthCheckItem("معاینه فنی", "معاینه فنی منقضی شده", false, true, R.drawable.fani)
                        !statusApproved ->
                            VehicleHealthCheckItem("معاینه فنی", statusText, false, true, R.drawable.fani)
                        else ->
                            VehicleHealthCheckItem("معاینه فنی", "دارای اعتبار", true, false, R.drawable.fani)
                    }
                }
            },
            serviceStatus("روغن موتور", listOf(ServiceType.OIL_CHANGE), R.drawable.motor),
            serviceStatus("فیلتر روغن", listOf(ServiceType.OIL_FILTER_CHANGE), R.drawable.filter),
            serviceStatus("فیلتر هوا", listOf(ServiceType.AIR_FILTER_CHANGE), R.drawable.filter),
            serviceStatus("روغن گیربکس", listOf(ServiceType.TRANSMISSION_OIL_CHANGE), R.drawable.girbox),
            serviceStatus("تسمه دینام", listOf(ServiceType.ALTERNATOR_BELT_CHANGE), R.drawable.dinam),
            serviceStatus("تسمه تایم", listOf(ServiceType.TIMING_BELT_CHANGE), R.drawable.time)
        )

        // The vehicle health checklist is intentionally limited to these nine fixed items.
        // Other recorded service types remain available in service history, not in this checklist.
        val checklist = fixedChecklist

        // Each of the nine checklist items contributes equally to the score.
        // A completed service with no configured next reminder remains completed until
        // an explicit due date or mileage indicates that attention is needed.
        val score = if (fixedChecklist.isEmpty()) {
            0
        } else {
            (fixedChecklist.count { it.isOk } * 100f / fixedChecklist.size).roundToInt()
        }
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
        }.fold(0L) { total, expense -> Math.addExact(total, MoneyAmount.of(expense.amount, expense.amountUnit).asRialsLongExact()) }
        val yearlyCost = carExpenses.filter { 
            val normalized = normalizeExpenseDate(it.date)
            normalized != null && normalized.startsWith(currentYearStr)
        }.fold(0L) { total, expense -> Math.addExact(total, MoneyAmount.of(expense.amount, expense.amountUnit).asRialsLongExact()) }

        val totalAll = carExpenses.fold(0L) { total, expense -> Math.addExact(total, MoneyAmount.of(expense.amount, expense.amountUnit).asRialsLongExact()) }
        val categoryTotals = VehicleExpenseCategory.values().map { cat ->
            val total = carExpenses.filter { it.category == cat }.fold(0L) { total, expense -> Math.addExact(total, MoneyAmount.of(expense.amount, expense.amountUnit).asRialsLongExact()) }
            val percentage = if (totalAll > 0L) BigInteger.valueOf(total).multiply(BigInteger.valueOf(100L)).divide(BigInteger.valueOf(totalAll)).toInt().coerceIn(0, 100) else 0
            VehicleCostCategoryStat(cat, total, percentage)
        }.filter { it.amount > 0 }.sortedByDescending { it.amount }

        val topCategory = categoryTotals.firstOrNull() ?: VehicleCostCategoryStat(VehicleExpenseCategory.SERVICE, 0L, 0)

        // Expense flows are ordered by their stored date string, which is not a
        // reliable chronological order when legacy rows use mixed date formats.
        // Select the newest repair using a validated, normalized Jalali date instead.
        val lastRepair = carExpenses
            .asSequence()
            .filter { it.category == VehicleExpenseCategory.REPAIRS }
            .mapNotNull { expense ->
                parseAndValidateJalaliDate(expense.date)?.let { normalizedDate -> normalizedDate to expense }
            }
            .maxByOrNull { it.first }
            ?.second
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
            val fuel = monthExpenses.filter { it.category == VehicleExpenseCategory.FUEL }.fold(0L) { total, expense -> Math.addExact(total, MoneyAmount.of(expense.amount, expense.amountUnit).asRialsLongExact()) }
            val repair = monthExpenses.filter { it.category == VehicleExpenseCategory.REPAIRS || it.category == VehicleExpenseCategory.PARTS }.fold(0L) { total, expense -> Math.addExact(total, MoneyAmount.of(expense.amount, expense.amountUnit).asRialsLongExact()) }
            val service = monthExpenses.filter { it.category == VehicleExpenseCategory.SERVICE }.fold(0L) { total, expense -> Math.addExact(total, MoneyAmount.of(expense.amount, expense.amountUnit).asRialsLongExact()) }
            val insurance = monthExpenses.filter { it.category == VehicleExpenseCategory.INSURANCE }.fold(0L) { total, expense -> Math.addExact(total, MoneyAmount.of(expense.amount, expense.amountUnit).asRialsLongExact()) }
            val total = monthExpenses.fold(0L) { total, expense -> Math.addExact(total, MoneyAmount.of(expense.amount, expense.amountUnit).asRialsLongExact()) }
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
            totalServicesCount = carServices.count { it.status.equals("COMPLETED", ignoreCase = true) },
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
                    mileage = s.mileage,
                    amountUnit = s.costUnit
                )
            )
        }

        // Hide only the deterministic expense rows generated for these records.
        // Category-only filtering incorrectly hid independent expenses that users
        // manually recorded under SERVICE, INSURANCE, or INSPECTION.
        val linkedExpenseIds = buildSet {
            services.filter { it.vehicleId == vehicleId }.forEach { add("exp_svc_${it.id}") }
            insurances.filter { it.vehicleId == vehicleId }.forEach { add("exp_ins_${it.id}") }
            inspections.filter { it.vehicleId == vehicleId }.forEach { add("exp_insp_${it.id}") }
        }
        expenses.filter { it.vehicleId == vehicleId && it.id !in linkedExpenseIds }.forEach { e ->
            timeline.add(
                VehicleTimelineEvent.ExpenseEvent(
                    id = e.id,
                    date = e.date,
                    title = e.title,
                    amount = e.amount,
                    subtitle = "${e.category.title} - ${e.description.ifEmpty { "هزینه جاری خودرو" }}",
                    iconRes = e.category.iconRes,
                    colorHex = e.category.colorHex,
                    category = e.category,
                    amountUnit = e.amountUnit
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
                    iconRes = R.drawable.img_health_third_party,
                    colorHex = 0xFFF59E0B,
                    company = ins.company,
                    amountUnit = ins.amountUnit
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
                    expiryDate = inspection.expiryDate,
                    amountUnit = inspection.costUnit
                )
            )
        }

        // Normalize and validate Jalali dates before chronological sorting.
        return timeline.sortedByDescending { parseAndValidateJalaliDate(it.date) ?: "0000/00/00" }
    }
}
