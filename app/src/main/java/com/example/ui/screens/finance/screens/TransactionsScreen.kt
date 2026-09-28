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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ui.screens.finance.model.FinanceFilterPeriod
import com.example.ui.screens.finance.model.TransactionCategory
import com.example.ui.screens.finance.model.TransactionItemData
import com.example.ui.screens.finance.model.TransactionType
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.RadiusMD

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
    onAddNewTransaction: () -> Unit,
    onDuplicateTransaction: (String) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf<TransactionType?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var selectedPeriodFilter by remember { mutableStateOf(FinanceFilterPeriod.ALL) }
    var selectedSortOption by remember { mutableStateOf(TransactionSortOption.NEWEST) }
    var showSortMenu by remember { mutableStateOf(false) }

    var transactionToDelete by remember { mutableStateOf<TransactionItemData?>(null) }

    // Filter & Sort Logic
    val filteredTransactions = remember(
        transactions,
        searchQuery,
        selectedTypeFilter,
        selectedCategoryFilter,
        selectedPeriodFilter,
        selectedSortOption
    ) {
        transactions
            .filter { tx ->
                val matchesSearch = searchQuery.isBlank() ||
                        tx.title.contains(searchQuery, ignoreCase = true) ||
                        tx.category.title.contains(searchQuery, ignoreCase = true) ||
                        tx.tags.any { it.contains(searchQuery, ignoreCase = true) }

                val matchesType = selectedTypeFilter == null || tx.type == selectedTypeFilter
                val matchesCategory = selectedCategoryFilter == null || tx.categoryId == selectedCategoryFilter

                matchesSearch && matchesType && matchesCategory
            }
            .let { list ->
                when (selectedSortOption) {
                    TransactionSortOption.NEWEST -> list.sortedByDescending { it.dateMillis }
                    TransactionSortOption.OLDEST -> list.sortedBy { it.dateMillis }
                    TransactionSortOption.HIGHEST_AMOUNT -> list.sortedByDescending { it.amount }
                    TransactionSortOption.LOWEST_AMOUNT -> list.sortedBy { it.amount }
                }
            }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("transactions_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "همه تراکنش‌ها",
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNewTransaction,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("fab_add_tx_list")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "ثبت تراکنش")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_tx_input"),
                placeholder = { Text("جستجو در عنوان، دسته یا برچسب...", style = MaterialTheme.typography.bodySmall) },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "پاک کردن", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f)
                )
            )

            // 2. Filter Chips Row (Type: همه، هزینه، درآمد، انتقال)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // All Filter
                FilterChipItem(
                    title = "همه انواع",
                    isSelected = selectedTypeFilter == null,
                    onClick = { selectedTypeFilter = null }
                )

                TransactionType.entries.forEach { type ->
                    FilterChipItem(
                        title = type.title,
                        isSelected = selectedTypeFilter == type,
                        onClick = { selectedTypeFilter = if (selectedTypeFilter == type) null else type }
                    )
                }

                // Separator / Category Filters
                categories.take(6).forEach { cat ->
                    val isCatSelected = selectedCategoryFilter == cat.id
                    FilterChipItem(
                        title = "${cat.iconEmoji} ${cat.title}",
                        isSelected = isCatSelected,
                        onClick = { selectedCategoryFilter = if (isCatSelected) null else cat.id }
                    )
                }
            }

            // 3. Transactions Count & Active Sort Label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredTransactions.size} تراکنش یافت شد",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "مرتب‌سازی: ${selectedSortOption.title}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 4. Lazy List of Transactions
            if (filteredTransactions.isEmpty()) {
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
                            text = "تراکنشی با این مشخصات پیدا نشد",
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
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredTransactions, key = { it.id }) { tx ->
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
private fun FilterChipItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11.sp
            ),
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
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
