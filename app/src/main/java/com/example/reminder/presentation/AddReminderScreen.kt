package com.example.reminder.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.preferences.AppPreferencesRepository
import com.example.reminder.data.ReminderEntity
import com.example.reminder.domain.PredefinedOffset
import com.example.reminder.domain.Priority
import com.example.reminder.domain.ReminderType
import com.example.reminder.domain.RepeatType
import com.example.ui.components.Layered3DCard
import com.example.ui.components.PersianAmountInputField
import com.example.ui.components.PersianDateInputField
import com.example.ui.screens.reminder.components.PersianTimeInputField
import com.example.util.MoneyFormatter
import com.example.ui.components.Soft3DIcon
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusSM
import com.example.ui.theme.WarningAmberLight
import com.example.util.IranianAmountUtils
import com.example.util.MoneyAmount
import com.example.ui.screens.settings.model.AppCurrency
import com.example.util.IranianDateUtils
import com.example.util.IranianPhoneUtils
import com.example.util.PersianCalendarHelper
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderScreen(
    onBackClick: () -> Unit,
    onSaveReminderWithResult: (
        reminder: ReminderEntity,
        offsets: List<PredefinedOffset>,
        repeatType: RepeatType,
        repeatInterval: Int,
        onComplete: (Result<Int>) -> Unit
    ) -> Unit,
    initialDate: String? = null,
    initialType: ReminderType = ReminderType.GENERAL,
    modifier: Modifier = Modifier
) {
    val currentPersianDate = remember {
        PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate()
    }

    val context = LocalContext.current
    val preferences = remember { AppPreferencesRepository.getInstance(context) }
    val defaultReminderMinute by preferences.defaultReminderTimeMinute.collectAsState()
    val globalDeliveryMode by preferences.alertDelivery.collectAsState()
    val globalNotificationsEnabled by preferences.notificationsEnabled.collectAsState()
    var showNoAlertConfirmation by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var validationMessage by remember { mutableStateOf<String?>(null) }
    var saveErrorMessage by remember { mutableStateOf<String?>(null) }
    val reminderId = remember { UUID.randomUUID().toString() }
    val defaultReminderTimeText = remember(defaultReminderMinute) {
        val hour = (defaultReminderMinute / 60).toString().padStart(2, '0')
        val minute = (defaultReminderMinute % 60).toString().padStart(2, '0')
        IranianPhoneUtils.convertDigitsToPersian("$hour:$minute")
    }
    var selectedType by remember(initialType) { mutableStateOf(initialType) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember(initialDate, currentPersianDate) { mutableStateOf(initialDate ?: currentPersianDate) }
    var time by remember(defaultReminderTimeText) { mutableStateOf(defaultReminderTimeText) }
    var priority by remember { mutableStateOf(Priority.NORMAL) }
    var repeatType by remember { mutableStateOf(RepeatType.NONE) }
    var repeatInterval by remember { mutableStateOf("1") }
    var amountText by remember { mutableStateOf("") }
    var targetKmText by remember { mutableStateOf("") }

    // Multi-Offset selection
    // No delivery channel or trigger time is selected by default. Saving a reminder
    // without explicitly opting in must never create a system notification.
    val selectedOffsets = remember { mutableStateListOf<PredefinedOffset>() }

    fun submitReminder(enableAlerts: Boolean) {
        if (isSaving || title.isBlank()) return
        validationMessage = null
        saveErrorMessage = null

        val normalizedDate = IranianPhoneUtils.convertDigitsToEnglish(date).trim()
        val hasValidDateShape = Regex("^(?:\\d{4}/\\d{2}/\\d{2}|\\d{8})$").matches(normalizedDate)
        if (!hasValidDateShape || !IranianDateUtils.isValidPersianDate(normalizedDate)) {
            validationMessage = "تاریخ واردشده معتبر نیست. تاریخ را به‌صورت سال/ماه/روز شمسی کامل وارد کنید."
            return
        }
        val normalizedTime = IranianPhoneUtils.convertDigitsToEnglish(time).trim()
        if (!Regex("^(?:[01]\\d|2[0-3]):[0-5]\\d$").matches(normalizedTime)) {
            validationMessage = "ساعت معتبر نیست. ساعت را به‌صورت ۰۰:۰۰ تا ۲۳:۵۹ وارد کنید."
            return
        }
        val interval = repeatInterval.toIntOrNull()
        if (enableAlerts && selectedOffsets.isNotEmpty() && repeatType != RepeatType.NONE && (interval == null || interval !in 1..365)) {
            validationMessage = "بازه تکرار باید عددی بین ۱ تا ۳۶۵ باشد."
            return
        }

        val offsetsToSave = if (enableAlerts) selectedOffsets.toList() else emptyList()
        if (offsetsToSave.isNotEmpty()) {
            val dateDigits = IranianDateUtils.cleanDateDigits(normalizedDate)
            val dueAt = PersianCalendarHelper.jalaliToEpochMillis(
                dateDigits.substring(0, 4).toInt(),
                dateDigits.substring(4, 6).toInt(),
                dateDigits.substring(6, 8).toInt(),
                normalizedTime.substringBefore(":").toInt(),
                normalizedTime.substringAfter(":").toInt()
            )
            val now = System.currentTimeMillis()
            val pastOffsets = offsetsToSave.filter { offset ->
                dueAt - (offset.value.toLong() * offset.unit.millisMultiplier) <= now
            }
            if (pastOffsets.isNotEmpty()) {
                validationMessage = "زمان هشدار ${pastOffsets.joinToString("، ") { it.title }} گذشته است. تاریخ سررسید یا زمان‌های هشدار را تغییر دهید."
                return
            }
        }

        val effectiveRepeatType = if (offsetsToSave.isEmpty()) RepeatType.NONE else repeatType
        val effectiveRepeatInterval = if (effectiveRepeatType == RepeatType.NONE) 1 else interval ?: 1
        val reminder = ReminderEntity(
            id = reminderId,
            title = title.trim(),
            description = description.trim(),
            type = selectedType.key,
            sourceType = "MANUAL",
            priority = priority.name,
            status = "ACTIVE",
            date = date.trim(),
            time = time.trim(),
            amount = amountText.takeIf { it.isNotBlank() }?.let { raw ->
                IranianAmountUtils.parseAmountToBigDecimal(raw)?.let { entered ->
                    runCatching { MoneyAmount(entered, MoneyFormatter.activeCurrency).asRialsLongExact() }.getOrNull()
                }
            },
            amountUnit = AppCurrency.RIAL.name,
            targetKilometer = targetKmText.toLongOrNull(),
            notificationEnabled = offsetsToSave.isNotEmpty(),
            fullScreenAlarmEnabled = false
        )
        isSaving = true
        val handleResult: (Result<Int>) -> Unit = { result ->
            isSaving = false
            result.onSuccess { onBackClick() }
            result.onFailure { error ->
                saveErrorMessage = error.localizedMessage ?: "ثبت یادآور انجام نشد. اطلاعات فرم حفظ شده است؛ دوباره تلاش کنید."
            }
        }
        onSaveReminderWithResult(
            reminder,
            offsetsToSave,
            effectiveRepeatType,
            effectiveRepeatInterval,
            handleResult
        )
    }

    val globalDeliveryConfigured = globalNotificationsEnabled &&
        globalDeliveryMode != com.example.ui.screens.settings.model.AlertDeliveryPreference.NONE

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ثبت یادآوری و سررسید جدید",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(enabled = !isSaving, onClick = onBackClick, modifier = Modifier.testTag("btn_add_reminder_back")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "بازگشت"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Type Selector Grid
            Text(
                text = "دسته‌بندی یادآور",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    ReminderType.GENERAL,
                    ReminderType.PERSONAL,
                    ReminderType.FINANCE,
                    ReminderType.INSTALLMENT,
                    ReminderType.VEHICLE,
                    ReminderType.MAINTENANCE,
                    ReminderType.INSURANCE,
                    ReminderType.FUEL,
                    ReminderType.CUSTOM
                ).forEach { type ->
                    val isSelected = selectedType == type
                    Surface(
                        shape = RoundedCornerShape(RadiusMD),
                        color = if (isSelected) type.accentColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) type.accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .width(110.dp)
                            .clickable { selectedType = type }
                            .testTag("type_selector_${type.key}")
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Soft3DIcon(
                                imageRes = type.iconRes,
                                contentDescription = type.title,
                                size = 32.dp,
                                accentColor = type.accentColor
                            )
                            Text(
                                text = type.title,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) type.accentColor else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }

            // 2. Title & Description
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("عنوان یادآوری") },
                placeholder = { Text("مثلاً: قسط وام مسکن یا تمدید بیمه شخص ثالث") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_reminder_title"),
                shape = RoundedCornerShape(RadiusMD),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("توضیحات تکمیلی (اختیاری)") },
                placeholder = { Text("شماره شبا، جزئیات چک یا نکات مربوطه") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_reminder_desc"),
                shape = RoundedCornerShape(RadiusMD),
                maxLines = 3
            )

            // 3. Date & Time Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PersianDateInputField(
                    value = date,
                    onValueChange = { date = it },
                    label = "تاریخ سررسید (شمسی)",
                    placeholder = "۱۴۰۴/۰۱/۰۱",
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_reminder_date")
                )

                PersianTimeInputField(
                    value = time,
                    onValueChange = { time = it },
                    label = "ساعت سررسید",
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_reminder_time")
                )
            }

            // 4. Amount / Target Kilometer depending on type
            if (selectedType == ReminderType.INSTALLMENT || selectedType == ReminderType.FINANCE || selectedType == ReminderType.INSURANCE) {
                PersianAmountInputField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = "مبلغ به ${MoneyFormatter.getUnitLabel()} (اختیاری)",
                    placeholder = "مثال: ۲,۵۰۰,۰۰۰",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_reminder_amount")
                )
            } else if (selectedType == ReminderType.VEHICLE || selectedType == ReminderType.MAINTENANCE) {
                OutlinedTextField(
                    value = targetKmText,
                    onValueChange = { targetKmText = IranianPhoneUtils.convertDigitsToEnglish(it).filter { ch -> ch.isDigit() } },
                    label = { Text("کیلومتر هدف سرویس (اختیاری)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_reminder_km"),
                    shape = RoundedCornerShape(RadiusMD),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Speed, contentDescription = null, tint = InfoIndigoLight) }
                )
            }

            // Alert presentation is configured once for the whole app in General Settings.
            Text(
                text = "نوع نمایش هشدار برای همه یادآورها از تنظیمات عمومی برنامه تعیین می‌شود.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 6. Multi-Offset Alert Selector (زمان‌های ارسال هشدار)
            Text(
                text = "زمان‌های ارسال هشدار (چندانتخابی)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    PredefinedOffset.BEFORE_7_DAYS,
                    PredefinedOffset.BEFORE_3_DAYS,
                    PredefinedOffset.BEFORE_1_DAY,
                    PredefinedOffset.AT_TIME
                ).chunked(2).forEach { offsetRow ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        offsetRow.forEach { offset ->
                            val isChecked = selectedOffsets.contains(offset)
                            FilterChip(
                                selected = isChecked,
                                onClick = {
                                    if (isChecked) {
                                        selectedOffsets.remove(offset)
                                    } else {
                                        selectedOffsets.add(offset)
                                    }
                                },
                                label = {
                                    Text(
                                        text = offset.title.substringBefore(" ("),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(RadiusMD)
                            )
                        }
                    }
                }
            }

            // 6a. Recurrence Selector
            Text(
                text = "تکرار یادآوری",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val noRepeatSelected = repeatType == RepeatType.NONE
                FilterChip(
                    selected = noRepeatSelected,
                    onClick = { repeatType = RepeatType.NONE },
                    label = {
                        Text(
                            text = RepeatType.NONE.title,
                            fontSize = 10.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(RadiusMD)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RepeatType.entries.filter { it != RepeatType.NONE }.forEach { type ->
                        FilterChip(
                            selected = repeatType == type,
                            onClick = { repeatType = type },
                            label = {
                                Text(
                                    text = type.title,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(RadiusMD)
                        )
                    }
                }
            }

            if (repeatType != RepeatType.NONE) {
                OutlinedTextField(
                    value = repeatInterval,
                    onValueChange = { repeatInterval = IranianPhoneUtils.convertDigitsToEnglish(it).filter { ch -> ch.isDigit() }.take(3) },
                    label = { Text("بازه تکرار") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            // 6. Priority Selector
            Text(
                text = "اولویت یادآوری",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(Priority.LOW, Priority.NORMAL, Priority.HIGH).forEach { p ->
                    val isSelected = priority == p
                    Surface(
                        shape = RoundedCornerShape(RadiusMD),
                        color = if (isSelected) p.color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) p.color else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { priority = p }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = p.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) p.color else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            validationMessage?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            saveErrorMessage?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            // Save Button
            Button(
                onClick = {
                    if (isSaving) return@Button
                    if (selectedOffsets.isEmpty() || !globalDeliveryConfigured) {
                        showNoAlertConfirmation = true
                    } else {
                        submitReminder(enableAlerts = true)
                    }
                },
                enabled = title.isNotBlank() && !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_save_reminder"),
                shape = RoundedCornerShape(RadiusMD),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimaryLight)
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isSaving) "در حال ثبت..." else "ثبت یادآور", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }

    if (showNoAlertConfirmation) {
        val noOffsetsSelected = selectedOffsets.isEmpty()
        AlertDialog(
            onDismissRequest = { showNoAlertConfirmation = false },
            title = { Text("ثبت یادآور بدون اعلان؟") },
            text = {
                Text(
                    if (noOffsetsSelected) {
                        "برای این یادآور هیچ زمان هشداری انتخاب نکرده‌اید. با تأیید، یادآور بدون اعلان ثبت می‌شود و تکرار هشدار نیز فعال نخواهد بود."
                    } else {
                        "زمان هشدار انتخاب شده، اما روش دریافت هشدار در تنظیمات عمومی فعال نیست. با تأیید، یادآور بدون اعلان ثبت می‌شود و تکرار هشدار اجرا نخواهد شد."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showNoAlertConfirmation = false
                    submitReminder(enableAlerts = false)
                }) { Text("تأیید و ثبت بدون اعلان") }
            },
            dismissButton = {
                TextButton(onClick = { showNoAlertConfirmation = false }) { Text("لغو و بازگشت به فرم") }
            }
        )
    }
}
