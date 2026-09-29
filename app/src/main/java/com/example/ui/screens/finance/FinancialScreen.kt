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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.finance.components.AddBudgetSheet
import com.example.ui.screens.finance.components.AddRecurringTransactionSheet
import com.example.ui.screens.finance.components.AddSavingsGoalSheet
import com.example.ui.screens.finance.components.AddTransactionSheet
import com.example.ui.screens.finance.components.DepositGoalSheet
import com.example.ui.screens.finance.components.FinanceQuickLinks
import com.example.ui.screens.finance.components.FinancialFilterSheet
import com.example.ui.screens.finance.components.FinancialHeader
import com.example.ui.screens.finance.components.FinancialQuickActions
import com.example.ui.screens.finance.components.FinancialSummaryCard
import com.example.ui.screens.finance.components.MonthlyBudgetCard
import com.example.ui.screens.finance.components.RecentTransactionsSection
import com.example.ui.screens.finance.model.Budget
import com.example.ui.screens.finance.model.FinanceFilterPeriod
import com.example.ui.screens.finance.model.RecurringTransaction
import com.example.ui.screens.finance.model.SavingsGoal
import com.example.ui.screens.finance.model.TransactionCategory
import com.example.ui.screens.finance.model.TransactionItemData
import com.example.ui.screens.finance.model.TransactionType
import com.example.ui.screens.finance.screens.BudgetScreen
import com.example.ui.screens.finance.screens.CategoryManagementScreen
import com.example.ui.screens.finance.screens.RecurringTransactionsScreen
import com.example.ui.screens.finance.screens.SavingsGoalsScreen
import com.example.ui.screens.finance.screens.TransactionDetailScreen
import com.example.ui.screens.finance.screens.TransactionsScreen
import com.example.ui.screens.finance.viewmodel.FinancialViewModel
import com.example.ui.screens.home.components.BottomNavItem
import com.example.ui.screens.home.components.HomeBottomNavigation
import kotlinx.coroutines.launch

