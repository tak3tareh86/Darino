package com.example.ui.screens.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.NotificationRepository
import com.example.ui.screens.finance.data.LocalFinanceRepository
import com.example.ui.screens.finance.model.TransactionType
import com.example.ui.screens.installments.data.LocalInstallmentRepository
import com.example.vehicle.data.VehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.example.util.PersianCalendarHelper
import com.example.util.MoneyFormatter

data class ReportsState(
    val totalIncome: Long = 0,
    val totalExpense: Long = 0,
    val savings: Long = 0,
    val savingsPercent: Int = 0,
    
    // Comparison
    val currentPeriodExpense: Long = 0,
    val previousPeriodExpense: Long = 0,
    val expenseChangePercent: Double = 0.0,
    val isExpenseIncreased: Boolean = false,
    
    // Category Analysis
    val topExpenseCategory: String = "نامشخص",
    val topExpenseCategoryAmount: Long = 0,
    val categoryBreakdown: Map<String, Long> = emptyMap(),
    
    // Installments
    val totalInstallmentAmount: Long = 0,
    val paidInstallmentAmount: Long = 0,
    val activeInstallmentsCount: Int = 0,
    
    // Vehicle
    val vehicleExpenseTotal: Long = 0,
    val expenseRatio: Float = 0f,
    
    // Active Filters (Canonical)
    val selectedCategory: ReportCategory = ReportCategory.FINANCIAL,
    val selectedPeriod: ReportPeriod = ReportPeriod.MONTH,
    val customStartDate: ShamsiDate? = null,
    val customEndDate: ShamsiDate? = null,
    
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val smartAdvice: String = ""
)

class ReportsViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ReportsState())
    val uiState: StateFlow<ReportsState> = _uiState.asStateFlow()

    private val financeRepo = LocalFinanceRepository.instance
    private val installmentRepo = LocalInstallmentRepository.instance
    private val vehicleRepo = VehicleRepository.instance

    private val _selectedCategory = MutableStateFlow(ReportCategory.FINANCIAL)
    private val _selectedPeriod = MutableStateFlow(ReportPeriod.MONTH)
    
    private val now = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis())
    private val _customStartDate = MutableStateFlow(ShamsiDate(now.year, now.month, 1))
    private val _customEndDate = MutableStateFlow(ShamsiDate(now.year, now.month, PersianCalendarHelper.getDaysInMonth(now.year, now.month)))

    init {
        // Initialize repos if needed
        financeRepo.init(application)
        
        viewModelScope.launch {
            combine(
                financeRepo.getTransactions(),
                installmentRepo.installments,
                vehicleRepo.expenses,
                vehicleRepo.services,
                vehicleRepo.insurances,
                vehicleRepo.inspections,
                _selectedCategory,
                _selectedPeriod,
                _customStartDate,
                _customEndDate
            ) { array ->
                @Suppress("UNCHECKED_CAST")
                calculateState(
                    allTransactions = array[0] as List<com.example.ui.screens.finance.model.TransactionItemData>,
                    allInstallments = array[1] as List<com.example.ui.screens.installments.model.InstallmentItem>,
                    vExpenses = array[2] as List<com.example.vehicle.data.VehicleExpenseEntity>,
                    vServices = array[3] as List<com.example.vehicle.data.VehicleServiceEntity>,
                    vInsurances = array[4] as List<com.example.vehicle.data.VehicleInsuranceEntity>,
                    vInspections = array[5] as List<com.example.vehicle.data.VehicleInspectionEntity>,
                    category = array[6] as ReportCategory,
                    period = array[7] as ReportPeriod,
                    customStart = array[8] as ShamsiDate,
                    customEnd = array[9] as ShamsiDate
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    private fun calculateState(
        allTransactions: List<com.example.ui.screens.finance.model.TransactionItemData>,
        allInstallments: List<com.example.ui.screens.installments.model.InstallmentItem>,
        vExpenses: List<com.example.vehicle.data.VehicleExpenseEntity>,
        vServices: List<com.example.vehicle.data.VehicleServiceEntity>,
        vInsurances: List<com.example.vehicle.data.VehicleInsuranceEntity>,
        vInspections: List<com.example.vehicle.data.VehicleInspectionEntity>,
        category: ReportCategory,
        period: ReportPeriod,
        customStart: ShamsiDate,
        customEnd: ShamsiDate
    ): ReportsState {
        val (startTime, endTime) = getPeriodBounds(period, customStart, customEnd)
        val (prevStartTime, prevEndTime) = getPreviousPeriodBounds(period, startTime, endTime)

        // 1. Financial Transactions
        val periodTransactions = allTransactions.filter { it.dateMillis in startTime..endTime }
        
        val filteredTransactions = when (category) {
            ReportCategory.FINANCIAL -> periodTransactions
            ReportCategory.VEHICLE -> periodTransactions.filter { 
                it.sourceType == com.example.ui.screens.finance.model.TransactionSourceType.VEHICLE ||
                it.category.id == "vehicle" || it.category.id == "fuel"
            }
            ReportCategory.INSTALLMENTS -> periodTransactions.filter { 
                it.category.id == "installment" || it.category.id == "loan"
            }
            ReportCategory.SMART_ANALYSIS -> periodTransactions
        }

        val income = filteredTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = filteredTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val savings = if (category == ReportCategory.FINANCIAL) income - expense else 0 
        val savingsPercent = if (category == ReportCategory.FINANCIAL && income > 0) ((savings.toDouble() / income.toDouble()) * 100).toInt() else 0

        // 2. Comparison
        val currentCatExpense = filteredTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val prevTransactions = allTransactions.filter { it.dateMillis in prevStartTime..prevEndTime }
        val prevFilteredTransactions = when (category) {
            ReportCategory.FINANCIAL -> prevTransactions
            ReportCategory.VEHICLE -> prevTransactions.filter { 
                it.sourceType == com.example.ui.screens.finance.model.TransactionSourceType.VEHICLE ||
                it.category.id == "vehicle" || it.category.id == "fuel"
            }
            ReportCategory.INSTALLMENTS -> prevTransactions.filter { 
                it.category.id == "installment" || it.category.id == "loan"
            }
            ReportCategory.SMART_ANALYSIS -> prevTransactions
        }
        val prevExpense = prevFilteredTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val expenseDiff = currentCatExpense - prevExpense
        val expenseChangePercent = if (prevExpense > 0) (Math.abs(expenseDiff).toDouble() / prevExpense.toDouble()) * 100 else 0.0

        // 3. Category Analysis
        val categoryGroups = filteredTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category.title }
            .mapValues { it.value.sumOf { it.amount } }
        val topCategory = categoryGroups.maxByOrNull { it.value }

        // 4. Installments Report (Period-aware for amounts, global for count)
        var totalInstAmountInPeriod = 0L
        var paidInstAmountInPeriod = 0L
        
        allInstallments.forEach { installment ->
            val paymentsInPeriod = installment.paymentHistory.filter { payment ->
                val dueMillis = parsePersianDateToMillis(payment.dueDate)
                dueMillis in startTime..endTime
            }
            
            if (paymentsInPeriod.isNotEmpty()) {
                totalInstAmountInPeriod += paymentsInPeriod.sumOf { it.amount }
                paidInstAmountInPeriod += paymentsInPeriod.filter { it.status == com.example.ui.screens.installments.model.InstallmentStatus.PAID || it.status == com.example.ui.screens.installments.model.InstallmentStatus.COMPLETED }.sumOf { it.amount }
            }
        }
        
        val activeInstallmentsCount = allInstallments.count { it.status != com.example.ui.screens.installments.model.InstallmentStatus.COMPLETED }

        // 5. Vehicle Report (Canonical data from VehicleRepository)
        fun calculateVehicleSum(start: Long, end: Long): Long {
            val vExp = vExpenses.filter { parsePersianDateToMillis(it.date) in start..end }.sumOf { it.amount }
            val vSer = vServices.filter { parsePersianDateToMillis(it.date) in start..end }.sumOf { it.cost }
            val vInsp = vInspections.filter { parsePersianDateToMillis(it.lastInspectionDate) in start..end }.sumOf { it.cost }
            val vIns = vInsurances.filter { parsePersianDateToMillis(it.startDate) in start..end }.sumOf { it.amount }
            return vExp + vSer + vInsp + vIns
        }

        val totalVehicleExpense = calculateVehicleSum(startTime, endTime)
        val prevVehicleExpense = calculateVehicleSum(prevStartTime, prevEndTime)

        // 6. Final Metric Overrides for Category Consistency
        var finalIncome = income
        var finalExpense = expense
        var currentCompExpense = currentCatExpense
        var previousCompExpense = prevExpense

        if (category == ReportCategory.VEHICLE) {
            finalIncome = 0 // Vehicle category is expense-focused
            finalExpense = totalVehicleExpense
            currentCompExpense = totalVehicleExpense
            previousCompExpense = prevVehicleExpense
        }

        val smartAdvice = when {
            category == ReportCategory.SMART_ANALYSIS && savings < 0 -> "هزینه‌های شما در این دوره بیشتر از درآمد بوده است. پیشنهاد می‌شود هزینه‌های غیرضروری را کاهش دهید."
            category == ReportCategory.SMART_ANALYSIS && savingsPercent > 30 -> "وضعیت پس‌انداز شما عالی است! بیش از ۳۰٪ درآمد خود را ذخیره کرده‌اید."
            category == ReportCategory.SMART_ANALYSIS && topCategory != null && expense > 0 && (topCategory.value.toDouble() / expense.toDouble()) > 0.5 -> "بیش از نیمی از هزینه‌های شما صرف ${topCategory.key} شده است. بررسی کنید آیا امکان کاهش در این بخش وجود دارد؟"
            category == ReportCategory.INSTALLMENTS && totalInstAmountInPeriod > 0 -> "شما $activeInstallmentsCount قسط فعال دارید. مجموع تعهدات در این بازه: ${MoneyFormatter.formatToman(totalInstAmountInPeriod)}."
            category == ReportCategory.VEHICLE && totalVehicleExpense > 1000000 -> "هزینه‌های خودروی شما در این دوره قابل توجه بوده است. سرویس‌های دوره‌ای را برای جلوگیری از خرابی‌های سنگین چک کنید."
            else -> "تراکنش‌های شما با موفقیت ثبت و تحلیل شد."
        }

        val expenseRatio = if (category == ReportCategory.FINANCIAL && finalIncome > 0) {
            (finalExpense.toDouble() / finalIncome.toDouble()).coerceIn(0.0, 1.0).toFloat()
        } else if (category == ReportCategory.FINANCIAL && finalExpense > 0) 1.0f else 0.0f

        val finalExpenseDiff = currentCompExpense - previousCompExpense
        val finalExpenseChangePercent = if (previousCompExpense > 0) (Math.abs(finalExpenseDiff).toDouble() / previousCompExpense.toDouble()) * 100 else 0.0

        return ReportsState(
            totalIncome = finalIncome,
            totalExpense = finalExpense,
            savings = savings,
            savingsPercent = savingsPercent,
            currentPeriodExpense = currentCompExpense,
            previousPeriodExpense = previousCompExpense,
            expenseChangePercent = finalExpenseChangePercent,
            isExpenseIncreased = finalExpenseDiff > 0,
            topExpenseCategory = topCategory?.key ?: "نامشخص",
            topExpenseCategoryAmount = topCategory?.value ?: 0,
            categoryBreakdown = categoryGroups,
            totalInstallmentAmount = totalInstAmountInPeriod,
            paidInstallmentAmount = paidInstAmountInPeriod,
            activeInstallmentsCount = activeInstallmentsCount,
            vehicleExpenseTotal = totalVehicleExpense,
            expenseRatio = expenseRatio,
            selectedCategory = category,
            selectedPeriod = period,
            customStartDate = customStart,
            customEndDate = customEnd,
            smartAdvice = smartAdvice
        )
    }

    private fun parsePersianDateToMillis(persianDate: String): Long {
        if (persianDate.isBlank()) return 0L
        val cleanDate = com.example.util.IranianPhoneUtils.convertDigitsToEnglish(persianDate)
        val parts = cleanDate.split("/")
        if (parts.size != 3) return 0L
        return try {
            val y = parts[0].toInt()
            val m = parts[1].toInt()
            val d = parts[2].toInt()
            PersianCalendarHelper.jalaliToEpochMillis(y, m, d, 9, 0)
        } catch (e: Exception) {
            0L
        }
    }

    private fun getPeriodBounds(period: ReportPeriod, customStart: ShamsiDate, customEnd: ShamsiDate): Pair<Long, Long> {
        val cal = java.util.Calendar.getInstance()
        val nowMillis = System.currentTimeMillis()
        val currentPersian = PersianCalendarHelper.fromEpochMillis(nowMillis)
        
        return when (period) {
            ReportPeriod.WEEK -> {
                val start = nowMillis - (7 * 24 * 60 * 60 * 1000L)
                Pair(start, nowMillis)
            }
            ReportPeriod.MONTH -> {
                val start = PersianCalendarHelper.jalaliToEpochMillis(currentPersian.year, currentPersian.month, 1, 0, 0)
                Pair(start, nowMillis)
            }
            ReportPeriod.YEAR -> {
                val start = PersianCalendarHelper.jalaliToEpochMillis(currentPersian.year, 1, 1, 0, 0)
                Pair(start, nowMillis)
            }
            ReportPeriod.CUSTOM -> {
                val start = PersianCalendarHelper.jalaliToEpochMillis(customStart.year, customStart.month, customStart.day, 0, 0)
                val end = PersianCalendarHelper.jalaliToEpochMillis(customEnd.year, customEnd.month, customEnd.day, 23, 59)
                Pair(start, end)
            }
        }
    }

    private fun getPreviousPeriodBounds(period: ReportPeriod, startTime: Long, endTime: Long): Pair<Long, Long> {
        return when (period) {
            ReportPeriod.WEEK -> Pair(startTime - (7 * 24 * 60 * 60 * 1000L), startTime)
            ReportPeriod.MONTH -> {
                val startPersian = PersianCalendarHelper.fromEpochMillis(startTime)
                var prevYear = startPersian.year
                var prevMonth = startPersian.month - 1
                if (prevMonth == 0) {
                    prevMonth = 12
                    prevYear -= 1
                }
                val prevStart = PersianCalendarHelper.jalaliToEpochMillis(prevYear, prevMonth, 1, 0, 0)
                val prevEnd = startTime - 1
                Pair(prevStart, prevEnd)
            }
            ReportPeriod.YEAR -> {
                val startPersian = PersianCalendarHelper.fromEpochMillis(startTime)
                val prevStart = PersianCalendarHelper.jalaliToEpochMillis(startPersian.year - 1, 1, 1, 0, 0)
                val prevEnd = startTime - 1
                Pair(prevStart, prevEnd)
            }
            ReportPeriod.CUSTOM -> {
                val duration = endTime - startTime
                Pair(startTime - duration, startTime)
            }
        }
    }

    fun setPeriod(period: ReportPeriod) {
        _selectedPeriod.value = period
    }

    fun setCategory(category: ReportCategory) {
        _selectedCategory.value = category
    }

    fun setCustomRange(start: ShamsiDate, end: ShamsiDate) {
        _customStartDate.value = start
        _customEndDate.value = end
        _selectedPeriod.value = ReportPeriod.CUSTOM
    }
}
