package com.example.ui.screens.reminder

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.api.SyncStatus
import com.example.data.database.ReminderEntity
import com.example.data.security.SessionManager
import com.example.data.security.SessionState
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusXL
import com.example.ui.theme.WarningAmberLight
import com.example.ui.screens.reminder.components.ReminderDateTimeSection
import com.example.util.IranianPhoneUtils
import com.example.util.PersianCalendarHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen(
    bottomBar: @Composable () -> Unit = {},
    onOpenAuth: () -> Unit = {},
    viewModel: ReminderViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val allReminders by viewModel.reminders.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val sessionState by viewModel.sessionState.collectAsState()

    var showAddReminderSheet by remember { mutableStateOf(false) }
    var snoozeReminderTarget by remember { mutableStateOf<ReminderEntity?>(null) }
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("امروز", "آینده نزدیک", "همه یادآورها", "انجام‌شده")

    val backgroundBrush = remember(isDark) {
        if (isDark) {
            Brush.verticalGradient(
                colors = listOf(Color(0xFF090D16), Color(0xFF0C1322), Color(0xFF090D16))
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9), Color(0xFFE2E8F0))
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = bottomBar,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddReminderSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .shadow(12.dp, CircleShape, spotColor = MaterialTheme.colorScheme.primary)
                    .testTag("add_reminder_fab")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "افزودن یادآور جدید",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header with live sync status & trigger
                ReminderHeaderSection(
                    syncStatus = uiState.syncStatus,
                    isRefreshing = uiState.isRefreshing,
                    onSyncClick = {
                        if (SessionManager.accessToken != null) {
                            viewModel.syncNow()
                        } else {
                            onOpenAuth()
                        }
                    },
                    sessionState = sessionState,
                    onAuthClick = onOpenAuth
                )

                // Section Tabs (امروز, آینده نزدیک, همه یادآورها, انجام‌شده)
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 16.dp,
                    containerColor = Color.Transparent,
                    divider = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        FilterChip(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            label = {
                                Text(
                                    title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }

                // Filter reminder states based on tab
                val filteredReminders = remember(allReminders, selectedTab) {
                    when (selectedTab) {
                        0 -> allReminders.filter {
                            val isPast = it.scheduledDateTime < System.currentTimeMillis()
                            val isToday = Math.abs(it.scheduledDateTime - System.currentTimeMillis()) < 43200000
                            (isToday || (isPast && it.completedAt == null)) && it.completedAt == null
                        }
                        1 -> allReminders.filter {
                            it.scheduledDateTime > System.currentTimeMillis() &&
                                    Math.abs(it.scheduledDateTime - System.currentTimeMillis()) >= 43200000 &&
                                    it.completedAt == null
                        }
                        2 -> allReminders
                        3 -> allReminders.filter { it.completedAt != null }
                        else -> allReminders
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
                ) {
                    if (filteredReminders.isNotEmpty()) {
                        items(filteredReminders, key = { it.id }) { reminder ->
                            ReminderCard(
                                reminder = reminder,
                                onComplete = {
                                    viewModel.markCompleted(reminder)
                                    scope.launch { snackbarHostState.showSnackbar("وضعیت یادآور به‌روز شد.") }
                                },
                                onSnooze = {
                                    snoozeReminderTarget = reminder
                                },
                                onDelete = {
                                    viewModel.deleteReminder(reminder.id)
                                    scope.launch { snackbarHostState.showSnackbar("یادآور حذف شد.") }
                                },
                                onToggle = {
                                    viewModel.toggleEnabled(reminder)
                                },
                                onRefreshSms = {
                                    viewModel.refreshSmsStatus(reminder.id)
                                }
                            )
                        }
                    } else {
                        item {
                            EmptyRemindersState()
                        }
                    }
                }
            }

            // Snooze Dialog
            if (snoozeReminderTarget != null) {
                SnoozeDialog(
                    reminder = snoozeReminderTarget!!,
                    onDismiss = { snoozeReminderTarget = null },
                    onSnooze = { minutes ->
                        viewModel.snoozeReminder(snoozeReminderTarget!!.id, minutes)
                        snoozeReminderTarget = null
                        scope.launch { snackbarHostState.showSnackbar("هشدار برای $minutes دقیقه به تعویق افتاد.") }
                    }
                )
            }

            // Add Reminder Dialog
            if (showAddReminderSheet) {
                Dialog(
                    onDismissRequest = { showAddReminderSheet = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .wrapContentHeight(),
                        shape = RoundedCornerShape(RadiusXL),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp
                    ) {
                        AddReminderSheetContent(
                            onDismiss = { showAddReminderSheet = false },
                            onSave = { title, desc, scheduledMs, repeat ->
                                viewModel.createReminder(
                                    type = "GENERAL",
                                    title = title,
                                    description = desc,
                                    scheduledDateTime = scheduledMs,
                                    repeatRule = repeat,
                                    notificationEnabled = true,
                                    smsEnabled = false,
                                    phoneNumber = null
                                )
                                showAddReminderSheet = false
                                scope.launch { snackbarHostState.showSnackbar("یادآور با موفقیت ثبت شد.") }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReminderHeaderSection(
    syncStatus: SyncStatus = SyncStatus.IDLE,
    isRefreshing: Boolean = false,
    onSyncClick: () -> Unit = {},
    sessionState: SessionState = SessionState.LoggedOut,
    onAuthClick: () -> Unit = {}
) {
    Layered3DCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "مرکز یادآوری هوشمند",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))

                    // Sync Status Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (syncStatus) {
                            SyncStatus.SYNCING -> InfoIndigoLight.copy(alpha = 0.15f)
                            SyncStatus.SUCCESS -> EmeraldPrimaryLight.copy(alpha = 0.15f)
                            SyncStatus.OFFLINE -> WarningAmberLight.copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = when (syncStatus) {
                                SyncStatus.SYNCING -> "در حال همگام‌سازی..."
                                SyncStatus.SUCCESS -> "ابری همگام"
                                SyncStatus.OFFLINE -> "حالت آفلاین"
                                else -> if (sessionState is SessionState.Authenticated) "آنلاین" else "محلی"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = when (syncStatus) {
                                SyncStatus.SYNCING -> InfoIndigoLight
                                SyncStatus.SUCCESS -> EmeraldPrimaryLight
                                SyncStatus.OFFLINE -> WarningAmberLight
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "مدیریت سررسید اقساط، موعد بیمه خودرو و یادآورهای شخصی با پیامک",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = onSyncClick,
                    modifier = Modifier.size(24.dp).testTag("reminder_sync_button")
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 1.5.dp,
                            color = EmeraldPrimaryLight
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Sync,
                            contentDescription = "همگام‌سازی",
                            tint = if (sessionState is SessionState.Authenticated) EmeraldPrimaryLight else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                if (sessionState !is SessionState.Authenticated) {
                    FilledTonalButton(
                        onClick = onAuthClick,
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("ورود", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ReminderCard(
    reminder: ReminderEntity,
    onComplete: () -> Unit,
    onSnooze: () -> Unit,
    onDelete: () -> Unit,
    onToggle: () -> Unit,
    onRefreshSms: () -> Unit = {}
) {
    val isCompleted = reminder.completedAt != null
    val isPast = reminder.scheduledDateTime < System.currentTimeMillis()
    val isDueSoon = Math.abs(reminder.scheduledDateTime - System.currentTimeMillis()) < 86400000

    val accentColor = when (reminder.type.uppercase(Locale.ROOT)) {
        "INSTALLMENT" -> EmeraldPrimaryLight
        "INSURANCE" -> WarningAmberLight
        "VEHICLE", "MAINTENANCE" -> InfoIndigoLight
        else -> MaterialTheme.colorScheme.primary
    }

    val typeLabel = when (reminder.type.uppercase(Locale.ROOT)) {
        "INSTALLMENT" -> "قسط بانکی"
        "INSURANCE" -> "بیمه‌نامه"
        "VEHICLE", "MAINTENANCE" -> "سرویس خودرو"
        else -> "یادآور شخصی"
    }

    val dateFormatted = remember(reminder.scheduledDateTime) {
        val pdt = PersianCalendarHelper.fromEpochMillis(reminder.scheduledDateTime)
        pdt.toFullPersianString()
    }
    val relativeFormatted = remember(reminder.scheduledDateTime) {
        PersianCalendarHelper.formatRelativeTimePersian(reminder.scheduledDateTime)
    }

    Layered3DCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reminder_card_${reminder.id}"),
        elevation = if (isCompleted) 1.dp else 4.dp,
        backgroundColor = if (isCompleted) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Soft3DIcon(
                        imageRes = when (reminder.type.uppercase(Locale.ROOT)) {
                            "INSTALLMENT" -> R.drawable.img_3d_installment
                            "INSURANCE" -> R.drawable.img_3d_insurance
                            "VEHICLE", "MAINTENANCE" -> R.drawable.img_3d_car
                            else -> R.drawable.img_3d_bell_notification
                        },
                        contentDescription = typeLabel,
                        accentColor = accentColor,
                        size = 42.dp
                    )

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = reminder.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = if (isCompleted) {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }

                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor
                        )
                    }
                }

                // Switch or Completed Check
                if (isCompleted) {
                    Surface(
                        shape = CircleShape,
                        color = EmeraldPrimaryLight.copy(alpha = 0.2f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = "انجام شده",
                            tint = EmeraldPrimaryLight,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                } else {
                    Switch(
                        checked = reminder.enabled,
                        onCheckedChange = { onToggle() },
                        modifier = Modifier.testTag("reminder_toggle_${reminder.id}")
                    )
                }
            }

            if (reminder.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = reminder.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metadata Row (Scheduled date, Sync state, SMS Badge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = if (isPast && !isCompleted) ExpenseRoseLight else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "$dateFormatted ($relativeFormatted)",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = if (isPast && !isCompleted) ExpenseRoseLight else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Badges: Sync State & SMS
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Sync badge
                    if (reminder.syncState != "SYNCED") {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = WarningAmberLight.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "در انتظار ارسال",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = WarningAmberLight,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // SMS Badge
                    if (reminder.smsEnabled) {
                        val smsStatus = reminder.smsDeliveryStatus ?: "QUEUED"
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (smsStatus) {
                                "DELIVERED" -> EmeraldPrimaryLight.copy(alpha = 0.15f)
                                "SENT" -> InfoIndigoLight.copy(alpha = 0.15f)
                                "FAILED" -> ExpenseRoseLight.copy(alpha = 0.15f)
                                else -> WarningAmberLight.copy(alpha = 0.15f)
                            },
                            modifier = Modifier.clickable { onRefreshSms() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Sms,
                                    contentDescription = null,
                                    tint = when (smsStatus) {
                                        "DELIVERED" -> EmeraldPrimaryLight
                                        "SENT" -> InfoIndigoLight
                                        "FAILED" -> ExpenseRoseLight
                                        else -> WarningAmberLight
                                    },
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = when (smsStatus) {
                                        "DELIVERED" -> "پیامک تحویل شد"
                                        "SENT" -> "پیامک ارسال شد"
                                        "FAILED" -> "خطا در پیامک"
                                        else -> "پیامک فعال"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = when (smsStatus) {
                                        "DELIVERED" -> EmeraldPrimaryLight
                                        "SENT" -> InfoIndigoLight
                                        "FAILED" -> ExpenseRoseLight
                                        else -> WarningAmberLight
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Action Buttons (Complete, Snooze, Delete)
            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onComplete,
                        shape = RoundedCornerShape(RadiusMD),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = if (isCompleted) "بازگردانی" else "انجام شد",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (!isCompleted) {
                        FilledTonalButton(
                            onClick = onSnooze,
                            shape = RoundedCornerShape(RadiusMD),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("تعویق", fontSize = 12.sp)
                        }
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = "حذف یادآور",
                        tint = ExpenseRoseLight,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddReminderSheetContent(
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String,
        scheduledDateTime: Long,
        repeatRule: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var repeatRule by remember { mutableStateOf("NONE") }

    // Persian Date & Time selection state
    val currentMillis = remember { System.currentTimeMillis() }
    val initialPdt = remember {
        // Default to tomorrow at 09:00 AM
        val tomorrowMillis = currentMillis + 24 * 3600 * 1000L
        PersianCalendarHelper.fromEpochMillis(tomorrowMillis)
    }

    var selectedYear by remember { mutableStateOf(initialPdt.year) }
    var selectedMonth by remember { mutableStateOf(initialPdt.month) }
    var selectedDay by remember { mutableStateOf(initialPdt.day) }
    var selectedHour by remember { mutableStateOf(9) }
    var selectedMinute by remember { mutableStateOf(0) }

    val scheduledTime = remember(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute) {
        PersianCalendarHelper.jalaliToEpochMillis(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute)
    }
    val isPast = scheduledTime <= System.currentTimeMillis()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "ثبت یادآور هوشمند جدید",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
            color = MaterialTheme.colorScheme.onSurface
        )

        // Repeat selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val repeatOptions = listOf(
                "NONE" to "بدون تکرار",
                "DAILY" to "هر روز",
                "WEEKLY" to "هر هفته",
                "MONTHLY" to "هر ماه"
            )
            repeatOptions.forEach { (rule, label) ->
                FilterChip(
                    selected = repeatRule == rule,
                    onClick = { repeatRule = rule },
                    label = { Text(label, fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("عنوان یادآور") },
            placeholder = { Text("مثلاً: خرید نان") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("توضیحات کوتاه (اختیاری)") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 2
        )

        // Date and Time selection (Interactive Persian Date & Time Picker with Presets)
        ReminderDateTimeSection(
            year = selectedYear,
            month = selectedMonth,
            day = selectedDay,
            hour = selectedHour,
            minute = selectedMinute,
            onDateTimeChanged = { y, m, d, h, min ->
                selectedYear = y
                selectedMonth = m
                selectedDay = d
                selectedHour = h
                selectedMinute = min
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text("انصراف")
            }
            Button(
                onClick = {
                    if (title.isNotBlank() && !isPast) {
                        onSave(
                            title,
                            description,
                            scheduledTime,
                            repeatRule
                        )
                    }
                },
                enabled = title.isNotBlank() && !isPast,
                modifier = Modifier.weight(1.3f)
            ) {
                Text("ثبت یادآور")
            }
        }
    }
}

@Composable
fun SnoozeDialog(
    reminder: ReminderEntity,
    onDismiss: () -> Unit,
    onSnooze: (minutes: Long) -> Unit
) {
    var showCustomPicker by remember { mutableStateOf(false) }

    val currentMillis = remember { System.currentTimeMillis() }
    val initialPdt = remember {
        val defaultTarget = reminder.scheduledDateTime.coerceAtLeast(currentMillis) + 24 * 3600 * 1000L
        PersianCalendarHelper.fromEpochMillis(defaultTarget)
    }

    var selectedYear by remember { mutableStateOf(initialPdt.year) }
    var selectedMonth by remember { mutableStateOf(initialPdt.month) }
    var selectedDay by remember { mutableStateOf(initialPdt.day) }
    var selectedHour by remember { mutableStateOf(initialPdt.hour) }
    var selectedMinute by remember { mutableStateOf(initialPdt.minute) }

    val customScheduledTime = remember(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute) {
        PersianCalendarHelper.jalaliToEpochMillis(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("به تعویق انداختن یادآور") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("یادآوری «${reminder.title}» چه مدت دیگر یادآوری شود؟")

                if (!showCustomPicker) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onSnooze(15L) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("۱۵ دقیقه دیگر", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { onSnooze(60L) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("۱ ساعت دیگر", fontSize = 11.sp)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onSnooze(1440L) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("فردا همین موقع", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { onSnooze(3L * 1440L) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("۳ روز دیگر", fontSize = 11.sp)
                            }
                        }
                        Button(
                            onClick = { showCustomPicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("زمان دلخواه...", fontSize = 12.sp)
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("انتخاب زمان سفارشی:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        ReminderDateTimeSection(
                            year = selectedYear,
                            month = selectedMonth,
                            day = selectedDay,
                            hour = selectedHour,
                            minute = selectedMinute,
                            onDateTimeChanged = { y, m, d, h, min ->
                                selectedYear = y
                                selectedMonth = m
                                selectedDay = d
                                selectedHour = h
                                selectedMinute = min
                            }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = { showCustomPicker = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("بازگشت")
                            }
                            Button(
                                onClick = {
                                    val diffMs = customScheduledTime - System.currentTimeMillis()
                                    val diffMinutes = (diffMs / (60 * 1000L)).coerceAtLeast(1L)
                                    onSnooze(diffMinutes)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("تایید تعویق")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            if (!showCustomPicker) {
                TextButton(onClick = onDismiss) {
                    Text("انصراف")
                }
            }
        }
    )
}

@Composable
fun EmptyRemindersState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Soft3DIcon(
            imageRes = R.drawable.img_3d_bell_notification,
            contentDescription = "بدون یادآور",
            size = 80.dp
        )
        Text(
            text = "هیچ یادآوری یافت نشد!",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = "با زدن دکمه + یادآور جدیدی ثبت کنید.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}
