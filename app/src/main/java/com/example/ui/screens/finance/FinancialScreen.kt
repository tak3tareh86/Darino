package com.example.ui.screens.finance

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.finance.components.AddBudgetSheet
import com.example.ui.screens.finance.components.AddRecurringTransactionSheet
import com.example.ui.screens.finance.components.AddSavingsGoalSheet
import com.example.ui.screens.finance.components.AddTransactionSheet
import com.example.ui.screens.finance.components.DepositGoalSheet
import com.example.ui.screens.finance.components.FinanceQuickLinks
import com.example.ui.screens.finance.components.FinancialHeader
import com.example.ui.screens.finance.components.FinancialQuickActions
import com.example.ui.screens.finance.components.FinancialSummaryCard
import com.example.ui.screens.finance.components.TransferSheet
import com.example.ui.screens.finance.model.TransactionItemData
import com.example.ui.screens.finance.model.TransactionType
import com.example.ui.screens.finance.model.FinanceDefaultCategories
import com.example.util.PersianCalendarHelper
import com.example.ui.screens.finance.data.TransactionOperationResult
import com.example.ui.screens.finance.components.RecentTransactionsSection
import com.example.ui.screens.finance.model.Budget
import com.example.ui.screens.finance.model.FinanceFilterPeriod
import com.example.ui.screens.finance.model.RecurringTransaction
import com.example.ui.screens.finance.model.SavingsGoal
import com.example.ui.screens.finance.model.TransactionCategory
import com.example.ui.screens.finance.screens.BudgetScreen
import com.example.ui.screens.finance.screens.CategoryManagementScreen
import com.example.ui.screens.finance.screens.RecurringTransactionsScreen
import com.example.ui.screens.finance.screens.SavingsGoalsScreen
import com.example.ui.screens.finance.screens.TransactionDetailScreen
import com.example.ui.screens.finance.screens.TransactionsScreen
import com.example.ui.screens.finance.screens.FinancialReportScreen
import com.example.ui.screens.finance.viewmodel.FinancialViewModel
import com.example.ui.screens.home.components.BottomNavItem
import com.example.ui.screens.home.components.HomeBottomNavigation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

enum class FinanceSubScreen {
    MAIN,
    ALL_TRANSACTIONS,
    TRANSACTION_DETAIL,
    BUDGETS,
    SAVINGS_GOALS,
    RECURRING_TRANSACTIONS,
    CATEGORIES,
    REPORT
}