enum class FinanceSubScreen {
    MAIN,
    ALL_TRANSACTIONS,
    TRANSACTION_DETAIL,
    BUDGETS,
    SAVINGS_GOALS,
    RECURRING_TRANSACTIONS,
    CATEGORIES
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialScreen(
    currentNavTab: BottomNavItem = BottomNavItem.FINANCE,
    onNavigateToTab: (BottomNavItem) -> Unit = {},
    bottomBar: (@Composable () -> Unit)? = null,
    viewModel: FinancialViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentSubScreen by remember { mutableStateOf(FinanceSubScreen.MAIN) }
    var selectedPeriod by remember { mutableStateOf(FinanceFilterPeriod.THIS_MONTH) }

    // Bottom Sheets State
    var showFilterSheet by remember { mutableStateOf(false) }
    var showAddTxSheet by remember { mutableStateOf(false) }
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

    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                        FinancialHeader(
                            currentFilterText = selectedPeriod.title,
                            onFilterClick = { showFilterSheet = true }
                        )
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
                        // 1. Monthly Financial Status Card (Income, Expense, Balance, Savings Rate)
                        FinancialSummaryCard(
                            formattedIncome = state.formattedIncome,
                            formattedExpense = state.formattedExpense,
                            formattedBalance = state.formattedBalance,
                            savingsRate = state.savingsRate,
                            onCardClick = { currentSubScreen = FinanceSubScreen.ALL_TRANSACTIONS }
                        )

                        // 2. Monthly Budget Card (Total, Spent, Remaining, Progress, Status)
                        MonthlyBudgetCard(
                            totalBudgetFormatted = state.formattedMonthlyBudget,
                            spentFormatted = state.formattedBudgetUsed,
                            remainingFormatted = state.formattedBudgetRemaining,
                            usagePercentage = state.budgetProgress,
                            status = state.budgetStatus,
                            onCardClick = { currentSubScreen = FinanceSubScreen.BUDGETS }
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
                                        addTxInitialType = TransactionType.TRANSFER
                                        editingTransaction = null
                                        showAddTxSheet = true
                                    }
                                }
                            }
                        )

                        // 4. Quick Links: بودجه‌ها، اهداف پس‌انداز، تکرارشونده، دسته‌بندی‌ها
                        FinanceQuickLinks(
                            onNavigateToBudgets = { currentSubScreen = FinanceSubScreen.BUDGETS },
                            onNavigateToSavingsGoals = { currentSubScreen = FinanceSubScreen.SAVINGS_GOALS },
                            onNavigateToRecurring = { currentSubScreen = FinanceSubScreen.RECURRING_TRANSACTIONS },
                            onNavigateToCategories = { currentSubScreen = FinanceSubScreen.CATEGORIES }
                        )

                        // 5. Recent Transactions Section (Max 4 items + "همه ›" button)
                        RecentTransactionsSection(
                            transactions = state.recentTransactions,
                            onTransactionClick = { tx ->
                                selectedTransactionDetail = tx
                                currentSubScreen = FinanceSubScreen.TRANSACTION_DETAIL
                            },
                            onEditClick = { tx ->
                                editingTransaction = tx
                                showAddTxSheet = true
                            },
                            onDuplicateClick = { tx ->
                                viewModel.duplicateTransaction(tx.id)
                                scope.launch { snackbarHostState.showSnackbar("تراکنش کپی و تکرار شد") }
                            },
                            onDeleteClick = { tx ->
                                viewModel.deleteTransaction(tx.id)
                                scope.launch { snackbarHostState.showSnackbar("تراکنش با موفقیت حذف شد") }
                            },
                            onSeeAllClick = {
                                currentSubScreen = FinanceSubScreen.ALL_TRANSACTIONS
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
                    onBackClick = { currentSubScreen = FinanceSubScreen.MAIN },
                    onTransactionClick = { tx ->
                        selectedTransactionDetail = tx
                        currentSubScreen = FinanceSubScreen.TRANSACTION_DETAIL
                    },
                    onAddNewTransaction = {
                        addTxInitialType = TransactionType.EXPENSE
                        editingTransaction = null
                        showAddTxSheet = true
                    },
                    onDuplicateTransaction = { txId ->
                        viewModel.duplicateTransaction(txId)
                        scope.launch { snackbarHostState.showSnackbar("تراکنش با موفقیت تکرار شد") }
                    },
                    onDeleteTransaction = { txId ->
                        viewModel.deleteTransaction(txId)
                        scope.launch { snackbarHostState.showSnackbar("تراکنش حذف شد") }
                    }
                )
            }

            FinanceSubScreen.TRANSACTION_DETAIL -> {
                selectedTransactionDetail?.let { tx ->
                    TransactionDetailScreen(
                        transaction = tx,
                        onBackClick = { currentSubScreen = FinanceSubScreen.MAIN },
                        onEditClick = {
                            editingTransaction = tx
                            showAddTxSheet = true
                        },
                        onDuplicateClick = {
                            viewModel.duplicateTransaction(tx.id)
                            currentSubScreen = FinanceSubScreen.MAIN
                            scope.launch { snackbarHostState.showSnackbar("تراکنش کپی شد") }
                        },
                        onDeleteClick = {
                            viewModel.deleteTransaction(tx.id)
                            currentSubScreen = FinanceSubScreen.MAIN
                            scope.launch { snackbarHostState.showSnackbar("تراکنش حذف شد") }
                        }
                    )
                } ?: run {
                    currentSubScreen = FinanceSubScreen.MAIN
                }
            }

            FinanceSubScreen.BUDGETS -> {
                BudgetScreen(
                    budgets = state.activeBudgets,
                    onBackClick = { currentSubScreen = FinanceSubScreen.MAIN },
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
                        viewModel.toggleBudget(bId, enabled)
                    }
                )
            }

            FinanceSubScreen.SAVINGS_GOALS -> {
                SavingsGoalsScreen(
                    goals = state.savingsGoals,
                    onBackClick = { currentSubScreen = FinanceSubScreen.MAIN },
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
                    onDeleteGoalClick = { gId ->
                        viewModel.deleteSavingsGoal(gId)
                        scope.launch { snackbarHostState.showSnackbar("هدف پس‌انداز حذف شد") }
                    }
                )
            }

            FinanceSubScreen.RECURRING_TRANSACTIONS -> {
                RecurringTransactionsScreen(
                    recurringList = state.recurringTransactions,
                    onBackClick = { currentSubScreen = FinanceSubScreen.MAIN },
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
                    onBackClick = { currentSubScreen = FinanceSubScreen.MAIN },
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
    if (showFilterSheet) {
        FinancialFilterSheet(
            sheetState = filterSheetState,
            selectedPeriod = selectedPeriod,
            onPeriodSelected = { period ->
                selectedPeriod = period
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false }
        )
    }

    if (showAddTxSheet) {
        AddTransactionSheet(
            initialType = addTxInitialType,
            initialTransaction = editingTransaction,
            categories = state.categories,
            onDismiss = {
                showAddTxSheet = false
                editingTransaction = null
            },
            onSubmitTransaction = { item ->
                if (editingTransaction != null) {
                    viewModel.updateTransaction(item)
                    if (selectedTransactionDetail?.id == item.id) {
                        selectedTransactionDetail = item
                    }
                    scope.launch { snackbarHostState.showSnackbar("تراکنش به‌روزرسانی شد") }
                } else {
                    viewModel.addTransaction(item)
                    scope.launch { snackbarHostState.showSnackbar("تراکنش جدید با موفقیت ثبت شد") }
                }
                showAddTxSheet = false
                editingTransaction = null
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
