package com.example.ui.screens.installments

import com.example.util.IranianPhoneUtils
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BuildCircle
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loan.presentation.LoanCalculatorScreen
import com.example.loan.presentation.components.LoanCalculatorCard
import com.example.ui.screens.installments.OverdueInstallmentsScreen
import com.example.ui.screens.installments.category.BankLoansScreen
import com.example.ui.screens.installments.category.CarInsuranceInstallmentsScreen
import com.example.ui.screens.installments.category.HomeLoansScreen
import com.example.ui.screens.installments.category.MiscInstallmentsScreen
import com.example.ui.screens.installments.components.AddInstallmentSheet
import com.example.ui.screens.installments.components.InstallmentCategoryGrid
import com.example.ui.screens.installments.components.InstallmentFilterSheet
import com.example.ui.screens.installments.components.InstallmentQuickActions
import com.example.ui.screens.installments.components.InstallmentSearchSheet
import com.example.ui.screens.installments.components.InstallmentsEmptyState
import com.example.ui.screens.installments.components.InstallmentsHeader
import com.example.ui.screens.installments.components.InstallmentsSummaryCard
import com.example.ui.screens.installments.components.OverdueInstallmentsSection
import com.example.ui.screens.installments.components.UpcomingInstallmentsSection
import com.example.ui.screens.installments.detail.InstallmentDetailScreen
import com.example.ui.screens.installments.detail.InstallmentScheduleScreen
import com.example.ui.screens.installments.data.LocalInstallmentRepository
import com.example.ui.screens.installments.model.InstallmentCategory
import com.example.ui.screens.installments.model.InstallmentItem
import com.example.ui.screens.installments.model.InstallmentStatus
import com.example.ui.screens.installments.model.PaymentHistoryItem
import com.example.util.PersianCalendarHelper
import kotlinx.coroutines.launch

import com.example.financial_health.presentation.components.FinancialHealthCard
import com.example.financial_health.presentation.FinancialHealthScreen
import com.example.financial_health.viewmodel.FinancialHealthViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState

