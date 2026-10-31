package com.example.ui.screens.check.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.PersianAmountInputField
import com.example.ui.components.SharedPersianDatePickerDialog
import com.example.ui.screens.check.model.Check
import com.example.ui.screens.check.model.CheckStatus
import com.example.ui.screens.check.model.CheckType
import com.example.util.IranianAmountUtils
import com.example.util.MoneyFormatter
import com.example.util.PersianCalendarHelper
import java.util.*

private val IRANIAN_BANK_NAMES = listOf(
    "ملی", "ملت", "صادرات", "تجارت",
    "سپه", "رفاه", "مسکن", "کشاورزی",
    "پاسارگاد", "پارسیان", "سامان", "شهر",
    "اقتصاد نوین", "کارآفرین", "گردشگری", "دی",
    "ایران‌زمین", "سینا", "سرمایه", "توسعه تعاون",
    "پست بانک", "قوامین", "آینده", "مهر ایران",
    "رسالت", "خاورمیانه", "توسعه صادرات", "صنعت و معدن",
    "حکمت ایرانیان", "انصار", "موسسه ملل", "موسسه نور"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCheckDialog(
    check: Check? = null,
    onDismiss: () -> Unit,
    onConfirm: (Check) -> Unit
) {
    var type by remember { mutableStateOf(check?.type ?: CheckType.RECEIVED) }
    var amount by remember {
        mutableStateOf(
            check?.amount?.let { MoneyFormatter.convertFromToman(it).toString() }.orEmpty()
        )
    }
    var showBankPickerDialog by remember { mutableStateOf(false) }
    var checkNumber by remember { mutableStateOf(check?.checkNumber ?: "") }
    var sayadId by remember { mutableStateOf(check?.sayadId ?: "") }
    var bankName by remember { mutableStateOf(check?.bankName ?: "") }
    var branch by remember { mutableStateOf(check?.branch ?: "") }
    var holderName by remember { mutableStateOf(check?.holderName ?: "") }
    var receiverName by remember { mutableStateOf(check?.receiverName ?: "") }
    var issueDate by remember { mutableStateOf(check?.issueDate ?: System.currentTimeMillis()) }
    var dueDate by remember { mutableStateOf(check?.dueDate ?: (System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000)) }
    var status by remember { mutableStateOf(check?.status ?: CheckStatus.REGISTERED) }
    var reminderDateTime by remember { mutableStateOf(check?.reminderDateTime) }
    var notes by remember { mutableStateOf(check?.notes.orEmpty()) }

    var showIssueDatePicker by remember { mutableStateOf(false) }
    var showDueDatePicker by remember { mutableStateOf(false) }
    var showReminderDatePicker by remember { mutableStateOf(false) }
    var showReminderTimePicker by remember { mutableStateOf(false) }

    val isDateInvalid = dueDate < issueDate
    // Reminder validation is based on calendar day, not time-of-day.
    val dueCalendarDay = Calendar.getInstance().apply {
        timeInMillis = dueDate
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val reminderCalendarDay = reminderDateTime?.let { reminder ->
        Calendar.getInstance().apply {
            timeInMillis = reminder
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val isReminderInvalid = reminderCalendarDay != null && reminderCalendarDay > dueCalendarDay
    val amountValueForValidation = amount.toEnglishDigits().toLongOrNull()
    val amountUnitLabel = MoneyFormatter.getUnitLabel()
    val isAmountInvalid = amountValueForValidation == null ||
        MoneyFormatter.convertToToman(amountValueForValidation ?: 0L) <= 0L
    val isCheckNumberInvalid = checkNumber.isBlank()
    val isBankNameInvalid = bankName.isBlank()
    val normalizedSayadId = sayadId.toEnglishDigits().filter { it.isDigit() }
    val isSayadIdInvalid = normalizedSayadId.isNotBlank() && normalizedSayadId.length != 16
    var showError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize().imePadding(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = if (check == null) "ثبت چک جدید" else "ویرایش چک",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "بستن")
                        }
                    },
                )
            },
        ) { padding ->
            val formScrollState = rememberScrollState()
            LaunchedEffect(showError) {
                if (showError) formScrollState.animateScrollTo(formScrollState.maxValue)
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(formScrollState)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (showError && (isDateInvalid || isReminderInvalid || isSayadIdInvalid || isAmountInvalid || isCheckNumberInvalid || isBankNameInvalid)) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth().imePadding()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Error, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                when {
                                    isAmountInvalid -> "مبلغ چک باید عددی بزرگ‌تر از صفر و به $amountUnitLabel باشد."
                                    isCheckNumberInvalid -> "شماره چک را وارد کنید."
                                    isBankNameInvalid -> "نام بانک را وارد کنید."
                                    isDateInvalid -> "تاریخ سررسید نمی‌تواند قبل از تاریخ صدور باشد."
                                    isReminderInvalid -> "زمان یادآور باید در روز سررسید یا قبل از آن باشد."
                                    else -> "شناسه صیادی واردشده باید ۱۶ رقم داشته باشد."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Type Toggle
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = type == CheckType.RECEIVED,
                        onClick = { type = CheckType.RECEIVED },
                        enabled = check?.status != CheckStatus.CLEARED,
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = { Icon(Icons.Rounded.CallReceived, null) }
                    ) {
                        Text(CheckType.RECEIVED.title)
                    }
                    SegmentedButton(
                        selected = type == CheckType.PAID,
                        onClick = { type = CheckType.PAID },
                        enabled = check?.status != CheckStatus.CLEARED,
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = { Icon(Icons.Rounded.CallMade, null) }
                    ) {
                        Text(CheckType.PAID.title)
                    }
                }

                PersianAmountInputField(
                    value = amount,
                    onValueChange = { amount = IranianAmountUtils.cleanAmountDigits(it) },
                    label = "مبلغ چک ($amountUnitLabel)",
                    unitLabel = amountUnitLabel,
                    showWordsPreview = false,
                    isError = showError && isAmountInvalid,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = check?.status != CheckStatus.CLEARED,
                    leadingIcon = { Icon(Icons.Rounded.Payments, null) },
                    testTag = "check_amount_input"
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = checkNumber,
                        onValueChange = { checkNumber = it },
                        label = { Text("شماره چک") },
                        isError = showError && isCheckNumberInvalid,
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = sayadId,
                        onValueChange = { sayadId = it.toEnglishDigits().filter { char -> char.isDigit() } },
                        label = { Text("شناسه صیادی") },
                        modifier = Modifier.weight(1.5f),
                        singleLine = true
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1.2f)) {
                        OutlinedTextField(
                            value = bankName,
                            onValueChange = { bankName = it },
                            label = { Text("نام بانک") },
                            isError = showError && isBankNameInvalid,
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Rounded.AccountBalance, null) },
                            trailingIcon = {
                                IconButton(onClick = { showBankPickerDialog = true }) {
                                    Icon(
                                        Icons.Rounded.ArrowDropDown,
                                        contentDescription = "انتخاب بانک"
                                    )
                                }
                            },
                            singleLine = true
                        )
                    }
                    OutlinedTextField(
                        value = branch,
                        onValueChange = { branch = it },
                        label = { Text("شعبه (اختیاری)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = holderName,
                        onValueChange = { holderName = it },
                        label = { Text(if (type == CheckType.RECEIVED) "صادرکننده" else "صاحب حساب ما") },
                        modifier = Modifier.weight(1f),
                        leadingIcon = { Icon(Icons.Rounded.Person, null) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = receiverName,
                        onValueChange = { receiverName = it },
                        label = { Text(if (type == CheckType.RECEIVED) "ذینفع (ما)" else "دریافت‌کننده") },
                        modifier = Modifier.weight(1f),
                        leadingIcon = { Icon(Icons.Rounded.PersonOutline, null) },
                        singleLine = true
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Issue Date
                    OutlinedCard(
                        onClick = { showIssueDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("تاریخ صدور", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = PersianCalendarHelper.fromEpochMillis(issueDate).toFormattedDate(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Due Date
                    OutlinedCard(
                        onClick = { showDueDatePicker = true },
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("تاریخ سررسید", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = PersianCalendarHelper.fromEpochMillis(dueDate).toFormattedDate(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Reminder Section
                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.NotificationsActive, 
                            null, 
                            tint = if (reminderDateTime != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "یادآور چک",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Switch(
                        checked = reminderDateTime != null,
                        onCheckedChange = { 
                            if (it) {
                                // Default reminder: 1 day before due date at 09:00
                                val calendar = Calendar.getInstance()
                                calendar.timeInMillis = dueDate
                                calendar.add(Calendar.DAY_OF_YEAR, -1)
                                calendar.set(Calendar.HOUR_OF_DAY, 9)
                                calendar.set(Calendar.MINUTE, 0)
                                reminderDateTime = calendar.timeInMillis
                            } else {
                                reminderDateTime = null
                            }
                        }
                    )
                }

                if (reminderDateTime != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedCard(
                            onClick = { showReminderDatePicker = true },
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("تاریخ یادآور", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = PersianCalendarHelper.fromEpochMillis(reminderDateTime!!).toFormattedDate(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        OutlinedCard(
                            onClick = { showReminderTimePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("ساعت", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = PersianCalendarHelper.fromEpochMillis(reminderDateTime!!).toFormattedTime(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("توضیحات (اختیاری)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    leadingIcon = { Icon(Icons.Rounded.Notes, null) }
                )

                Button(
                    onClick = {
                        val enteredAmount = amount.toEnglishDigits().toLongOrNull() ?: 0L
                        val amountInToman = MoneyFormatter.convertToToman(enteredAmount)
                        if (amountInToman > 0 && checkNumber.isNotBlank() && bankName.isNotBlank() &&
                            !isDateInvalid && !isReminderInvalid && !isSayadIdInvalid) {
                            onConfirm(
                                Check(
                                    id = check?.id ?: 0,
                                    userId = check?.userId ?: "",
                                    type = type,
                                    amount = amountInToman,
                                    checkNumber = checkNumber,
                                    sayadId = normalizedSayadId.ifBlank { null },
                                    bankName = bankName.trim(),
                                    branch = branch.ifBlank { null },
                                    holderName = holderName.trim(),
                                    receiverName = receiverName.trim(),
                                    issueDate = issueDate,
                                    dueDate = dueDate,
                                    status = status,
                                    notes = notes.trim().ifBlank { null },
                                    accountId = check?.accountId,
                                    reminderDateTime = reminderDateTime,
                                    createdAt = check?.createdAt ?: System.currentTimeMillis(),
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                        } else {
                            showError = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("ذخیره", fontWeight = FontWeight.Bold)
                }

                if (check != null) {
                    Text("وضعیت چک", style = MaterialTheme.typography.titleSmall)
                    if (check.status == CheckStatus.CLEARED) {
                        Text(
                            "این چک تسویه شده است؛ برای جلوگیری از مغایرت حساب، وضعیت تسویه از این فرم تغییر نمی‌کند.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CheckStatus.entries.filter { it != CheckStatus.CLEARED }.forEach { s ->
                                FilterChip(
                                    selected = status == s,
                                    onClick = { status = s },
                                    label = { Text(s.title) }
                                )
                            }
                        }
                    }
                }

            }
        }

        if (showBankPickerDialog) {
            AlertDialog(
                onDismissRequest = { showBankPickerDialog = false },
                title = {
                    Text("انتخاب بانک", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                },
                text = {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(IRANIAN_BANK_NAMES) { bank ->
                            Surface(
                                onClick = {
                                    bankName = bank
                                    showBankPickerDialog = false
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (bankName == bank) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = bank,
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (bankName == bank) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showBankPickerDialog = false }) { Text("بستن") }
                }
            )
        }

        if (showIssueDatePicker) {
            val dateParts = PersianCalendarHelper.fromEpochMillis(issueDate)
            SharedPersianDatePickerDialog(
                initialYear = dateParts.year,
                initialMonth = dateParts.month,
                initialDay = dateParts.day,
                onConfirm = { y, m, d ->
                    issueDate = PersianCalendarHelper.jalaliToEpochMillis(y, m, d, 9, 0)
                    showIssueDatePicker = false
                },
                onDismiss = { showIssueDatePicker = false }
            )
        }

        if (showDueDatePicker) {
            val dateParts = PersianCalendarHelper.fromEpochMillis(dueDate)
            SharedPersianDatePickerDialog(
                initialYear = dateParts.year,
                initialMonth = dateParts.month,
                initialDay = dateParts.day,
                onConfirm = { y, m, d ->
                    dueDate = PersianCalendarHelper.jalaliToEpochMillis(y, m, d, 9, 0)
                    showDueDatePicker = false
                },
                onDismiss = { showDueDatePicker = false }
            )
        }

        if (showReminderDatePicker) {
            val dateParts = PersianCalendarHelper.fromEpochMillis(reminderDateTime ?: dueDate)
            SharedPersianDatePickerDialog(
                initialYear = dateParts.year,
                initialMonth = dateParts.month,
                initialDay = dateParts.day,
                onConfirm = { y, m, d ->
                    val currentTime = PersianCalendarHelper.fromEpochMillis(reminderDateTime ?: System.currentTimeMillis())
                    reminderDateTime = PersianCalendarHelper.jalaliToEpochMillis(y, m, d, currentTime.hour, currentTime.minute)
                    showReminderDatePicker = false
                },
                onDismiss = { showReminderDatePicker = false }
            )
        }

        if (showReminderTimePicker) {
            val current = PersianCalendarHelper.fromEpochMillis(reminderDateTime ?: System.currentTimeMillis())
            SimpleTimePickerDialog(
                initialHour = current.hour,
                initialMinute = current.minute,
                onDismiss = { showReminderTimePicker = false },
                onConfirm = { h, m ->
                    val currentDate = PersianCalendarHelper.fromEpochMillis(reminderDateTime ?: System.currentTimeMillis())
                    reminderDateTime = PersianCalendarHelper.jalaliToEpochMillis(currentDate.year, currentDate.month, currentDate.day, h, m)
                    showReminderTimePicker = false
                }
            )
        }
    }
}

@Composable
fun SimpleTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var hour by remember { mutableStateOf(initialHour) }
    var minute by remember { mutableStateOf(initialMinute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("انتخاب ساعت یادآور", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    NumberPicker(
                        value = hour,
                        onValueChange = { hour = it },
                        range = 0..23,
                        label = "ساعت"
                    )
                    Text(":", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(horizontal = 8.dp))
                    NumberPicker(
                        value = minute,
                        onValueChange = { minute = it },
                        range = 0..59,
                        label = "دقیقه"
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(hour, minute) }) {
                Text("تأیید")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}

@Composable
fun NumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = { if (value < range.last) onValueChange(value + 1) else onValueChange(range.first) }) {
            Icon(Icons.Rounded.KeyboardArrowUp, null)
        }
        Text(
            text = value.toString().padStart(2, '0').let { com.example.util.IranianPhoneUtils.convertDigitsToPersian(it) },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = { if (value > range.first) onValueChange(value - 1) else onValueChange(range.last) }) {
            Icon(Icons.Rounded.KeyboardArrowDown, null)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement
    ) {
        content()
    }
}


private fun String.toEnglishDigits(): String = map { char ->
    when (char) {
        in '۰'..'۹' -> ('0'.code + (char.code - '۰'.code)).toChar()
        in '٠'..'٩' -> ('0'.code + (char.code - '٠'.code)).toChar()
        else -> char
    }
}.joinToString("")
