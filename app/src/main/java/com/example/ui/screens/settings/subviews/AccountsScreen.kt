package com.example.ui.screens.settings.subviews

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import com.example.ui.components.PersianAmountInputField
import com.example.util.IranianAmountUtils
import com.example.util.IranianPhoneUtils
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.finance.viewmodel.FinancialViewModel
import com.example.ui.screens.finance.model.Account as DomainAccount
import com.example.ui.screens.finance.model.AccountType
import com.example.ui.screens.finance.data.TransactionOperationResult
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.screens.settings.components.SettingsConfirmationDialog
import com.example.ui.screens.settings.components.SettingsEmptyState
import com.example.ui.screens.settings.components.SettingsHeader
import com.example.ui.screens.settings.model.AccountSettingItem
import com.example.ui.screens.settings.model.SettingsMockDataSource
import com.example.ui.theme.ButtonShape
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusSM

@Composable
fun AccountsScreen(
    onBackClick: () -> Unit,
    viewModel: FinancialViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    
    var isAddSheetOpen by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<DomainAccount?>(null) }
    var accountToDelete by remember { mutableStateOf<DomainAccount?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SettingsHeader(
                title = "مدیریت حساب‌ها",
                subtitle = "حساب‌های بانکی، کارت‌ها و کیف پول",
                showBack = true,
                showSearch = false,
                onBackClick = onBackClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingAccount = null
                    isAddSheetOpen = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(RadiusMD),
                modifier = Modifier.testTag("add_account_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "افزودن حساب",
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "افزودن حساب",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.accounts.isEmpty()) {
                SettingsEmptyState(
                    title = "هنوز حسابی اضافه نکرده‌اید",
                    description = "برای مدیریت هزینه‌ها و درآمدهای خود، حداقل یک حساب بانکی یا کیف پول نقدی تعریف کنید.",
                    iconRes = R.drawable.img_3d_empty_wallet,
                    actionButtonText = "افزودن اولین حساب",
                    onActionClick = {
                        editingAccount = null
                        isAddSheetOpen = true
                    }
                )
            } else {
                val accountBalances = remember(state.accounts, state.allTransactions) {
                    state.accounts.associate { acc ->
                        var balance = acc.initialBalance
                        state.allTransactions.forEach { tx ->
                            if (tx.accountId == acc.id) {
                                when (tx.type) {
                                    com.example.ui.screens.finance.model.TransactionType.INCOME -> balance += tx.amount
                                    com.example.ui.screens.finance.model.TransactionType.EXPENSE -> balance -= tx.amount
                                    else -> {}
                                }
                            } else if (tx.type == com.example.ui.screens.finance.model.TransactionType.TRANSFER) {
                                if (tx.transferSourceAccountId == acc.id) {
                                    balance -= tx.amount
                                } else if (tx.transferDestinationAccountId == acc.id) {
                                    balance += tx.amount
                                }
                            }
                        }
                        acc.id to balance
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        // Total Balance Card
                        val totalBalance = accountBalances.values.sum()
                        TotalAccountsBalanceCard(
                            totalBalanceFormatted = com.example.util.MoneyFormatter.formatToman(totalBalance), 
                            count = state.accounts.size
                        )
                    }

                    item {
                        Text(
                            text = "لیست حساب‌های فعال (${state.accounts.size})",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                        )
                    }

                    items(state.accounts, key = { it.id }) { account ->
                        val realBal = accountBalances[account.id] ?: account.initialBalance
                        AccountManageCard(
                            account = account,
                            realBalance = realBal,
                            onEdit = {
                                editingAccount = account
                                isAddSheetOpen = true
                            },
                            onDelete = {
                                accountToDelete = account
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    // Add / Edit Account Bottom Sheet
    if (isAddSheetOpen) {
        AddEditAccountSheet(
            account = editingAccount,
            onDismiss = { isAddSheetOpen = false },
            onSave = { savedAccount ->
                if (editingAccount != null) {
                    viewModel.updateAccount(savedAccount)
                } else {
                    viewModel.addAccount(savedAccount)
                }
                isAddSheetOpen = false
            }
        )
    }

    // Confirmation Dialog
    SettingsConfirmationDialog(
        isOpen = accountToDelete != null,
        title = "حذف حساب ${accountToDelete?.name ?: ""}",
        message = "آیا از حذف این حساب مطمئن هستید؟ تمامی تراکنش‌های مرتبط با این حساب از برنامه حذف خواهند شد.",
        confirmButtonText = "حذف حساب",
        onConfirm = {
            accountToDelete?.let { viewModel.deleteAccount(it.id) }
            accountToDelete = null
        },
        onDismiss = { accountToDelete = null }
    )
}

@Composable
private fun TotalAccountsBalanceCard(
    totalBalanceFormatted: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 8.dp else 4.dp,
                shape = RoundedCornerShape(RadiusLG),
                ambientColor = Color.Black.copy(alpha = 0.2f),
                spotColor = EmeraldPrimaryLight.copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(RadiusLG),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                listOf(
                    EmeraldPrimaryLight.copy(alpha = 0.4f),
                    Color.Transparent
                )
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "مجموع موجودی حساب‌ها",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = totalBalanceFormatted,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "تومان",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(EmeraldPrimaryLight.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "$count حساب فعال",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    color = EmeraldPrimaryLight
                )
            }
        }
    }
}

@Composable
fun AccountManageCard(
    account: DomainAccount,
    realBalance: Long,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    var showMenu by remember { mutableStateOf(false) }

    val iconRes = when(account.type) {
        AccountType.CASH -> R.drawable.img_3d_wallet
        AccountType.BANK -> R.drawable.img_3d_bank
        AccountType.CARD -> R.drawable.img_3d_card
        else -> R.drawable.img_3d_bank
    }
    
    val accentColor = when(account.type) {
        AccountType.CASH -> EmeraldPrimaryLight
        AccountType.BANK -> Color(0xFFE11D48)
        AccountType.CARD -> Color(0xFF2563EB)
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 6.dp else 2.dp,
                shape = RoundedCornerShape(RadiusLG),
                ambientColor = Color.Black.copy(alpha = 0.1f),
                spotColor = Color.Black.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(RadiusLG),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(RadiusMD))
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = iconRes),
                            contentDescription = account.name,
                            modifier = Modifier.size(36.dp),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = account.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.12f))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = account.type.name,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = accentColor
                                )
                            }
                        }

                        Text(
                            text = "${account.bankName ?: "بانک من"} • ${account.accountNumberMasked ?: "•••• ••••"}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "عملیات",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        shape = RoundedCornerShape(RadiusMD)
                    ) {
                        DropdownMenuItem(
                            text = { Text("ویرایش حساب") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("حذف حساب", color = ExpenseRoseLight) },
                            leadingIcon = {
                                Icon(Icons.Rounded.Delete, contentDescription = null, tint = ExpenseRoseLight)
                            },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            // Balance rows (Initial and Current)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(RadiusSM))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.3f else 0.5f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Initial Balance Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "موجودی اولیه:",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = com.example.util.MoneyFormatter.formatToman(account.initialBalance),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "تومان",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }

                // Current Real Balance Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "موجودی فعلی:",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = com.example.util.MoneyFormatter.formatToman(realBalance),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = if (realBalance >= 0) EmeraldPrimaryLight else ExpenseRoseLight
                        )
                        Text(
                            text = "تومان",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = if (realBalance >= 0) EmeraldPrimaryLight else ExpenseRoseLight
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAccountSheet(
    account: DomainAccount?,
    onDismiss: () -> Unit,
    onSave: (DomainAccount) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accountTypes = mapOf(
        "بانک" to AccountType.BANK,
        "کیف پول" to AccountType.CASH,
        "کارت" to AccountType.CARD,
        "سایر" to AccountType.OTHER
    )

    var name by remember { mutableStateOf(account?.name ?: "") }
    var bankName by remember { mutableStateOf(account?.bankName ?: "") }
    var balance by remember { mutableStateOf(account?.initialBalance?.toString() ?: "") }
    var accountNumber by remember { mutableStateOf(account?.accountNumberMasked ?: "") }
    var selectedType by remember { mutableStateOf(account?.type ?: AccountType.BANK) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = RadiusLG, topEnd = RadiusLG)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (account != null) "ویرایش حساب" else "افزودن حساب جدید",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Account Type Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "نوع حساب",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(accountTypes.entries.toList()) { (label, type) ->
                        val isSelected = type == selectedType
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(RadiusSM))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedType = type }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                ),
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Input Fields
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("نام حساب (مثلاً کارت حقوق)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(RadiusMD)
            )

            OutlinedTextField(
                value = bankName,
                onValueChange = { bankName = it },
                label = { Text("نام بانک / موسسه") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(RadiusMD)
            )

            PersianAmountInputField(
                value = balance,
                onValueChange = { balance = it },
                label = "موجودی اولیه (تومان)",
                placeholder = "مثال: ۵,۰۰۰,۰۰۰",
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = accountNumber,
                onValueChange = { accountNumber = it },
                label = { Text("شماره کارت / حساب (اختیاری)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(RadiusMD)
            )

            Button(
                onClick = {
                    val saved = DomainAccount(
                        id = account?.id ?: "acc_${System.currentTimeMillis()}",
                        userId = account?.userId ?: "",
                        name = name.ifBlank { "حساب جدید" },
                        bankName = bankName.ifBlank { null },
                        accountNumberMasked = accountNumber.ifBlank { null },
                        type = selectedType,
                        initialBalance = IranianAmountUtils.parseAmountToLong(balance) ?: 0L,
                        isActive = account?.isActive ?: true
                    )
                    onSave(saved)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_account_button"),
                shape = ButtonShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = "ذخیره حساب",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = Color.White
                )
            }
        }
    }
}
