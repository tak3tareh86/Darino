package com.example.ui.screens.check

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AppSectionHeader
import com.example.ui.screens.check.components.AddEditCheckDialog
import com.example.ui.screens.check.components.CheckCard
import com.example.ui.screens.check.components.CheckSummaryCards
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.ui.screens.check.model.Check
import com.example.ui.screens.check.model.CheckStatus
import com.example.ui.screens.check.model.CheckType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CheckScreen(
    viewModel: CheckViewModel = viewModel(),
    bottomBar: @Composable () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showMaturityScreen by rememberSaveable { mutableStateOf(false) }
    var selectedMainTab by rememberSaveable { mutableIntStateOf(0) }
    var reportSearchQuery by rememberSaveable { mutableStateOf("") }
    var reportSelectedYear by rememberSaveable { mutableIntStateOf(0) }
    var reportSelectedMonth by rememberSaveable { mutableIntStateOf(0) }
    var reportSelectedTypeName by rememberSaveable { mutableStateOf("ALL") }
    val reportSelectedType = when (reportSelectedTypeName) {
        "RECEIVED" -> CheckType.RECEIVED
        "PAID" -> CheckType.PAID
        else -> null
    }

    val reportAvailableYears = remember(uiState.checks, reportSelectedType) {
        CheckReportFilter.availableYears(uiState.checks, reportSelectedType)
    }
    val reportYearFilter = reportSelectedYear.takeIf { it != 0 }
    val reportMonthFilter = reportSelectedMonth.takeIf { it != 0 && reportYearFilter != null }
    val reportResults = remember(uiState.checks, reportYearFilter, reportMonthFilter, reportSearchQuery, reportSelectedType) {
        CheckReportFilter.filter(
            uiState.checks,
            reportYearFilter,
            reportMonthFilter,
            reportSearchQuery,
            reportSelectedType
        )
    }
    val reportSummary = remember(reportResults) {
        CheckReportFilter.summarize(reportResults)
    }

    LaunchedEffect(reportAvailableYears, uiState.isLoading, reportSelectedYear) {
        if (!uiState.isLoading && reportAvailableYears.isNotEmpty() &&
            reportSelectedYear != 0 && reportSelectedYear !in reportAvailableYears
        ) {
            reportSelectedYear = 0
            reportSelectedMonth = 0
        }
    }
    LaunchedEffect(selectedMainTab) {
        if (selectedMainTab < 3) {
            viewModel.onTypeSelected(
                when (selectedMainTab) {
                    1 -> CheckType.RECEIVED
                    2 -> CheckType.PAID
                    else -> null
                }
            )
        }
    }
    
    var showStatusFilters by remember { mutableStateOf(false) }
    val tabBackStack = remember { mutableStateListOf(selectedMainTab) }
    fun selectMainTab(index: Int) {
        if (selectedMainTab != index) {
            tabBackStack.add(index)
            selectedMainTab = index
        }
    }

    var selectedCheckForEdit by remember { mutableStateOf<Check?>(null) }
    var selectedCheckForDetail by remember { mutableStateOf<Check?>(null) }

    val accounts by viewModel.accounts.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var operationErrorDialog by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = showMaturityScreen || showAddDialog || selectedCheckForEdit != null || selectedCheckForDetail != null || operationErrorDialog != null || showStatusFilters || tabBackStack.size > 1) {
        when {
            showMaturityScreen -> showMaturityScreen = false
            operationErrorDialog != null -> operationErrorDialog = null
            selectedCheckForDetail != null -> selectedCheckForDetail = null
            selectedCheckForEdit != null -> selectedCheckForEdit = null
            showAddDialog -> showAddDialog = false
            showStatusFilters -> showStatusFilters = false
            tabBackStack.size > 1 -> {
                tabBackStack.removeAt(tabBackStack.lastIndex)
                selectedMainTab = tabBackStack.last()
            }
        }
    }

    fun showOperationResult(result: com.example.ui.screens.check.data.CheckOperationResult, successMessage: String) {
        val message = when (result) {
            com.example.ui.screens.check.data.CheckOperationResult.SUCCESS -> successMessage
            com.example.ui.screens.check.data.CheckOperationResult.VALIDATION_ERROR -> "اطلاعات واردشده یا وضعیت مالی چک با این عملیات سازگار نیست؛ مبلغ، تاریخ‌ها و وضعیت تسویه را بررسی کنید."
            com.example.ui.screens.check.data.CheckOperationResult.NOT_FOUND -> "این چک دیگر در پایگاه داده وجود ندارد؛ فهرست را بررسی کنید."
            com.example.ui.screens.check.data.CheckOperationResult.PERSISTENCE_ERROR -> "ذخیره‌سازی انجام نشد. اطلاعات تغییری نکرده؛ دوباره تلاش کنید."
            com.example.ui.screens.check.data.CheckOperationResult.REMINDER_SETUP_ERROR -> "یادآور چک ثبت نشد؛ چک ذخیره نشد. مجوز اعلان‌های اندروید، تنظیمات اعلان و مجوز آلارم دقیق دستگاه را بررسی کنید."
            com.example.ui.screens.check.data.CheckOperationResult.NO_AUTHENTICATED_USER -> "برای مدیریت چک‌ها ابتدا وارد حساب کاربری شوید."
        }
        if (result == com.example.ui.screens.check.data.CheckOperationResult.SUCCESS) {
            coroutineScope.launch { snackbarHostState.showSnackbar(message) }
        } else {
            operationErrorDialog = message
        }
    }

    Scaffold(
        topBar = {
            AppSectionHeader(
                title = if (showMaturityScreen) "راس‌گیری چک" else "مدیریت چک‌ها",
                subtitle = if (showMaturityScreen) "محاسبه میانگین موزون سررسید" else "نمای کلی چک‌های دریافتی و پرداختی",
                leadingIcon = {
                    if (showMaturityScreen) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { showMaturityScreen = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowForward,
                                contentDescription = "بازگشت به مدیریت چک‌ها",
                                tint = if (MaterialTheme.colorScheme.background.red < 0.2f) Color.White else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.ReceiptLong,
                            contentDescription = "مدیریت چک‌ها",
                            tint = if (MaterialTheme.colorScheme.background.red < 0.2f) Color.White else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    if (!showMaturityScreen) {
                        TextButton(onClick = { showMaturityScreen = true }) {
                            Icon(Icons.Rounded.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("راس‌گیری", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            if (!showMaturityScreen) ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text("ثبت چک جدید", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp),
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 8.dp
                )
            )
        },
        bottomBar = bottomBar
    ) { padding ->
        if (showMaturityScreen) {
            Box(Modifier.fillMaxSize().padding(padding)) {
                CheckMaturityScreen(checks = uiState.checks, onBack = { showMaturityScreen = false })
            }
        } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {


            // Existing operational summary remains outside the historical report tab.
            if (selectedMainTab != 3) {
                item { CheckSummaryCards(checks = uiState.checks) }
            }

            // Search and status controls retain their original ViewModel callbacks.
            if (selectedMainTab != 3) {
                item(key = "check-search-controls") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = { viewModel.onSearchQueryChanged(it) },
                                placeholder = { Text("جست‌وجو در چک‌ها...", maxLines = 1) },
                                modifier = Modifier.weight(1f).heightIn(min = 54.dp),
                                shape = RoundedCornerShape(18.dp),
                                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                                trailingIcon = if (uiState.searchQuery.isNotEmpty()) {
                                    {
                                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                            Icon(Icons.Rounded.Clear, null)
                                        }
                                    }
                                } else null,
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            FilledTonalButton(
                                onClick = { showStatusFilters = !showStatusFilters },
                                modifier = Modifier.height(54.dp),
                                shape = RoundedCornerShape(18.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Icon(Icons.Rounded.FilterList, null)
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    if (uiState.selectedStatus == null) "فیلتر" else uiState.selectedStatus?.title.orEmpty(),
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }

            // Compact segmented navigation for operational tabs and report.
            item(key = "check-main-tabs") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    PrimaryTabRow(
                        selectedTabIndex = selectedMainTab,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = {},
                        indicator = {
                            TabRowDefaults.PrimaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(selectedMainTab),
                                width = 48.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                            )
                        }
                    ) {
                        Tab(selected = selectedMainTab == 0, onClick = { selectMainTab(0) }, text = { Text("همه", maxLines = 1) })
                        Tab(selected = selectedMainTab == 1, onClick = { selectMainTab(1) }, text = { Text("دریافتی", maxLines = 1) })
                        Tab(selected = selectedMainTab == 2, onClick = { selectMainTab(2) }, text = { Text("پرداختی", maxLines = 1) })
                        Tab(selected = selectedMainTab == 3, onClick = { selectMainTab(3); showStatusFilters = false }, text = { Text("گزارش", maxLines = 1) })
                    }
                }
            }

            if (selectedMainTab != 3) {
            // 4. Expandable, fixed two-column status-filter panel
            item {
                AnimatedVisibility(visible = showStatusFilters) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val statusOptions: List<CheckStatus?> = listOf(null) + CheckStatus.entries
                            statusOptions.chunked(2).forEach { rowOptions ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    rowOptions.forEach { status ->
                                        FilterChip(
                                            selected = uiState.selectedStatus == status,
                                            onClick = {
                                                viewModel.onStatusSelected(status)
                                                showStatusFilters = false
                                            },
                                            modifier = Modifier.weight(1f),
                                            label = {
                                                Text(
                                                    text = status?.title ?: "همه وضعیت‌ها",
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }
                                        )
                                    }
                                    if (rowOptions.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            }

            if (selectedMainTab == 3) {
                checkReportSection(
                    results = reportResults,
                    availableYears = reportAvailableYears,
                    summary = reportSummary,
                    hasAnyChecks = uiState.checks.isNotEmpty(),
                    isLoading = uiState.isLoading,
                    selectedYear = reportSelectedYear,
                    selectedMonth = reportSelectedMonth,
                    selectedType = reportSelectedType,
                    query = reportSearchQuery,
                    onQueryChange = { reportSearchQuery = it },
                    onYearChange = { reportSelectedYear = it; reportSelectedMonth = 0 },
                    onMonthChange = { reportSelectedMonth = it },
                    onTypeChange = { type ->
                        reportSelectedTypeName = type?.name ?: "ALL"
                        reportSelectedYear = 0
                        reportSelectedMonth = 0
                    },
                    onCheckClick = { selectedCheckForDetail = it }
                )
            } else {
                if (uiState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator() }
                    }
                } else if (uiState.filteredChecks.isEmpty()) {
                    item { EmptyChecksState() }
                } else {
                    items(uiState.filteredChecks, key = { it.id }) { check ->
                        CheckCard(check = check, onClick = { selectedCheckForDetail = check })
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
        }
    }

    if (showAddDialog) {
        AddEditCheckDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { newCheck ->
                viewModel.addCheck(newCheck) { result ->
                    showOperationResult(result, "چک با موفقیت ثبت شد.")
                    if (result == com.example.ui.screens.check.data.CheckOperationResult.SUCCESS) showAddDialog = false
                }
            }
        )
    }

    if (selectedCheckForEdit != null) {
        AddEditCheckDialog(
            check = selectedCheckForEdit,
            onDismiss = { selectedCheckForEdit = null },
            onConfirm = { updatedCheck ->
                viewModel.updateCheck(updatedCheck) { result ->
                    showOperationResult(result, "تغییرات چک ذخیره شد.")
                    if (result == com.example.ui.screens.check.data.CheckOperationResult.SUCCESS) selectedCheckForEdit = null
                }
            }
        )
    }

    if (selectedCheckForDetail != null) {
        com.example.ui.screens.check.components.CheckDetailDialog(
            check = selectedCheckForDetail!!,
            accounts = accounts,
            onDismiss = { selectedCheckForDetail = null },
            onEdit = {
                selectedCheckForEdit = selectedCheckForDetail
                selectedCheckForDetail = null
            },
            onDelete = {
                viewModel.deleteCheck(selectedCheckForDetail!!.id) { result ->
                    showOperationResult(result, "چک حذف شد.")
                    if (result == com.example.ui.screens.check.data.CheckOperationResult.SUCCESS) selectedCheckForDetail = null
                }
            },
            onSettle = { account ->
                viewModel.settleCheck(selectedCheckForDetail!!, account) { result ->
                    showOperationResult(result, "تسویه ثبت شد و تراکنش مالی به حساب اضافه شد.")
                    if (result == com.example.ui.screens.check.data.CheckOperationResult.SUCCESS) selectedCheckForDetail = null
                }
            },
        )
    }

    operationErrorDialog?.let { message ->
        AlertDialog(
            onDismissRequest = { operationErrorDialog = null },
            title = { Text("عملیات انجام نشد") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { operationErrorDialog = null }) { Text("متوجه شدم") }
            }
        )
    }
}

@Composable
private fun EmptyChecksState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(104.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Assignment,
                contentDescription = null,
                modifier = Modifier.size(54.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "هنوز چکی ثبت نکرده‌اید",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "برای مدیریت چک‌های دریافتی و پرداختی خود، از دکمه «ثبت چک جدید» استفاده کنید.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
