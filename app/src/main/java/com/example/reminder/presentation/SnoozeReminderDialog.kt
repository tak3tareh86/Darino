package com.example.reminder.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.reminder.domain.SnoozeOption
import com.example.ui.screens.reminder.components.PersianDatePickerDialog
import com.example.ui.screens.reminder.components.PersianTimePickerDialog
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusSM
import com.example.ui.theme.WarningAmberLight
import com.example.util.IranianPhoneUtils
import com.example.util.PersianCalendarHelper

@Composable
fun SnoozeReminderDialog(
    reminderTitle: String,
    onDismiss: () -> Unit,
    onSnoozePreset: (SnoozeOption) -> Unit,
    onSnoozeCustom: (targetEpochMillis: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isCustomMode by remember { mutableStateOf(false) }

    // Custom Mode Date & Time states
    val now = remember { System.currentTimeMillis() }
    val defaultFuturePdt = remember {
        // Default to either 2 hours ahead or tomorrow morning at 09:00
        val currentPdt = PersianCalendarHelper.fromEpochMillis(now)
        if (currentPdt.hour >= 20) {
            val tomorrowMillis = now + (24 * 60 * 60 * 1000L)
            val tomorrowPdt = PersianCalendarHelper.fromEpochMillis(tomorrowMillis)
            PersianCalendarHelper.PersianDateTime(tomorrowPdt.year, tomorrowPdt.month, tomorrowPdt.day, 9, 0)
        } else {
            val aheadHour = (currentPdt.hour + 2).coerceAtMost(23)
            PersianCalendarHelper.PersianDateTime(currentPdt.year, currentPdt.month, currentPdt.day, aheadHour, 0)
        }
    }

    var customYear by remember { mutableIntStateOf(defaultFuturePdt.year) }
    var customMonth by remember { mutableIntStateOf(defaultFuturePdt.month) }
    var customDay by remember { mutableIntStateOf(defaultFuturePdt.day) }
    var customHour by remember { mutableIntStateOf(defaultFuturePdt.hour) }
    var customMinute by remember { mutableIntStateOf(defaultFuturePdt.minute) }

    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }

    val targetMillis = remember(customYear, customMonth, customDay, customHour, customMinute) {
        PersianCalendarHelper.jalaliToEpochMillis(customYear, customMonth, customDay, customHour, customMinute)
    }
    val currentMillis = System.currentTimeMillis()
    val isPast = targetMillis <= currentMillis
    val relativeText = remember(targetMillis) {
        PersianCalendarHelper.formatRelativeTimePersian(targetMillis)
    }
    val targetPdt = remember(customYear, customMonth, customDay, customHour, customMinute) {
        PersianCalendarHelper.PersianDateTime(customYear, customMonth, customDay, customHour, customMinute)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(RadiusLG),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
                .testTag("snooze_reminder_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (!isCustomMode) {
                    // PRESET SNOOZE OPTIONS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = WarningAmberLight.copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Snooze,
                                    contentDescription = null,
                                    tint = WarningAmberLight,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "تعویق زمان یادآوری",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = reminderTitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = "بستن")
                        }
                    }

                    Text(
                        text = "یادآوری را به چه زمانی به تعویق می‌اندازید؟",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Presets List
                    val presetList = listOf(
                        SnoozeOption.MINUTES_15 to Icons.Rounded.Schedule,
                        SnoozeOption.HOUR_1 to Icons.Rounded.HourglassBottom,
                        SnoozeOption.TOMORROW to Icons.Rounded.Today,
                        SnoozeOption.THREE_DAYS to Icons.Rounded.CalendarMonth
                    )

                    presetList.forEach { (option, icon) ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(RadiusMD))
                                .clickable { onSnoozePreset(option) }
                                .testTag("snooze_preset_${option.name}"),
                            shape = RoundedCornerShape(RadiusMD),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = WarningAmberLight,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = option.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Custom Time Button Option
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(RadiusMD))
                            .clickable { isCustomMode = true }
                            .testTag("snooze_option_custom"),
                        shape = RoundedCornerShape(RadiusMD),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.EditCalendar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "زمان دلخواه...",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Text(
                                    text = "انتخاب تاریخ و ساعت دقیق سررسید آینده",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.ChevronLeft,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().testTag("btn_snooze_dismiss"),
                        shape = RoundedCornerShape(RadiusMD)
                    ) {
                        Text("انصراف")
                    }
                } else {
                    // CUSTOM DATE & TIME PICKER MODE
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(onClick = { isCustomMode = false }, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "بازگشت به گزینه‌ها"
                                )
                            }
                            Text(
                                text = "تعیین زمان دلخواه تعویق",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = "بستن")
                        }
                    }

                    Text(
                        text = "تاریخ و ساعت آینده را برای ارسال مجدد هشدار تعیین کنید:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Interactive Date & Time Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Date Card
                        Surface(
                            onClick = { showDatePickerDialog = true },
                            shape = RoundedCornerShape(RadiusMD),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .weight(1.1f)
                                .height(64.dp)
                                .testTag("btn_custom_snooze_date")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.CalendarToday,
                                    contentDescription = "انتخاب تاریخ",
                                    tint = EmeraldPrimaryLight,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "تاریخ سررسید",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${IranianPhoneUtils.convertDigitsToPersian(customDay.toString())} ${targetPdt.monthName}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // Time Card
                        Surface(
                            onClick = { showTimePickerDialog = true },
                            shape = RoundedCornerShape(RadiusMD),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .weight(0.9f)
                                .height(64.dp)
                                .testTag("btn_custom_snooze_time")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Schedule,
                                    contentDescription = "انتخاب ساعت",
                                    tint = InfoIndigoLight,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "ساعت هشدار",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = targetPdt.toFormattedTime(),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Scheduled Moment / Past Warning Banner
                    Surface(
                        shape = RoundedCornerShape(RadiusMD),
                        color = if (isPast) ExpenseRoseLight.copy(alpha = 0.12f) else EmeraldPrimaryLight.copy(alpha = 0.10f),
                        border = BorderStroke(
                            1.dp,
                            if (isPast) ExpenseRoseLight.copy(alpha = 0.4f) else EmeraldPrimaryLight.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isPast) Icons.Rounded.Warning else Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                tint = if (isPast) ExpenseRoseLight else EmeraldPrimaryLight,
                                modifier = Modifier.size(18.dp)
                            )

                            Text(
                                text = if (isPast) {
                                    "زمان انتخابی در گذشته است! لطفاً تاریخ و ساعت آینده را انتخاب کنید."
                                } else {
                                    "هشدار در تاریخ ${targetPdt.toFormattedDate()} ساعت ${targetPdt.toFormattedTime()} ($relativeText) ارسال می‌شود."
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isPast) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isPast) ExpenseRoseLight else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isCustomMode = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("بازگشت")
                        }

                        Button(
                            onClick = {
                                if (!isPast) {
                                    onSnoozeCustom(targetMillis)
                                }
                            },
                            enabled = !isPast,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_confirm_custom_snooze"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimaryLight)
                        ) {
                            Text("تأیید تعویق")
                        }
                    }
                }
            }
        }
    }

    if (showDatePickerDialog) {
        PersianDatePickerDialog(
            initialYear = customYear,
            initialMonth = customMonth,
            initialDay = customDay,
            onDismiss = { showDatePickerDialog = false },
            onConfirm = { newY, newM, newD ->
                customYear = newY
                customMonth = newM
                customDay = newD
                showDatePickerDialog = false
            }
        )
    }

    if (showTimePickerDialog) {
        PersianTimePickerDialog(
            initialHour = customHour,
            initialMinute = customMinute,
            onDismiss = { showTimePickerDialog = false },
            onConfirm = { newH, newMin ->
                customHour = newH
                customMinute = newMin
                showTimePickerDialog = false
            }
        )
    }
}
