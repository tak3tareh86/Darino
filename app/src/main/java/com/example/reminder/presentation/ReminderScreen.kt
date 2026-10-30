package com.example.reminder.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.reminder.data.ReminderEntity
import com.example.reminder.domain.ReminderType
import com.example.reminder.domain.SnoozeOption
import com.example.reminder.viewmodel.ReminderFilterChip
import com.example.reminder.viewmodel.ReminderTab
import com.example.reminder.viewmodel.ReminderViewModel
import com.example.ui.screens.reminder.NotificationCenterScreen
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

    var currentSubScreen by remember { mutableStateOf(ReminderSubScreen.LIST) }
    var selectedReminderForDetail by remember { mutableStateOf<ReminderEntity?>(null) }
    var showSnoozeSheetForReminder by remember { mutableStateOf<ReminderEntity?>(null) }

    when (currentSubScreen) {
        ReminderSubScreen.ADD -> {
            AddReminderScreen(
                onBackClick = { currentSubScreen = ReminderSubScreen.LIST },
                onSaveReminder = { reminder, offsets, repeatType, repeatInterval ->
                    viewModel.createReminder(reminder, offsets, repeatType, repeatInterval)
                    currentSubScreen = ReminderSubScreen.LIST
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
                    onToggleCompleted = {
                        if (rem.status == "COMPLETED") {
                            viewModel.toggleReminderEnabled(rem.id, true)
                        } else {
                            viewModel.completeReminder(rem.id)
                        }
                    },
                    onSnooze = { option, customMillis ->
                        viewModel.snoozeReminder(rem.id, option, customMillis)
                    },
                    onDelete = {
                        viewModel.deleteReminder(rem.id)
                        selectedReminderForDetail = null
                        currentSubScreen = ReminderSubScreen.LIST
                    },
                    onUpdateDateTime = { newDate, newTime ->
                        viewModel.updateReminder(rem.copy(date = newDate, time = newTime))
                    }
                )
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
                    Surface(
                        modifier = Modifier.fillMaxWidth().shadow(4.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "یادآورها و سررسیدها",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                )
                                Text(
                                    text = "مدیریت سررسید اقساط، بیمه و سرویس خودرو",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 9.sp
                                    )
                                )
                            }

                            IconButton(
                                onClick = { currentSubScreen = ReminderSubScreen.NOTIFICATION_CENTER },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                    .testTag("btn_notification_center")
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
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
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

                    // Summary Stats Card (امروز / این هفته / نزدیک‌ترین سررسید)
                    item {
                        Layered3DCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Text(
                                        text = "سررسید امروز",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                    Text(
                                        text = "${uiState.todayCount}",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = WarningAmberLight
                                        )
                                    )
                                }

                                VerticalDivider(
                                    modifier = Modifier.height(42.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )

                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Text(
                                        text = "این هفته",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                    Text(
                                        text = "${uiState.thisWeekCount}",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = InfoIndigoLight
                                        )
                                    )
                                }

                                VerticalDivider(
                                    modifier = Modifier.height(42.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )

                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Text(
                                        text = "نزدیک‌ترین",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                    Text(
                                        text = uiState.nearestReminder?.title ?: "بدون سررسید",
                                        modifier = Modifier.fillMaxWidth(),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldPrimaryLight
                                        ),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        softWrap = true
                                    )
                                    uiState.nearestReminder?.date?.let { date ->
                                        Text(
                                            text = date,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
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
                                    onClick = {
                                        selectedReminderForDetail = reminder
                                        currentSubScreen = ReminderSubScreen.DETAIL
                                    },
                                    onToggleCompleted = {
                                        viewModel.completeReminder(reminder.id)
                                    },
                                    onSnoozeClick = {
                                        showSnoozeSheetForReminder = reminder
                                    }
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
                                            onClick = {
                                                selectedReminderForDetail = reminder
                                                currentSubScreen = ReminderSubScreen.DETAIL
                                            },
                                            onToggleCompleted = {
                                                viewModel.toggleReminderEnabled(reminder.id, true)
                                            },
                                            onSnoozeClick = {
                                                showSnoozeSheetForReminder = reminder
                                            }
                                        )
                                    }
                                }
                            } else {
                                // If viewing only completed reminders (e.g. Completed Tab)
                                items(completedReminders, key = { it.id }) { reminder ->
                                    ReminderCard(
                                        reminder = reminder,
                                        onClick = {
                                            selectedReminderForDetail = reminder
                                            currentSubScreen = ReminderSubScreen.DETAIL
                                        },
                                        onToggleCompleted = {
                                            viewModel.toggleReminderEnabled(reminder.id, true)
                                        },
                                        onSnoozeClick = {
                                            showSnoozeSheetForReminder = reminder
                                        }
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
