package com.example.ui.screens.installments

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
import com.example.ui.screens.installments.model.InstallmentCategory
import com.example.ui.screens.installments.model.InstallmentItem
import com.example.ui.screens.installments.model.InstallmentMockDataSource
import com.example.ui.screens.installments.model.InstallmentStatus
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

    // Dynamic mock state that supports additions
    var bankLoansList by remember { mutableStateOf(InstallmentMockDataSource.bankLoans) }
    var homeLoansList by remember { mutableStateOf(InstallmentMockDataSource.homeLoans) }
    var carInsuranceList by remember { mutableStateOf(InstallmentMockDataSource.carInsurance) }
    var miscInstallmentsList by remember { mutableStateOf(InstallmentMockDataSource.miscInstallments) }
    val snackbarHostState = remember { SnackbarHostState() }

    fun refreshData() {
        bankLoansList = InstallmentMockDataSource.bankLoans
        homeLoansList = InstallmentMockDataSource.homeLoans
        carInsuranceList = InstallmentMockDataSource.carInsurance
        miscInstallmentsList = InstallmentMockDataSource.miscInstallments
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    val allInstallments = remember(bankLoansList, homeLoansList, carInsuranceList, miscInstallmentsList) {
        bankLoansList + homeLoansList + carInsuranceList + miscInstallmentsList
    }

    val overdueList = remember(allInstallments) {
        allInstallments.filter { it.status == InstallmentStatus.OVERDUE }
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
                        InstallmentsHeader(
                            onSearchClick = { showSearchSheet = true },
                            onFilterClick = { showFilterSheet = true }
                        )
                    },
                    bottomBar = { bottomBar?.invoke() }
                ) { innerPadding ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (allInstallments.isEmpty()) {
                            item {
                                InstallmentsEmptyState(
                                    onAddClick = {
                                        addInitialCategory = InstallmentCategory.BANK_LOANS
                                        showAddSheet = true
                                    }
                                )
                            }
                        } else {
                            // 1. Quick Actions at the very top (Add Installment & Payment Schedule)
                            item {
                                InstallmentQuickActions(
                                    onAddInstallmentClick = {
                                        addInitialCategory = InstallmentCategory.BANK_LOANS
                                        prefillTitle = ""
                                        prefillTotal = ""
                                        prefillMonthly = ""
                                        prefillCount = ""
                                        showAddSheet = true
                                    },
                                    onViewScheduleClick = {
                                        allInstallments.firstOrNull()?.let {
                                            navigateTo(InstallmentNavigationState.ScheduleView(it))
                                        }
                                    },
                                    onSearchFilterClick = {
                                        showSearchSheet = true
                                    }
                                )
                            }

                            // 2. Summary Card
                            item {
                                InstallmentsSummaryCard(
                                    summary = InstallmentMockDataSource.summary,
                                    onClick = {
                                        allInstallments.firstOrNull()?.let {
                                            navigateTo(InstallmentNavigationState.ScheduleView(it))
                                        }
                                    }
                                )
                            }

                            // 3. Collapsible Smart Tools & Financial Health Section
                            item {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { isToolsExpanded = !isToolsExpanded }
                                                .padding(horizontal = 6.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.BuildCircle,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Column {
                                                    Text(
                                                        text = "ابزارهای هوشمند و سلامت مالی",
                                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "محاسبه‌گر وام و پایش فشار اقساط ماهانه",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Icon(
                                                imageVector = if (isToolsExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        AnimatedVisibility(
                                            visible = isToolsExpanded,
                                            enter = expandVertically() + fadeIn(),
                                            exit = shrinkVertically() + fadeOut()
                                        ) {
                                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                LoanCalculatorCard(
                                                    onClick = {
                                                        navigateTo(InstallmentNavigationState.LoanCalculator)
                                                    }
                                                )
                                                FinancialHealthCard(
                                                    pressurePercentage = healthState.pressurePercentage,
                                                    healthStatus = healthState.healthStatus,
                                                    monthlyIncome = healthState.monthlyIncome,
                                                    totalCommitments = healthState.totalCommitments,
                                                    onClick = {
                                                        navigateTo(InstallmentNavigationState.FinancialHealth)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 4. Compact Overdue Section (Alert if overdue exist)
                            if (overdueList.isNotEmpty()) {
                                item {
                                    OverdueInstallmentsSection(
                                        items = overdueList,
                                        onShowOverdueClick = {
                                            navigateTo(InstallmentNavigationState.OverdueView)
                                        }
                                    )
                                }
                            }

                            // 5. Collapsible Categories Grid
                            item {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                            categories = InstallmentMockDataSource.categorySummaries,
                                            onCategoryClick = { category ->
                                                navigateTo(InstallmentNavigationState.CategoryView(category))
                                            }
                                        )
                                    }
                                }
                            }

                            // 6. Collapsible Upcoming Payments Section (Max 3)
                            item {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { isUpcomingExpanded = !isUpcomingExpanded }
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.EventRepeat,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "اقساط و موعدهای نزدیک",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Icon(
                                            imageVector = if (isUpcomingExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    AnimatedVisibility(
                                        visible = isUpcomingExpanded,
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        UpcomingInstallmentsSection(
                                            items = upcomingList,
                                            onItemClick = { item ->
                                                navigateTo(InstallmentNavigationState.DetailView(item))
                                            },
                                            onSeeAllClick = {
                                                allInstallments.firstOrNull()?.let {
                                                    navigateTo(InstallmentNavigationState.ScheduleView(it))
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
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
                        refreshData()
                    },
                    onItemClick = { item ->
                        navigateTo(InstallmentNavigationState.DetailView(item))
                    },
                    onMarkAsPaid = { installmentId, paymentDate ->
                        InstallmentMockDataSource.markOverdueAsPaid(installmentId, paymentDate)
                        refreshData()
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
                        prefillTotal = total.replace(" تومان", "").trim()
                        prefillMonthly = monthly.replace(" تومان", "").trim()
                        prefillCount = count.toString()
                        showAddSheet = true
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
            onAddConfirm = { cat, title, total, monthly, provider, reminderEnabled, reminderDays, reminderTime ->
                prefillTitle = ""
                prefillTotal = ""
                prefillMonthly = ""
                prefillCount = ""
                val newItem = InstallmentItem(
                    id = "inst_new_${System.currentTimeMillis()}",
                    title = title,
                    category = cat,
                    providerOrPerson = provider,
                    totalAmount = 12_000_000,
                    totalAmountFormatted = com.example.util.MoneyFormatter.formatToman(12_000_000),
                    paidAmount = 2_000_000,
                    paidAmountFormatted = com.example.util.MoneyFormatter.formatToman(2_000_000),
                    remainingAmount = 10_000_000,
                    remainingAmountFormatted = com.example.util.MoneyFormatter.formatToman(10_000_000),
                    monthlyPaymentFormatted = "$monthly ${com.example.util.MoneyFormatter.getUnitLabel()}",
                    totalInstallments = 12,
                    remainingInstallments = 10,
                    nextPaymentDate = "۱۴۰۴/۰۸/۱۵",
                    nextDueDaysText = "۳۰ روز دیگر",
                    startDate = "۱۴۰۴/۰۷/۱۵",
                    endDate = "۱۴۰۵/۰۷/۱۵",
                    status = InstallmentStatus.PENDING,
                    notes = "یادآور فعال: $reminderEnabled، $reminderDays روز قبل در ساعت $reminderTime"
                )
                when (cat) {
                    InstallmentCategory.BANK_LOANS -> bankLoansList = listOf(newItem) + bankLoansList
                    InstallmentCategory.HOME_LOANS -> homeLoansList = listOf(newItem) + homeLoansList
                    InstallmentCategory.CAR_INSURANCE -> carInsuranceList = listOf(newItem) + carInsuranceList
                    InstallmentCategory.MISC -> miscInstallmentsList = listOf(newItem) + miscInstallmentsList
                }

                if (reminderEnabled) {
                    coroutineScope.launch {
                        try {
                            val cleanAmount = monthly.filter { it.isDigit() }.toLongOrNull() ?: 1_000_000L
                            com.example.reminder.domain.ReminderManager(context).syncInstallmentReminder(
                                installmentId = newItem.id,
                                title = newItem.title,
                                amount = cleanAmount,
                                dueDatePersian = newItem.nextPaymentDate,
                                dueTimePersian = reminderTime
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
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
