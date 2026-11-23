package com.example.ui.screens.finance.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.util.IranianAmountUtils
import com.example.util.IranianPhoneUtils
import com.example.util.MoneyFormatter
import com.example.util.MoneyAmount
import com.example.ui.screens.settings.model.AppCurrency
import com.example.util.PersianCalendarHelper
import com.example.calendar.domain.CalendarDateUtils
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
    val activeCurrency = MoneyFormatter.activeCurrency
    var rawAmount by remember {
        mutableStateOf(
            initialTransaction?.let {
                runCatching {
                    MoneyAmount.of(it.amount, it.amountUnit).inUnit(activeCurrency).value.stripTrailingZeros().toPlainString()
                }.getOrDefault("")
            } ?: ""
        )
    }
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
            initialTransaction?.category ?: if (selectedType == TransactionType.TRANSFER) {
                FinanceDefaultCategories.transferCategory
            } else {
                availableCategories.firstOrNull() ?: FinanceDefaultCategories.defaultExpenseCategories[0]
            }
        )
    }

    var selectedSubCategory by remember { mutableStateOf(initialTransaction?.subCategory) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var categoryDraft by remember { mutableStateOf(selectedCategory) }
    var categorySearchQuery by remember { mutableStateOf("") }
    val categoryPickerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var paymentMethod by remember { mutableStateOf(initialTransaction?.paymentMethod ?: PaymentMethod.BANK_CARD) }
    var datePersian by remember { mutableStateOf(initialTransaction?.datePersian ?: todayPersian) }
    var timePersian by remember {
        mutableStateOf(
            initialTransaction?.timePersian ?: PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedTime()
        )
    }
    var description by remember { mutableStateOf(initialTransaction?.description ?: "") }
    var transferRecipientName by remember { mutableStateOf(initialTransaction?.transferRecipientName.orEmpty()) }
    var tagsText by remember { mutableStateOf(initialTransaction?.tags?.joinToString("، ") ?: "") }
    val activeAccounts = remember(accounts) { accounts.filter { it.isActive } }
    var selectedSourceAccount by remember(activeAccounts, initialTransaction) {
        mutableStateOf(
            activeAccounts.find { it.id == initialTransaction?.transferSourceAccountId }
                ?: if (initialTransaction == null) {
                    activeAccounts.find { it.name == "کارت بانکی" || it.name.startsWith("کارت بانکی") } ?: activeAccounts.firstOrNull()
                } else null
        )
    }
    LaunchedEffect(activeAccounts) {
        if (initialTransaction == null && selectedSourceAccount == null && activeAccounts.isNotEmpty()) {
            selectedSourceAccount = activeAccounts.find { it.name == "کارت بانکی" || it.name.startsWith("کارت بانکی") } ?: activeAccounts.firstOrNull()
        }
    }
    var isRecurring by remember { mutableStateOf(initialTransaction?.isRecurring ?: false) }
    var recurringFrequency by remember { mutableStateOf(initialTransaction?.recurringFrequency ?: RecurringFrequency.MONTHLY) }

    var isAdvancedExpanded by remember { mutableStateOf(initialTransaction != null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val parsedAmountRials = remember(rawAmount, activeCurrency) {
        IranianAmountUtils.parseAmountToBigDecimal(rawAmount)?.let { entered ->
            runCatching { MoneyAmount(entered, activeCurrency).asRialsLongExact() }.getOrNull()
        }
    }

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
                                        selectedSubCategory = null
                                    }
                                    TransactionType.EXPENSE -> {
                                        val cats = categories.filter { it.type == TransactionType.EXPENSE && it.isActive }
                                        cats.firstOrNull()?.let { selectedCategory = it }
                                        selectedSubCategory = null
                                    }
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

            if (selectedType != TransactionType.TRANSFER) {
                // Payment method selection remains independent of account management.
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "روش پرداخت",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            PaymentMethod.BANK_CARD,
                            PaymentMethod.CASH,
                            PaymentMethod.WALLET
                        ).forEach { method ->
                            val isSelected = paymentMethod == method
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { paymentMethod = method },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                border = if (isSelected) null
                                else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text(
                                    text = "${method.iconEmoji} ${method.title}",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
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

                }
            }

            // 2. Amount Input Field in the selected IRR unit
            PersianAmountInputField(
                value = rawAmount,
                onValueChange = { formatted ->
                    rawAmount = formatted
                    errorMessage = null
                },
                allowDecimalInput = true,
                lenientGroupingWhileEditing = true,
                onRawDecimalAmountChange = { rawValue, _ ->
                    rawAmount = rawValue
                    errorMessage = null
                },
                label = "مبلغ (${MoneyFormatter.getUnitLabel(activeCurrency)})",
                placeholder = "مثال: ۴۵۰,۰۰۰ یا ۱۲۳٫۴",
                unitLabel = MoneyFormatter.getUnitLabel(activeCurrency),
                showWordsPreview = false,
                testTag = "tx_amount_input"
            )

            // 3. Category Selection (Three-column dropdown)
            if (selectedType != TransactionType.TRANSFER) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "دسته‌بندی",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(RadiusMD))
                            .clickable {
                                categoryDraft = selectedCategory
                                categorySearchQuery = ""
                                showCategoryPicker = true
                            },
                        shape = RoundedCornerShape(RadiusMD),
                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(text = selectedCategory.iconEmoji, fontSize = 18.sp)
                            Text(
                                text = selectedCategory.title,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Rounded.ExpandMore,
                                contentDescription = "نمایش دسته‌بندی‌ها",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (showCategoryPicker) {
                    ModalBottomSheet(
                        onDismissRequest = { showCategoryPicker = false },
                        sheetState = categoryPickerSheetState,
                        containerColor = if (isDark) Color(0xFF0B1320) else MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        dragHandle = {
                            Box(
                                modifier = Modifier
                                    .padding(top = 10.dp, bottom = 4.dp)
                                    .size(width = 48.dp, height = 5.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                            )
                        }
                    ) {
                        androidx.compose.foundation.layout.BoxWithConstraints(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val sheetMaxHeight = maxHeight
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = maxHeight * 0.82f)
                                    .padding(horizontal = 20.dp)
                                    .padding(bottom = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Spacer(modifier = Modifier.size(40.dp))
                                    Text(
                                        text = "انتخاب دسته‌بندی",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    IconButton(
                                        onClick = { showCategoryPicker = false },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "بستن",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = categorySearchQuery,
                                    onValueChange = { categorySearchQuery = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("category_picker_search"),
                                    singleLine = true,
                                    placeholder = { Text("جستجو در دسته‌بندی‌ها...") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Search,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    )
                                )

                                val filteredCategories = availableCategories.filter {
                                    it.title.contains(categorySearchQuery.trim(), ignoreCase = true)
                                }

                                if (filteredCategories.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (availableCategories.isEmpty()) {
                                                "دسته‌بندی فعالی برای این نوع تراکنش وجود ندارد."
                                            } else {
                                                "دسته‌بندی موردنظر پیدا نشد."
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(2),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 180.dp, max = sheetMaxHeight * 0.44f)
                                            .testTag("category_picker_grid"),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(filteredCategories, key = { it.id }) { category ->
                                            val isSelected = categoryDraft.id == category.id
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = 64.dp)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .clickable { categoryDraft = category },
                                                shape = RoundedCornerShape(16.dp),
                                                color = if (isSelected) {
                                                    MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.20f else 0.10f)
                                                } else if (isDark) {
                                                    Color(0xFF172238)
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                                },
                                                border = BorderStroke(
                                                    width = if (isSelected) 1.5.dp else 1.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.outlineVariant
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                                                ) {
                                                    Text(text = category.iconEmoji, fontSize = 22.sp)
                                                    Text(
                                                        text = category.title,
                                                        modifier = Modifier.weight(1f),
                                                        style = MaterialTheme.typography.labelLarge.copy(
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                            fontSize = 12.sp
                                                        ),
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 2
                                                    )
                                                    if (isSelected) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.CheckCircle,
                                                            contentDescription = "انتخاب‌شده",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(19.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    androidx.compose.material3.OutlinedButton(
                                        onClick = { showCategoryPicker = false },
                                        modifier = Modifier.weight(0.85f).height(50.dp),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text("لغو")
                                    }
                                    Button(
                                        onClick = {
                                            selectedCategory = categoryDraft
                                            selectedSubCategory = null
                                            showCategoryPicker = false
                                        },
                                        modifier = Modifier
                                            .weight(1.15f)
                                            .height(50.dp)
                                            .testTag("confirm_category_selection"),
                                        enabled = filteredCategories.any { it.id == categoryDraft.id },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Text("تأیید انتخاب", fontWeight = FontWeight.Bold)
                                    }
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
                    placeholder = { Text("مثال: کرایه اسنپ، مترو، حقوق ماهانه", style = MaterialTheme.typography.bodyMedium) },
                    singleLine = true,
                    shape = RoundedCornerShape(RadiusMD),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f)
                    )
                )
            }



            if (selectedType == TransactionType.TRANSFER) {
                OutlinedTextField(
                    value = transferRecipientName,
                    onValueChange = { transferRecipientName = it },
                    label = { Text("کسی که پول را برایش انتقال دادیم (اختیاری)") },
                    modifier = Modifier.fillMaxWidth().testTag("tx_transfer_recipient_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(RadiusMD),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = InfoIndigoLight,
                        unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f)
                    )
                )
            }

            // Date & Time Row (Directly visible)
            val context = LocalContext.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                PersianDateInputField(
                    value = datePersian,
                    onValueChange = { datePersian = it },
                    modifier = Modifier.weight(1.3f),
                    label = "تاریخ",
                    useOuterHeader = true,
                    placeholder = "۱۴۰۴/۰۷/۱۵",
                    dialogTitle = "انتخاب تاریخ تراکنش"
                )

                Column(
                    modifier = Modifier.weight(0.7f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "ساعت",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    var showTimePicker by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = timePersian,
                            onValueChange = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tx_time_input"),
                            readOnly = true,
                            singleLine = true,
                            shape = RoundedCornerShape(RadiusMD),
                            trailingIcon = {
                                IconButton(
                                    onClick = { showTimePicker = true },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ExpandMore,
                                        contentDescription = "انتخاب ساعت",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showTimePicker = true }
                        )
                    }

                    if (showTimePicker) {
                        val asciiTime = IranianPhoneUtils.convertDigitsToEnglish(timePersian)
                        val timeParts = asciiTime.split(":")
                        val initialHour = (timeParts.getOrNull(0)?.trim()?.toIntOrNull() ?: 12).coerceIn(0, 23)
                        val initialMinute = (timeParts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0).coerceIn(0, 59)
                        val timePickerState = rememberTimePickerState(
                            initialHour = initialHour,
                            initialMinute = initialMinute,
                            is24Hour = true
                        )

                        Dialog(
                            onDismissRequest = { showTimePicker = false },
                            properties = DialogProperties(usePlatformDefaultWidth = false)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                tonalElevation = 6.dp,
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .widthIn(max = 340.dp)
                                    .padding(horizontal = 16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = "انتخاب ساعت",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    TimePicker(
                                        state = timePickerState
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(onClick = { showTimePicker = false }) {
                                            Text("لغو", style = MaterialTheme.typography.labelLarge)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                val h = IranianPhoneUtils.convertDigitsToPersian(timePickerState.hour.toString().padStart(2, '0'))
                                                val m = IranianPhoneUtils.convertDigitsToPersian(timePickerState.minute.toString().padStart(2, '0'))
                                                timePersian = "$h:$m"
                                                showTimePicker = false
                                            }
                                        ) {
                                            Text("تأیید", style = MaterialTheme.typography.labelLarge)
                                        }
                                    }
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
                    if (selectedType == TransactionType.TRANSFER) {
                        if (selectedSourceAccount == null) {
                            errorMessage = "لطفاً حساب مبدأ را انتخاب کنید."
                            return@Button
                        }
                    }
                    if (parsedAmountRials == null || parsedAmountRials <= 0L) {
                        errorMessage = "لطفاً مبلغ معتبری وارد کنید."
                        return@Button
                    }
                    if (selectedType != TransactionType.TRANSFER && title.isBlank()) {
                        errorMessage = "لطفاً عنوان دقیق تراکنش را وارد کنید."
                        return@Button
                    }

                    // Expense and income transactions must be linked to a real active account.
                    // Keep the existing payment-method UI and resolve its matching default account.
                    val transactionAccount = if (selectedType != TransactionType.TRANSFER) {
                        activeAccounts.firstOrNull { it.id == initialTransaction?.accountId }
                            ?: when (paymentMethod) {
                                PaymentMethod.BANK_CARD, PaymentMethod.BANK_TRANSFER ->
                                    activeAccounts.firstOrNull {
                                        it.type == com.example.ui.screens.finance.model.AccountType.CARD ||
                                            it.name.startsWith("کارت بانکی")
                                    } ?: activeAccounts.firstOrNull()
                                PaymentMethod.CASH ->
                                    activeAccounts.firstOrNull {
                                        it.type == com.example.ui.screens.finance.model.AccountType.CASH ||
                                            it.name.contains("نقد")
                                    } ?: activeAccounts.firstOrNull()
                                PaymentMethod.WALLET ->
                                    activeAccounts.firstOrNull { it.name.contains("کیف پول") }
                                        ?: activeAccounts.firstOrNull { it.type == com.example.ui.screens.finance.model.AccountType.OTHER }
                                        ?: activeAccounts.firstOrNull()
                                PaymentMethod.OTHER -> activeAccounts.firstOrNull()
                            }
                    } else null

                    if (selectedType != TransactionType.TRANSFER && transactionAccount == null) {
                        errorMessage = "برای ثبت هزینه یا درآمد، حداقل یک حساب فعال لازم است."
                        return@Button
                    }

                    if (selectedType != TransactionType.TRANSFER &&
                        availableCategories.none { it.id == selectedCategory.id && it.type == selectedType }
                    ) {
                        errorMessage = if (selectedType == TransactionType.INCOME) {
                            "لطفاً یک دسته‌بندی فعال درآمد انتخاب کنید."
                        } else {
                            "لطفاً یک دسته‌بندی فعال هزینه انتخاب کنید."
                        }
                        return@Button
                    }
                    val parsedDate = CalendarDateUtils.parseJalali(datePersian)
                    if (parsedDate == null || !CalendarDateUtils.isValidJalaliDate(datePersian)) {
                        errorMessage = "لطفاً تاریخ معتبر وارد کنید."
                        return@Button
                    }

                    val timeParts = IranianPhoneUtils.convertDigitsToEnglish(timePersian).trim().split(":")
                    val hour = timeParts.getOrNull(0)?.toIntOrNull()
                    val minute = timeParts.getOrNull(1)?.toIntOrNull()
                    if (timeParts.size != 2 || hour == null || minute == null || hour !in 0..23 || minute !in 0..59) {
                        errorMessage = "لطفاً ساعت معتبر وارد کنید."
                        return@Button
                    }

                    val transactionDateMillis = PersianCalendarHelper.jalaliToEpochMillis(
                        parsedDate.first,
                        parsedDate.second,
                        parsedDate.third,
                        hour,
                        minute
                    )

                    val finalTitle = if (selectedType == TransactionType.TRANSFER) {
                        transferRecipientName.trim().takeIf { it.isNotEmpty() }
                            ?.let { "انتقال وجه به $it" } ?: "انتقال وجه"
                    } else {
                        title.trim()
                    }

                    val tags = tagsText.split("،", ",").map { it.trim() }.filter { it.isNotEmpty() }

                    val item = TransactionItemData(
                        id = initialTransaction?.id ?: UUID.randomUUID().toString(),
                        title = finalTitle,
                        amount = parsedAmountRials,
                        amountUnit = AppCurrency.RIAL,
                        type = selectedType,
                        category = selectedCategory,
                        categoryId = selectedCategory.id,
                        subCategory = selectedSubCategory,
                        datePersian = datePersian,
                        timePersian = timePersian,
                        dateMillis = transactionDateMillis,
                        description = description,
                        paymentMethod = paymentMethod,
                        accountName = if (selectedType == TransactionType.TRANSFER)
                            (selectedSourceAccount?.name ?: "کارت بانکی")
                        else
                            (transactionAccount?.name ?: paymentMethod.title),
                        accountId = if (selectedType == TransactionType.TRANSFER) null else transactionAccount?.id,
                        transferSourceAccountId = if (selectedType == TransactionType.TRANSFER) selectedSourceAccount?.id else null,
                        transferDestinationAccountId = null,
                        transferRecipientName = if (selectedType == TransactionType.TRANSFER) transferRecipientName.trim().takeIf { it.isNotEmpty() } else null,
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
