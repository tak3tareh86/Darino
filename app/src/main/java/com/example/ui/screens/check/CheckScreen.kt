package com.example.ui.screens.check

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
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
    var selectedCheckForEdit by remember { mutableStateOf<Check?>(null) }
    var selectedCheckForDetail by remember { mutableStateOf<Check?>(null) }

    val accounts by viewModel.accounts.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var operationErrorDialog by remember { mutableStateOf<String?>(null) }

    fun showOperationResult(result: com.example.ui.screens.check.data.CheckOperationResult, successMessage: String) {
        val message = when (result) {
            com.example.ui.screens.check.data.CheckOperationResult.SUCCESS -> successMessage
            com.example.ui.screens.check.data.CheckOperationResult.VALIDATION_ERROR -> "اطلاعات واردشده یا وضعیت مالی چک با این عملیات سازگار نیست؛ مبلغ، تاریخ‌ها و وضعیت تسویه را بررسی کنید."
            com.example.ui.screens.check.data.CheckOperationResult.NOT_FOUND -> "این چک دیگر در پایگاه داده وجود ندارد؛ فهرست را بررسی کنید."
            com.example.ui.screens.check.data.CheckOperationResult.PERSISTENCE_ERROR -> "ذخیره‌سازی انجام نشد. اطلاعات تغییری نکرده؛ دوباره تلاش کنید."
            com.example.ui.screens.check.data.CheckOperationResult.NO_AUTHENTICATED_USER -> "برای مدیریت چک‌ها ابتدا وارد حساب کاربری شوید."
        }
        if (result == com.example.ui.screens.check.data.CheckOperationResult.SUCCESS) {
            coroutineScope.launch { snackbarHostState.showSnackbar(message) }
        } else {
            operationErrorDialog = message
        }
    }

    Scaffold(
        topBar = {},
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text("ثبت چک جدید") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            )
        },
        bottomBar = bottomBar
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Two Summary Cards at the very top of page content
            item {
                CheckSummaryCards(checks = uiState.checks)
            }

            // 2. Search Bar
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text("جستجو در چک‌ها...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
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
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
            }

            // 3. Main Tabs ("همه", "دریافتی", "پرداختی") right below search bar
            item {
                val selectedTabIndex = when(uiState.selectedType) {
                    null -> 0
                    CheckType.RECEIVED -> 1
                    CheckType.PAID -> 2
                }

                PrimaryTabRow(
                    selectedTabIndex = selectedTabIndex,
                    modifier = Modifier.fillMaxWidth(),
                    divider = {},
                    indicator = {
                        TabRowDefaults.PrimaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(selectedTabIndex),
                            width = 64.dp,
                            shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                        )
                    }
                ) {
                    Tab(
                        selected = uiState.selectedType == null,
                        onClick = { viewModel.onTypeSelected(null) },
                        text = { Text("همه") }
                    )
                    Tab(
                        selected = uiState.selectedType == CheckType.RECEIVED,
                        onClick = { viewModel.onTypeSelected(CheckType.RECEIVED) },
                        text = { Text("دریافتی") }
                    )
                    Tab(
                        selected = uiState.selectedType == CheckType.PAID,
                        onClick = { viewModel.onTypeSelected(CheckType.PAID) },
                        text = { Text("پرداختی") }
                    )
                }
            }

            // 4. Status Filters appropriately below tabs
            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = uiState.selectedStatus == null,
                        onClick = { viewModel.onStatusSelected(null) },
                        label = { Text("همه وضعیت‌ها") }
                    )

                    CheckStatus.entries.forEach { status ->
                        FilterChip(
                            selected = uiState.selectedStatus == status,
                            onClick = { viewModel.onStatusSelected(status) },
                            label = { Text(status.title) }
                        )
                    }
                }
            }

            // 5. List of checks or empty state
            if (uiState.filteredChecks.isEmpty()) {
                item {
                    EmptyChecksState()
                }
            } else {
                items(uiState.filteredChecks, key = { it.id }) { check ->
                    CheckCard(
                        check = check,
                        onClick = { selectedCheckForDetail = check }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
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
}

@Composable
private fun EmptyChecksState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Assignment,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
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
