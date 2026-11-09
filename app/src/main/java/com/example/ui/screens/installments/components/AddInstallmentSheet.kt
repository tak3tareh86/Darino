package com.example.ui.screens.installments.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import kotlin.math.absoluteValue
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.preferences.AppPreferencesRepository
import com.example.reminder.domain.PredefinedOffset
import com.example.ui.components.PersianAmountInputField
import com.example.ui.components.PersianDateInputField
import com.example.ui.components.Soft3DIcon
import com.example.ui.screens.installments.model.InstallmentCategory
import com.example.ui.screens.reminder.components.PersianTimePickerDialog
import com.example.util.IranianAmountUtils
import com.example.util.IranianPhoneUtils
import com.example.util.MoneyFormatter
import com.example.util.MoneyAmount
import com.example.ui.screens.settings.model.AppCurrency
import com.example.util.PersianCalendarHelper

/**
 * World-Class, Ultra-Modern Two-Step Add Installment Flow with Centered Mini Pop-up Dialog for Reminders.
 *
 * Step 1: Category selection screen (Bank loans, Home loans, Car insurance, Misc).
 * Step 2: Full detailed form for Commitment & Counterparty, Financials, Schedule, and Reminder toggle.
 * Dialog: Centered mini pop-up for configuring notification timing offsets (1 day before, 2 days before, 1 week before, on due date, and time).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInstallmentSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onAddConfirm: (
        category: InstallmentCategory,
        title: String,
        totalAmountStr: String,
        installmentAmountStr: String,
        installmentsCountStr: String,
        paidInstallmentsCountStr: String,
        dueDateStr: String,
        provider: String,
        notes: String,
        reminderEnabled: Boolean,
        reminderDays: String,
        reminderTime: String,
        selectedOffsets: List<PredefinedOffset>,
        customScheduleItems: List<com.example.ui.screens.installments.model.PaymentHistoryItem>?,
        insuranceType: com.example.ui.screens.installments.model.VehicleInsuranceType?
    ) -> Unit,
    initialCategory: InstallmentCategory = InstallmentCategory.BANK_LOANS,
    initialTitle: String = "",
    initialTotalAmount: String = "",
    initialInstallmentAmount: String = "",
    initialInstallmentsCount: String = "",
    modifier: Modifier = Modifier
) {
    // Current step: 1 = Category List, 2 = Form Details
    var currentStep by remember { mutableStateOf(1) }
    var selectedCategory by remember(initialCategory) { mutableStateOf(initialCategory) }

    // Form fields
    var title by remember(initialTitle) { mutableStateOf(initialTitle) }
    var totalAmount by remember(initialTotalAmount) { mutableStateOf(initialTotalAmount) }
    var installmentAmount by remember(initialInstallmentAmount) { mutableStateOf(initialInstallmentAmount) }
    var installmentsCount by remember(initialInstallmentsCount) { mutableStateOf(initialInstallmentsCount.ifBlank { "۱۲" }) }
    var paidInstallmentsCount by remember { mutableStateOf("۰") }
    var submitValidationError by remember { mutableStateOf<String?>(null) }
    // The first installment date must be explicitly provided by the user.
    var dueDate by remember { mutableStateOf("") }
    var providerOrPerson by remember { mutableStateOf("") }
    var insuranceType by remember { mutableStateOf<com.example.ui.screens.installments.model.VehicleInsuranceType?>(null) }
    var notes by remember { mutableStateOf("") }

    // Reminder state
    var reminderEnabled by remember { mutableStateOf(false) }
    var showReminderModal by remember { mutableStateOf(false) }

    // Selected offsets for reminder:
    val context = LocalContext.current
    val preferences = remember { AppPreferencesRepository.getInstance(context) }
    val defaultReminderMinute by preferences.defaultReminderTimeMinute.collectAsState()
    val globalInstallmentReminderDays by preferences.installmentReminderDays.collectAsState()
    val globalDeliveryMode by preferences.alertDelivery.collectAsState()
    val globalNotificationsEnabled by preferences.notificationsEnabled.collectAsState()
    val globalDeliveryConfigured = globalNotificationsEnabled &&
        globalDeliveryMode != com.example.ui.screens.settings.model.AlertDeliveryPreference.NONE
    var noAlertWarningMessage by remember { mutableStateOf("") }
    var showNoAlertConfirmation by remember { mutableStateOf(false) }
    var pendingNoAlertSubmit by remember { mutableStateOf<(() -> Unit)?>(null) }
    val defaultReminderTimeText = remember(defaultReminderMinute) {
        val hour = (defaultReminderMinute / 60).toString().padStart(2, '0')
        val minute = (defaultReminderMinute % 60).toString().padStart(2, '0')
        IranianPhoneUtils.convertDigitsToPersian("$hour:$minute")
    }
    var remindAtTime by remember { mutableStateOf(false) }
    var remind1DayBefore by remember { mutableStateOf(false) }
    var remind2DaysBefore by remember { mutableStateOf(false) }
    var remind1WeekBefore by remember { mutableStateOf(false) }
    var reminderTime by remember(defaultReminderTimeText) { mutableStateOf(defaultReminderTimeText) }
    var showTimePicker by remember { mutableStateOf(false) }

    var customScheduleItems by remember { mutableStateOf<List<com.example.ui.screens.installments.model.PaymentHistoryItem>?>(null) }

    fun submitInstallment(
        reminderActive: Boolean,
        offsets: List<PredefinedOffset>,
        daysSummary: String,
        scheduleItems: List<com.example.ui.screens.installments.model.PaymentHistoryItem>?
    ) {
        onAddConfirm(
            selectedCategory,
            title.ifBlank { selectedCategory.title },
            totalAmount,
            installmentAmount,
            installmentsCount,
            paidInstallmentsCount,
            dueDate,
            providerOrPerson,
            notes,
            reminderActive,
            daysSummary,
            reminderTime,
            offsets,
            scheduleItems,
            insuranceType.takeIf { selectedCategory == InstallmentCategory.CAR_INSURANCE }
        )
        onDismiss()
    }
    var showCustomScheduleDialog by remember { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val activeCurrency = MoneyFormatter.activeCurrency

    // Smart Auto-Calculation:
    LaunchedEffect(totalAmount, installmentsCount) {
        val parsedTotalRials = IranianAmountUtils.parseAmountToBigDecimal(totalAmount)?.let {
            runCatching { MoneyAmount(it, activeCurrency).asRialsLongExact() }.getOrNull()
        }
        val parsedCount = IranianPhoneUtils.convertDigitsToEnglish(installmentsCount).filter { it.isDigit() }.toIntOrNull() ?: 0
        if (parsedTotalRials != null && parsedTotalRials > 0L && parsedCount > 0) {
            val calcMonthlyRials = parsedTotalRials / parsedCount
            val displayMonthly = MoneyAmount.of(calcMonthlyRials, AppCurrency.RIAL)
                .inUnit(activeCurrency).value.stripTrailingZeros().toPlainString()
            installmentAmount = IranianAmountUtils.formatDecimalWithCommas(displayMonthly)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        modifier = modifier.testTag("add_installment_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            if (currentStep == 1) {
                // STEP 1: CATEGORY SELECTION SCREEN
                CategorySelectionStep(
                    onSelectCategory = { category ->
                        selectedCategory = category
                        if (category != InstallmentCategory.CAR_INSURANCE) insuranceType = null
                        currentStep = 2
                    },
                    onDismiss = onDismiss
                )
            } else {
                // STEP 2: COMMITMENT & FINANCIAL FORM SCREEN
                FormDetailsStep(
                    category = selectedCategory,
                    title = title,
                    onTitleChange = { title = it },
                    provider = providerOrPerson,
                    onProviderChange = { providerOrPerson = it },
                    insuranceType = insuranceType,
                    onInsuranceTypeChange = { insuranceType = it },
                    totalAmount = totalAmount,
                    onTotalAmountChange = {
                        totalAmount = it
                        submitValidationError = null
                    },
                    installmentAmount = installmentAmount,
                    onInstallmentAmountChange = {
                        installmentAmount = it
                        submitValidationError = null
                    },
                    installmentsCount = installmentsCount,
                    onInstallmentsCountChange = {
                        installmentsCount = it
                        submitValidationError = null
                    },
                    paidInstallmentsCount = paidInstallmentsCount,
                    onPaidInstallmentsCountChange = {
                        paidInstallmentsCount = it
                        submitValidationError = null
                    },
                    submitValidationError = submitValidationError,
                    dueDate = dueDate,
                    onDueDateChange = {
                        dueDate = it
                        submitValidationError = null
                    },
                    notes = notes,
                    onNotesChange = { notes = it },
                    reminderEnabled = reminderEnabled,
                    onReminderEnabledChange = { reminderEnabled = it },
                    reminderTime = reminderTime,
                    remindAtTime = remindAtTime,
                    remind1DayBefore = remind1DayBefore,
                    remind2DaysBefore = remind2DaysBefore,
                    remind1WeekBefore = remind1WeekBefore,
                    onOpenReminderModal = { showReminderModal = true },
                    onOpenCustomSchedule = {
                        if (dueDate.isBlank() || !com.example.calendar.domain.CalendarDateUtils.isValidJalaliDate(dueDate)) {
                            submitValidationError = "ابتدا تاریخ واقعی اولین قسط را وارد یا از تقویم انتخاب کنید."
                        } else {
                            submitValidationError = null
                            showCustomScheduleDialog = true
                        }
                    },
                    isCustomScheduleActive = customScheduleItems != null,
                    onBackToCategories = { currentStep = 1 },
                    onDismiss = onDismiss,
                    onSubmit = {
                        val currentCustomScheduleItems = customScheduleItems
                        val paidCountValue = IranianPhoneUtils.convertDigitsToEnglish(paidInstallmentsCount)
                            .filter { it.isDigit() }.toIntOrNull() ?: 0
                        val totalCountValue = if (!currentCustomScheduleItems.isNullOrEmpty()) {
                            currentCustomScheduleItems.size
                        } else {
                            IranianPhoneUtils.convertDigitsToEnglish(installmentsCount)
                                .filter { it.isDigit() }.toIntOrNull() ?: 0
                        }
                        val parsedTotalForValidation = IranianAmountUtils.parseAmountToBigDecimal(totalAmount)?.let {
                            runCatching { MoneyAmount(it, activeCurrency).asRialsLongExact() }.getOrNull()
                        } ?: 0L
                        val parsedMonthlyForValidation = IranianAmountUtils.parseAmountToBigDecimal(installmentAmount)?.let {
                            runCatching { MoneyAmount(it, activeCurrency).asRialsLongExact() }.getOrNull()
                        } ?: 0L
                        if (selectedCategory == InstallmentCategory.CAR_INSURANCE && insuranceType == null) {
                            submitValidationError = "نوع بیمه را انتخاب کنید."
                            return@FormDetailsStep
                        }
                        if (dueDate.isBlank() || !com.example.calendar.domain.CalendarDateUtils.isValidJalaliDate(dueDate)) {
                            submitValidationError = "ابتدا تاریخ واقعی اولین قسط را وارد یا از تقویم انتخاب کنید."
                            return@FormDetailsStep
                        }
                        if (paidCountValue > totalCountValue) {
                            submitValidationError = "تعداد اقساط پرداخت‌شده نمی‌تواند از تعداد کل اقساط بیشتر باشد."
                            return@FormDetailsStep
                        }
                        if (currentCustomScheduleItems.isNullOrEmpty() &&
                            totalCountValue > 0 &&
                            Math.multiplyExact(parsedMonthlyForValidation, totalCountValue.toLong()) < parsedTotalForValidation
                        ) {
                            submitValidationError = "مجموع مبلغ اقساط نمی‌تواند از مبلغ کل وام کمتر باشد؛ لطفاً مبالغ را بررسی کنید."
                            return@FormDetailsStep
                        }
                        submitValidationError = null
                        val offsets = mutableListOf<PredefinedOffset>()
                        if (remindAtTime) offsets.add(PredefinedOffset.AT_TIME)
                        if (remind1DayBefore) offsets.add(PredefinedOffset.BEFORE_1_DAY)
                        if (remind2DaysBefore) offsets.add(PredefinedOffset.BEFORE_2_DAYS)
                        if (remind1WeekBefore) offsets.add(PredefinedOffset.BEFORE_7_DAYS)

                        val daysSummary = offsets.joinToString("، ") { it.title }

                        val noTimingConfigured = offsets.isEmpty() && globalInstallmentReminderDays == 0
                        val noDeliveryConfigured = !globalDeliveryConfigured
                        if (reminderEnabled && (noTimingConfigured || noDeliveryConfigured)) {
                            noAlertWarningMessage = when {
                                noTimingConfigured && noDeliveryConfigured ->
                                    "هیچ زمان هشداری انتخاب نشده و روش دریافت هشدار هم در تنظیمات عمومی فعال نیست. اگر تأیید کنید، اقساط بدون اعلان ثبت می‌شوند."
                                noTimingConfigured ->
                                    "هیچ زمان هشداری برای اقساط انتخاب نشده است. اگر تأیید کنید، اقساط بدون اعلان ثبت می‌شوند."
                                else ->
                                    "زمان هشدار انتخاب شده، اما روش دریافت هشدار یا اعلان‌های سراسری فعال نیست. اگر تأیید کنید، اقساط بدون اعلان ثبت می‌شوند."
                            }
                            pendingNoAlertSubmit = {
                                submitInstallment(
                                    reminderActive = false,
                                    offsets = emptyList(),
                                    daysSummary = "",
                                    scheduleItems = currentCustomScheduleItems
                                )
                            }
                            showNoAlertConfirmation = true
                        } else {
                            submitInstallment(
                                reminderActive = reminderEnabled,
                                offsets = offsets,
                                daysSummary = daysSummary,
                                scheduleItems = currentCustomScheduleItems
                            )
                        }
                    }
                )
            }
        }
    }

    if (showNoAlertConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showNoAlertConfirmation = false
                pendingNoAlertSubmit = null
            },
            title = { Text("ثبت اقساط بدون اعلان؟") },
            text = {
                Text(noAlertWarningMessage)
            },
            confirmButton = {
                TextButton(onClick = {
                    showNoAlertConfirmation = false
                    pendingNoAlertSubmit?.invoke()
                    pendingNoAlertSubmit = null
                }) { Text("تأیید و ثبت بدون اعلان") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNoAlertConfirmation = false
                    pendingNoAlertSubmit = null
                }) { Text("لغو و تنظیم یادآوری") }
            }
        )
    }

    // CENTERED MINI POP-UP DIALOG FOR REMINDER SETTINGS
    if (showReminderModal) {
        Dialog(
            onDismissRequest = { showReminderModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .widthIn(min = 300.dp, max = 360.dp)
                    .fillMaxWidth(0.94f)
                    .heightIn(max = 440.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Compact header keeps the reminder settings as a centered dialog.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "تنظیمات یادآور و نوتیفیکیشن",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "زمان‌های اطلاع‌رسانی پیش از سررسید را انتخاب کنید:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
                    )

                    // Two-column reminder choices reduce the dialog's vertical height.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                ReminderOptionRow(
                                    title = "روز سررسید",
                                    checked = remindAtTime,
                                    onCheckedChange = { remindAtTime = it }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                ReminderOptionRow(
                                    title = "۱ روز قبل",
                                    checked = remind1DayBefore,
                                    onCheckedChange = { remind1DayBefore = it }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                ReminderOptionRow(
                                    title = "۲ روز قبل",
                                    checked = remind2DaysBefore,
                                    onCheckedChange = { remind2DaysBefore = it }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                ReminderOptionRow(
                                    title = "۱ هفته قبل",
                                    checked = remind1WeekBefore,
                                    onCheckedChange = { remind1WeekBefore = it }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Time Picker Button
                        OutlinedButton(
                            onClick = { showTimePicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ساعت یادآوری: $reminderTime", fontWeight = FontWeight.Bold)
                        }
                    
                    Spacer(modifier = Modifier.height(10.dp))

                    // Dialog Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showReminderModal = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("انصراف")
                        }
                        Button(
                            onClick = { showReminderModal = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("تایید")
                        }
                    }
                }
            }
        }
    }

    if (showCustomScheduleDialog) {
        CustomInstallmentsDialog(
            totalAmountStr = totalAmount,
            activeCurrency = activeCurrency,
            installmentAmountStr = installmentAmount,
            installmentsCountStr = installmentsCount,
            dueDateStr = dueDate,
            initialCustomList = customScheduleItems,
            onDismiss = { showCustomScheduleDialog = false },
            onConfirm = { updatedList ->
                customScheduleItems = updatedList
                submitValidationError = null
                showCustomScheduleDialog = false
            }
        )
    }

    if (showTimePicker) {
        val parsedHour = remember(reminderTime) {
            val eng = IranianPhoneUtils.convertDigitsToEnglish(reminderTime)
            eng.substringBefore(":").toIntOrNull() ?: 9
        }
        val parsedMinute = remember(reminderTime) {
            val eng = IranianPhoneUtils.convertDigitsToEnglish(reminderTime)
            eng.substringAfter(":", "0").toIntOrNull() ?: 0
        }

        PersianTimePickerDialog(
            initialHour = parsedHour,
            initialMinute = parsedMinute,
            onDismiss = { showTimePicker = false },
            onConfirm = { h, m ->
                val hStr = h.toString().padStart(2, '0')
                val mStr = m.toString().padStart(2, '0')
                reminderTime = IranianPhoneUtils.convertDigitsToPersian("$hStr:$mStr")
                showTimePicker = false
            }
        )
    }
}

/**
 * STEP 1: CATEGORY SELECTION SCREEN
 */
