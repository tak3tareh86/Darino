package com.example.calendar.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.domain.CalendarDateUtils
import com.example.calendar.domain.model.FinancialEvent
import com.example.calendar.domain.model.FinancialEventType
import com.example.ui.components.Layered3DCard
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.WarningAmberLight

@Composable
fun CalendarView(
    year: Int,
    month: Int,
    selectedDate: String,
    events: List<FinancialEvent>,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val (todayYear, todayMonth, todayDay) = remember { CalendarDateUtils.getCurrentJalaliDate() }
    val isCurrentMonthToday = year == todayYear && month == todayMonth

    val daysInMonth = remember(year, month) { CalendarDateUtils.getDaysInJalaliMonth(year, month) }
    val firstWeekday = remember(year, month) { CalendarDateUtils.getFirstDayOfWeekForJalaliMonth(year, month) }

    // Map days in this month to their events
    val monthPrefix = "${year.toString().padStart(4, '0')}/${month.toString().padStart(2, '0')}"
    val eventsByDay = remember(events, year, month) {
        val map = mutableMapOf<Int, MutableList<FinancialEvent>>()
        events.forEach { ev ->
            val norm = CalendarDateUtils.normalizeDate(ev.date)
            if (norm.startsWith(monthPrefix)) {
                val day = norm.substringAfterLast('/').toIntOrNull()
                if (day != null) {
                    map.getOrPut(day) { mutableListOf() }.add(ev)
                }
            }
        }
        map
    }

    val weekdays = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")

    Layered3DCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("calendar_month_grid_card"),
        backgroundColor = if (isDark) Color(0xFF131B2B) else Color(0xFFFFFFFF),
        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
        elevation = 2.dp,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Weekday Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekdays.forEachIndexed { index, name ->
                    val isFriday = index == 6
                    Text(
                        text = name,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = if (isFriday) ExpenseRoseLight else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Calendar Grid: calculate weeks
            val totalCells = firstWeekday + daysInMonth
            val totalRows = (totalCells + 6) / 7

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                for (row in 0 until totalRows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (col in 0 until 7) {
                            val cellIndex = row * 7 + col
                            val dayNumber = cellIndex - firstWeekday + 1
                            val isValidDay = dayNumber in 1..daysInMonth

                            if (isValidDay) {
                                val currentDateStr = CalendarDateUtils.formatJalali(year, month, dayNumber)
                                val isSelected = CalendarDateUtils.normalizeDate(selectedDate) == currentDateStr
                                val isToday = isCurrentMonthToday && dayNumber == todayDay
                                val isFriday = col == 6
                                val dayEvents = eventsByDay[dayNumber] ?: emptyList()

                                CalendarDayCell(
                                    day = dayNumber,
                                    isSelected = isSelected,
                                    isToday = isToday,
                                    isFriday = isFriday,
                                    events = dayEvents,
                                    onClick = { onDateSelected(currentDateStr) },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                // Empty spacer cell
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarDayCell(
    day: Int,
    isSelected: Boolean,
    isToday: Boolean,
    isFriday: Boolean,
    events: List<FinancialEvent>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val interactionSource = remember { MutableInteractionSource() }

    val cellBg = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        else -> Color.Transparent
    }

    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.primary
        isFriday -> ExpenseRoseLight
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .height(38.dp)
            .padding(1.dp)
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (isSelected) {
                    Modifier.shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(10.dp),
                        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                } else if (isToday) {
                    Modifier.background(cellBg, RoundedCornerShape(10.dp))
                } else {
                    Modifier
                }
            )
            .background(cellBg)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag("day_cell_$day"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Day Number (in Persian Digits)
            Text(
                text = com.example.util.IranianPhoneUtils.convertDigitsToPersian(day.toString()),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.Medium,
                    fontSize = 12.sp
                ),
                color = textColor
            )

            // Event Dots Indicator
            if (events.isNotEmpty()) {
                Spacer(modifier = Modifier.height(1.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Show up to 3 distinct event indicator dots
                    val types = events.map { it.type }.distinct().take(3)
                    types.forEach { type ->
                        val dotColor = if (isSelected) {
                            Color.White
                        } else {
                            when (type) {
                                FinancialEventType.INSTALLMENT -> Color(0xFF2563EB)
                                FinancialEventType.REMINDER -> WarningAmberLight
                                FinancialEventType.VEHICLE -> InfoIndigoLight
                                FinancialEventType.EXPENSE -> EmeraldPrimaryLight
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(3.5.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }
            }
        }
    }
}
