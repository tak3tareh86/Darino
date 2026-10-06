package com.example.ui.screens.finance.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PersianAmountInputField
import com.example.ui.components.PersianDateInputField
import com.example.ui.screens.finance.model.FinanceDefaultCategories
import com.example.ui.screens.finance.model.PaymentMethod
import com.example.ui.screens.finance.model.RecurringFrequency
import com.example.ui.screens.finance.model.TransactionCategory
import com.example.ui.screens.finance.model.TransactionItemData
import com.example.ui.screens.finance.model.TransactionSourceType
import com.example.ui.screens.finance.model.TransactionType
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.util.IranianAmountUtils
import com.example.util.MoneyFormatter
import com.example.util.PersianCalendarHelper
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionSheet(
    initialType: TransactionType = TransactionType.EXPENSE,
    initialTransaction: TransactionItemData? = null,
    categories: List<TransactionCategory> = FinanceDefaultCategories.allDefaultCategories,
    accounts: List<com.example.ui.screens.finance.model.Account> = emptyList(),
    onDismiss: () -> Unit,
    onSubmitTransaction: (TransactionItemData) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    val todayPersian = remember {
        PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate()
    }

    var selectedType by remember { mutableStateOf(initialTransaction?.type ?: initialType) }
    var rawAmount by remember { mutableStateOf(initialTransaction?.amount?.toString() ?: "") }
    var title by remember { mutableStateOf(initialTransaction?.title ?: "") }

    val availableCategories = remember(selectedType, categories) {
        if (selectedType == TransactionType.INCOME) {
            categories.filter { it.type == TransactionType.INCOME && it.isActive }
        } else {
            categories.filter { it.type == TransactionType.EXPENSE && it.isActive }
        }
    }

    var selectedCategory by remember {
        mutableStateOf(
            initialTransaction?.category ?: availableCategories.firstOrNull() ?: FinanceDefaultCategories.defaultExpenseCategories[0]
        )
    }

    var selectedSubCategory by remember { mutableStateOf(initialTransaction?.subCategory) }
    var paymentMethod by remember { mutableStateOf(initialTransaction?.paymentMethod ?: PaymentMethod.BANK_CARD) }
    var datePersian by remember { mutableStateOf(initialTransaction?.datePersian ?: todayPersian) }
    var timePersian by remember {
        mutableStateOf(
            initialTransaction?.timePersian ?: PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedTime()
        )
    }
    var description by remember { mutableStateOf(initialTransaction?.description ?: "") }
    var tagsText by remember { mutableStateOf(initialTransaction?.tags?.joinToString("، ") ?: "") }
    val activeAccounts = remember(accounts) { accounts.filter { it.isActive } }
    var selectedAccount by remember(activeAccounts, initialTransaction) {
        mutableStateOf(
            activeAccounts.find { it.id == initialTransaction?.accountId }
        )
    }
    var selectedSourceAccount by remember(activeAccounts, initialTransaction) {
        mutableStateOf(
            activeAccounts.find { it.id == initialTransaction?.transferSourceAccountId }
        )
    }
    var selectedDestinationAccount by remember(activeAccounts, initialTransaction) {
        mutableStateOf(
            activeAccounts.find { it.id == initialTransaction?.transferDestinationAccountId }
        )
    }
    var isRecurring by remember { mutableStateOf(initialTransaction?.isRecurring ?: false) }
    var recurringFrequency by remember { mutableStateOf(initialTransaction?.recurringFrequency ?: RecurringFrequency.MONTHLY) }

    var isAdvancedExpanded by remember { mutableStateOf(initialTransaction != null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val parsedAmount = IranianAmountUtils.parseAmountToLong(rawAmount)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = RadiusLG, topEnd = RadiusLG),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
                .testTag("add_transaction_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialTransaction != null) "ویرایش تراکنش" else "ثبت تراکنش جدید",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "بستن",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 1. Transaction Type Selector Tabs (Expense / Income / Transfer)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TransactionType.entries.forEach { type ->
                    val isSelected = selectedType == type
                    val activeBg = when (type) {
                        TransactionType.EXPENSE -> ExpenseRoseLight
                        TransactionType.INCOME -> EmeraldPrimaryLight
                        TransactionType.TRANSFER -> InfoIndigoLight
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) activeBg else Color.Transparent)
                            .clickable {
                                selectedType = type
                                when (type) {
                                    TransactionType.TRANSFER -> {
                                        selectedCategory = FinanceDefaultCategories.transferCategory
                                        selectedSubCategory = null
                                    }
                                    TransactionType.INCOME -> {
                                        val cats = categories.filter { it.type == TransactionType.INCOME && it.isActive }
                                        cats.firstOrNull()?.let { selectedCategory = it }
                                        selectedSubCategory = selectedCategory.subCategories.firstOrNull()
                                    }
                                    TransactionType.EXPENSE -> {
                                        val cats = categories.filter { it.type == TransactionType.EXPENSE && it.isActive }
                                        cats.firstOrNull()?.let { selectedCategory = it }
                                        selectedSubCategory = selectedCategory.subCategories.firstOrNull()
                                    }
                                }
                                if (title.isBlank() || title == FinanceDefaultCategories.transferCategory.title) {
                                    title = if (type == TransactionType.TRANSFER) "" else selectedCategory.title
                                }
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = type.title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.5.sp
                            ),
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Account Selector Section
            if (selectedType != TransactionType.TRANSFER) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "انتخاب حساب",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (activeAccounts.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(RadiusMD),
                            color = Color(0xFFEF4444).copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "هیچ حساب فعالی یافت نشد. لطفاً ابتدا از بخش تنظیمات حساب اضافه کنید.",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFEF4444)),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            activeAccounts.forEach { acc ->
                                val isSelected = selectedAccount?.id == acc.id
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedAccount = acc },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                    border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = acc.name,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 12.sp
                                            ),
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = com.example.util.MoneyFormatter.formatToman(acc.initialBalance) + " تومان",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Transfer Account Selection
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Source Account
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "حساب مبدأ",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            activeAccounts.forEach { acc ->
                                val isSelected = selectedSourceAccount?.id == acc.id
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedSourceAccount = acc },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) InfoIndigoLight else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                    border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Text(
                                        text = acc.name,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    
                    // Destination Account
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "حساب مقصد",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            activeAccounts.forEach { acc ->
                                val isSelected = selectedDestinationAccount?.id == acc.id
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedDestinationAccount = acc },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) EmeraldPrimaryLight else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                    border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Text(
                                        text = acc.name,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Amount Input Field with Live Toman Formatter & Persian Words
            PersianAmountInputField(
                value = rawAmount,
                onValueChange = { formatted ->
                    rawAmount = formatted
                    errorMessage = null
                },
                onRawAmountChange = { rawDigits, _ ->
                    rawAmount = rawDigits
                    errorMessage = null
                },
                label = "مبلغ (تومان)",
                placeholder = "مثال: ۴۵۰,۰۰۰",
                unitLabel = "تومان",
                showWordsPreview = true,
                testTag = "tx_amount_input"
            )

            // 3. Category Selection (Horizontal Chips)
            if (selectedType != TransactionType.TRANSFER) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "دسته‌بندی",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableCategories.forEach { category ->
                            val isSelected = selectedCategory.id == category.id
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) category.accentColor.copy(alpha = 0.18f) else if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) category.accentColor else if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        selectedCategory = category
                                        selectedSubCategory = category.subCategories.firstOrNull()
                                        if (title.isBlank()) {
                                            title = category.title
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(text = category.iconEmoji, fontSize = 14.sp)
                                    Text(
                                        text = category.title,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 11.sp
                                        ),
                                        color = if (isSelected) category.accentColor else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Title Input Field
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "عنوان تراکنش",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorMessage = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_title_input"),
                    placeholder = { Text("مثال: خرید سوپرمارکت، حقوق، بنزین", style = MaterialTheme.typography.bodyMedium) },
                    singleLine = true,
                    shape = RoundedCornerShape(RadiusMD),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f)
                    )
                )
            }

            // Expandable Advanced Options (Subcategory, Date, Time, Payment Method, Description, Tags, Recurring)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isAdvancedExpanded = !isAdvancedExpanded }
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isAdvancedExpanded) "بستن گزینه‌های پیشرفته" else "گزینه‌های بیشتر (روش پرداخت، تاریخ، تکرار و...)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                Icon(
                    imageVector = if (isAdvancedExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = isAdvancedExpanded) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Subcategories if available
                    if (selectedCategory.subCategories.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "زیردسته‌بندی",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                selectedCategory.subCategories.forEach { sub ->
                                    val isSubSelected = selectedSubCategory == sub
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSubSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSubSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                        ),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { selectedSubCategory = sub }
                                    ) {
                                        Text(
                                            text = sub,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                            color = if (isSubSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Payment Method Selector
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "روش پرداخت / حساب",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PaymentMethod.entries.forEach { method ->
                                val isSelected = paymentMethod == method
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { paymentMethod = method }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(text = method.iconEmoji, fontSize = 12.sp)
                                        Text(
                                            text = method.title,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 10.5.sp
                                            ),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Date & Time Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PersianDateInputField(
                            value = datePersian,
                            onValueChange = { datePersian = it },
                            modifier = Modifier.weight(1.3f),
                            label = "تاریخ",
                            placeholder = "۱۴۰۴/۰۷/۱۵",
                            dialogTitle = "انتخاب تاریخ تراکنش"
                        )

                        Column(modifier = Modifier.weight(0.7f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "ساعت",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = timePersian,
                                onValueChange = { timePersian = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(RadiusMD)
                            )
                        }
                    }

                    // Description & Tags
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "توضیحات و یادداشت (اختیاری)",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("توضیحات تکمیلی...", style = MaterialTheme.typography.bodySmall) },
                            maxLines = 3,
                            shape = RoundedCornerShape(RadiusMD)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "برچسب‌ها (با ویرگول جدا کنید)",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = tagsText,
                            onValueChange = { tagsText = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("مثال: ضروری، شخصی، سفر", style = MaterialTheme.typography.bodySmall) },
                            singleLine = true,
                            shape = RoundedCornerShape(RadiusMD)
                        )
                    }

                    // Recurring Transaction Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(RadiusMD))
                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Repeat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "تراکنش تکرارشونده",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Switch(
                            checked = isRecurring,
                            onCheckedChange = { isRecurring = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    if (isRecurring) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            RecurringFrequency.entries.forEach { freq ->
                                val isSelected = recurringFrequency == freq
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { recurringFrequency = freq },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                                ) {
                                    Text(
                                        text = freq.title,
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Error Message
            errorMessage?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    color = Color(0xFFEF4444)
                )
            }

            // Submit Button
            Button(
                onClick = {
                    if (selectedType != TransactionType.TRANSFER) {
                        if (selectedAccount == null) {
                            errorMessage = "لطفاً حساب مورد نظر را انتخاب کنید."
                            return@Button
                        }
                        if (activeAccounts.isEmpty()) {
                            errorMessage = "هیچ حساب فعالی موجود نیست."
                            return@Button
                        }
                    } else {
                        if (selectedSourceAccount == null || selectedDestinationAccount == null) {
                            errorMessage = "لطفاً حساب‌های مبدأ و مقصد را انتخاب کنید."
                            return@Button
                        }
                        if (selectedSourceAccount?.id == selectedDestinationAccount?.id) {
                            errorMessage = "حساب مبدأ و مقصد نمی‌توانند یکسان باشند."
                            return@Button
                        }
                    }
                    if (parsedAmount <= 0L) {
                        errorMessage = "لطفاً مبلغ معتبری وارد کنید."
                        return@Button
                    }
                    val finalTitle = if (selectedType == TransactionType.TRANSFER) {
                        "انتقال از ${selectedSourceAccount?.name} به ${selectedDestinationAccount?.name}"
                    } else {
                        title.ifBlank { selectedCategory.title }
                    }

                    val tags = tagsText.split("،", ",").map { it.trim() }.filter { it.isNotEmpty() }

                    val item = TransactionItemData(
                        id = initialTransaction?.id ?: UUID.randomUUID().toString(),
                        title = finalTitle,
                        amount = parsedAmount,
                        type = selectedType,
                        category = selectedCategory,
                        categoryId = selectedCategory.id,
                        subCategory = selectedSubCategory,
                        datePersian = datePersian,
                        timePersian = timePersian,
                        description = description,
                        paymentMethod = paymentMethod,
                        accountName = if (selectedType == TransactionType.TRANSFER) 
                            "انتقال وجه" 
                        else 
                            (selectedAccount?.name ?: paymentMethod.title),
                        accountId = selectedAccount?.id,
                        transferSourceAccountId = if (selectedType == TransactionType.TRANSFER) selectedSourceAccount?.id else null,
                        transferDestinationAccountId = if (selectedType == TransactionType.TRANSFER) selectedDestinationAccount?.id else null,
                        sourceType = initialTransaction?.sourceType ?: TransactionSourceType.MANUAL,
                        sourceId = initialTransaction?.sourceId,
                        tags = tags,
                        isRecurring = isRecurring,
                        recurringFrequency = if (isRecurring) recurringFrequency else null
                    )

                    onSubmitTransaction(item)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("submit_tx_btn"),
                shape = RoundedCornerShape(RadiusMD),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (selectedType) {
                        TransactionType.EXPENSE -> ExpenseRoseLight
                        TransactionType.INCOME -> EmeraldPrimaryLight
                        TransactionType.TRANSFER -> InfoIndigoLight
                    }
                )
            ) {
                Text(
                    text = if (initialTransaction != null) "ذخیره تغییرات" else "ثبت نهایی ${selectedType.title}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
