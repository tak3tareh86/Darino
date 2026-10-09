package com.example.ui.screens.reminder.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*
import com.example.util.IranianPhoneUtils
import com.example.util.PersianCalendarHelper

/**
 * Composite Persian Date & Time Picker Section with Presets, Visual Cards,
 * and Interactive Dialogs for choosing exact Date & Time.
 */
@Composable
fun ReminderDateTimeSection(
    year: Int,
    month: Int,
    day: Int,
    hour: Int,
    minute: Int,
    onDateTimeChanged: (year: Int, month: Int, day: Int, hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }

    val currentMillis = System.currentTimeMillis()
    val scheduledMillis = remember(year, month, day, hour, minute) {
        PersianCalendarHelper.jalaliToEpochMillis(year, month, day, hour, minute)
    }
    val isPast = scheduledMillis <= currentMillis
    val relativeText = remember(scheduledMillis) {
        PersianCalendarHelper.formatRelativeTimePersian(scheduledMillis)
    }

    val pdt = remember(year, month, day, hour, minute) {
        PersianCalendarHelper.PersianDateTime(year, month, day, hour, minute)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Rounded.EventAvailable,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "تاریخ و ساعت یادآوری",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "تقویم خورشیدی و زمان دقیق",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Quick Date Presets Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val presets = listOf(
                "امروز" to 0L,
                "فردا" to 1L,
                "۳ روز بعد" to 3L,
                "۱ هفته بعد" to 7L,
                "۱ ماه بعد" to 30L
            )

            presets.forEach { (label, daysAhead) ->
                val targetMillis = currentMillis + (daysAhead * 24 * 3600 * 1000L)
                val targetPdt = PersianCalendarHelper.fromEpochMillis(targetMillis)
                val isSelected = (year == targetPdt.year && month == targetPdt.month && day == targetPdt.day)

                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val defaultHour = if (daysAhead == 0L) {
                            val nowPdt = PersianCalendarHelper.fromEpochMillis(currentMillis)
                            (nowPdt.hour + 2).coerceAtMost(23)
                        } else {
                            9
                        }
                        onDateTimeChanged(targetPdt.year, targetPdt.month, targetPdt.day, defaultHour, 0)
                    },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Interactive Date and Time Clickable Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Date Card
            Surface(
                onClick = { showDatePickerDialog = true },
                shape = RoundedCornerShape(RadiusMD),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                modifier = Modifier
                    .weight(1.1f)
                    .height(60.dp)
                    .testTag("reminder_date_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = EmeraldPrimaryLight.copy(alpha = 0.15f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.CalendarToday,
                            contentDescription = "انتخاب تاریخ",
                            tint = EmeraldPrimaryLight,
                            modifier = Modifier.padding(6.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "تاریخ سررسید",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${IranianPhoneUtils.convertDigitsToPersian(day.toString())} ${pdt.monthName} ${IranianPhoneUtils.convertDigitsToPersian(year.toString())}",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }

                    Icon(
                        Icons.Rounded.EditCalendar,
                        contentDescription = "تغییر تاریخ",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 2. Time Card
            Surface(
                onClick = { showTimePickerDialog = true },
                shape = RoundedCornerShape(RadiusMD),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                modifier = Modifier
                    .weight(0.9f)
                    .height(60.dp)
                    .testTag("reminder_time_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = InfoIndigoLight.copy(alpha = 0.15f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Schedule,
                            contentDescription = "انتخاب ساعت",
                            tint = InfoIndigoLight,
                            modifier = Modifier.padding(6.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "ساعت هشدار",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = pdt.toFormattedTime(),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Icon(
                        Icons.Rounded.AccessTime,
                        contentDescription = "تغییر ساعت",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Live Scheduled Moment Summary / Past Warning Banner
        Surface(
            shape = RoundedCornerShape(RadiusMD),
            color = if (isPast) ExpenseRoseLight.copy(alpha = 0.12f) else EmeraldPrimaryLight.copy(alpha = 0.10f),
            border = BorderStroke(
                1.dp,
                if (isPast) ExpenseRoseLight.copy(alpha = 0.35f) else EmeraldPrimaryLight.copy(alpha = 0.25f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
                        "زمان انتخابی در گذشته است! لطفاً تاریخ و ساعت آینده را تعیین کنید."
                    } else {
                        "هشدار در تاریخ ${pdt.toFormattedDate()} ساعت ${pdt.toFormattedTime()} ($relativeText) ارسال می‌شود."
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        fontWeight = if (isPast) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isPast) ExpenseRoseLight else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    // Dialog for Persian Date Selection
    if (showDatePickerDialog) {
        PersianDatePickerDialog(
            initialYear = year,
            initialMonth = month,
            initialDay = day,
            onDismiss = { showDatePickerDialog = false },
            onConfirm = { newY, newM, newD ->
                onDateTimeChanged(newY, newM, newD, hour, minute)
                showDatePickerDialog = false
            }
        )
    }

    // Dialog for Time Selection
    if (showTimePickerDialog) {
        PersianTimePickerDialog(
            initialHour = hour,
            initialMinute = minute,
            onDismiss = { showTimePickerDialog = false },
            onConfirm = { newH, newMin ->
                onDateTimeChanged(year, month, day, newH, newMin)
                showTimePickerDialog = false
            }
        )
    }
}

/**
 * Dedicated Persian Solar Calendar Picker Dialog.
 */
@Composable
fun PersianDatePickerDialog(
    initialYear: Int,
    initialMonth: Int,
    initialDay: Int,
    onDismiss: () -> Unit,
    onConfirm: (year: Int, month: Int, day: Int) -> Unit
) {
    com.example.ui.components.SharedPersianDatePickerDialog(
        initialYear = initialYear,
        initialMonth = initialMonth,
        initialDay = initialDay,
        title = "انتخاب تاریخ (شمسی)",
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

/**
 * Dedicated Time Picker Dialog for Hour (0-23) & Minute (0-59) with Quick Presets.
 */
@Composable
fun PersianTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    var hour by remember { mutableIntStateOf(initialHour.coerceIn(0, 23)) }
    var minute by remember { mutableIntStateOf(initialMinute.coerceIn(0, 59)) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(RadiusLG),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .testTag("persian_time_picker_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "تنظیم ساعت یادآوری (۲۴ ساعته)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "بستن")
                    }
                }

                // Time Display Box
                Surface(
                    shape = RoundedCornerShape(RadiusMD),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth().height(80.dp)
                ) {
                    val hStr = IranianPhoneUtils.convertDigitsToPersian(hour.toString().padStart(2, '0'))
                    val mStr = IranianPhoneUtils.convertDigitsToPersian(minute.toString().padStart(2, '0'))

                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$hStr:$mStr",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 34.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Quick Hour Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        "۰۸:۰۰" to 8,
                        "۰۹:۰۰" to 9,
                        "۱۲:۰۰" to 12,
                        "۱۵:۰۰" to 15,
                        "۱۸:۰۰" to 18,
                        "۲۱:۰۰" to 21
                    ).forEach { (label, hVal) ->
                        val isSelected = (hour == hVal && minute == 0)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                hour = hVal
                                minute = 0
                            },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Hour Adjuster (+1/-1) with 24-hour wrap
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ساعت (۰۰ تا ۲۳):",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = { hour = (hour - 1 + 24) % 24 },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Rounded.Remove, contentDescription = "-1 ساعت")
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.width(46.dp).height(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = IranianPhoneUtils.convertDigitsToPersian(hour.toString().padStart(2, '0')),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        FilledTonalIconButton(
                            onClick = { hour = (hour + 1) % 24 },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "+1 ساعت")
                        }
                    }
                }

                // Minute Adjuster (+1/-1 and +5/-5) with 60-minute wrap
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "دقیقه (۰۰ تا ۵۹):",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = { minute = (minute - 5 + 60) % 60 },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("-۵", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        FilledTonalIconButton(
                            onClick = { minute = (minute - 1 + 60) % 60 },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Rounded.Remove, contentDescription = "-1 دقیقه", modifier = Modifier.size(16.dp))
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.width(44.dp).height(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = IranianPhoneUtils.convertDigitsToPersian(minute.toString().padStart(2, '0')),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        FilledTonalIconButton(
                            onClick = { minute = (minute + 1) % 60 },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "+1 دقیقه", modifier = Modifier.size(16.dp))
                        }

                        FilledTonalIconButton(
                            onClick = { minute = (minute + 5) % 60 },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("+۵", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Quick Minute Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        ":۰۰" to 0,
                        ":۱۵" to 15,
                        ":۳۰" to 30,
                        ":۴۵" to 45
                    ).forEach { (label, minVal) ->
                        val isSelected = (minute == minVal)
                        FilterChip(
                            selected = isSelected,
                            onClick = { minute = minVal },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("انصراف")
                    }

                    Button(
                        onClick = { onConfirm(hour, minute) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("تأیید ساعت")
                    }
                }
            }
        }
    }
}

/**
 * Interactive Standard Persian & 24-hour Time Input Field.
 * Replaces manual text entry with an interactive picker dialog.
 * Preserves HH:mm standard format.
 */
@Composable
fun PersianTimeInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "ساعت هشدار",
    enabled: Boolean = true,
    testTag: String = "input_reminder_time"
) {
    var showTimePickerDialog by remember { mutableStateOf(false) }

    val (currentHour, currentMinute) = remember(value) {
        try {
            val clean = IranianPhoneUtils.convertDigitsToEnglish(value).trim()
            val parts = clean.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9
            val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
            Pair(h, m)
        } catch (e: Exception) {
            Pair(9, 0)
        }
    }

    val displayTime = remember(value) {
        val clean = IranianPhoneUtils.convertDigitsToEnglish(value).trim()
        val parts = clean.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9
        val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        val formatted = "%02d:%02d".format(h, m)
        IranianPhoneUtils.convertDigitsToPersian(formatted)
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = displayTime,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.AccessTime,
                    contentDescription = "انتخاب ساعت",
                    tint = WarningAmberLight
                )
            },
            trailingIcon = {
                IconButton(
                    onClick = { if (enabled) showTimePickerDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = "تنظیم ساعت",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            shape = RoundedCornerShape(RadiusMD),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )

        // Transparent overlay to ensure entire touch area triggers the dialog without opening keyboard
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(RadiusMD))
                .clickable(enabled = enabled) { showTimePickerDialog = true }
        )
    }

    if (showTimePickerDialog) {
        PersianTimePickerDialog(
            initialHour = currentHour,
            initialMinute = currentMinute,
            onDismiss = { showTimePickerDialog = false },
            onConfirm = { newH, newMin ->
                val formatted = "%02d:%02d".format(newH, newMin)
                onValueChange(formatted)
                showTimePickerDialog = false
            }
        )
    }
}
