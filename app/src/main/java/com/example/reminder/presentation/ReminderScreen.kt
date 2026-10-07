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
                    onSnooze = { option ->
                        viewModel.snoozeReminder(rem.id, option)
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
            var isSuggestionsExpanded by remember { mutableStateOf(false) }
            var isCompletedExpanded by remember { mutableStateOf(false) }

            Scaffold(
                modifier = modifier.fillMaxSize(),
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
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${uiState.todayCount}",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = WarningAmberLight
                                        )
                                    )
                                    Text(
                                        text = "سررسید امروز",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }

                                VerticalDivider(
                                    modifier = Modifier.height(32.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${uiState.thisWeekCount}",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = InfoIndigoLight
                                        )
                                    )
                                    Text(
                                        text = "این هفته",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }

                                VerticalDivider(
                                    modifier = Modifier.height(32.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.widthIn(max = 120.dp)
                                ) {
                                    Text(
                                        text = uiState.nearestReminder?.title ?: "بدون سررسید",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldPrimaryLight
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = uiState.nearestReminder?.date ?: "نزدیک‌ترین",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Smart Suggestions Section (Collapsible)
                    if (uiState.smartSuggestions.isNotEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { isSuggestionsExpanded = !isSuggestionsExpanded }
                                            .padding(horizontal = 6.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.AutoAwesome,
                                                contentDescription = null,
                                                tint = WarningAmberLight,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = "پیشنهادات هوشمند دارینو",
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "${IranianPhoneUtils.convertDigitsToPersian(uiState.smartSuggestions.size.toString())} پیشنهاد خودکار برای تنظیم یادآور",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Icon(
                                            imageVector = if (isSuggestionsExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = WarningAmberLight,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    AnimatedVisibility(
                                        visible = isSuggestionsExpanded,
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            uiState.smartSuggestions.forEach { suggestion ->
                                                Surface(
                                                    shape = RoundedCornerShape(RadiusMD),
                                                    color = MaterialTheme.colorScheme.surface,
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        1.dp,
                                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                                    ),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(12.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        Soft3DIcon(
                                                            imageRes = suggestion.type.iconRes,
                                                            contentDescription = suggestion.title,
                                                            size = 36.dp,
                                                            accentColor = suggestion.type.accentColor
                                                        )
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = suggestion.title,
                                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            )
                                                            Text(
                                                                text = suggestion.message,
                                                                style = MaterialTheme.typography.bodySmall.copy(
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                    fontSize = 11.sp
                                                                )
                                                            )
                                                        }

                                                        Button(
                                                            onClick = { viewModel.acceptSuggestion(suggestion) },
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                            shape = RoundedCornerShape(RadiusSM),
                                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimaryLight)
                                                        ) {
                                                            Text("فعال‌سازی", fontSize = 11.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Main Tabs (همه / امروز / مالی / اقساط / خودرو / شخصی / انجام‌شده)
                    item {
                        ScrollableTabRow(
                            selectedTabIndex = uiState.selectedTab.ordinal,
                            edgePadding = 0.dp,
                            containerColor = Color.Transparent,
                            divider = {}
                        ) {
                            ReminderTab.entries.forEach { tab ->
                                val isSelected = uiState.selectedTab == tab
                                Tab(
                                    selected = isSelected,
                                    onClick = { viewModel.selectTab(tab) },
                                    text = {
                                        Text(
                                            text = tab.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) EmeraldPrimaryLight else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // Filter Chips (همه، شخصی، اقساط، خودرو، بیمه، سرویس، سوخت)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ReminderFilterChip.entries.forEach { chip ->
                                val isSelected = uiState.selectedFilterChip == chip
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.selectFilterChip(chip) },
                                    label = { Text(chip.title, fontSize = 12.sp) }
                                )
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

            // Snooze Bottom Sheet Dialog
            val snoozeReminder = showSnoozeSheetForReminder
            if (snoozeReminder != null) {
                AlertDialog(
                    onDismissRequest = { showSnoozeSheetForReminder = null },
                    icon = {
                        Icon(Icons.Rounded.Snooze, contentDescription = null, tint = WarningAmberLight)
                    },
                    title = {
                        Text("به تعویق انداختن یادآور", fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "زمان تعویق برای «${snoozeReminder.title}» را انتخاب کنید:",
                                style = MaterialTheme.typography.bodySmall
                            )
                            listOf(
                                SnoozeOption.MINUTES_15,
                                SnoozeOption.HOUR_1,
                                SnoozeOption.TOMORROW,
                                SnoozeOption.THREE_DAYS
                            ).forEach { option ->
                                Surface(
                                    shape = RoundedCornerShape(RadiusSM),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.snoozeReminder(snoozeReminder.id, option)
                                            showSnoozeSheetForReminder = null
                                        }
                                ) {
                                    Text(
                                        text = option.title,
                                        modifier = Modifier.padding(12.dp),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showSnoozeSheetForReminder = null }) {
                            Text("انصراف")
                        }
                    }
                )
            }
        }
    }
}
