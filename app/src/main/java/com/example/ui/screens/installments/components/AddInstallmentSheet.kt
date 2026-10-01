package com.example.ui.screens.installments.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.reminder.domain.PredefinedOffset
import com.example.ui.components.PersianAmountInputField
import com.example.ui.components.PersianDateInputField
import com.example.ui.components.Soft3DIcon
import com.example.ui.screens.installments.model.InstallmentCategory
import com.example.ui.screens.reminder.components.PersianTimePickerDialog
import com.example.util.IranianAmountUtils
import com.example.util.MoneyFormatter
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
        dueDateStr: String,
        provider: String,
        notes: String,
        reminderEnabled: Boolean,
        reminderDays: String,
        reminderTime: String,
        selectedOffsets: List<PredefinedOffset>,
        customScheduleItems: List<com.example.ui.screens.installments.model.PaymentHistoryItem>?
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
    var dueDate by remember {
        mutableStateOf(PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate())
    }
    var providerOrPerson by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // Reminder state
    var reminderEnabled by remember { mutableStateOf(true) }
    var showReminderModal by remember { mutableStateOf(false) }

    // Selected offsets for reminder:
    var remindAtTime by remember { mutableStateOf(true) }
    var remind1DayBefore by remember { mutableStateOf(true) }
    var remind2DaysBefore by remember { mutableStateOf(false) }
    var remind1WeekBefore by remember { mutableStateOf(false) }
    var reminderTime by remember { mutableStateOf("۰۹:۰۰") }
    var showTimePicker by remember { mutableStateOf(false) }

    var customScheduleItems by remember { mutableStateOf<List<com.example.ui.screens.installments.model.PaymentHistoryItem>?>(null) }
    var showCustomScheduleDialog by remember { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    // Smart Auto-Calculation:
    LaunchedEffect(totalAmount, installmentsCount) {
        val parsedTotal = IranianAmountUtils.parseAmountToLong(totalAmount)
        val parsedCount = installmentsCount.filter { it.isDigit() }.toIntOrNull() ?: 0
        if (parsedTotal > 0 && parsedCount > 0) {
            val calcMonthly = parsedTotal / parsedCount
            installmentAmount = IranianAmountUtils.formatWithCommas(calcMonthly.toString())
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
                    totalAmount = totalAmount,
                    onTotalAmountChange = { totalAmount = it },
                    installmentAmount = installmentAmount,
                    onInstallmentAmountChange = { installmentAmount = it },
                    installmentsCount = installmentsCount,
                    onInstallmentsCountChange = { installmentsCount = it },
                    dueDate = dueDate,
                    onDueDateChange = { dueDate = it },
                    notes = notes,
                    onNotesChange = { notes = it },
                    reminderEnabled = reminderEnabled,
                    reminderTime = reminderTime,
                    remindAtTime = remindAtTime,
                    remind1DayBefore = remind1DayBefore,
                    remind2DaysBefore = remind2DaysBefore,
                    remind1WeekBefore = remind1WeekBefore,
                    onOpenReminderModal = { showReminderModal = true },
                    onOpenCustomSchedule = { showCustomScheduleDialog = true },
                    isCustomScheduleActive = customScheduleItems != null,
                    onBackToCategories = { currentStep = 1 },
                    onDismiss = onDismiss,
                    onSubmit = {
                        val offsets = mutableListOf<PredefinedOffset>()
                        if (remindAtTime) offsets.add(PredefinedOffset.AT_TIME)
                        if (remind1DayBefore) offsets.add(PredefinedOffset.BEFORE_1_DAY)
                        if (remind2DaysBefore) offsets.add(PredefinedOffset.BEFORE_2_DAYS)
                        if (remind1WeekBefore) offsets.add(PredefinedOffset.BEFORE_7_DAYS)

                        val daysSummary = offsets.joinToString("، ") { it.title }

                        onAddConfirm(
                            selectedCategory,
                            title.ifBlank { selectedCategory.title },
                            totalAmount,
                            installmentAmount,
                            installmentsCount,
                            dueDate,
                            providerOrPerson,
                            notes,
                            reminderEnabled,
                            daysSummary,
                            reminderTime,
                            offsets,
                            customScheduleItems
                        )
                        onDismiss()
                    }
                )
            }
        }
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
                    .widthIn(max = 340.dp)
                    .fillMaxWidth(0.92f)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Icon & Title
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "تنظیمات یادآور و نوتیفیکیشن",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "زمان‌های اطلاع‌رسانی پیش از سررسید را انتخاب کنید:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    // Enable/Disable Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "فعال‌سازی یادآور",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { reminderEnabled = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (reminderEnabled) {
                        // Checkbox Options
                        ReminderOptionRow(
                            title = "روز سررسید قسط",
                            checked = remindAtTime,
                            onCheckedChange = { remindAtTime = it }
                        )
                        ReminderOptionRow(
                            title = "۱ روز قبل از سررسید",
                            checked = remind1DayBefore,
                            onCheckedChange = { remind1DayBefore = it }
                        )
                        ReminderOptionRow(
                            title = "۲ روز قبل از سررسید",
                            checked = remind2DaysBefore,
                            onCheckedChange = { remind2DaysBefore = it }
                        )
                        ReminderOptionRow(
                            title = "۱ هفته قبل از سررسید",
                            checked = remind1WeekBefore,
                            onCheckedChange = { remind1WeekBefore = it }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

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
                    }

                    Spacer(modifier = Modifier.height(16.dp))

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
            installmentAmountStr = installmentAmount,
            installmentsCountStr = installmentsCount,
            dueDateStr = dueDate,
            initialCustomList = customScheduleItems,
            onDismiss = { showCustomScheduleDialog = false },
            onConfirm = { updatedList ->
                customScheduleItems = updatedList
                showCustomScheduleDialog = false
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
@Composable
private fun FormDetailsStep(
    category: InstallmentCategory,
    title: String,
    onTitleChange: (String) -> Unit,
    provider: String,
    onProviderChange: (String) -> Unit,
    totalAmount: String,
    onTotalAmountChange: (String) -> Unit,
    installmentAmount: String,
    onInstallmentAmountChange: (String) -> Unit,
    installmentsCount: String,
    onInstallmentsCountChange: (String) -> Unit,
    dueDate: String,
    onDueDateChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    reminderEnabled: Boolean,
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
            }

            // SECTION 2: FINANCIAL DETAILS
            FormSectionCard(title = "مشخصات مالی وام") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PersianAmountInputField(
                        value = totalAmount,
                        onValueChange = onTotalAmountChange,
                        label = "مبلغ کل وام / تعهد",
                        useOuterHeader = false,
                        modifier = Modifier.testTag("installment_total_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1.2f)) {
                            PersianAmountInputField(
                                value = installmentAmount,
                                onValueChange = onInstallmentAmountChange,
                                label = "مبلغ هر قسط",
                                showWordsPreview = false,
                                useOuterHeader = false,
                                modifier = Modifier.testTag("installment_monthly_input")
                            )
                        }

                        OutlinedTextField(
                            value = installmentsCount,
                            onValueChange = onInstallmentsCountChange,
                            label = { Text("تعداد اقساط") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(0.8f)
                                .testTag("installment_count_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenCustomSchedule,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isCustomScheduleActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = if (isCustomScheduleActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isCustomScheduleActive) "اقساط غیرهمسان فعال است" else "تنظیم اقساط غیرهمسان / کارمزددار",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isCustomScheduleActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
                    Box(modifier = Modifier.weight(1.1f)) {
                        PersianDateInputField(
                            value = dueDate,
                            onValueChange = onDueDateChange,
                            label = "تاریخ اولین قسط",
                            useOuterHeader = false,
                            showSubLabel = false,
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
                            .weight(0.9f)
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
    installmentAmountStr: String,
    installmentsCountStr: String,
    dueDateStr: String,
    initialCustomList: List<com.example.ui.screens.installments.model.PaymentHistoryItem>?,
    onDismiss: () -> Unit,
    onConfirm: (List<com.example.ui.screens.installments.model.PaymentHistoryItem>) -> Unit
) {
    val totalAmountLong = IranianAmountUtils.parseAmountToLong(totalAmountStr)
    val countInt = installmentsCountStr.filter { it.isDigit() }.toIntOrNull()?.coerceAtLeast(1) ?: 12

    var feeAmountStr by remember { mutableStateOf("") }

    var scheduleList by remember {
        mutableStateOf(
            if (!initialCustomList.isNullOrEmpty()) {
                initialCustomList
            } else {
                val defaultMonthly = if (totalAmountLong > 0) totalAmountLong / countInt else 1_000_000L
                (1..countInt).map { i ->
                    com.example.ui.screens.installments.model.PaymentHistoryItem(
                        id = "p_custom_$i",
                        installmentNumber = i,
                        dueDate = if (i == 1) dueDateStr.ifBlank { "۱۴۰۴/۰۸/۱۵" } else "قسط $i",
                        amountFormatted = MoneyFormatter.formatToman(defaultMonthly),
                        status = if (i == 1) com.example.ui.screens.installments.model.InstallmentStatus.DUE_SOON else com.example.ui.screens.installments.model.InstallmentStatus.PENDING,
                        amount = defaultMonthly
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
                    text = "مبلغ قسط اول (کارمزد ۴٪) یا هر قسط را به صورت دلخواه تغییر دهید:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            PersianAmountInputField(
                                value = feeAmountStr,
                                onValueChange = { feeAmountStr = it },
                                label = "مبلغ قسط ۱ (کارمزد)",
                                showWordsPreview = false,
                                useOuterHeader = false
                            )
                        }
                        Button(
                            onClick = {
                                val feeLong = IranianAmountUtils.parseAmountToLong(feeAmountStr)
                                if (feeLong > 0 && countInt > 1) {
                                    val remTotal = (totalAmountLong - feeLong).coerceAtLeast(0L)
                                    val remMonthly = remTotal / (countInt - 1)
                                    scheduleList = scheduleList.mapIndexed { idx, item ->
                                        if (idx == 0) {
                                            item.copy(amount = feeLong, amountFormatted = MoneyFormatter.formatToman(feeLong))
                                        } else {
                                            item.copy(amount = remMonthly, amountFormatted = MoneyFormatter.formatToman(remMonthly))
                                        }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("توزیع", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    scheduleList.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "قسط ${item.installmentNumber}:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.width(55.dp)
                            )
                            var rawInput by remember(item.amount) { mutableStateOf(IranianAmountUtils.formatWithCommas(item.amount.toString())) }
                            Box(modifier = Modifier.weight(1f)) {
                                PersianAmountInputField(
                                    value = rawInput,
                                    onValueChange = { newStr ->
                                        rawInput = newStr
                                        val parsedLong = IranianAmountUtils.parseAmountToLong(newStr)
                                        scheduleList = scheduleList.toMutableList().apply {
                                            this[index] = item.copy(
                                                amount = parsedLong,
                                                amountFormatted = MoneyFormatter.formatToman(parsedLong)
                                            )
                                        }
                                    },
                                    showWordsPreview = false,
                                    useOuterHeader = false
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val sumCustom = scheduleList.sumOf { it.amount }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "جمع کل اقساط: ${MoneyFormatter.formatToman(sumCustom)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
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
                            onConfirm(scheduleList)
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
