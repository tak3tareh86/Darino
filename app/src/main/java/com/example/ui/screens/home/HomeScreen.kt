package com.example.ui.screens.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.ui.screens.home.components.QuickActions
import com.example.ui.screens.home.components.UpcomingObligationsCard
import com.example.ui.screens.home.components.UpcomingRemindersCard
import com.example.ui.screens.home.components.VehicleSummaryCard
import com.example.ui.screens.home.domain.ObligationType
import com.example.ui.screens.home.viewmodel.HomeDashboardViewModel
import com.example.ui.screens.installments.OverdueInstallmentsScreen
import com.example.ui.screens.installments.components.AddInstallmentSheet
import com.example.ui.screens.installments.model.InstallmentCategory
import com.example.ui.screens.installments.model.InstallmentMockDataSource
import com.example.vehicle.presentation.components.AddVehicleSheet
import com.example.ui.screens.subscription.SubscriptionStatusCard
import com.example.ui.screens.subscription.SubscriptionViewModel
import com.example.ui.theme.ExpenseRoseLight
import com.example.util.IranianPhoneUtils
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
    val context = LocalContext.current
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

    // Overdue Installments Screen state
    var showOverdueScreen by remember { mutableStateOf(false) }
    var overdueItemsList by remember { mutableStateOf(InstallmentMockDataSource.overdueInstallments) }

    LaunchedEffect(uiState) {
        overdueItemsList = InstallmentMockDataSource.overdueInstallments
    }

    if (showOverdueScreen) {
        BackHandler {
            showOverdueScreen = false
            overdueItemsList = InstallmentMockDataSource.overdueInstallments
            viewModel.loadDashboardData()
        }

        OverdueInstallmentsScreen(
            overdueItems = overdueItemsList,
            onBackClick = {
                showOverdueScreen = false
                overdueItemsList = InstallmentMockDataSource.overdueInstallments
                viewModel.loadDashboardData()
            },
            onItemClick = { /* detail or click */ },
            onMarkAsPaid = { installmentId, paymentDate ->
                InstallmentMockDataSource.markOverdueAsPaid(installmentId, paymentDate, context)
                overdueItemsList = InstallmentMockDataSource.overdueInstallments
                viewModel.loadDashboardData()
                scope.launch {
                    snackbarHostState.showSnackbar("قسط با موفقیت در تاریخ $paymentDate به عنوان پرداخت شده ثبت شد.")
                }
            }
        )
    } else {

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
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            bottomBar?.invoke()
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
                            // 2.5 Bank SMS Assistant Card (Smart suggestions queue)
                            if (pendingSms.isNotEmpty()) {
                                BankSmsAssistantCard(
                                    queue = pendingSms,
                                    onAccept = { id, cat, acc ->
                                        viewModel.acceptSmsSuggestion(id, cat, acc)
                                        scope.launch {
                                            snackbarHostState.showSnackbar("تراکنش با موفقیت ثبت و تأیید گردید.")
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

                            // 3. Compact Overdue Installments Box (فقط یک کادر کوچک و مرتب شامل عنوان، تعداد و دکمه نمایش اقساط معوق)
                            if (overdueItemsList.isNotEmpty()) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(
                                            elevation = 2.dp,
                                            shape = RoundedCornerShape(12.dp),
                                            ambientColor = ExpenseRoseLight.copy(alpha = 0.2f),
                                            spotColor = ExpenseRoseLight.copy(alpha = 0.15f)
                                        ),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDark) Color(0xFF221114) else Color(0xFFFFF1F2),
                                    border = BorderStroke(1.dp, ExpenseRoseLight.copy(alpha = 0.35f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(ExpenseRoseLight.copy(alpha = 0.18f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Warning,
                                                    contentDescription = null,
                                                    tint = ExpenseRoseLight,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = "اقساط سررسید گذشته (معوق)",
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.5.sp
                                                    ),
                                                    color = if (isDark) Color(0xFFFECDD3) else Color(0xFF9F1239)
                                                )
                                                Text(
                                                    text = "تعداد: ${IranianPhoneUtils.convertDigitsToPersian(overdueItemsList.size.toString())} قسط معوق",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium
                                                    ),
                                                    color = if (isDark) Color(0xFFFDA4AF) else Color(0xFFBE123C)
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = { showOverdueScreen = true },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = ExpenseRoseLight,
                                                contentColor = Color.White
                                            ),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            modifier = Modifier
                                                .height(34.dp)
                                                .testTag("btn_show_overdue_home")
                                        ) {
                                            Text(
                                                text = "نمایش اقساط معوق",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }

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
        val context = androidx.compose.ui.platform.LocalContext.current
        AddInstallmentSheet(
            sheetState = addInstallmentSheetState,
            onDismiss = { showAddInstallmentSheet = false },
            initialCategory = InstallmentCategory.BANK_LOANS,
            onAddConfirm = { category, title, totalStr, monthlyStr, countStr, dueDateStr, provider, itemNotes, reminderEnabled, reminderDays, reminderTime, selectedOffsets, customScheduleItems ->
                showAddInstallmentSheet = false

                val parsedTotal = com.example.util.IranianAmountUtils.parseAmountToLong(totalStr).let { if (it <= 0L) 10_000_000L else it }
                val parsedMonthly = com.example.util.IranianAmountUtils.parseAmountToLong(monthlyStr).let { if (it <= 0L) 1_000_000L else it }
                val parsedCount = countStr.filter { it.isDigit() }.toIntOrNull().let {
                    if (it == null || it <= 0) (parsedTotal / parsedMonthly.coerceAtLeast(1L)).toInt().coerceAtLeast(1) else it
                }

                val finalTotal = if (!customScheduleItems.isNullOrEmpty()) customScheduleItems.sumOf { it.amount } else parsedTotal
                val finalCount = if (!customScheduleItems.isNullOrEmpty()) customScheduleItems.size else parsedCount

                val monthlyFormatted = com.example.util.MoneyFormatter.formatToman(parsedMonthly)
                val totalFormatted = com.example.util.MoneyFormatter.formatToman(finalTotal)
                val remainingFormatted = com.example.util.MoneyFormatter.formatToman(finalTotal)

                val paymentHistoryList = if (!customScheduleItems.isNullOrEmpty()) {
                    customScheduleItems
                } else {
                    val defaultList = mutableListOf<com.example.ui.screens.installments.model.PaymentHistoryItem>()
                    for (i in 1..finalCount) {
                        defaultList.add(
                            com.example.ui.screens.installments.model.PaymentHistoryItem(
                                id = "p_${System.currentTimeMillis()}_$i",
                                installmentNumber = i,
                                dueDate = if (i == 1) dueDateStr.ifBlank { "۱۴۰۴/۰۸/۱۵" } else "قسط شماره $i",
                                paidDate = null,
                                amountFormatted = monthlyFormatted,
                                status = if (i == 1) com.example.ui.screens.installments.model.InstallmentStatus.DUE_SOON else com.example.ui.screens.installments.model.InstallmentStatus.PENDING,
                                note = null,
                                amount = parsedMonthly,
                                isPaidLate = false
                            )
                        )
                    }
                    defaultList
                }

                val todayJalali = com.example.util.PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate()
                val noteText = buildString {
                    if (itemNotes.isNotBlank()) append(itemNotes)
                    if (reminderEnabled) {
                        if (isNotEmpty()) append(" | ")
                        append("یادآور فعال: $reminderDays در ساعت $reminderTime")
                    }
                }

                val newItem = com.example.ui.screens.installments.model.InstallmentItem(
                    id = "inst_new_${System.currentTimeMillis()}",
                    title = title,
                    category = category,
                    providerOrPerson = provider,
                    totalAmount = parsedTotal,
                    totalAmountFormatted = totalFormatted,
                    paidAmount = 0L,
                    paidAmountFormatted = com.example.util.MoneyFormatter.formatToman(0L),
                    remainingAmount = parsedTotal,
                    remainingAmountFormatted = remainingFormatted,
                    monthlyPaymentFormatted = monthlyFormatted,
                    totalInstallments = parsedCount,
                    remainingInstallments = parsedCount,
                    nextPaymentDate = dueDateStr.ifBlank { "۱۴۰۴/۰۸/۱۵" },
                    nextDueDaysText = "در انتظار سررسید",
                    startDate = todayJalali,
                    endDate = "۱۴۰۵/۰۸/۱۵",
                    status = com.example.ui.screens.installments.model.InstallmentStatus.PENDING,
                    notes = noteText,
                    paymentHistory = paymentHistoryList
                )

                com.example.ui.screens.installments.model.InstallmentMockDataSource.addInstallment(newItem, context)
                viewModel.loadDashboardData()

                if (reminderEnabled) {
                    scope.launch {
                        try {
                            com.example.reminder.domain.ReminderManager(context).syncInstallmentReminder(
                                installmentId = newItem.id,
                                title = title,
                                amount = parsedMonthly,
                                dueDatePersian = newItem.nextPaymentDate,
                                dueTimePersian = reminderTime,
                                selectedOffsets = selectedOffsets
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                scope.launch {
                    snackbarHostState.showSnackbar("قسط «$title» با موفقیت ثبت شد.")
                }
            }
        )
    }

    if (showAddVehicleSheet) {
        AddVehicleSheet(
            onAddVehicle = { brand, model, year, color, plate, vin, mileage, estVal ->
                showAddVehicleSheet = false
                viewModel.loadDashboardData()
                scope.launch {
                    snackbarHostState.showSnackbar("خودروی $brand $model با موفقیت اضافه شد.")
                }
            },
            onDismiss = { showAddVehicleSheet = false }
        )
    }
    }
}