@Composable
fun FinancialScreen(
    currentNavTab: BottomNavItem = BottomNavItem.FINANCE,
    onNavigateToTab: (BottomNavItem) -> Unit = {},
    bottomBar: (@Composable () -> Unit)? = null,
    viewModel: FinancialViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var selectedSummaryPeriod by remember { mutableStateOf(FinanceFilterPeriod.THIS_MONTH) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Refresh immediately when returning to the app and at each local midnight.
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshMonthSummary()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        viewModel.refreshMonthSummary()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(viewModel) {
        while (true) {
            val now = System.currentTimeMillis()
            val nextMidnight = Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_YEAR, 1)
            }.timeInMillis
            delay((nextMidnight - now).coerceAtLeast(1_000L))
            viewModel.refreshMonthSummary()
        }
    }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val subScreenBackStack = remember { mutableStateListOf(FinanceSubScreen.MAIN) }
    val currentSubScreen = subScreenBackStack.last()
    fun navigateToSubScreen(destination: FinanceSubScreen) {
        if (subScreenBackStack.lastOrNull() != destination) subScreenBackStack.add(destination)
    }
    fun navigateBack() {
        if (subScreenBackStack.size > 1) subScreenBackStack.removeAt(subScreenBackStack.lastIndex)
    }
    fun returnToMain() {
        subScreenBackStack.clear()
        subScreenBackStack.add(FinanceSubScreen.MAIN)
    }
    BackHandler(enabled = subScreenBackStack.size > 1) { navigateBack() }

    // Bottom Sheets State
    var showAddTxSheet by remember { mutableStateOf(false) }
    var showTransferSheet by remember { mutableStateOf(false) }
    var addTxInitialType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var editingTransaction by remember { mutableStateOf<TransactionItemData?>(null) }
    var selectedTransactionDetail by remember { mutableStateOf<TransactionItemData?>(null) }

    var showAddBudgetSheet by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<Budget?>(null) }

    var showAddGoalSheet by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var depositingGoal by remember { mutableStateOf<SavingsGoal?>(null) }

    var showAddRecurringSheet by remember { mutableStateOf(false) }
    var editingRecurring by remember { mutableStateOf<RecurringTransaction?>(null) }


    Crossfade(
        targetState = currentSubScreen,
        animationSpec = tween(220),
        label = "FinanceSubScreenCrossfade"
    ) { screen ->
        when (screen) {
            FinanceSubScreen.MAIN -> {
                Scaffold(
                    modifier = modifier
                        .fillMaxSize()
                        .testTag("financial_screen"),
                    contentWindowInsets = WindowInsets(0),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        FinancialHeader()
                    },
                    bottomBar = {
                        bottomBar?.invoke()
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // One unified financial summary card with an independent month/year selector.
                        val isYearSummary = selectedSummaryPeriod == FinanceFilterPeriod.THIS_YEAR
                        FinancialSummaryCard(
                            title = if (isYearSummary) "وضعیت مالی سال ${state.currentYear}" else "وضعیت مالی ${state.currentMonthName}",
                            formattedIncome = if (isYearSummary) state.currentYearFormattedIncome else state.currentMonthFormattedIncome,
                            formattedExpense = if (isYearSummary) state.currentYearFormattedExpense else state.currentMonthFormattedExpense,
                            formattedBalance = if (isYearSummary) state.currentYearFormattedBalance else state.currentMonthFormattedBalance,
                            savingsRate = if (isYearSummary) state.currentYearSavingsRate else state.currentMonthSavingsRate,
                            hasIncome = if (isYearSummary) state.currentYearHasIncome else state.currentMonthHasIncome,
                            savingsStatus = if (isYearSummary) state.currentYearSavingsStatus else state.currentMonthSavingsStatus,
                            balanceLabel = if (isYearSummary) "خالص عملکرد مالی سال" else "خالص عملکرد مالی ماه",
                            selectedSummaryPeriod = selectedSummaryPeriod,
                            yearLabel = state.currentYear,
                            onSummaryPeriodChange = { selectedSummaryPeriod = it },
                            cardTestTag = "unified_financial_status_card",
                            onCardClick = {
                                viewModel.setPeriod(selectedSummaryPeriod)
                                navigateToSubScreen(FinanceSubScreen.ALL_TRANSACTIONS)
                            }
                        )

                        // 3. Quick Actions: ثبت هزینه، ثبت درآمد، انتقال
                        FinancialQuickActions(
                            onActionClick = { actionId ->
                                when (actionId) {
                                    "add_expense" -> {
                                        addTxInitialType = TransactionType.EXPENSE
                                        editingTransaction = null
                                        showAddTxSheet = true
                                    }
                                    "add_income" -> {
                                        addTxInitialType = TransactionType.INCOME
                                        editingTransaction = null
                                        showAddTxSheet = true
                                    }
                                    "transfer" -> {
                                        showTransferSheet = true
                                    }
                                    "reports" -> {
                                        navigateToSubScreen(FinanceSubScreen.REPORT)
                                    }
                                }
                            }
                        )

                        // 4. Quick Links: بودجه‌ها، اهداف پس‌انداز، تکرارشونده، دسته‌بندی‌ها
                        FinanceQuickLinks(
                            onNavigateToBudgets = { navigateToSubScreen(FinanceSubScreen.BUDGETS) },
                            onNavigateToSavingsGoals = { navigateToSubScreen(FinanceSubScreen.SAVINGS_GOALS) },
                            onNavigateToRecurring = { navigateToSubScreen(FinanceSubScreen.RECURRING_TRANSACTIONS) },
                            onNavigateToCategories = { navigateToSubScreen(FinanceSubScreen.CATEGORIES) }
                        )

                        // 5. Recent Transactions Section (Max 4 items + "همه ›" button)
                        RecentTransactionsSection(
                            transactions = state.recentTransactions,
                            onTransactionClick = { tx ->
                                selectedTransactionDetail = tx
                                navigateToSubScreen(FinanceSubScreen.TRANSACTION_DETAIL)
                            },
                            onEditClick = { tx ->
                                editingTransaction = tx
                                showAddTxSheet = true
                            },
                            onDuplicateClick = { tx ->
                                viewModel.duplicateTransaction(tx.id) { duplicatedTx ->
                                    if (duplicatedTx != null) {
                                        scope.launch { snackbarHostState.showSnackbar("تراکنش کپی و تکرار شد") }
                                    } else {
                                        scope.launch { snackbarHostState.showSnackbar("خطا در کپی تراکنش") }
                                    }
                                }
                            },
                            onDeleteClick = { tx ->
                                viewModel.deleteTransaction(tx.id) { res ->
                                    if (res == TransactionOperationResult.SUCCESS) {
                                        scope.launch { snackbarHostState.showSnackbar("تراکنش با موفقیت حذف شد") }
                                    } else {
                                        scope.launch { snackbarHostState.showSnackbar("خطا در حذف تراکنش") }
                                    }
                                }
                            },
                            onSeeAllClick = {
                                navigateToSubScreen(FinanceSubScreen.ALL_TRANSACTIONS)
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }

            FinanceSubScreen.ALL_TRANSACTIONS -> {
                TransactionsScreen(
                    transactions = state.allTransactions,
                    categories = state.categories,
                    onBackClick = { navigateBack() },
                    onTransactionClick = { tx ->
                        selectedTransactionDetail = tx
                        navigateToSubScreen(FinanceSubScreen.TRANSACTION_DETAIL)
                    },
                    onAddNewTransaction = {
                        addTxInitialType = TransactionType.EXPENSE
                        editingTransaction = null
                        showAddTxSheet = true
                    },
                    onDuplicateTransaction = { txId ->
                        viewModel.duplicateTransaction(txId) { duplicated ->
                            if (duplicated != null) {
                                scope.launch { snackbarHostState.showSnackbar("تراکنش با موفقیت تکرار شد") }
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("خطا در تکرار تراکنش") }
                            }
                        }
                    },
                    onDeleteTransaction = { txId ->
                        viewModel.deleteTransaction(txId) { res ->
                            if (res == TransactionOperationResult.SUCCESS) {
                                scope.launch { snackbarHostState.showSnackbar("تراکنش حذف شد") }
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("خطا در حذف تراکنش") }
                            }
                        }
                    }
                )
            }

            FinanceSubScreen.REPORT -> {
                FinancialReportScreen(
                    transactions = state.reportTransactions,
                    categories = state.categories,
                    onBackClick = { navigateBack() }
                )
            }

            FinanceSubScreen.TRANSACTION_DETAIL -> {
                selectedTransactionDetail?.let { tx ->
                    TransactionDetailScreen(
                        transaction = tx,
                        onBackClick = { navigateBack() },
                        onEditClick = {
                            editingTransaction = tx
                            showAddTxSheet = true
                        },
                        onDuplicateClick = {
                            viewModel.duplicateTransaction(tx.id) { duplicated ->
                                if (duplicated != null) {
                                    returnToMain()
                                    scope.launch { snackbarHostState.showSnackbar("تراکنش کپی شد") }
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar("خطا در کپی تراکنش") }
                                }
                            }
                        },
                        onDeleteClick = {
                            viewModel.deleteTransaction(tx.id) { res ->
                                if (res == TransactionOperationResult.SUCCESS) {
                                    returnToMain()
                                    scope.launch { snackbarHostState.showSnackbar("تراکنش حذف شد") }
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar("خطا در حذف تراکنش") }
                                }
                            }
                        }
                    )
                } ?: run {
                    returnToMain()
                }
            }

            FinanceSubScreen.BUDGETS -> {
                BudgetScreen(
                    budgets = state.activeBudgets,
                    onBackClick = { navigateBack() },
                    onAddBudgetClick = {
                        editingBudget = null
                        showAddBudgetSheet = true
                    },
                    onEditBudgetClick = { b ->
                        editingBudget = b
                        showAddBudgetSheet = true
                    },
                    onDeleteBudgetClick = { bId ->
                        viewModel.deleteBudget(bId)
                        scope.launch { snackbarHostState.showSnackbar("بودجه حذف شد") }
                    },
                    onToggleBudget = { bId, enabled ->
                        viewModel.toggleBudget(bId, enabled) { success ->
                            if (!success) {
                                scope.launch { snackbarHostState.showSnackbar("خطا در ذخیره‌سازی وضعیت بودجه") }
                            }
                        }
                    }
                )
            }

            FinanceSubScreen.SAVINGS_GOALS -> {
                SavingsGoalsScreen(
                    goals = state.savingsGoals,
                    onBackClick = { navigateBack() },
                    onAddGoalClick = {
                        editingGoal = null
                        showAddGoalSheet = true
                    },
                    onEditGoalClick = { g ->
                        editingGoal = g
                        showAddGoalSheet = true
                    },
                    onDepositClick = { g ->
                        depositingGoal = g
                    },
                    onTogglePause = { goal ->
                        val nextStatus = if (goal.status == com.example.ui.screens.finance.model.SavingsGoalStatus.PAUSED) {
                            com.example.ui.screens.finance.model.SavingsGoalStatus.IN_PROGRESS
                        } else {
                            com.example.ui.screens.finance.model.SavingsGoalStatus.PAUSED
                        }
                        viewModel.setSavingsGoalStatus(goal.id, nextStatus)
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (nextStatus == com.example.ui.screens.finance.model.SavingsGoalStatus.PAUSED) "هدف پس‌انداز متوقف شد" else "پس‌انداز هدف ادامه یافت"
                            )
                        }
                    },
                    onDeleteGoalClick = { gId ->
                        viewModel.deleteSavingsGoal(gId)
                        scope.launch { snackbarHostState.showSnackbar("هدف پس‌انداز حذف شد") }
                    }
                )
            }

            FinanceSubScreen.RECURRING_TRANSACTIONS -> {
                RecurringTransactionsScreen(
                    recurringList = state.recurringTransactions,
                    onBackClick = { navigateBack() },
                    onAddClick = {
                        editingRecurring = null
                        showAddRecurringSheet = true
                    },
                    onEditClick = { rec ->
                        editingRecurring = rec
                        showAddRecurringSheet = true
                    },
                    onDeleteClick = { recId ->
                        viewModel.deleteRecurringTransaction(recId)
                        scope.launch { snackbarHostState.showSnackbar("تراکنش دوره‌ای حذف شد") }
                    },
                    onToggleEnable = { recId, enabled ->
                        viewModel.toggleRecurringTransaction(recId, enabled)
                    }
                )
            }

            FinanceSubScreen.CATEGORIES -> {
                CategoryManagementScreen(
                    categories = state.categories,
                    onBackClick = { navigateBack() },
                    onSaveCategory = { cat ->
                        viewModel.saveCategory(cat)
                        scope.launch { snackbarHostState.showSnackbar("دسته‌بندی ذخیره شد") }
                    },
                    onToggleCategoryActive = { catId, active ->
                        viewModel.toggleCategoryActive(catId, active)
                    },
                    onDeleteCategory = { catId ->
                        viewModel.deleteCategory(catId)
                        scope.launch { snackbarHostState.showSnackbar("دسته‌بندی حذف شد") }
                    }
                )
            }
        }
    }

    // Modal Bottom Sheets
    if (showTransferSheet) {
        TransferSheet(
            accounts = state.accounts,
            onDismiss = { showTransferSheet = false },
            onSubmitTransfer = { srcId, amountStr, desc, transferDate, recipientName ->
                val parsedAmtRials = com.example.util.IranianAmountUtils.parseAmountToBigDecimal(amountStr)?.let { entered ->
                    runCatching { com.example.util.MoneyAmount(entered, com.example.util.MoneyFormatter.activeCurrency).asRialsLongExact() }.getOrNull()
                }
                if (parsedAmtRials != null && parsedAmtRials > 0L) {
                    val now = System.currentTimeMillis()
                    val currentTime = PersianCalendarHelper.fromEpochMillis(now)
                    val (transferYear, transferMonth, transferDay) = com.example.util.IranianDateUtils.parsePersianDate(transferDate)
                    val transferDateMillis = PersianCalendarHelper.jalaliToEpochMillis(
                        transferYear, transferMonth, transferDay, currentTime.hour, currentTime.minute
                    )
                    val transferTx = TransactionItemData(
                        id = java.util.UUID.randomUUID().toString(),
                        title = "انتقال وجه",
                        amount = parsedAmtRials,
                        amountUnit = com.example.ui.screens.settings.model.AppCurrency.RIAL,
                        type = TransactionType.TRANSFER,
                        category = FinanceDefaultCategories.transferCategory,
                        dateMillis = transferDateMillis,
                        datePersian = PersianCalendarHelper.fromEpochMillis(transferDateMillis).toFormattedDate(),
                        timePersian = PersianCalendarHelper.fromEpochMillis(now).toFormattedTime(),
                        description = desc,
                        accountName = state.accounts.find { it.id == srcId }?.name ?: "کارت بانکی",
                        transferSourceAccountId = srcId,
                        transferDestinationAccountId = null,
                        transferRecipientName = recipientName.trim().takeIf { it.isNotEmpty() }
                    )
                    viewModel.addTransaction(transferTx) { res ->
                        if (res == TransactionOperationResult.SUCCESS) {
                            scope.launch { snackbarHostState.showSnackbar("انتقال وجه با موفقیت انجام شد") }
                            showTransferSheet = false
                        } else {
                            val errorMsg = when(res) {
                                TransactionOperationResult.VALIDATION_ERROR -> "خطا در تأیید اطلاعات انتقال"
                                TransactionOperationResult.NOT_FOUND -> "حساب مورد نظر یافت نشد"
                                else -> "خطا در انجام انتقال وجه"
                            }
                            scope.launch { snackbarHostState.showSnackbar(errorMsg) }
                        }
                    }
                }
            }
        )
    }

    if (showAddTxSheet) {
        AddTransactionSheet(
            initialType = addTxInitialType,
            initialTransaction = editingTransaction,
            categories = state.categories,
            accounts = state.accounts,
            onDismiss = {
                showAddTxSheet = false
                editingTransaction = null
            },
            onSubmitTransaction = { item ->
                if (editingTransaction != null) {
                    viewModel.updateTransaction(item) { res ->
                        if (res == TransactionOperationResult.SUCCESS) {
                            if (selectedTransactionDetail?.id == item.id) {
                                selectedTransactionDetail = item
                            }
                            scope.launch { snackbarHostState.showSnackbar("تراکنش به‌روزرسانی شد") }
                            showAddTxSheet = false
                            editingTransaction = null
                        } else {
                            scope.launch { snackbarHostState.showSnackbar("خطا در ویرایش تراکنش") }
                        }
                    }
                } else {
                    viewModel.addTransaction(item) { res ->
                        if (res == TransactionOperationResult.SUCCESS) {
                            scope.launch { snackbarHostState.showSnackbar("تراکنش جدید با موفقیت ثبت شد") }
                            showAddTxSheet = false
                            editingTransaction = null
                        } else {
                            scope.launch { snackbarHostState.showSnackbar("خطا در ثبت تراکنش") }
                        }
                    }
                }
            }
        )
    }

    if (showAddBudgetSheet) {
        AddBudgetSheet(
            initialBudget = editingBudget,
            categories = state.categories,
            onDismiss = {
                showAddBudgetSheet = false
                editingBudget = null
            },
            onSubmit = { b ->
                if (editingBudget != null) {
                    viewModel.updateBudget(b)
                    scope.launch { snackbarHostState.showSnackbar("بودجه به‌روزرسانی شد") }
                } else {
                    viewModel.addBudget(b)
                    scope.launch { snackbarHostState.showSnackbar("بودجه جدید افزوده شد") }
                }
                showAddBudgetSheet = false
                editingBudget = null
            }
        )
    }

    if (showAddGoalSheet) {
        AddSavingsGoalSheet(
            initialGoal = editingGoal,
            onDismiss = {
                showAddGoalSheet = false
                editingGoal = null
            },
            onSubmit = { goal ->
                if (editingGoal != null) {
                    viewModel.updateSavingsGoal(goal)
                    scope.launch { snackbarHostState.showSnackbar("هدف پس‌انداز ویرایش شد") }
                } else {
                    viewModel.addSavingsGoal(goal)
                    scope.launch { snackbarHostState.showSnackbar("هدف پس‌انداز جدید ثبت شد") }
                }
                showAddGoalSheet = false
                editingGoal = null
            }
        )
    }

    depositingGoal?.let { goal ->
        DepositGoalSheet(
            goal = goal,
            onDismiss = { depositingGoal = null },
            onDeposit = { depositAmount ->
                viewModel.depositToSavingsGoal(goal.id, depositAmount)
                depositingGoal = null
                scope.launch { snackbarHostState.showSnackbar("مبلغ به هدف پس‌انداز واریز شد") }
            }
        )
    }

    if (showAddRecurringSheet) {
        AddRecurringTransactionSheet(
            initialItem = editingRecurring,
            categories = state.categories,
            accounts = state.accounts,
            onDismiss = {
                showAddRecurringSheet = false
                editingRecurring = null
            },
            onSubmit = { rec ->
                if (editingRecurring != null) {
                    viewModel.updateRecurringTransaction(rec)
                    scope.launch { snackbarHostState.showSnackbar("تراکنش دوره‌ای ویرایش شد") }
                } else {
                    viewModel.addRecurringTransaction(rec)
                    scope.launch { snackbarHostState.showSnackbar("تراکنش دوره‌ای جدید ثبت شد") }
                }
                showAddRecurringSheet = false
                editingRecurring = null
            }
        )
    }
}
