package com.example.ui.screens.finance.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowDropUp
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Layered3DCard
import com.example.ui.screens.finance.model.FinanceDefaultCategories
import com.example.ui.screens.finance.model.TransactionCategory
import com.example.ui.screens.finance.model.TransactionItemData
import com.example.ui.screens.finance.model.TransactionType
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusSM
import com.example.util.MoneyFormatter

enum class TransactionSortOption(val title: String) {
    NEWEST("جدیدترین"),
    OLDEST("قدیمی‌ترین"),
    HIGHEST_AMOUNT("بیشترین مبلغ"),
    LOWEST_AMOUNT("کمترین مبلغ")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    transactions: List<TransactionItemData>,
    categories: List<TransactionCategory> = FinanceDefaultCategories.allDefaultCategories,
    onBackClick: () -> Unit,
    onTransactionClick: (TransactionItemData) -> Unit,
    onAddNewTransaction: () -> Unit = {},
    onDuplicateTransaction: (String) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    // Selected Tab Index: 0 = همه تراکنش‌ها, 1 = هزینه‌ها, 2 = درآمدها, 3 = انتقال وجه
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Search queries per tab
    var allSearchQuery by remember { mutableStateOf("") }
    var incomeSearchQuery by remember { mutableStateOf("") }
    var transferSearchQuery by remember { mutableStateOf("") }

    // Expense Dropdown Category Selector
    var selectedExpenseCategory by remember { mutableStateOf<String>("همه دسته‌بندی‌ها") }
    var showExpenseCategoryDropdown by remember { mutableStateOf(false) }