@Composable
private fun CategorySelectionStep(
    onSelectCategory: (InstallmentCategory) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "افزودن قسط و تعهد جدید",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "نوع وام یا قسط خود را انتخاب کنید:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "بستن",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Category Cards List
        CategoryCardItem(
            title = "وام‌های بانکی",
            subtitle = "تسهیلات بانکی، وام مسکن، ازدواج و مرابحه",
            iconRes = InstallmentCategory.BANK_LOANS.iconRes,
            vectorIcon = Icons.Rounded.AccountBalance,
            accentColor = InstallmentCategory.BANK_LOANS.accentColor,
            onClick = { onSelectCategory(InstallmentCategory.BANK_LOANS) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        CategoryCardItem(
            title = "وام‌های خانگی و فامیلی",
            subtitle = "صندوق‌های قرعه‌کشی خانوادگی، همکاران و محلی",
            iconRes = InstallmentCategory.HOME_LOANS.iconRes,
            vectorIcon = Icons.Rounded.Home,
            accentColor = InstallmentCategory.HOME_LOANS.accentColor,
            onClick = { onSelectCategory(InstallmentCategory.HOME_LOANS) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        CategoryCardItem(
            title = "اقساط بیمه خودرو",
            subtitle = "بیمه شخص ثالث، بدنه، اقساط ماشین و تعمیرات",
            iconRes = InstallmentCategory.CAR_INSURANCE.iconRes,
            vectorIcon = Icons.Rounded.DirectionsCar,
            accentColor = InstallmentCategory.CAR_INSURANCE.accentColor,
            onClick = { onSelectCategory(InstallmentCategory.CAR_INSURANCE) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        CategoryCardItem(
            title = "اقساط متفرقه و شخصی",
            subtitle = "خرید کالا، اقساط فروشگاهی و سایر تعهدات",
            iconRes = InstallmentCategory.MISC.iconRes,
            vectorIcon = Icons.Rounded.ReceiptLong,
            accentColor = InstallmentCategory.MISC.accentColor,
            onClick = { onSelectCategory(InstallmentCategory.MISC) }
        )
    }
}

/**
 * CATEGORY CARD COMPONENT FOR STEP 1
 */
@Composable
private fun CategoryCardItem(
    title: String,
    subtitle: String,
    @androidx.annotation.DrawableRes iconRes: Int,
    vectorIcon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Soft3DIcon(
                    imageRes = iconRes,
                    vectorIcon = vectorIcon,
                    contentDescription = title,
                    accentColor = accentColor,
                    size = 48.dp
                )

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronLeft,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * STEP 2: FORM DETAILS SCREEN (Compact single-screen layout without vertical scrolling)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormDetailsStep(
    category: InstallmentCategory,
    title: String,
    onTitleChange: (String) -> Unit,
    provider: String,
    onProviderChange: (String) -> Unit,
    insuranceType: com.example.ui.screens.installments.model.VehicleInsuranceType?,
    onInsuranceTypeChange: (com.example.ui.screens.installments.model.VehicleInsuranceType) -> Unit,
    totalAmount: String,
    onTotalAmountChange: (String) -> Unit,
    installmentAmount: String,
    onInstallmentAmountChange: (String) -> Unit,
    installmentsCount: String,
    onInstallmentsCountChange: (String) -> Unit,
    paidInstallmentsCount: String,
    onPaidInstallmentsCountChange: (String) -> Unit,
    submitValidationError: String?,
    dueDate: String,
    onDueDateChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    reminderEnabled: Boolean,
    onReminderEnabledChange: (Boolean) -> Unit,
    reminderTime: String,
    remindAtTime: Boolean,
    remind1DayBefore: Boolean,
    remind2DaysBefore: Boolean,
    remind1WeekBefore: Boolean,
    onOpenReminderModal: () -> Unit,
    onOpenCustomSchedule: () -> Unit,
    isCustomScheduleActive: Boolean,
    onBackToCategories: () -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        // Top Header with Back Button, Category Pill, and Close
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackToCategories,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "بازگشت",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = category.accentColor.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, category.accentColor.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "ثبت: ${category.title}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = category.accentColor,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "بستن",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // SECTION 1: COMMITMENT & COUNTERPARTY DETAILS (Title & Bank/Provider Side-by-Side)
            FormSectionCard(title = "مشخصات تعهد و طرف حساب") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = onTitleChange,
                        label = { Text("عنوان وام/تعهد") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("installment_title_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = provider,
                        onValueChange = onProviderChange,
                        label = { Text("نام بانک/طرف حساب") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                if (category == InstallmentCategory.CAR_INSURANCE) {
                    var insuranceTypeExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = insuranceTypeExpanded,
                        onExpandedChange = { insuranceTypeExpanded = !insuranceTypeExpanded }
                    ) {
                        OutlinedTextField(
                            value = insuranceType?.title.orEmpty(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("نوع بیمه *") },
                            placeholder = { Text("شخص ثالث یا بدنه را انتخاب کنید") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = insuranceTypeExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = insuranceTypeExpanded,
                            onDismissRequest = { insuranceTypeExpanded = false }
                        ) {
                            com.example.ui.screens.installments.model.VehicleInsuranceType.values().forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.title) },
                                    onClick = {
                                        onInsuranceTypeChange(type)
                                        insuranceTypeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // SECTION 2: FINANCIAL DETAILS
            FormSectionCard(title = "مشخصات مالی وام") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PersianAmountInputField(
                        value = totalAmount,
                        onValueChange = onTotalAmountChange,
                        allowDecimalInput = true,
                        onRawDecimalAmountChange = { rawValue, _ -> onTotalAmountChange(rawValue) },
                        label = "مبلغ کل وام / تعهد",
                        useOuterHeader = false,
                        modifier = Modifier.testTag("installment_total_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = paidInstallmentsCount,
                            onValueChange = { input ->
                                onPaidInstallmentsCountChange(
                                    IranianPhoneUtils.convertDigitsToPersian(
                                        IranianPhoneUtils.convertDigitsToEnglish(input).filter { it.isDigit() }
                                    )
                                )
                            },
                            label = {
                                Text(
                                    "تعداد اقساط پرداخت‌شده",
                                    maxLines = 1,
                                    softWrap = false,
                                    fontSize = 10.sp
                                )
                            },
                            isError = submitValidationError != null,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("installment_paid_count_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        var installmentsDropdownExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = installmentsDropdownExpanded,
                            onExpandedChange = { installmentsDropdownExpanded = !installmentsDropdownExpanded },
                            modifier = Modifier.weight(0.8f)
                        ) {
                            OutlinedTextField(
                                value = installmentsCount,
                                onValueChange = { input ->
                                    onInstallmentsCountChange(
                                        IranianPhoneUtils.convertDigitsToPersian(
                                            IranianPhoneUtils.convertDigitsToEnglish(input).filter { it.isDigit() }
                                        )
                                    )
                                },
                                label = { Text("تعداد اقساط", maxLines = 1, softWrap = false) },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = installmentsDropdownExpanded)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("installment_count_input"),
                                shape = RoundedCornerShape(10.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = installmentsDropdownExpanded,
                                onDismissRequest = { installmentsDropdownExpanded = false },
                                modifier = Modifier.heightIn(max = 280.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 260.dp)
                                        .verticalScroll(rememberScrollState())
                                        .padding(horizontal = 6.dp)
                                ) {
                                    (4..60).toList().chunked(4).forEach { rowCounts ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            rowCounts.forEach { count ->
                                                TextButton(
                                                    onClick = {
                                                        onInstallmentsCountChange(
                                                            IranianPhoneUtils.convertDigitsToPersian(count.toString())
                                                        )
                                                        installmentsDropdownExpanded = false
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = IranianPhoneUtils.convertDigitsToPersian(count.toString()),
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                }
                                            }
                                            repeat(4 - rowCounts.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenCustomSchedule,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isCustomScheduleActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = if (isCustomScheduleActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isCustomScheduleActive) "اقساط غیرهمسان فعال است" else "تنظیم اقساط",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isCustomScheduleActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (submitValidationError != null) {
                        Text(
                            text = submitValidationError.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                }
            }

            // SECTION 3: SCHEDULE & SMART REMINDER (Side by Side)
            FormSectionCard(title = "زمان‌بندی و یادآور سررسید") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1.0f)) {
                        PersianDateInputField(
                            value = dueDate,
                            onValueChange = onDueDateChange,
                            label = "تاریخ اولین قسط",
                            useOuterHeader = false,
                            showSubLabel = false,
                            fieldHeight = 56.dp,
                            singleLineLabel = true,
                            contentStartPadding = 16.dp,
                            modifier = Modifier
                                .testTag("installment_date_input")
                        )
                    }

                    Surface(
                        onClick = onOpenReminderModal,
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .weight(0.85f)
                            .height(56.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "تنظیم یادآور",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (reminderEnabled) "ساعت $reminderTime" else "غیرفعال",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Rounded.ChevronLeft,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = onReminderEnabledChange,
                        modifier = Modifier
                            .width(48.dp)
                            .testTag("installment_reminder_enabled")
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Bottom Action Button
            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("installment_confirm_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = category.accentColor)
            ) {
                Icon(imageVector = Icons.Rounded.Done, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ثبت و ذخیره قسط",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

/**
 * FORM SECTION CARD CONTAINER
 */
@Composable
private fun FormSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            content()
        }
    }
}

/**
 * CHECKBOX OPTION ROW FOR POP-UP DIALOG
 */
@Composable
private fun ReminderOptionRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * CENTERED POP-UP DIALOG FOR CONFIGURING CUSTOM / VARIABLE INSTALLMENTS
 */
@Composable
private fun CustomInstallmentsDialog(
    totalAmountStr: String,
    activeCurrency: AppCurrency,
    installmentAmountStr: String,
    installmentsCountStr: String,
    dueDateStr: String,
    initialCustomList: List<com.example.ui.screens.installments.model.PaymentHistoryItem>?,
    onDismiss: () -> Unit,
    onConfirm: (List<com.example.ui.screens.installments.model.PaymentHistoryItem>) -> Unit
) {
    val totalAmountLong = IranianAmountUtils.parseAmountToBigDecimal(totalAmountStr)?.let { runCatching { MoneyAmount(it, activeCurrency).asRialsLongExact() }.getOrNull() } ?: 0L
    val countInt = IranianPhoneUtils.convertDigitsToEnglish(installmentsCountStr).filter { it.isDigit() }.toIntOrNull()?.coerceAtLeast(1) ?: 12

    var rangeStartStr by remember { mutableStateOf("۱") }
    var rangeEndStr by remember { mutableStateOf(IranianPhoneUtils.convertDigitsToPersian(countInt.toString())) }
    var rangeAmountStr by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    var scheduleList by remember {
        mutableStateOf(
            if (!initialCustomList.isNullOrEmpty()) {
                initialCustomList
            } else {
                (1..countInt).map { i ->
                    com.example.ui.screens.installments.model.PaymentHistoryItem(
                        id = "p_custom_$i",
                        installmentNumber = i,
                        dueDate = if (i == 1) dueDateStr else com.example.util.PersianCalendarHelper.addMonthsToPersianDate(dueDateStr, i - 1),
                        amountFormatted = "",
                        status = if (i == 1) com.example.ui.screens.installments.model.InstallmentStatus.DUE_SOON else com.example.ui.screens.installments.model.InstallmentStatus.PENDING,
                        amount = 0L,
                        amountUnit = AppCurrency.RIAL
                    )
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            modifier = Modifier
                .widthIn(max = 380.dp)
                .fillMaxWidth(0.94f)
                .heightIn(max = 520.dp)
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "تنظیم اقساط غیرهمسان یا کارمزددار",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "شماره شروع و پایان بازه را مشخص کنید و مبلغ هر قسط در آن بازه را وارد کنید:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = rangeStartStr,
                                onValueChange = {
                                    rangeStartStr = IranianPhoneUtils.convertDigitsToPersian(it.filter { ch -> ch.isDigit() })
                                    validationError = null
                                },
                                modifier = Modifier.weight(1f),
                                label = { Text("از قسط شماره") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = rangeEndStr,
                                onValueChange = {
                                    rangeEndStr = IranianPhoneUtils.convertDigitsToPersian(it.filter { ch -> ch.isDigit() })
                                    validationError = null
                                },
                                modifier = Modifier.weight(1f),
                                label = { Text("تا قسط شماره") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                        PersianAmountInputField(
                            value = rangeAmountStr,
                            onValueChange = {
                                rangeAmountStr = it
                                validationError = null
                            },
                            allowDecimalInput = true,
                            onRawDecimalAmountChange = { rawValue, _ ->
                                rangeAmountStr = rawValue
                                validationError = null
                            },
                            label = "مبلغ هر قسط در این بازه (${MoneyFormatter.getUnitLabel()})",
                            showWordsPreview = false,
                            useOuterHeader = false
                        )
                        Button(
                            onClick = {
                                val first = IranianPhoneUtils.convertDigitsToEnglish(rangeStartStr).filter { it.isDigit() }.toIntOrNull()
                                val last = IranianPhoneUtils.convertDigitsToEnglish(rangeEndStr).filter { it.isDigit() }.toIntOrNull()
                                val amountRials = IranianAmountUtils.parseAmountToBigDecimal(rangeAmountStr)?.let {
                                    runCatching { MoneyAmount(it, activeCurrency).asRialsLongExact() }.getOrNull()
                                } ?: 0L
                                if (first == null || last == null || first < 1 || last < first || last > scheduleList.size) {
                                    validationError = "بازه شماره اقساط معتبر نیست؛ شماره شروع و پایان را بررسی کنید."
                                    return@Button
                                }
                                if (amountRials <= 0L) {
                                    validationError = "لطفاً مبلغی بیشتر از صفر برای هر قسط وارد کنید."
                                    return@Button
                                }
                                scheduleList = scheduleList.map { item ->
                                    if (item.installmentNumber in first..last) {
                                        item.copy(amount = amountRials, amountUnit = AppCurrency.RIAL, amountFormatted = MoneyFormatter.formatExact(MoneyAmount.of(amountRials, AppCurrency.RIAL)))
                                    } else item
                                }
                                validationError = null
                            },
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("اعمال", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    scheduleList.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "قسط ${IranianPhoneUtils.convertDigitsToPersian(item.installmentNumber.toString())}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (item.amount > 0L) MoneyFormatter.formatExact(MoneyAmount.of(item.amount, item.amountUnit)) else "تعیین نشده",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (item.amount > 0L) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val sumCustom = scheduleList.fold(0L) { total, item -> Math.addExact(total, item.amount) }
                val expectedTotal = totalAmountLong
                val totalDifference = Math.subtractExact(expectedTotal, sumCustom)
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "جمع کل اقساط: ${MoneyFormatter.formatExact(MoneyAmount.of(sumCustom, AppCurrency.RIAL))}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "مبلغ کل وام: ${MoneyFormatter.formatExact(MoneyAmount.of(expectedTotal, AppCurrency.RIAL))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (totalDifference == 0L) {
                            "مجموع اقساط با مبلغ کل وام برابر است."
                        } else if (totalDifference > 0L) {
                            "مبلغ اقساط ${MoneyFormatter.formatExact(MoneyAmount.of(totalDifference, AppCurrency.RIAL))} کمتر از مبلغ وام است."
                        } else {
                            "مبلغ اقساط ${MoneyFormatter.formatExact(MoneyAmount.of(totalDifference.absoluteValue, AppCurrency.RIAL))} بیشتر از مبلغ وام است."
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (totalDifference == 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }

                if (validationError != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("انصراف")
                    }
                    Button(
                        onClick = {
                            val invalidItem = scheduleList.firstOrNull { item ->
                                item.installmentNumber <= 0 ||
                                item.amount <= 0L ||
                                item.dueDate.isBlank() ||
                                !com.example.calendar.domain.CalendarDateUtils.isValidJalaliDate(item.dueDate)
                            }
                            val expectedTotal = totalAmountLong
                            val actualTotal = scheduleList.fold(0L) { total, item -> Math.addExact(total, item.amount) }
                            val expectedNumbers = (1..countInt).toList()
                            val actualNumbers = scheduleList.map { it.installmentNumber }.sorted()
                            if (scheduleList.size != countInt || actualNumbers != expectedNumbers) {
                                validationError = "شماره اقساط باید از ۱ تا ${IranianPhoneUtils.convertDigitsToPersian(countInt.toString())} کامل و بدون تکرار باشد."
                            } else if (invalidItem != null) {
                                validationError = "تمام اقساط باید شماره معتبر، مبلغ بیشتر از صفر و تاریخ سررسید معتبر شمسی داشته باشند."
                            } else if (expectedTotal <= 0L || actualTotal < expectedTotal) {
                                val difference = expectedTotal - actualTotal
                                validationError = "جمع اقساط نمی‌تواند از مبلغ کل وام کمتر باشد. مبلغ کل: ${MoneyFormatter.formatExact(MoneyAmount.of(expectedTotal, AppCurrency.RIAL))}، جمع اقساط: ${MoneyFormatter.formatExact(MoneyAmount.of(actualTotal, AppCurrency.RIAL))}، کسری: ${MoneyFormatter.formatExact(MoneyAmount.of(difference, AppCurrency.RIAL))}"
                            } else {
                                validationError = null
                                onConfirm(scheduleList)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("تایید")
                    }
                }
            }
        }
    }
}
