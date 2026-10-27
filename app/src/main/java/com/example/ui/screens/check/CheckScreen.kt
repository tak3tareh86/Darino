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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.check.components.AddEditCheckDialog
import com.example.ui.screens.check.components.CheckCard
import com.example.ui.screens.check.components.CheckStats
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.ui.screens.check.model.Check
import com.example.ui.screens.check.model.CheckStatus
import com.example.ui.screens.check.model.CheckType

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(top = 8.dp)
            ) {
                // Header with Search
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = { Text("جستجو در چک‌ها...") },
                        modifier = Modifier.weight(1f),
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

                val selectedTabIndex = when(uiState.selectedType) {
                    null -> 0
                    CheckType.RECEIVED -> 1
                    CheckType.PAID -> 2
                }

                // Main Tabs
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
        },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Status Filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedStatus == null,
                    onClick = { viewModel.onStatusSelected(null) },
                    label = { Text("همه وضعیت‌ها") }
                )
                
                CheckStatus.entries.take(3).forEach { status ->
                    FilterChip(
                        selected = uiState.selectedStatus == status,
                        onClick = { viewModel.onStatusSelected(status) },
                        label = { Text(status.title) }
                    )
                }
            }

            if (uiState.checks.isNotEmpty()) {
                CheckStats(
                    checks = uiState.checks,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            if (uiState.filteredChecks.isEmpty()) {
                EmptyChecksState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.filteredChecks, key = { it.id }) { check ->
                        CheckCard(
                            check = check,
                            onClick = { selectedCheckForDetail = check }
                        )
                    }
                    
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditCheckDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = {
                viewModel.addCheck(it)
                showAddDialog = false
            }
        )
    }

    if (selectedCheckForEdit != null) {
        AddEditCheckDialog(
            check = selectedCheckForEdit,
            onDismiss = { selectedCheckForEdit = null },
            onConfirm = {
                viewModel.updateCheck(it)
                selectedCheckForEdit = null
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
                viewModel.deleteCheck(selectedCheckForDetail!!.id)
                selectedCheckForDetail = null
            },
            onSettle = { account ->
                viewModel.settleCheck(selectedCheckForDetail!!, account)
                selectedCheckForDetail = null
            },
            onStatusChange = { newStatus ->
                viewModel.updateCheckStatus(selectedCheckForDetail!!, newStatus)
                selectedCheckForDetail = null
            }
        )
    }
}

@Composable
private fun EmptyChecksState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
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