private sealed class InstallmentNavigationState {
    data object Dashboard : InstallmentNavigationState()
    data object LoanCalculator : InstallmentNavigationState()
    data object FinancialHealth : InstallmentNavigationState()
    data object OverdueView : InstallmentNavigationState()
    data object GroupedLoansView : InstallmentNavigationState()
    data class CategoryView(val category: InstallmentCategory) : InstallmentNavigationState()
    data class DetailView(val item: InstallmentItem) : InstallmentNavigationState()
    data class ScheduleView(val item: InstallmentItem) : InstallmentNavigationState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallmentsScreen(
    bottomBar: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val navBackStack = remember { mutableStateListOf<InstallmentNavigationState>(InstallmentNavigationState.Dashboard) }
    val currentNavState = navBackStack.lastOrNull() ?: InstallmentNavigationState.Dashboard

    fun navigateTo(destination: InstallmentNavigationState) {
        navBackStack.add(destination)
    }

    fun navigateBack() {
        if (navBackStack.size > 1) {
            navBackStack.removeAt(navBackStack.lastIndex)
        }
    }

    BackHandler(enabled = navBackStack.size > 1) {
        navigateBack()
    }

    // Repository State
    val repo = LocalInstallmentRepository.instance
    val bankLoansList by repo.bankLoans.collectAsState()
    val homeLoansList by repo.homeLoans.collectAsState()
    val carInsuranceList by repo.carInsurance.collectAsState()
    val miscInstallmentsList by repo.miscInstallments.collectAsState()
    val summaryData by repo.summary.collectAsState()
    val categorySummariesData by repo.categorySummaries.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val allInstallments = remember(bankLoansList, homeLoansList, carInsuranceList, miscInstallmentsList) {
        bankLoansList + homeLoansList + carInsuranceList + miscInstallmentsList
    }

    var currentEpochDay by remember {
        mutableStateOf(
            PersianCalendarHelper.getJalaliEpochDay(
                PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).year,
                PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).month,
                PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).day
            )
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60000L)
            val nowDt = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis())
            val todayEpoch = PersianCalendarHelper.getJalaliEpochDay(nowDt.year, nowDt.month, nowDt.day)
            if (todayEpoch != currentEpochDay) {
                currentEpochDay = todayEpoch
            }
        }
    }

    val overdueList = remember(allInstallments, currentEpochDay) {
        allInstallments.filter { item ->
            if (item.status == InstallmentStatus.PAID || item.status == InstallmentStatus.COMPLETED || item.remainingInstallments == 0) {
                false
            } else {
                val triple = com.example.calendar.domain.CalendarDateUtils.parseJalali(item.nextPaymentDate)
                if (triple != null) {
                    val targetEpochDay = PersianCalendarHelper.getJalaliEpochDay(triple.first, triple.second, triple.third)
                    targetEpochDay < currentEpochDay
                } else {
                    item.status == InstallmentStatus.OVERDUE
                }
            }
        }
    }

    val upcomingList = remember(allInstallments) {
        allInstallments
            .filter { it.status == InstallmentStatus.DUE_SOON || it.status == InstallmentStatus.PENDING }
            .sortedBy { it.remainingInstallments }
            .take(3)
    }

    // Modal Sheet states
    var showAddSheet by remember { mutableStateOf(false) }
    var addInitialCategory by remember { mutableStateOf(InstallmentCategory.BANK_LOANS) }
    var prefillTitle by remember { mutableStateOf("") }
    var prefillTotal by remember { mutableStateOf("") }
    var prefillMonthly by remember { mutableStateOf("") }
    var prefillCount by remember { mutableStateOf("") }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showSearchSheet by remember { mutableStateOf(false) }

    val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val searchSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val healthViewModel: FinancialHealthViewModel = viewModel()
    val healthState by healthViewModel.uiState.collectAsState()

    Crossfade(targetState = currentNavState, label = "InstallmentNavCrossfade") { state ->
        when (state) {
            is InstallmentNavigationState.Dashboard -> {
                var isToolsExpanded by remember { mutableStateOf(false) }
                var isCategoriesExpanded by remember { mutableStateOf(true) }
                var isUpcomingExpanded by remember { mutableStateOf(true) }

                Scaffold(
                    modifier = modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            InstallmentsHeader(
                                onSearchClick = { showSearchSheet = true },
                                onFilterClick = { showFilterSheet = true }
                            )
                            if (allInstallments.isNotEmpty()) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.background
                                ) {
                                    InstallmentQuickActions(
                                        onAddInstallmentClick = {
                                            addInitialCategory = InstallmentCategory.BANK_LOANS
                                            prefillTitle = ""
                                            prefillTotal = ""
                                            prefillMonthly = ""
                                            prefillCount = ""
                                            showAddSheet = true
                                        },
                                        onLoanCalculatorClick = {
                                            navigateTo(InstallmentNavigationState.LoanCalculator)
                                        },
                                        onFinancialHealthClick = {
                                            navigateTo(InstallmentNavigationState.FinancialHealth)
                                        }
                                    )
                                }
                            }
                        }
                    },
                    bottomBar = { bottomBar?.invoke() }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (allInstallments.isEmpty()) {
                            InstallmentsEmptyState(
                                onAddClick = {
                                    addInitialCategory = InstallmentCategory.BANK_LOANS
                                    showAddSheet = true
                                }
                            )
                        } else {
                            // 1. Summary Card
                            InstallmentsSummaryCard(
                                summary = summaryData,
                                onClick = {
                                    navigateTo(InstallmentNavigationState.GroupedLoansView)
                                }
                            )

                            // 2. Compact Overdue Section (Alert if overdue exist)
                            if (overdueList.isNotEmpty()) {
                                OverdueInstallmentsSection(
                                    items = overdueList,
                                    onShowOverdueClick = {
                                        navigateTo(InstallmentNavigationState.OverdueView)
                                    }
                                )
                            }

                            // 3. Collapsible Categories Grid
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { isCategoriesExpanded = !isCategoriesExpanded }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Category,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "دسته‌بندی‌های تسهیلات و اقساط",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Icon(
                                        imageVector = if (isCategoriesExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                AnimatedVisibility(
                                    visible = isCategoriesExpanded,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    InstallmentCategoryGrid(
                                        categories = categorySummariesData,
                                        onCategoryClick = { category ->
                                            navigateTo(InstallmentNavigationState.CategoryView(category))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            is InstallmentNavigationState.CategoryView -> {
                when (state.category) {
                    InstallmentCategory.BANK_LOANS -> {
                        BankLoansScreen(
                            loans = bankLoansList,
                            onBackClick = { navigateBack() },
                            onLoanClick = { loan ->
                                navigateTo(InstallmentNavigationState.DetailView(loan))
                            },
                            onAddLoanClick = {
                                addInitialCategory = InstallmentCategory.BANK_LOANS
                                showAddSheet = true
                            }
                        )
                    }
                    InstallmentCategory.HOME_LOANS -> {
                        HomeLoansScreen(
                            loans = homeLoansList,
                            onBackClick = { navigateBack() },
                            onLoanClick = { loan ->
                                navigateTo(InstallmentNavigationState.DetailView(loan))
                            },
                            onAddLoanClick = {
                                addInitialCategory = InstallmentCategory.HOME_LOANS
                                showAddSheet = true
                            }
                        )
                    }
                    InstallmentCategory.CAR_INSURANCE -> {
                        CarInsuranceInstallmentsScreen(
                            installments = carInsuranceList,
                            onBackClick = { navigateBack() },
                            onItemClick = { item ->
                                navigateTo(InstallmentNavigationState.DetailView(item))
                            },
                            onAddClick = {
                                addInitialCategory = InstallmentCategory.CAR_INSURANCE
                                showAddSheet = true
                            }
                        )
                    }
                    InstallmentCategory.MISC -> {
                        MiscInstallmentsScreen(
                            installments = miscInstallmentsList,
                            onBackClick = { navigateBack() },
                            onItemClick = { item ->
                                navigateTo(InstallmentNavigationState.DetailView(item))
                            },
                            onAddClick = {
                                addInitialCategory = InstallmentCategory.MISC
                                showAddSheet = true
                            }
                        )
                    }
                }
            }

            is InstallmentNavigationState.DetailView -> {
                InstallmentDetailScreen(
                    item = state.item,
                    onBackClick = {
                        navigateBack()
                    },
                    onViewScheduleClick = { item ->
                        navigateTo(InstallmentNavigationState.ScheduleView(item))
                    }
                )
            }

            is InstallmentNavigationState.ScheduleView -> {
                InstallmentScheduleScreen(
                    installment = state.item,
                    onBackClick = {
                        navigateBack()
                    }
                )
            }

            is InstallmentNavigationState.OverdueView -> {
                OverdueInstallmentsScreen(
                    overdueItems = overdueList,
                    onBackClick = {
                        navigateBack()
                    },
                    onItemClick = { item ->
                        navigateTo(InstallmentNavigationState.DetailView(item))
                    },
                    onMarkAsPaid = { installmentId, paymentDate ->
                        LocalInstallmentRepository.instance.markOverdueAsPaid(installmentId, paymentDate, context)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("قسط با موفقیت در تاریخ $paymentDate به عنوان پرداخت شده ثبت شد.")
                        }
                    }
                )
            }

            is InstallmentNavigationState.LoanCalculator -> {
                LoanCalculatorScreen(
                    onBackClick = {
                        navigateBack()
                    },
                    onCreateInstallment = { cat, title, total, monthly, count ->
                        addInitialCategory = cat
                        prefillTitle = title
                        val rawTotalToman = com.example.util.IranianAmountUtils.parseAmountToLong(total)
                        val rawMonthlyToman = com.example.util.IranianAmountUtils.parseAmountToLong(monthly)
                        val displayTotal = com.example.util.MoneyFormatter.convertFromToman(rawTotalToman)
                        val displayMonthly = com.example.util.MoneyFormatter.convertFromToman(rawMonthlyToman)
                        prefillTotal = com.example.util.IranianAmountUtils.formatWithCommas(displayTotal.toString())
                        prefillMonthly = com.example.util.IranianAmountUtils.formatWithCommas(displayMonthly.toString())
                        prefillCount = count.toString()
                        showAddSheet = true
                    }
                )
            }
            is InstallmentNavigationState.GroupedLoansView -> {
                AllGroupedLoansScreen(
                    allItems = allInstallments,
                    onBackClick = { navigateBack() },
                    onLoanClick = { loan ->
                        navigateTo(InstallmentNavigationState.ScheduleView(loan))
                    }
                )
            }
            is InstallmentNavigationState.FinancialHealth -> {
                FinancialHealthScreen(
                    onBackClick = {
                        navigateBack()
                    }
                )
            }
        }
    }

    // Modal Bottom Sheets
    if (showAddSheet) {
        AddInstallmentSheet(
            sheetState = addSheetState,
            initialCategory = addInitialCategory,
            initialTitle = prefillTitle,
            initialTotalAmount = prefillTotal,
            initialInstallmentAmount = prefillMonthly,
            initialInstallmentsCount = prefillCount,
            onDismiss = {
                showAddSheet = false
                prefillTitle = ""
                prefillTotal = ""
                prefillMonthly = ""
                prefillCount = ""
            },
            onAddConfirm = { cat, title, totalStr, monthlyStr, countStr, paidCountStr, dueDateStr, provider, itemNotes, reminderEnabled, reminderDays, reminderTime, selectedOffsets, customScheduleItems ->
                prefillTitle = ""
                prefillTotal = ""
                prefillMonthly = ""
                prefillCount = ""

                val rawTotal = com.example.util.IranianAmountUtils.parseAmountToLong(totalStr)
                val rawMonthly = com.example.util.IranianAmountUtils.parseAmountToLong(monthlyStr)
                val parsedCount = com.example.util.IranianPhoneUtils.convertDigitsToEnglish(countStr).filter { it.isDigit() }.toIntOrNull() ?: 0
                val requestedPaidCount = com.example.util.IranianPhoneUtils.convertDigitsToEnglish(paidCountStr).filter { it.isDigit() }.toIntOrNull() ?: 0

                if (customScheduleItems.isNullOrEmpty()) {
                    if (rawTotal <= 0L || parsedCount <= 0 || rawMonthly <= 0L) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("مبلغ کل، مبلغ هر قسط و تعداد اقساط باید معتبر و بزرگتر از صفر باشند.")
                        }
                        return@AddInstallmentSheet
                    }
                } else {
                    if (customScheduleItems.isEmpty() || customScheduleItems.sumOf { it.amount } <= 0L) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("برنامه زمان‌بندی سفارشی وارد شده نامعتبر است.")
                        }
                        return@AddInstallmentSheet
                    }
                }

                val parsedTotal = com.example.util.MoneyFormatter.convertToToman(rawTotal)
                val parsedMonthly = com.example.util.MoneyFormatter.convertToToman(rawMonthly)

                val totalAmount = if (!customScheduleItems.isNullOrEmpty()) customScheduleItems.sumOf { it.amount } else parsedTotal
                val totalInstallments = if (!customScheduleItems.isNullOrEmpty()) customScheduleItems.size else parsedCount

                if (requestedPaidCount < 0 || requestedPaidCount > totalInstallments) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("تعداد اقساط پرداخت‌شده باید بین صفر و تعداد کل اقساط باشد.")
                    }
                    return@AddInstallmentSheet
                }
                if (customScheduleItems.isNullOrEmpty() &&
                    totalInstallments > 1 &&
                    parsedMonthly * (totalInstallments - 1L) >= totalAmount
                ) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("مبلغ هر قسط با مبلغ کل و تعداد اقساط سازگار نیست؛ لطفاً مبالغ را بررسی کنید.")
                    }
                    return@AddInstallmentSheet
                }

                val monthlyFormatted = com.example.util.MoneyFormatter.formatToman(parsedMonthly)
                val totalFormatted = com.example.util.MoneyFormatter.formatToman(totalAmount)

                val effectiveDueDate = if (!customScheduleItems.isNullOrEmpty()) {
                    customScheduleItems.firstOrNull()?.dueDate.orEmpty()
                } else {
                    dueDateStr
                }

                if (effectiveDueDate.isBlank() || !com.example.calendar.domain.CalendarDateUtils.isValidJalaliDate(effectiveDueDate)) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("تاریخ سررسید نامعتبر یا خالی است")
                    }
                    return@AddInstallmentSheet
                }

                val parsedTriple = com.example.calendar.domain.CalendarDateUtils.parseJalali(effectiveDueDate) ?: return@AddInstallmentSheet

                if (!customScheduleItems.isNullOrEmpty()) {
                    val numbers = customScheduleItems.map { it.installmentNumber }
                    val hasDuplicates = numbers.toSet().size != numbers.size
                    val invalidCustom = customScheduleItems.firstOrNull { 
                        it.installmentNumber <= 0 || 
                        it.amount <= 0 || 
                        it.dueDate.isBlank() || 
                        !com.example.calendar.domain.CalendarDateUtils.isValidJalaliDate(it.dueDate) 
                    }
                    if (hasDuplicates || invalidCustom != null) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("شماره اقساط سفارشی باید یکتا و معتبر باشند و مبلغ/تاریخ نباید خالی باشد")
                        }
                        return@AddInstallmentSheet
                    }
                }

                val computedEndDate = PersianCalendarHelper.addMonthsToPersianDate(effectiveDueDate, totalInstallments)
                val todayJalali = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate()

                val initialPaymentHistory = if (!customScheduleItems.isNullOrEmpty()) {
                    customScheduleItems
                } else {
                    val defaultList = mutableListOf<PaymentHistoryItem>()
                    for (i in 1..totalInstallments) {
                        defaultList.add(
                            PaymentHistoryItem(
                                id = "p_${System.currentTimeMillis()}_$i",
                                installmentNumber = i,
                                dueDate = com.example.util.PersianCalendarHelper.addMonthsToPersianDate(effectiveDueDate, i - 1),
                                paidDate = null,
                                amountFormatted = monthlyFormatted,
                                status = if (i == 1) InstallmentStatus.DUE_SOON else InstallmentStatus.PENDING,
                                note = null,
                                amount = if (i == totalInstallments) {
                                    (totalAmount - parsedMonthly * (totalInstallments - 1)).coerceAtLeast(1L)
                                } else parsedMonthly,
                                isPaidLate = false
                            )
                        )
                    }
                    defaultList
                }

                val paidInstallmentNumbers = initialPaymentHistory
                    .sortedBy { it.installmentNumber }
                    .take(requestedPaidCount)
                    .map { it.installmentNumber }
                    .toSet()
                val paymentHistoryList = initialPaymentHistory.map { payment ->
                    if (payment.installmentNumber in paidInstallmentNumbers) {
                        payment.copy(
                            status = InstallmentStatus.PAID,
                            paidDate = todayJalali
                        )
                    } else {
                        payment.copy(
                            status = if (payment.status == InstallmentStatus.PAID || payment.status == InstallmentStatus.COMPLETED) {
                                InstallmentStatus.PENDING
                            } else payment.status,
                            paidDate = null
                        )
                    }
                }
                val paidAmount = paymentHistoryList
                    .filter { it.status == InstallmentStatus.PAID || it.status == InstallmentStatus.COMPLETED }
                    .sumOf { it.amount }
                val remainingAmount = (totalAmount - paidAmount).coerceAtLeast(0L)
                val remainingInstallments = (totalInstallments - requestedPaidCount).coerceAtLeast(0)
                val nextUnpaidPayment = paymentHistoryList
                    .filter { it.status != InstallmentStatus.PAID && it.status != InstallmentStatus.COMPLETED }
                    .minByOrNull { payment ->
                        com.example.calendar.domain.CalendarDateUtils.parseJalali(payment.dueDate)?.let {
                            PersianCalendarHelper.jalaliToEpochMillis(it.first, it.second, it.third, 9, 0)
                        } ?: Long.MAX_VALUE
                    }
                val nextPaymentDate = nextUnpaidPayment?.dueDate ?: effectiveDueDate
                val paidFormatted = com.example.util.MoneyFormatter.formatToman(paidAmount)
                val remainingFormatted = com.example.util.MoneyFormatter.formatToman(remainingAmount)

                val nextPaymentTriple = com.example.calendar.domain.CalendarDateUtils.parseJalali(nextPaymentDate) ?: parsedTriple
                val targetMillis = PersianCalendarHelper.jalaliToEpochMillis(
                    nextPaymentTriple.first,
                    nextPaymentTriple.second,
                    nextPaymentTriple.third,
                    9,
                    0
                )

                val targetDt = PersianCalendarHelper.fromEpochMillis(targetMillis)
                val nowDt = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis())

                val targetEpochDay = PersianCalendarHelper.getJalaliEpochDay(targetDt.year, targetDt.month, targetDt.day)
                val nowEpochDay = PersianCalendarHelper.getJalaliEpochDay(nowDt.year, nowDt.month, nowDt.day)

                val diffDays = (targetEpochDay - nowEpochDay).toInt()
                val computedDueDaysText = when {
                    remainingInstallments == 0 -> "تکمیل شده"
                    diffDays == 0 -> "امروز"
                    diffDays > 0 -> "${IranianPhoneUtils.convertDigitsToPersian(diffDays.toString())} روز دیگر"
                    else -> "${IranianPhoneUtils.convertDigitsToPersian(kotlin.math.abs(diffDays).toString())} روز گذشته"
                }

                val noteText = buildString {
                    if (itemNotes.isNotBlank()) append(itemNotes)
                    if (reminderEnabled) {
                        if (isNotEmpty()) append(" | ")
                        append("یادآور فعال: $reminderDays در ساعت $reminderTime")
                    }
                }

                val newItem = InstallmentItem(
                    id = "inst_new_${System.currentTimeMillis()}",
                    title = title,
                    category = cat,
                    providerOrPerson = provider,
                    totalAmount = totalAmount,
                    totalAmountFormatted = totalFormatted,
                    paidAmount = paidAmount,
                    paidAmountFormatted = paidFormatted,
                    remainingAmount = remainingAmount,
                    remainingAmountFormatted = remainingFormatted,
                    monthlyPaymentFormatted = monthlyFormatted,
                    totalInstallments = totalInstallments,
                    remainingInstallments = remainingInstallments,
                    nextPaymentDate = nextPaymentDate,
                    nextDueDaysText = computedDueDaysText,
                    startDate = todayJalali,
                    endDate = computedEndDate,
                    status = if (remainingInstallments == 0) InstallmentStatus.COMPLETED else InstallmentStatus.PENDING,
                    notes = noteText,
                    paymentHistory = paymentHistoryList
                )
                LocalInstallmentRepository.instance.addInstallment(newItem, context)

                if (reminderEnabled && remainingInstallments > 0) {
                    coroutineScope.launch {
                        try {
                            val scheduledCount = com.example.reminder.domain.ReminderManager(context).syncInstallmentReminder(
                                installmentId = newItem.id,
                                title = newItem.title,
                                amount = nextUnpaidPayment?.amount ?: parsedMonthly,
                                dueDatePersian = newItem.nextPaymentDate,
                                dueTimePersian = reminderTime,
                                selectedOffsets = selectedOffsets
                            )
                            if (scheduledCount > 0) {
                                snackbarHostState.showSnackbar("قسط و یادآور با موفقیت ثبت شدند ($scheduledCount یادآور فعال تنظیم شد).")
                            } else {
                                snackbarHostState.showSnackbar("قسط ثبت شد، اما هیچ یادآور آینده‌ای تنظیم نشد (زمان‌های سررسید انتخاب‌شده در گذشته بودند یا گزینه‌ای انتخاب نشده بود).")
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            android.util.Log.e("InstallmentsScreen", "Failed to sync reminder for installment ${newItem.id}", e)
                            snackbarHostState.showSnackbar("قسط ثبت شد، اما ثبت یادآور با خطا مواجه شد: ${e.localizedMessage ?: "خطای ناشناخته"}")
                        }
                    }
                } else {
                    coroutineScope.launch {
                        val message = if (remainingInstallments == 0) {
                            "وام با تمام اقساط پرداخت‌شده ثبت شد."
                        } else {
                            "قسط با موفقیت ثبت شد (یادآور غیرفعال است)."
                        }
                        snackbarHostState.showSnackbar(message)
                    }
                }
            }
        )
    }

    if (showFilterSheet) {
        InstallmentFilterSheet(
            sheetState = filterSheetState,
            onDismiss = { showFilterSheet = false },
            onApplyFilter = { category, status, period ->
                if (category != null) {
                    navigateTo(InstallmentNavigationState.CategoryView(category))
                }
            }
        )
    }

    if (showSearchSheet) {
        InstallmentSearchSheet(
            sheetState = searchSheetState,
            allItems = allInstallments,
            onDismiss = { showSearchSheet = false },
            onSelectItem = { selectedItem ->
                navigateTo(InstallmentNavigationState.DetailView(selectedItem))
            }
        )
    }
}
