package com.example.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.finance.components.AddTransactionSheet
import com.example.ui.screens.finance.model.TransactionType
import com.example.ui.screens.home.components.BottomNavItem
import com.example.ui.screens.home.components.FinancialInsightCard
import com.example.ui.screens.home.components.FinancialSummaryCard
import com.example.ui.screens.home.components.HomeBottomNavigation
import com.example.ui.screens.home.components.HomeEmptyState
import com.example.ui.screens.home.components.BankSmsAssistantCard
import com.example.ui.screens.home.components.HomeHeader
import com.example.ui.screens.home.components.OverdueAlertCard
import com.example.ui.screens.home.components.QuickActions
import com.example.ui.screens.home.components.UpcomingObligationsCard
import com.example.ui.screens.home.components.UpcomingRemindersCard
import com.example.ui.screens.home.components.VehicleSummaryCard
import com.example.ui.screens.home.domain.ObligationType
import com.example.ui.screens.home.viewmodel.HomeDashboardViewModel
import com.example.ui.screens.installments.components.AddInstallmentSheet
import com.example.ui.screens.installments.model.InstallmentCategory
import com.example.ui.screens.vehicle.components.AddVehicleSheet
import com.example.ui.screens.subscription.SubscriptionStatusCard
import com.example.ui.screens.subscription.SubscriptionViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToTab: (BottomNavItem) -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onOpenSubscription: () -> Unit = {},
    onQuickActionClick: (String) -> Unit = {},
    onNavigateToFinancialHealth: () -> Unit = {},
    bottomBar: (@Composable () -> Unit)? = null,
    viewModel: HomeDashboardViewModel = viewModel(),
    subscriptionViewModel: SubscriptionViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val subState by subscriptionViewModel.uiState.collectAsState()
    val pendingSms by viewModel.pendingSmsQueue.collectAsState()
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
    }

    // Sheet states for Quick Actions
    var showAddTxSheet by remember { mutableStateOf(false) }
    var addTxSheetType by remember { mutableStateOf(TransactionType.EXPENSE) }

    var showAddInstallmentSheet by remember { mutableStateOf(false) }
    val addInstallmentSheetState = rememberModalBottomSheetState()

    var showAddVehicleSheet by remember { mutableStateOf(false) }
    val addVehicleSheetState = rememberModalBottomSheetState()

    // Background gradient canvas
    val backgroundBrush = remember(isDark) {
        if (isDark) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF000000),
                    Color(0xFF050A14),
                    Color(0xFF000000)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFF8FAFC),
                    Color(0xFFF1F5F9),
                    Color(0xFFE2E8F0)
                )
            )
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_scaffold"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (bottomBar != null) {
                bottomBar()
            } else {
                var currentTab by remember { mutableStateOf(BottomNavItem.HOME) }
                HomeBottomNavigation(
                    selectedItem = currentTab,
                    onItemSelected = { tab ->
                        currentTab = tab
                        onNavigateToTab(tab)
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Header: Greeting + Persian Jalali Date + Notification Badge + Settings
                HomeHeader(
                    userName = uiState.userName,
                    todayDateText = uiState.todayDate,
                    unreadNotificationsCount = uiState.notificationCount,
                    onNotificationClick = onNotificationClick,
                    onSettingsClick = onSettingsClick
                )

                // Scrollable body content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // 1.5 Subscription Status Card
                    SubscriptionStatusCard(
                        state = subState,
                        onOpenSubscription = onOpenSubscription,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    // Main Dashboard Body
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Empty State Check for brand new users
                        if (uiState.isEmptyState) {
                            HomeEmptyState(
                                onAddExpense = {
                                    addTxSheetType = TransactionType.EXPENSE
                                    showAddTxSheet = true
                                },
                                onAddInstallment = {
                                    showAddInstallmentSheet = true
                                },
                                onAddVehicle = {
                                    showAddVehicleSheet = true
                                }
                            )
                        } else {
                            // 2. Overdue / Urgent Alert Card (Prioritized if exists)
                            if (uiState.overdueItems.isNotEmpty()) {
                                OverdueAlertCard(
                                    overdueItems = uiState.overdueItems,
                                    onViewItemClick = { item ->
                                        onNavigateToTab(item.destinationTab)
                                    },
                                    onDismissClick = { id ->
                                        viewModel.dismissOverdueAlert(id)
                                        scope.launch {
                                            snackbarHostState.showSnackbar("هشدار موقتاً بسته شد.")
                                        }
                                    }
                                )
                            }

                            // 2.5 Bank SMS Assistant Card (Smart suggestions queue)
                            if (pendingSms.isNotEmpty()) {
                                BankSmsAssistantCard(
                                    queue = pendingSms,
                                    onAccept = { id ->
                                        viewModel.acceptSmsSuggestion(id)
                                        scope.launch {
                                            snackbarHostState.showSnackbar("تراکنش با موفقیت ثبت و تایید گردید.")
                                        }
                                    },
                                    onDismiss = { id ->
                                        viewModel.dismissSmsSuggestion(id)
                                    },
                                    onSimulateClick = {
                                        viewModel.simulateIncomingSms()
                                    }
                                )
                            }

                            // 3. Financial Summary Card (وضعیت مالی این ماه)
                            FinancialSummaryCard(
                                formattedIncome = uiState.formattedIncome,
                                formattedExpense = uiState.formattedExpense,
                                formattedBalance = uiState.formattedBalance,
                                savingsRate = uiState.savingsRate,
                                onClick = { onNavigateToTab(BottomNavItem.FINANCE) }
                            )

                            // 4. Upcoming Obligations Card (تعهدات نزدیک)
                            UpcomingObligationsCard(
                                obligations = uiState.upcomingObligations,
                                onViewAllClick = { onNavigateToTab(BottomNavItem.INSTALLMENTS) },
                                onObligationClick = { item ->
                                    when (item.type) {
                                        ObligationType.INSTALLMENT -> onNavigateToTab(BottomNavItem.INSTALLMENTS)
                                        ObligationType.VEHICLE_INSURANCE -> onNavigateToTab(BottomNavItem.VEHICLE)
                                        ObligationType.BILL -> onNavigateToTab(BottomNavItem.CALENDAR)
                                        ObligationType.OTHER -> onNavigateToTab(BottomNavItem.FINANCE)
                                    }
                                },
                                onMarkDoneClick = { id ->
                                    viewModel.markObligationDone(id)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("تعهد مورد نظر با موفقیت پرداخت و تسویه شد.")
                                    }
                                }
                            )

                            // 5. Smart Reminders Card (یادآورهای من)
                            UpcomingRemindersCard(
                                reminders = uiState.upcomingReminders,
                                onViewAllClick = { onNavigateToTab(BottomNavItem.REMINDERS) },
                                onReminderClick = {
                                    onNavigateToTab(BottomNavItem.REMINDERS)
                                },
                                onMarkCompleted = { id ->
                                    viewModel.markReminderCompleted(id)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("یادآوری انجام شد.")
                                    }
                                }
                            )

                            // 6. Quick Actions (دسترسی سریع ضروری)
                            QuickActions(
                                onAddExpenseClick = {
                                    onQuickActionClick("add_expense")
                                    addTxSheetType = TransactionType.EXPENSE
                                    showAddTxSheet = true
                                },
                                onAddIncomeClick = {
                                    onQuickActionClick("add_income")
                                    addTxSheetType = TransactionType.INCOME
                                    showAddTxSheet = true
                                },
                                onAddInstallmentClick = {
                                    onQuickActionClick("add_installment")
                                    showAddInstallmentSheet = true
                                },
                                onAddReminderClick = {
                                    onQuickActionClick("add_reminder")
                                    onNavigateToTab(BottomNavItem.REMINDERS)
                                }
                            )

                            // 7. Vehicle Summary Card (وضعیت خودرو)
                            VehicleSummaryCard(
                                vehicle = uiState.vehicleSummary,
                                onViewVehicleClick = { onNavigateToTab(BottomNavItem.VEHICLE) },
                                onAddVehicleClick = { showAddVehicleSheet = true }
                            )

                            // 8. Financial Insight (تحلیل هوشمند دارینو)
                            uiState.financialInsight?.let { insight ->
                                FinancialInsightCard(
                                    insight = insight,
                                    onClick = { onNavigateToTab(BottomNavItem.REPORTS) }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }

    // Modal Sheets for Quick Actions
    if (showAddTxSheet) {
        AddTransactionSheet(
            initialType = addTxSheetType,
            onDismiss = { showAddTxSheet = false },
            onSubmitTransaction = { item ->
                showAddTxSheet = false
                viewModel.loadDashboardData()
                scope.launch {
                    val typeName = if (item.type == TransactionType.EXPENSE) "هزینه" else "درآمد"
                    snackbarHostState.showSnackbar("تراکنش $typeName به مبلغ ${item.amountFormatted} ثبت شد.")
                }
            }
        )
    }

    if (showAddInstallmentSheet) {
        AddInstallmentSheet(
            sheetState = addInstallmentSheetState,
            onDismiss = { showAddInstallmentSheet = false },
            initialCategory = InstallmentCategory.BANK_LOANS,
            onAddConfirm = { category, title, total, monthly, provider, reminderEnabled, reminderDays ->
                showAddInstallmentSheet = false
                viewModel.loadDashboardData()
                scope.launch {
                    snackbarHostState.showSnackbar("قسط «$title» ثبت شد. یادآور خودکار تنظیم گردید.")
                }
            }
        )
    }

    if (showAddVehicleSheet) {
        AddVehicleSheet(
            sheetState = addVehicleSheetState,
            onDismiss = { showAddVehicleSheet = false },
            onSubmitVehicle = { name, brand, modelYear, odometer, plate, color ->
                showAddVehicleSheet = false
                viewModel.loadDashboardData()
                scope.launch {
                    snackbarHostState.showSnackbar("خودروی $brand $name با موفقیت اضافه شد.")
                }
            }
        )
    }
}
