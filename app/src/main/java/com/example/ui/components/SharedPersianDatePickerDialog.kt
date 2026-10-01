package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowDropUp
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
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
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.util.IranianPhoneUtils
import com.example.util.PersianCalendarHelper
import java.util.Calendar

/**
 * Modern, Compact, European & Material 3 Standard Jalali (Persian Solar) Date Picker Dialog.
 * Features:
 * - Ultra-compact vertical footprint (~50% height reduction vs old version)
 * - Header Month/Year navigation (< مهر ۱۴۰۴ >) with quick year/month selector dropdown
 * - Standard 7-column Weekday Header (شنبه تا جمعه)
 * - Accurate Jalali Calendar Grid day placement
 * - High visual polish with Material 3 styling
 */
@Composable
fun SharedPersianDatePickerDialog(
    initialYear: Int,
    initialMonth: Int,
    initialDay: Int,
    title: String = "انتخاب تاریخ (شمسی)",
    onDismiss: () -> Unit,
    onConfirm: (year: Int, month: Int, day: Int) -> Unit
) {
    var currentYear by remember(initialYear) { mutableStateOf(initialYear) }
    var currentMonth by remember(initialMonth) { mutableStateOf(initialMonth.coerceIn(1, 12)) }
    var currentDay by remember(initialDay) { mutableStateOf(initialDay.coerceIn(1, 31)) }
    var showYearMonthPicker by remember { mutableStateOf(false) }

    val daysInMonth = remember(currentYear, currentMonth) {
        PersianCalendarHelper.getDaysInMonth(currentYear, currentMonth)
    }

    LaunchedEffect(daysInMonth) {
        if (currentDay > daysInMonth) {
            currentDay = daysInMonth
        }
    }

    // Calculate weekday offset for the 1st day of the current Jalali month
    val firstDayOffset = remember(currentYear, currentMonth) {
        val (gy, gm, gd) = PersianCalendarHelper.jalaliToGregorian(currentYear, currentMonth, 1)
        val cal = Calendar.getInstance().apply {
            set(gy, gm - 1, gd)
        }
        when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 0
        }
    }

    val monthName = PersianCalendarHelper.PERSIAN_MONTH_NAMES.getOrElse(currentMonth - 1) { "" }
    val yearStr = IranianPhoneUtils.convertDigitsToPersian(currentYear.toString())
    val dayStr = IranianPhoneUtils.convertDigitsToPersian(currentDay.toString())

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(RadiusLG),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .testTag("persian_calendar_picker_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "بستن",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Selected Date Summary Banner
                Surface(
                    shape = RoundedCornerShape(RadiusMD),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "تاریخ انتخابی:",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$dayStr $monthName $yearStr",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                // Month/Year Navigation Control Bar
                Surface(
                    shape = RoundedCornerShape(RadiusMD),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Previous Month Button (In RTL, ChevronRight moves back in time)
                        IconButton(
                            onClick = {
                                if (currentMonth == 1) {
                                    currentMonth = 12
                                    currentYear--
                                } else {
                                    currentMonth--
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = "ماه قبل",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Month & Year Selector Button (Toggle Quick Picker)
                        Surface(
                            shape = RoundedCornerShape(RadiusMD),
                            color = if (showYearMonthPicker) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(RadiusMD))
                                .clickable { showYearMonthPicker = !showYearMonthPicker }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "$monthName $yearStr",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Icon(
                                    imageVector = if (showYearMonthPicker) Icons.Rounded.ArrowDropUp else Icons.Rounded.ArrowDropDown,
                                    contentDescription = "انتخاب سریع ماه و سال",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Next Month Button (In RTL, ChevronLeft moves forward in time)
                        IconButton(
                            onClick = {
                                if (currentMonth == 12) {
                                    currentMonth = 1
                                    currentYear++
                                } else {
                                    currentMonth++
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ChevronLeft,
                                contentDescription = "ماه بعد",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (showYearMonthPicker) {
                    // Quick Year & Month Picker Mode
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Year Stepper
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            IconButton(
                                onClick = { currentYear-- },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Rounded.ChevronRight, contentDescription = "سال قبل")
                            }

                            Text(
                                text = "سال $yearStr",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            IconButton(
                                onClick = { currentYear++ },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Rounded.ChevronLeft, contentDescription = "سال بعد")
                            }
                        }

                        // Month Pills Grid (3 columns x 4 rows)
                        val months = PersianCalendarHelper.PERSIAN_MONTH_NAMES
                        for (row in 0..3) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (col in 0..2) {
                                    val mIdx = row * 3 + col
                                    val mNum = mIdx + 1
                                    val isSelected = currentMonth == mNum
                                    val mName = months[mIdx]

                                    Surface(
                                        onClick = {
                                            currentMonth = mNum
                                            showYearMonthPicker = false
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Text(
                                                text = mName,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 11.5.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        TextButton(
                            onClick = { showYearMonthPicker = false },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("تأیید و بازگشت به تقویم", fontSize = 12.sp)
                        }
                    }
                } else {
                    // Standard Modern Calendar View Mode
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Weekday Headers (Saturday to Friday)
                        val weekdays = listOf("ش", "۱ش", "۲ش", "۳ش", "۴ش", "۵ش", "ج")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            weekdays.forEach { dayName ->
                                Text(
                                    text = dayName,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )

                        // Days Grid Matrix
                        val totalCells = firstDayOffset + daysInMonth
                        val numRows = (totalCells + 6) / 7

                        for (r in 0 until numRows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                for (c in 0 until 7) {
                                    val cellIndex = r * 7 + c
                                    if (cellIndex < firstDayOffset || cellIndex >= totalCells) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    } else {
                                        val dayNum = cellIndex - firstDayOffset + 1
                                        val isSelected = currentDay == dayNum

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(vertical = 2.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Surface(
                                                onClick = { currentDay = dayNum },
                                                shape = CircleShape,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                border = if (!isSelected) BorderStroke(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                                                ) else null,
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    Text(
                                                        text = IranianPhoneUtils.convertDigitsToPersian(dayNum.toString()),
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                            fontSize = 12.sp
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Action Buttons Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(RadiusMD)
                    ) {
                        Text("انصراف")
                    }

                    Button(
                        onClick = {
                            onConfirm(currentYear, currentMonth, currentDay)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(RadiusMD)
                    ) {
                        Text("تأیید تاریخ")
                    }
                }
            }
        }
    }
}
