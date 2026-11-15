package com.example.vehicle.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.util.IranianPhoneUtils
import com.example.vehicle.data.VehicleTimelineEvent
import com.example.vehicle.domain.VehicleAnalyzer
import com.example.util.MoneyFormatter
import com.example.util.MoneyAmount
import com.example.util.PersianCalendarHelper

/**
 * Screen 11: Vehicle History Timeline (تایم‌لاین تاریخچه و رویدادهای خودرو)
 */
@Composable
fun VehicleTimelineScreen(
    vehicleId: String,
    events: List<VehicleTimelineEvent>,
    modifier: Modifier = Modifier
) {
    val today = remember(vehicleId) { PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()) }
    var filterModeName by rememberSaveable(vehicleId) { mutableStateOf(TimelineFilterMode.ALL.name) }
    var selectedYear by rememberSaveable(vehicleId) { mutableStateOf(today.year) }
    var selectedMonth by rememberSaveable(vehicleId) { mutableStateOf(today.month) }
    val filterMode = TimelineFilterMode.values().firstOrNull { it.name == filterModeName }
        ?: TimelineFilterMode.ALL

    val validEventDates = remember(events) {
        events.mapNotNull { VehicleAnalyzer.parseAndValidateJalaliDate(it.date) }
    }
    val availableYears = remember(validEventDates, today.year) {
        val eventYears = validEventDates.mapNotNull { it.substringBefore('/').toIntOrNull() }
        val firstYear = minOf(today.year, eventYears.minOrNull() ?: today.year)
        val lastYear = maxOf(today.year, eventYears.maxOrNull() ?: today.year)
        (firstYear..lastYear).toList().reversed()
    }
    val filteredEvents = remember(events, filterMode, selectedYear, selectedMonth) {
        filterVehicleTimelineEvents(events, filterMode, selectedYear, selectedMonth)
    }
    val invalidDateCount = remember(events) {
        events.count { VehicleAnalyzer.parseAndValidateJalaliDate(it.date) == null }
    }

    Column(
        modifier = modifier.fillMaxSize().testTag("vehicle_timeline_screen")
    ) {
        if (events.isNotEmpty()) {
            TimelineFilterControls(
                mode = filterMode,
                onModeChange = { filterModeName = it.name },
                selectedYear = selectedYear,
                selectedMonth = selectedMonth,
                availableYears = availableYears,
                invalidDateCount = invalidDateCount,
                onYearChange = { selectedYear = it },
                onMonthChange = { selectedMonth = it },
                onMoveMonth = { delta ->
                    var year = selectedYear
                    var month = selectedMonth + delta
                    while (month > 12) { month -= 12; year += 1 }
                    while (month < 1) { month += 12; year -= 1 }
                    selectedYear = year
                    selectedMonth = month
                },
            )
        }

        when {
            events.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Soft3DIcon(
                            imageRes = R.drawable.img_3d_car,
                            contentDescription = "بدون رویداد",
                            size = 56.dp,
                            accentColor = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "هنوز رویدادی برای این خودرو ثبت نشده است",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "با ثبت سرویس، هزینه، بیمه یا معاینه فنی، تایم‌لاین تشکیل خواهد شد.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            filteredEvents.isEmpty() -> {
                Box(Modifier.fillMaxWidth().weight(1f).padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Soft3DIcon(
                            imageRes = R.drawable.img_3d_car,
                            contentDescription = "بدون سابقه در بازه انتخابی",
                            size = 48.dp,
                            accentColor = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "در بازه انتخاب‌شده سابقه‌ای ثبت نشده است",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { filterModeName = TimelineFilterMode.ALL.name }) {
                            Text("نمایش همه سوابق")
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    val timelineItems = buildTimelineDisplayItems(filteredEvents)
                    val maxServiceRows = timelineItems.maxOfOrNull { item ->
                        if (item.serviceTitles.isEmpty()) 0 else (item.serviceTitles.size + 1) / 2
                    } ?: 0
                    val cardHeight = (120 + maxServiceRows * 24).coerceAtLeast(148).dp
                    itemsIndexed(timelineItems, key = { _, item -> item.primary.id }) { index, item ->
                        TimelineItemRow(
                            item = item,
                            isLast = index == timelineItems.lastIndex,
                            cardHeight = cardHeight
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineFilterControls(
    mode: TimelineFilterMode,
    onModeChange: (TimelineFilterMode) -> Unit,
    selectedYear: Int,
    selectedMonth: Int,
    availableYears: List<Int>,
    invalidDateCount: Int,
    onYearChange: (Int) -> Unit,
    onMonthChange: (Int) -> Unit,
    onMoveMonth: (Int) -> Unit
) {
    var yearMenuExpanded by remember { mutableStateOf(false) }
    var monthMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = mode == TimelineFilterMode.ALL,
                    onClick = { onModeChange(TimelineFilterMode.ALL) },
                    label = { Text("همه", maxLines = 1) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = mode == TimelineFilterMode.MONTH,
                    onClick = { onModeChange(TimelineFilterMode.MONTH) },
                    label = { Text("ماهانه", maxLines = 1) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = mode == TimelineFilterMode.YEAR,
                    onClick = { onModeChange(TimelineFilterMode.YEAR) },
                    label = { Text("سالانه", maxLines = 1) },
                    modifier = Modifier.weight(1f)
                )
            }

            if (mode == TimelineFilterMode.MONTH || mode == TimelineFilterMode.YEAR) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { yearMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "سال " + IranianPhoneUtils.convertDigitsToPersian(selectedYear.toString()),
                                maxLines = 1,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        DropdownMenu(
                            expanded = yearMenuExpanded,
                            onDismissRequest = { yearMenuExpanded = false }
                        ) {
                            availableYears.forEach { year ->
                                DropdownMenuItem(
                                    text = { Text(IranianPhoneUtils.convertDigitsToPersian(year.toString())) },
                                    onClick = { onYearChange(year); yearMenuExpanded = false }
                                )
                            }
                        }
                    }

                    if (mode == TimelineFilterMode.MONTH) {
                        Box(Modifier.weight(1.25f)) {
                            OutlinedButton(
                                onClick = { monthMenuExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    PersianCalendarHelper.PERSIAN_MONTH_NAMES[selectedMonth - 1],
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            DropdownMenu(
                                expanded = monthMenuExpanded,
                                onDismissRequest = { monthMenuExpanded = false }
                            ) {
                                PersianCalendarHelper.PERSIAN_MONTH_NAMES.forEachIndexed { index, name ->
                                    DropdownMenuItem(
                                        text = { Text(name) },
                                        onClick = { onMonthChange(index + 1); monthMenuExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (mode == TimelineFilterMode.MONTH) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { onMoveMonth(-1) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) { Text("ماه قبل") }
                    Text(
                        PersianCalendarHelper.PERSIAN_MONTH_NAMES[selectedMonth - 1] + " " +
                            IranianPhoneUtils.convertDigitsToPersian(selectedYear.toString()),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(
                        onClick = { onMoveMonth(1) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) { Text("ماه بعد") }
                }
            }

            if (mode != TimelineFilterMode.ALL && invalidDateCount > 0) {
                Text(
                    IranianPhoneUtils.convertDigitsToPersian(invalidDateCount.toString()) +
                        " سابقه به دلیل تاریخ نامعتبر در فیلتر زمانی قابل دسته‌بندی نیست.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private data class TimelineDisplayItem(
    val events: List<VehicleTimelineEvent>
) {
    val primary: VehicleTimelineEvent get() = events.first()
    val serviceEvents: List<VehicleTimelineEvent.ServiceEvent>
        get() = events.filterIsInstance<VehicleTimelineEvent.ServiceEvent>()
    val serviceTitles: List<String>
        get() = serviceEvents.flatMap { service ->
            service.title.split(Regex("\\s*[،,]\\s*"))
                .map(String::trim)
                .filter(String::isNotBlank)
        }.distinct()
    val amount: Long
        get() = if (serviceEvents.isNotEmpty()) serviceEvents.sumOf { it.amount } else primary.amount
}

private fun buildTimelineDisplayItems(events: List<VehicleTimelineEvent>): List<TimelineDisplayItem> {
    val grouped = mutableListOf<MutableList<VehicleTimelineEvent>>()
    val serviceGroupIndexes = mutableMapOf<String, Int>()

    events.forEach { event ->
        if (event is VehicleTimelineEvent.ServiceEvent) {
            // Batch-created service records share date, mileage and subtitle. Their
            // total cost is stored on the first record only, so summing is safe.
            val groupKey = "${event.date}|${event.mileage}|${event.subtitle}"
            val existingIndex = serviceGroupIndexes[groupKey]
            if (existingIndex == null) {
                serviceGroupIndexes[groupKey] = grouped.size
                grouped.add(mutableListOf(event))
            } else {
                grouped[existingIndex].add(event)
            }
        } else {
            grouped.add(mutableListOf(event))
        }
    }

    return grouped.map { TimelineDisplayItem(it) }
}

@Composable
private fun TimelineItemRow(
    item: TimelineDisplayItem,
    isLast: Boolean,
    cardHeight: androidx.compose.ui.unit.Dp
) {
    val event = item.primary
    val amountFormatted = MoneyFormatter.formatExact(MoneyAmount.of(item.amount, event.amountUnit))
    val eventColor = Color(event.colorHex)
    val isServiceItem = item.serviceTitles.isNotEmpty()

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(32.dp)) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape)
                    .background(eventColor.copy(alpha = 0.15f))
                    .border(1.5.dp, eventColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Soft3DIcon(
                    imageRes = event.iconRes,
                    contentDescription = event.title,
                    size = 18.dp,
                    accentColor = eventColor
                )
            }
            if (!isLast) {
                Box(
                    Modifier.width(2.dp).height(cardHeight - 32.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                )
            }
        }

        Layered3DCard(
            modifier = Modifier.weight(1f).height(cardHeight).padding(bottom = 12.dp),
            elevation = 2.dp,
            borderColor = eventColor.copy(alpha = 0.2f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                if (isServiceItem) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TimelineDateBadge(event.date, eventColor)
                        }
                        item.serviceTitles.chunked(2).forEach { rowTitles ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowTitles.forEach { title ->
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(7.dp),
                                        color = eventColor.copy(alpha = 0.09f)
                                    ) {
                                        Text(
                                            text = title,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                                if (rowTitles.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        TimelineDateBadge(event.date, eventColor)
                    }
                }

                Text(
                    text = event.subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.amount > 0) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text(
                            text = amountFormatted,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold, fontSize = 12.sp
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineDateBadge(date: String, eventColor: Color) {
    Surface(shape = RoundedCornerShape(6.dp), color = eventColor.copy(alpha = 0.12f)) {
        Text(
            text = IranianPhoneUtils.convertDigitsToPersian(date),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold, fontSize = 10.sp
            ),
            color = eventColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