    var selectedSortOption by remember { mutableStateOf(TransactionSortOption.NEWEST) }
    var showSortMenu by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<TransactionItemData?>(null) }

    val tabTitles = listOf("همه تراکنش‌ها", "هزینه‌ها", "درآمدها", "انتقال وجه")

    // Calculations for All Transactions Summary Box
    val totalExpenseAmount = remember(transactions) {
        transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }
    val totalIncomeAmount = remember(transactions) {
        transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }
    val netBalance = totalIncomeAmount - totalExpenseAmount

    // All Expense Categories list for Dropdown
    val expenseCategories = remember(categories, transactions) {
        val catNamesFromList = categories.filter { it.type == TransactionType.EXPENSE }.map { it.title }
        val catNamesFromTx = transactions.filter { it.type == TransactionType.EXPENSE }.map { it.category.title }
        (listOf("همه دسته‌بندی‌ها") + (catNamesFromList + catNamesFromTx)).distinct()
    }

    // Filter transactions per selected tab
    val displayedTransactions = remember(
        transactions,
        selectedTabIndex,
        allSearchQuery,
        selectedExpenseCategory,
        incomeSearchQuery,
        transferSearchQuery,
        selectedSortOption
    ) {
        val filtered = when (selectedTabIndex) {
            0 -> { // همه تراکنش‌ها
                transactions.filter { tx ->
                    allSearchQuery.isBlank() ||
                            tx.title.contains(allSearchQuery, ignoreCase = true) ||
                            tx.category.title.contains(allSearchQuery, ignoreCase = true) ||
                            tx.description.contains(allSearchQuery, ignoreCase = true) ||
                            tx.accountName.contains(allSearchQuery, ignoreCase = true)
                }
            }
            1 -> { // هزینه‌ها
                transactions.filter { tx ->
                    tx.type == TransactionType.EXPENSE &&
                            (selectedExpenseCategory == "همه دسته‌بندی‌ها" || tx.category.title.contains(selectedExpenseCategory, ignoreCase = true))
                }
            }
            2 -> { // درآمدها
                transactions.filter { tx ->
                    tx.type == TransactionType.INCOME &&
                            (incomeSearchQuery.isBlank() ||
                                    tx.title.contains(incomeSearchQuery, ignoreCase = true) ||
                                    tx.category.title.contains(incomeSearchQuery, ignoreCase = true) ||
                                    tx.description.contains(incomeSearchQuery, ignoreCase = true) ||
                                    tx.accountName.contains(incomeSearchQuery, ignoreCase = true))
                }
            }
            3 -> { // انتقال وجه
                transactions.filter { tx ->
                    tx.type == TransactionType.TRANSFER &&
                            (transferSearchQuery.isBlank() ||
                                    tx.title.contains(transferSearchQuery, ignoreCase = true) ||
                                    tx.description.contains(transferSearchQuery, ignoreCase = true) ||
                                    tx.accountName.contains(transferSearchQuery, ignoreCase = true))
                }
            }
            else -> transactions
        }

        when (selectedSortOption) {
            TransactionSortOption.NEWEST -> filtered.sortedByDescending { it.dateMillis }
            TransactionSortOption.OLDEST -> filtered.sortedBy { it.dateMillis }
            TransactionSortOption.HIGHEST_AMOUNT -> filtered.sortedByDescending { it.amount }
            TransactionSortOption.LOWEST_AMOUNT -> filtered.sortedBy { it.amount }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("transactions_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "مدیریت تراکنش‌ها",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "بازگشت",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                imageVector = Icons.Rounded.Sort,
                                contentDescription = "مرتب‌سازی",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            TransactionSortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (option == selectedSortOption) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = if (option == selectedSortOption) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        selectedSortOption = option
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 4 Tabs Row Header
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Content based on selected tab:
                when (selectedTabIndex) {
                    0 -> {
                        // ================= TAB 0: همه تراکنش‌ها =================
                        // Summary Card (کارت جمع هزینه‌ها و درآمدها)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(RadiusMD),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0xFF161E2E) else Color(0xFFF8FAFC)
                            ),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF232D42) else Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Calculate,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "خلاصه کل هزینه‌ها و درآمدها",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Total Income Box
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (isDark) Color(0xFF0F3A22) else Color(0xFFE6F9EE),
                                                RoundedCornerShape(RadiusSM)
                                            )
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.TrendingUp,
                                                contentDescription = null,
                                                tint = EmeraldPrimaryLight,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "جمع درآمدها",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                ),
                                                color = EmeraldPrimaryLight
                                            )
                                        }
                                        Text(
                                            text = MoneyFormatter.formatToman(totalIncomeAmount),
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            ),
                                            color = EmeraldPrimaryLight
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Total Expense Box
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (isDark) Color(0xFF3F161C) else Color(0xFFFFECEE),
                                                RoundedCornerShape(RadiusSM)
                                            )
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.TrendingDown,
                                                contentDescription = null,
                                                tint = ExpenseRoseLight,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "جمع هزینه‌ها",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                ),
                                                color = ExpenseRoseLight
                                            )
                                        }
                                        Text(
                                            text = MoneyFormatter.formatToman(totalExpenseAmount),
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            ),
                                            color = ExpenseRoseLight
                                        )
                                    }
                                }

                                // Net Balance Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "خالص عملکرد (مانده):",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 11.5.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = MoneyFormatter.formatSignedToman(netBalance, isExpense = netBalance < 0),
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        ),
                                        color = if (netBalance >= 0) EmeraldPrimaryLight else ExpenseRoseLight
                                    )
                                }
                            }
                        }

                        // Search Box for All Transactions
                        OutlinedTextField(
                            value = allSearchQuery,
                            onValueChange = { allSearchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("جستجو در همه تراکنش‌ها...", style = MaterialTheme.typography.bodySmall) },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            trailingIcon = {
                                if (allSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { allSearchQuery = "" }) {
                                        Icon(Icons.Rounded.Clear, contentDescription = "پاک کردن", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    1 -> {
                        // ================= TAB 1: هزینه‌ها =================
                        // Modern Category Selector Bar & Chip Selector
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showExpenseCategoryDropdown = true },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.FilterList,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "دسته‌بندی: $selectedExpenseCategory",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = "تغییر دسته‌بندی",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Icon(
                                                imageVector = Icons.Rounded.ArrowDropDown,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Horizontal Chip Bar for quick switching top categories
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(expenseCategories) { categoryName ->
                                    val isSelected = categoryName == selectedExpenseCategory
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedExpenseCategory = categoryName },
                                        label = { Text(categoryName, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        // Bottom Sheet for full category picker
                        if (showExpenseCategoryDropdown) {
                            ModalBottomSheet(
                                onDismissRequest = { showExpenseCategoryDropdown = false },
                                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                                containerColor = MaterialTheme.colorScheme.surface
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "انتخاب دسته‌بندی هزینه",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 380.dp)
                                    ) {
                                        items(expenseCategories) { categoryName ->
                                            val isSelected = categoryName == selectedExpenseCategory
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(RadiusMD))
                                                    .clickable {
                                                        selectedExpenseCategory = categoryName
                                                        showExpenseCategoryDropdown = false
                                                    },
                                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent,
                                                shape = RoundedCornerShape(RadiusMD)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = categoryName,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                        ),
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    if (isSelected) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Check,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }

                        // Total sum card for selected expense category
                        val selectedCategoryTotal = remember(displayedTransactions) {
                            displayedTransactions.sumOf { it.amount }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(RadiusSM),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0xFF3F161C) else Color(0xFFFFECEE)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "مجموع هزینه‌های «$selectedExpenseCategory»:",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                                    color = ExpenseRoseLight
                                )
                                Text(
                                    text = MoneyFormatter.formatToman(selectedCategoryTotal),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                    color = ExpenseRoseLight
                                )
                            }
                        }
                    }

                    2 -> {
                        // ================= TAB 2: درآمدها =================
                        // Search Box for Incomes
                        OutlinedTextField(
                            value = incomeSearchQuery,
                            onValueChange = { incomeSearchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("جستجو در درآمدها (عنوان، حساب، توضیحات)...", style = MaterialTheme.typography.bodySmall) },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            trailingIcon = {
                                if (incomeSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { incomeSearchQuery = "" }) {
                                        Icon(Icons.Rounded.Clear, contentDescription = "پاک کردن", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        val totalFilteredIncome = remember(displayedTransactions) {
                            displayedTransactions.sumOf { it.amount }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(RadiusSM),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0xFF0F3A22) else Color(0xFFE6F9EE)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "مجموع درآمدهای یافت‌شده:",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                                    color = EmeraldPrimaryLight
                                )
                                Text(
                                    text = MoneyFormatter.formatToman(totalFilteredIncome),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                    color = EmeraldPrimaryLight
                                )
                            }
                        }
                    }

                    3 -> {
                        // ================= TAB 3: انتقال وجه =================
                        // Search Box for Transfers
                        OutlinedTextField(
                            value = transferSearchQuery,
                            onValueChange = { transferSearchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("جستجو در انتقال وجه (حساب مبدأ/مقصد)...", style = MaterialTheme.typography.bodySmall) },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            trailingIcon = {
                                if (transferSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { transferSearchQuery = "" }) {
                                        Icon(Icons.Rounded.Clear, contentDescription = "پاک کردن", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        val totalFilteredTransfer = remember(displayedTransactions) {
                            displayedTransactions.sumOf { it.amount }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(RadiusSM),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0xFF1E1B4B) else Color(0xFFEEF2FF)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "مجموع انتقال وجه‌ها:",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                                    color = InfoIndigoLight
                                )
                                Text(
                                    text = MoneyFormatter.formatToman(totalFilteredTransfer),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                    color = InfoIndigoLight
                                )
                            }
                        }
                    }
                }

                // Header info row: count + active sort
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${displayedTransactions.size} تراکنش یافت شد",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "مرتب‌سازی: ${selectedSortOption.title}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Lazy List of Transactions
                if (displayedTransactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "🔎", fontSize = 36.sp)
                            Text(
                                text = "تراکنشی یافت نشد",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "فیلترها یا عبارت جستجو را تغییر دهید",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(displayedTransactions, key = { it.id }) { tx ->
                            TransactionCard(
                                transaction = tx,
                                onClick = { onTransactionClick(tx) },
                                onDuplicate = { onDuplicateTransaction(tx.id) },
                                onDelete = { transactionToDelete = tx }
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    transactionToDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("حذف تراکنش") },
            text = { Text("آیا از حذف تراکنش «${tx.title}» به مبلغ ${tx.amountFormatted} اطمینان دارید؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteTransaction(tx.id)
                        transactionToDelete = null
                    }
                ) {
                    Text("حذف", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("انصراف")
                }
            }
        )
    }
}

@Composable
fun TransactionCard(
    transaction: TransactionItemData,
    onClick: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isExpense = transaction.type == TransactionType.EXPENSE
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    val amountColor = when (transaction.type) {
        TransactionType.EXPENSE -> ExpenseRoseLight
        TransactionType.INCOME -> if (isDark) Color(0xFF34D399) else EmeraldPrimaryLight
        TransactionType.TRANSFER -> InfoIndigoLight
    }

    Layered3DCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusMD),
        backgroundColor = MaterialTheme.colorScheme.surface,
        elevation = 2.dp,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        testTag = "full_tx_card_${transaction.id}",
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(transaction.category.accentColor.copy(alpha = if (isDark) 0.25f else 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (transaction.category.iconRes != null) {
                        Image(
                            painter = painterResource(id = transaction.category.iconRes),
                            contentDescription = transaction.category.title,
                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(text = transaction.category.iconEmoji, fontSize = 20.sp)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = transaction.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = transaction.category.title,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "${transaction.datePersian} ${transaction.timePersian}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = transaction.amountFormatted,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = amountColor
                    )
                    Text(
                        text = transaction.paymentMethod.title,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDuplicate,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = "تکرار",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "حذف",
                        tint = Color(0xFFEF4444).copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
