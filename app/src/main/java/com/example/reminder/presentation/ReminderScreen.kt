package com.example.reminder.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.reminder.data.ReminderEntity
import com.example.vehicle.data.VehicleRepository
import com.example.data.preferences.AppPreferencesRepository
import com.example.ui.screens.settings.model.AlertDeliveryPreference
import androidx.compose.ui.platform.LocalContext
import com.example.reminder.domain.ReminderType
import com.example.reminder.domain.ReminderPolicyEngine
import com.example.reminder.domain.PredefinedOffset
import com.example.reminder.domain.SnoozeOption
import com.example.reminder.viewmodel.ReminderFilterChip
import com.example.reminder.viewmodel.ReminderTab
import com.example.reminder.viewmodel.ReminderViewModel
import com.example.ui.screens.reminder.NotificationCenterScreen
import com.example.ui.components.AppSectionHeader
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusSM
import com.example.ui.theme.WarningAmberLight
import com.example.util.IranianPhoneUtils

private enum class ReminderSubScreen {
    LIST,
    ADD,
    DETAIL,
    EDIT_INSTALLMENT_REMINDER,
    EDIT_REMINDER,
    NOTIFICATION_CENTER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen(
    bottomBar: @Composable () -> Unit = {},
    viewModel: ReminderViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val appContext = LocalContext.current
    val preferences = remember { AppPreferencesRepository.getInstance(appContext) }
    val globalNotificationsEnabled by preferences.notificationsEnabled.collectAsState()
    val globalDeliveryMode by preferences.alertDelivery.collectAsState()
    val globalDeliveryConfigured = globalNotificationsEnabled && globalDeliveryMode != AlertDeliveryPreference.NONE
    val vehicleRepository = remember { VehicleRepository.instance }
    val vehicles by vehicleRepository.vehicles.collectAsState()
    val vehicleServices by vehicleRepository.services.collectAsState()

    LaunchedEffect(vehicleRepository, appContext) {
        vehicleRepository.initDatabase(appContext)
    }

    val vehiclePlateForReminder: (ReminderEntity) -> String? = { reminder ->
        val isVehicleServiceReminder =
            reminder.title.startsWith("یادآور سرویس:") || reminder.targetKilometer != null
        if (isVehicleServiceReminder && reminder.sourceType.equals("VEHICLE", ignoreCase = true)) {
            val serviceId = reminder.sourceId ?: reminder.id
            val service = vehicleServices.firstOrNull { it.id == serviceId }
            service?.let { linkedService ->
                vehicles.firstOrNull { it.id == linkedService.vehicleId }
                    ?.plate
                    ?.takeIf { it.isNotBlank() }
            }
        } else {
            null
        }
    }

    var currentSubScreen by remember { mutableStateOf(ReminderSubScreen.LIST) }
    var selectedReminderForDetail by remember { mutableStateOf<ReminderEntity?>(null) }
    var showSnoozeSheetForReminder by remember { mutableStateOf<ReminderEntity?>(null) }

    BackHandler(enabled = currentSubScreen != ReminderSubScreen.LIST) {
        when (currentSubScreen) {
            ReminderSubScreen.ADD, ReminderSubScreen.NOTIFICATION_CENTER -> currentSubScreen = ReminderSubScreen.LIST
            ReminderSubScreen.EDIT_INSTALLMENT_REMINDER, ReminderSubScreen.EDIT_REMINDER -> currentSubScreen = ReminderSubScreen.DETAIL
            ReminderSubScreen.DETAIL -> {
                selectedReminderForDetail = null
                currentSubScreen = ReminderSubScreen.LIST
            }
            ReminderSubScreen.LIST -> Unit
        }
    }

    when (currentSubScreen) {
        ReminderSubScreen.ADD -> {
            AddReminderScreen(
                onBackClick = { currentSubScreen = ReminderSubScreen.LIST },
                onSaveReminderWithResult = { reminder, offsets, repeatType, repeatInterval, onComplete ->
                    viewModel.createReminder(reminder, offsets, repeatType, repeatInterval, onComplete)
                }
            )
        }

        ReminderSubScreen.DETAIL -> {
            val rem = selectedReminderForDetail
            if (rem != null) {
                ReminderDetailScreen(
                    reminder = rem,
                    onBackClick = {
                        selectedReminderForDetail = null
                        currentSubScreen = ReminderSubScreen.LIST
                    },
                    onDelete = {
                        viewModel.deleteReminder(rem.id)
                        selectedReminderForDetail = null
                        currentSubScreen = ReminderSubScreen.LIST
                    },
                    onEditInstallmentReminder = {
                        currentSubScreen = ReminderSubScreen.EDIT_INSTALLMENT_REMINDER
                    },
                    onEditManualReminder = {
                        currentSubScreen = ReminderSubScreen.EDIT_REMINDER
                    }
                )
            } else {
                currentSubScreen = ReminderSubScreen.LIST
            }
        }

        ReminderSubScreen.EDIT_REMINDER -> {
            val rem = selectedReminderForDetail
            if (rem != null) {
                var editConfig by remember(rem.id) { mutableStateOf<com.example.reminder.viewmodel.ReminderEditConfiguration?>(null) }
                var editLoadError by remember(rem.id) { mutableStateOf<String?>(null) }
                LaunchedEffect(rem.id) {
                    runCatching { viewModel.getReminderEditConfiguration(rem.id) }
                        .onSuccess { editConfig = it }
                        .onFailure { editLoadError = it.localizedMessage ?: "خواندن تنظیمات یادآور ممکن نشد." }
                }
                when {
                    editLoadError != null -> Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(editLoadError.orEmpty(), color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { currentSubScreen = ReminderSubScreen.DETAIL }) { Text("بازگشت") }
                    }
                    editConfig == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    else -> AddReminderScreen(
                        onBackClick = { currentSubScreen = ReminderSubScreen.DETAIL },
                        editingReminder = rem,
                        initialOffsets = editConfig!!.offsets,
                        initialRepeatType = editConfig!!.repeatType,
                        initialRepeatInterval = editConfig!!.repeatInterval,
                        onSaveReminderWithResult = { updated, offsets, repeatType, repeatInterval, onComplete ->
                            viewModel.updateReminder(updated, offsets, repeatType, repeatInterval) { result ->
                                result.fold(
                                    onSuccess = { saved ->
                                        selectedReminderForDetail = saved
                                        currentSubScreen = ReminderSubScreen.DETAIL
                                        onComplete(Result.success(offsets.size))
                                    },
                                    onFailure = { error -> onComplete(Result.failure(error)) }
                                )
                            }
                        }
                    )
                }
            } else {
                currentSubScreen = ReminderSubScreen.LIST
            }
        }

        ReminderSubScreen.EDIT_INSTALLMENT_REMINDER -> {
            val rem = selectedReminderForDetail
            if (rem != null) {
                var offsets by remember(rem.id) { mutableStateOf<List<PredefinedOffset>?>(null) }
                var offsetsLoadError by remember(rem.id) { mutableStateOf<String?>(null) }
                LaunchedEffect(rem.id) {
                    runCatching { viewModel.getReminderEditConfiguration(rem.id).offsets }
                        .onSuccess { offsets = it }
                        .onFailure { offsetsLoadError = it.localizedMessage ?: "خواندن تنظیمات فعلی یادآور ممکن نشد." }
                }
                val loadedOffsets = offsets
                if (offsetsLoadError != null) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            offsetsLoadError.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        TextButton(onClick = { currentSubScreen = ReminderSubScreen.DETAIL }) {
                            Text("بازگشت")
                        }
                    }
                } else if (loadedOffsets == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    EditInstallmentReminderScreen(
                        reminder = rem,
                        initialOffsets = loadedOffsets,
                        onBackClick = { currentSubScreen = ReminderSubScreen.DETAIL },
                        onSave = { time, selected, onComplete ->
                            viewModel.updateInstallmentReminder(rem, time, selected, onComplete)
                        },
                        onSaved = { updated ->
                            selectedReminderForDetail = updated
                            currentSubScreen = ReminderSubScreen.DETAIL
                        }
                    )
                }
            } else {
                currentSubScreen = ReminderSubScreen.LIST
            }
        }

        ReminderSubScreen.NOTIFICATION_CENTER -> {
            NotificationCenterScreen(
                onBackClick = { currentSubScreen = ReminderSubScreen.LIST }
            )
        }

        ReminderSubScreen.LIST -> {
            var isCompletedExpanded by remember { mutableStateOf(false) }
            var showDeleteCompletedDialog by remember { mutableStateOf(false) }
            var showCategoryFilterSheet by remember { mutableStateOf(false) }
            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(uiState.userMessage) {
                uiState.userMessage?.let { msg ->
                    snackbarHostState.showSnackbar(msg)
                    viewModel.clearUserMessage()
                }
            }

            LaunchedEffect(uiState.errorMessage) {
                uiState.errorMessage?.let { err ->
                    snackbarHostState.showSnackbar(err)
                    viewModel.clearErrorMessage()
                }
            }

            Scaffold(
                modifier = modifier.fillMaxSize(),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = bottomBar,
                topBar = {
                    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
                    AppSectionHeader(
                        title = "یادآورها و سررسیدها",
                        subtitle = "مدیریت سررسید اقساط، بیمه و سرویس خودرو",
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsNone,
                                contentDescription = "یادآورها",
                                tint = if (isDark) Color.White else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        actions = {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .clickable { currentSubScreen = ReminderSubScreen.NOTIFICATION_CENTER }
                                    .testTag("btn_notification_center"),
                                contentAlignment = Alignment.Center
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (uiState.notificationCount > 0) {
                                            Badge(
                                                containerColor = ExpenseRoseLight,
                                                contentColor = Color.White
                                            ) {
                                                Text("${uiState.notificationCount}", fontSize = 8.sp)
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.NotificationsNone,
                                        contentDescription = "مرکز اعلان‌ها",
                                        tint = if (isDark) Color.White else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { currentSubScreen = ReminderSubScreen.ADD },
                        containerColor = EmeraldPrimaryLight,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .testTag("fab_add_reminder")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "افزودن یادآور جدید",
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    // Compact reminder summary: today / nearest / tomorrow
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 112.dp)
                                .shadow(
                                    elevation = 3.dp,
                                    shape = RoundedCornerShape(20.dp),
                                    ambientColor = Color(0xFF0D9488).copy(alpha = 0.2f),
                                    spotColor = Color(0xFF0F172A).copy(alpha = 0.35f)
                                )
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF0B132B), Color(0xFF112240), Color(0xFF0A2B28))
                                    )
                                ),
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Transparent,
                            tonalElevation = 0.dp,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Color(0xFF2DD4BF).copy(alpha = 0.28f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // In RTL the first item is displayed on the right.
                                Column(
                                    modifier = Modifier.width(54.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = "امروز",
                                        maxLines = 1,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFFCBD5E1),
                                            fontSize = 10.sp
                                        )
                                    )
                                    Text(
                                        text = IranianPhoneUtils.convertDigitsToPersian(uiState.todayCount.toString()),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFF8FAFC)
                                        )
                                    )
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = "نزدیک‌ترین",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFFCBD5E1),
                                            fontSize = 10.sp
                                        )
                                    )
                                    Text(
                                        text = uiState.nearestReminder?.title ?: "بدون سررسید",
                                        modifier = Modifier.fillMaxWidth(),
                                        maxLines = 2,
                                        softWrap = true,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        ),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    uiState.nearestReminder?.let { reminder ->
                                        Text(
                                            text = "${if (uiState.nearestReminderTriggerAt != null) "تاریخ هشدار: " else "تاریخ یادآور: "}${uiState.nearestReminderTriggerAt?.let { com.example.util.PersianCalendarHelper.fromEpochMillis(it).toFormattedDate() } ?: reminder.date}",
                                            maxLines = 1,
                                            softWrap = false,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFFCBD5E1),
                                                fontSize = 10.sp
                                            ),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Text(
                                            text = "${if (uiState.nearestReminderTriggerAt != null) "ساعت هشدار: " else "ساعت یادآور: "}${uiState.nearestReminderTriggerAt?.let { com.example.util.PersianCalendarHelper.fromEpochMillis(it).toFormattedTime() } ?: reminder.time}",
                                            maxLines = 1,
                                            softWrap = false,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFFCBD5E1),
                                                fontSize = 10.sp
                                            ),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier.width(54.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = "فردا",
                                        maxLines = 1,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFFCBD5E1),
                                            fontSize = 10.sp
                                        )
                                    )
                                    Text(
                                        text = IranianPhoneUtils.convertDigitsToPersian(uiState.tomorrowCount.toString()),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFF8FAFC)
                                        )
                                    )
                                }
                            }
                        }
                    }
                    item {
                        val primaryTabs = listOf(ReminderTab.ALL, ReminderTab.TODAY, ReminderTab.UPCOMING, ReminderTab.COMPLETED)
                        TabRow(
                            selectedTabIndex = primaryTabs.indexOf(uiState.selectedTab).coerceAtLeast(0),
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = Color.Transparent,
                            divider = {},
                            indicator = {}
                        ) {
                            primaryTabs.forEach { tab ->
                                val selected = uiState.selectedTab == tab
                                Tab(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(tab) },
                                    modifier = Modifier.padding(horizontal = 1.dp),
                                    text = {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(18.dp),
                                            color = if (selected) EmeraldPrimaryLight else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                        ) {
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = tab.title,
                                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 9.dp),
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    style = MaterialTheme.typography.labelLarge.copy(
                                                        fontSize = 11.sp,
                                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    if (uiState.selectedTab == ReminderTab.COMPLETED && uiState.completedReminders.isNotEmpty()) {
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { showDeleteCompletedDialog = true }) {
                                    Icon(Icons.Rounded.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    Spacer(Modifier.width(6.dp))
                                    Text("حذف همه یادآورهای تکمیل‌شده", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("دسته‌بندی", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
                                Text(
                                    if (uiState.selectedFilterChip == ReminderFilterChip.ALL) "همه دسته‌ها" else "فیلتر: ${uiState.selectedFilterChip.title}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = { showCategoryFilterSheet = true },
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (uiState.selectedFilterChip != ReminderFilterChip.ALL) EmeraldPrimaryLight else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(if (uiState.selectedFilterChip == ReminderFilterChip.ALL) "انتخاب دسته" else "تغییر فیلتر")
                            }
                        }
                    }

                    // Reminders List or Empty State
                    if (uiState.reminders.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Soft3DIcon(
                                        imageRes = R.drawable.img_3d_bell_notification,
                                        contentDescription = "بدون یادآور",
                                        size = 64.dp,
                                        accentColor = InfoIndigoLight
                                    )
                                    Text(
                                        text = "هیچ یادآوری در این بخش یافت نشد",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                    Text(
                                        text = "با دکمه + یک یادآور جدید برای اقساط یا خودرو ثبت کنید.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    )
                                }
                            }
                        }
                    } else {
                        val activeReminders = uiState.reminders.filter { it.status != "COMPLETED" }
                        val completedReminders = uiState.reminders.filter { it.status == "COMPLETED" }

                        // Active Reminders
                        if (activeReminders.isNotEmpty()) {
                            items(activeReminders, key = { it.id }) { reminder ->
                                ReminderCard(
                                    reminder = reminder,
                                    vehiclePlate = vehiclePlateForReminder(reminder),
                                    scheduledTriggerAt = uiState.reminderDisplayTimes[reminder.id],
                                    onClick = {
                                        selectedReminderForDetail = reminder
                                        currentSubScreen = ReminderSubScreen.DETAIL
                                    },
                                    onToggleCompleted = {
                                        viewModel.completeReminder(reminder.id)
                                    },
                                    showSnooze = ReminderPolicyEngine.canSnooze(reminder, globalDeliveryConfigured),
                                    onSnoozeClick = {
                                        showSnoozeSheetForReminder = reminder
                                    },
                                    onDeleteClick = { viewModel.deleteReminder(reminder.id) }
                                )
                            }
                        }

                        // Collapsible Completed Reminders Section (If any exist)
                        if (completedReminders.isNotEmpty()) {
                            if (activeReminders.isNotEmpty()) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { isCompletedExpanded = !isCompletedExpanded }
                                            .padding(vertical = 6.dp, horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = null,
                                                tint = EmeraldPrimaryLight,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "یادآورهای تکمیل‌شده",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "(${IranianPhoneUtils.convertDigitsToPersian(completedReminders.size.toString())})",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            IconButton(
                                                onClick = { showDeleteCompletedDialog = true },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Rounded.DeleteSweep, contentDescription = "حذف همه یادآورهای تکمیل‌شده", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        Icon(
                                            imageVector = if (isCompletedExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                if (isCompletedExpanded) {
                                    items(completedReminders, key = { it.id }) { reminder ->
                                        ReminderCard(
                                            reminder = reminder,
                                            vehiclePlate = vehiclePlateForReminder(reminder),
                                            onClick = {
                                                selectedReminderForDetail = reminder
                                                currentSubScreen = ReminderSubScreen.DETAIL
                                            },
                                            onToggleCompleted = {
                                                viewModel.restoreReminder(reminder.id)
                                            },
                                            showSnooze = false,
                                            onSnoozeClick = {
                                                showSnoozeSheetForReminder = reminder
                                            },
                                            onDeleteClick = { viewModel.deleteReminder(reminder.id) }
                                        )
                                    }
                                }
                            } else {
                                // If viewing only completed reminders (e.g. Completed Tab)
                                items(completedReminders, key = { it.id }) { reminder ->
                                    ReminderCard(
                                        reminder = reminder,
                                        vehiclePlate = vehiclePlateForReminder(reminder),
                                        onClick = {
                                            selectedReminderForDetail = reminder
                                            currentSubScreen = ReminderSubScreen.DETAIL
                                        },
                                        onToggleCompleted = {
                                            viewModel.restoreReminder(reminder.id)
                                        },
                                        showSnooze = false,
                                        onSnoozeClick = {
                                            showSnoozeSheetForReminder = reminder
                                        },
                                        onDeleteClick = { viewModel.deleteReminder(reminder.id) }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(70.dp))
                    }
                }
            }

            if (showCategoryFilterSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showCategoryFilterSheet = false },
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("فیلتر دسته‌بندی", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text(
                            "یک دسته را انتخاب کنید؛ این فیلتر با بازه زمانی ترکیب می‌شود.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        ReminderFilterChip.entries.forEach { chip ->
                            val selected = uiState.selectedFilterChip == chip
                            Surface(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable {
                                    viewModel.selectFilterChip(chip)
                                    showCategoryFilterSheet = false
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = if (selected) EmeraldPrimaryLight.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimaryLight.copy(alpha = 0.65f)) else null
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(chip.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                    ))
                                    if (selected) Icon(Icons.Rounded.CheckCircle, contentDescription = "انتخاب‌شده", tint = EmeraldPrimaryLight)
                                }
                            }
                        }
                    }
                }
            }

            if (showDeleteCompletedDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteCompletedDialog = false },
                    title = { Text("حذف همه یادآورهای تکمیل‌شده؟") },
                    text = { Text("تمام یادآورهایی که وضعیت تکمیل‌شده دارند حذف می‌شوند. این کار قابل بازگشت نیست.") },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.deleteAllCompletedReminders()
                            showDeleteCompletedDialog = false
                        }) { Text("حذف همه", color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteCompletedDialog = false }) { Text("انصراف") }
                    }
                )
            }

            // Snooze Dialog
            val snoozeReminder = showSnoozeSheetForReminder
            if (snoozeReminder != null) {
                SnoozeReminderDialog(
                    reminderTitle = snoozeReminder.title,
                    onDismiss = { showSnoozeSheetForReminder = null },
                    onSnoozePreset = { option ->
                        viewModel.snoozeReminder(snoozeReminder.id, option, null)
                        showSnoozeSheetForReminder = null
                    },
                    onSnoozeCustom = { targetMillis ->
                        viewModel.snoozeReminder(snoozeReminder.id, SnoozeOption.CUSTOM, targetMillis)
                        showSnoozeSheetForReminder = null
                    }
                )
            }
        }
    }
}
