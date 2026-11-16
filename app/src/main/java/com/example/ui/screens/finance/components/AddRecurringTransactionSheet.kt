package com.example.ui.screens.finance.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PersianAmountInputField
import com.example.ui.components.PersianDateInputField
import com.example.ui.screens.finance.model.FinanceDefaultCategories
import com.example.ui.screens.finance.model.PaymentMethod
import com.example.ui.screens.finance.model.RecurringFrequency
import com.example.ui.screens.finance.model.RecurringTransaction
import com.example.ui.screens.finance.model.TransactionCategory
import com.example.ui.screens.finance.model.TransactionType
import com.example.ui.screens.finance.model.Account
import com.example.calendar.domain.CalendarDateUtils
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.util.IranianAmountUtils
import com.example.util.MoneyFormatter
import com.example.util.MoneyAmount
import com.example.util.PersianCalendarHelper
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecurringTransactionSheet(
    initialItem: RecurringTransaction? = null,
    categories: List<TransactionCategory> = FinanceDefaultCategories.allDefaultCategories,
    accounts: List<com.example.ui.screens.finance.model.Account> = emptyList(),
    onDismiss: () -> Unit,
    onSubmit: (RecurringTransaction) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    val today = remember { PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate() }

    var title by remember { mutableStateOf(initialItem?.title ?: "") }
    val activeCurrency = MoneyFormatter.activeCurrency
    val currencyUnit = MoneyFormatter.getUnitLabel(currency = activeCurrency)
    var rawAmount by remember {
        mutableStateOf(
            initialItem?.let { MoneyAmount.of(it.amount, it.amountUnit).inUnit(activeCurrency).value.stripTrailingZeros().toPlainString() } ?: ""
        )
    }
    var selectedType by remember { mutableStateOf(initialItem?.type ?: TransactionType.EXPENSE) }
    var selectedFrequency by remember { mutableStateOf(initialItem?.frequency ?: RecurringFrequency.MONTHLY) }
    var startDate by remember { mutableStateOf(initialItem?.startDate ?: today) }
    var nextDate by remember { mutableStateOf(initialItem?.nextExecutionDate ?: today) }
    var endDate by remember { mutableStateOf(initialItem?.endDate ?: "") }
    var paymentMethod by remember { mutableStateOf(initialItem?.paymentMethod ?: PaymentMethod.BANK_CARD) }
    var selectedAccountId by remember { mutableStateOf(initialItem?.accountId) }
    val selectedAccount = accounts.firstOrNull { it.id == selectedAccountId && it.isActive }
        ?: accounts.firstOrNull { it.name.equals(initialItem?.accountName, ignoreCase = true) && it.isActive }
        ?: accounts.firstOrNull { it.isActive }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val availableCategories = remember(selectedType, categories) {
        categories.filter { it.type == selectedType && it.isActive }
    }

    var selectedCategory by remember {
        mutableStateOf(
            availableCategories.find { it.id == initialItem?.categoryId }
                ?: availableCategories.firstOrNull()
                ?: FinanceDefaultCategories.defaultExpenseCategories[0]
        )
    }

    val parsedAmountRials = IranianAmountUtils.parseAmountToBigDecimal(rawAmount)?.let { runCatching { MoneyAmount(it, activeCurrency).asRialsLongExact() }.getOrNull() }

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
                .testTag("add_recurring_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialItem != null) "ویرایش تراکنش دوره‌ای" else "افزودن تراکنش تکرارشونده",
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

            // Type (Expense / Income)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            selectedType = TransactionType.EXPENSE
                            categories.find { it.type == TransactionType.EXPENSE }?.let { selectedCategory = it }
                        },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedType == TransactionType.EXPENSE) ExpenseRoseLight else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = "هزینه دوره‌ای",
                        modifier = Modifier.padding(vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (selectedType == TransactionType.EXPENSE) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        ),
                        color = if (selectedType == TransactionType.EXPENSE) Color.White else MaterialTheme.colorScheme.onSurface,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            selectedType = TransactionType.INCOME
                            categories.find { it.type == TransactionType.INCOME }?.let { selectedCategory = it }
                        },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedType == TransactionType.INCOME) EmeraldPrimaryLight else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = "درآمد دوره‌ای",
                        modifier = Modifier.padding(vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (selectedType == TransactionType.INCOME) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        ),
                        color = if (selectedType == TransactionType.INCOME) Color.White else MaterialTheme.colorScheme.onSurface,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            // Frequency (روزانه، هفتگی، ماهانه، سالانه)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "دوره تکرار",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RecurringFrequency.entries.forEach { freq ->
                        val isSelected = selectedFrequency == freq
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedFrequency = freq },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = freq.title,
                                modifier = Modifier.padding(vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            RecurringDropdownField(
                label = "دسته‌بندی",
                selectedLabel = "${selectedCategory.iconEmoji}  ${selectedCategory.title}",
                options = availableCategories,
                optionLabel = { "${it.iconEmoji}  ${it.title}" },
                onSelected = { category ->
                    selectedCategory = category
                    if (title.isBlank()) title = category.title
                }
            )

            RecurringDropdownField(
                label = "حساب",
                selectedLabel = selectedAccount?.name ?: "انتخاب حساب فعال",
                options = accounts.filter { it.isActive },
                optionLabel = { it.name },
                onSelected = { selectedAccountId = it.id }
            )

            RecurringDropdownField(
                label = "روش پرداخت",
                selectedLabel = paymentMethod.title,
                options = PaymentMethod.entries,
                optionLabel = { it.title },
                onSelected = { paymentMethod = it }
            )

            // Title
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "عنوان",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(RadiusMD)
                )
            }

            // Amount
            PersianAmountInputField(
                value = rawAmount,
                onValueChange = { rawAmount = it },
                allowDecimalInput = true,
                onRawDecimalAmountChange = { rawValue, _ -> rawAmount = rawValue },
                label = "مبلغ هر دوره ($currencyUnit)",
                placeholder = "مبلغ را وارد کنید",
                unitLabel = currencyUnit,
                showWordsPreview = false,
                testTag = "recurring_amount_input"
            )

            PersianDateInputField(
                value = startDate,
                onValueChange = { startDate = it },
                label = "تاریخ شروع (شمسی)",
                placeholder = "۱۴۰۵/۰۷/۰۱",
                dialogTitle = "انتخاب تاریخ شروع",
                testTag = "recurring_start_date_input"
            )

            PersianDateInputField(
                value = nextDate,
                onValueChange = { nextDate = it },
                label = "تاریخ اجرای بعدی (شمسی)",
                placeholder = "۱۴۰۵/۰۷/۰۱",
                dialogTitle = "انتخاب تاریخ اجرای بعدی",
                testTag = "recurring_next_date_input"
            )

            PersianDateInputField(
                value = endDate,
                onValueChange = { endDate = it },
                label = "تاریخ پایان (اختیاری)",
                placeholder = "در صورت نیاز انتخاب کنید",
                dialogTitle = "انتخاب تاریخ پایان",
                testTag = "recurring_end_date_input"
            )

            errorMessage?.let {
                Text(text = it, style = MaterialTheme.typography.labelSmall, color = Color(0xFFEF4444))
            }

            Button(
                onClick = {
                    if (title.isBlank()) {
                        errorMessage = "لطفاً عنوان را وارد کنید."
                        return@Button
                    }
                    if (parsedAmountRials == null || parsedAmountRials <= 0L) {
                        errorMessage = "مبلغ باید بیشتر از صفر باشد."
                        return@Button
                    }
                    if (selectedAccount == null) {
                        errorMessage = "لطفاً یک حساب فعال انتخاب کنید."
                        return@Button
                    }
                    if (!CalendarDateUtils.isValidJalaliDate(startDate) ||
                        !CalendarDateUtils.isValidJalaliDate(nextDate)
                    ) {
                        errorMessage = "تاریخ شروع و اجرای بعدی باید معتبر باشند."
                        return@Button
                    }
                    val startParts = CalendarDateUtils.parseJalali(startDate)!!
                    val nextParts = CalendarDateUtils.parseJalali(nextDate)!!
                    val startEpoch = com.example.util.PersianCalendarHelper.getJalaliEpochDay(startParts.first, startParts.second, startParts.third)
                    val nextEpoch = com.example.util.PersianCalendarHelper.getJalaliEpochDay(nextParts.first, nextParts.second, nextParts.third)
                    if (nextEpoch < startEpoch) {
                        errorMessage = "تاریخ اجرای بعدی نمی‌تواند قبل از تاریخ شروع باشد."
                        return@Button
                    }
                    if (endDate.isNotBlank()) {
                        if (!CalendarDateUtils.isValidJalaliDate(endDate)) {
                            errorMessage = "تاریخ پایان معتبر نیست."
                            return@Button
                        }
                        val endParts = CalendarDateUtils.parseJalali(endDate)!!
                        val endEpoch = com.example.util.PersianCalendarHelper.getJalaliEpochDay(endParts.first, endParts.second, endParts.third)
                        if (endEpoch < nextEpoch) {
                            errorMessage = "تاریخ پایان نمی‌تواند قبل از تاریخ اجرای بعدی باشد."
                            return@Button
                        }
                    }

                    val rec = RecurringTransaction(
                        id = initialItem?.id ?: UUID.randomUUID().toString(),
                        title = title.trim(),
                        amount = parsedAmountRials,
                        amountUnit = com.example.ui.screens.settings.model.AppCurrency.RIAL,
                        type = selectedType,
                        categoryId = selectedCategory.id,
                        categoryTitle = selectedCategory.title,
                        frequency = selectedFrequency,
                        startDate = CalendarDateUtils.toPersianDisplay(startDate),
                        endDate = endDate.takeIf { it.isNotBlank() }?.let(CalendarDateUtils::toPersianDisplay),
                        nextExecutionDate = CalendarDateUtils.toPersianDisplay(nextDate),
                        enabled = initialItem?.enabled ?: true,
                        paymentMethod = paymentMethod,
                        accountName = selectedAccount.name,
                        accountId = selectedAccount.id,
                        anchorDayOfMonth = nextParts.third,
                        anchorMonthOfYear = nextParts.second
                    )
                    onSubmit(rec)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("submit_recurring_btn"),
                shape = RoundedCornerShape(RadiusMD),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = if (initialItem != null) "ذخیره تغییرات" else "ثبت تراکنش دوره‌ای",
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


@Composable
private fun <T> RecurringDropdownField(
    label: String,
    selectedLabel: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        androidx.compose.foundation.layout.Box {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(RadiusMD))
                    .clickable { if (options.isNotEmpty()) expanded = true },
                shape = RoundedCornerShape(RadiusMD),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(selectedLabel, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                    Text("⌄", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionLabel(option)) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
