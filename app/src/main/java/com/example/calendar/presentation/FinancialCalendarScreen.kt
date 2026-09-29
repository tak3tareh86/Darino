package com.example.calendar.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.calendar.domain.CalendarDateUtils
import com.example.calendar.domain.model.FinancialEvent
import com.example.calendar.domain.model.FinancialEventType
import com.example.calendar.presentation.components.AddFinancialEventSheet
import com.example.calendar.presentation.components.CalendarView
import com.example.calendar.presentation.components.EventCard
import com.example.calendar.viewmodel.FinancialCalendarViewModel
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusXL
import com.example.util.IranianPhoneUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialCalendarScreen(
    onBack: (() -> Unit)? = null,
    viewModel: FinancialCalendarViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    var showAddSheet by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var selectedDayForFullDetail by remember { mutableStateOf<String?>(null) }
    val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val selectedDateNormalized = remember(state.selectedDate) {
        CalendarDateUtils.normalizeDate(state.selectedDate)
    }

    // Filter events for currently selected date (if search query is blank)
    val dayEvents = remember(state.events, selectedDateNormalized, state.searchQuery) {
        if (state.searchQuery.isNotBlank()) {
            state.events
        } else {
            state.events.filter {
                CalendarDateUtils.normalizeDate(it.date) == selectedDateNormalized
            }
        }
    }

    // If viewing full DayDetailsScreen
    if (selectedDayForFullDetail != null) {
        val detailDate = selectedDayForFullDetail!!
        val eventsOnDetailDate = state.events.filter {
            CalendarDateUtils.normalizeDate(it.date) == CalendarDateUtils.normalizeDate(detailDate)
        }
        DayDetailsScreen(
            date = detailDate,
            events = eventsOnDetailDate,
            onBackClick = { selectedDayForFullDetail = null },
            onAddEventClick = { showAddSheet = true },
            onToggleStatus = { viewModel.toggleEventStatus(it) },
            onDeleteEvent = { viewModel.deleteEvent(it) }
        )
        return
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("financial_calendar_screen"),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 3.dp,
                        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                        ambientColor = Color.Black.copy(alpha = 0.05f),
                        spotColor = Color.Black.copy(alpha = 0.08f)
                    ),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (onBack != null) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(26.dp)
                                    .testTag("calendar_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "بازگشت",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Soft3DIcon(
                            imageRes = R.drawable.img_3d_calendar,
                            contentDescription = "تقویم مالی",
                            size = 20.dp,
                            accentColor = MaterialTheme.colorScheme.primary
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            Text(
                                text = "تقویم مالی من",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                )
                            )
                            Text(
                                text = "مدیریت یکپارچه اقساط، یادآورها و خدمات خودرو",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 7.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showSearchBar = !showSearchBar },
                            modifier = Modifier
                                .size(26.dp)
                                .testTag("calendar_search_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (showSearchBar) Icons.Rounded.Close else Icons.Rounded.Search,
                                contentDescription = "جستجو",
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // Jump to Today
                        IconButton(
                            onClick = { viewModel.jumpToToday() },
                            modifier = Modifier
                                .size(26.dp)
                                .testTag("calendar_today_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Today,
                                contentDescription = "امروز",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp)
        ) {
            // Optional Search Bar
            if (showSearchBar) {
                item {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("جستجو در اقساط، بیمه، خودرو، هزینه و یادآورها...") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (state.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "پاک کردن")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calendar_search_input"),
                        shape = RoundedCornerShape(RadiusMD),
                        singleLine = true
                    )
                }
            }

            // Category Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // All Filter
                    FilterChip(
                        selected = state.selectedFilterType == null,
                        onClick = { viewModel.setFilterType(null) },
                        label = { Text("همه رویدادها") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )

                    FinancialEventType.entries.forEach { type ->
                        val isSelected = state.selectedFilterType == type
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.setFilterType(if (isSelected) null else type)
                            },
                            label = { Text(type.title) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = type.primaryColor.copy(alpha = if (isDark) 0.25f else 0.15f),
                                selectedLabelColor = type.primaryColor
                            )
                        )
                    }
                }
            }

            // Monthly Summary Card (Header Overview)
            item {
                MonthlySummaryCard(
                    monthTitle = state.currentMonthFormatted,
                    eventCountText = state.formattedEventCount,
                    totalPaymentsText = state.formattedMonthlyTotal,
                    onPreviousMonth = { viewModel.previousMonth() },
                    onNextMonth = { viewModel.nextMonth() }
                )
            }

            // Calendar Month Grid
            item {
                CalendarView(
                    year = state.currentYear,
                    month = state.currentMonth,
                    selectedDate = state.selectedDate,
                    events = state.events,
                    onDateSelected = { date ->
                        viewModel.selectDate(date)
                    }
                )
            }

            // Day Events Header
            item {
                val displaySelected = CalendarDateUtils.toPersianDisplay(state.selectedDate)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (state.searchQuery.isNotBlank()) "نتایج جستجو" else "رویدادهای $displaySelected",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${IranianPhoneUtils.convertDigitsToPersian(dayEvents.size.toString())} مورد در این بخش",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (state.searchQuery.isBlank() && dayEvents.isNotEmpty()) {
                        TextButton(
                            onClick = { selectedDayForFullDetail = state.selectedDate },
                            modifier = Modifier.testTag("open_day_details_button")
                        ) {
                            Text("مشاهده کامل روز")
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Day Events List or Empty State
            if (dayEvents.isEmpty()) {
                item {
                    Layered3DCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .testTag("empty_day_events_card"),
                        backgroundColor = if (isDark) Color(0xFF131A26) else Color(0xFFF8FAFC),
                        contentPadding = PaddingValues(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.EventAvailable,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "رویدادی برای این روز ثبت نشده",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "می‌توانید برای این تاریخ یک قسط، یادآور، هزینه یا سرویس خودرو اضافه کنید.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            FilledTonalButton(
                                onClick = { showAddSheet = true },
                                shape = RoundedCornerShape(RadiusMD)
                            ) {
                                Icon(imageVector = Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("افزودن برای این روز")
                            }
                        }
                    }
                }
            } else {
                items(dayEvents, key = { it.id }) { event ->
                    EventCard(
                        event = event,
                        onClick = {
                            selectedDayForFullDetail = event.date
                        },
                        onToggleStatus = {
                            viewModel.toggleEventStatus(event)
                        }
                    )
                }
            }
        }
    }

    // Add Financial Event BottomSheet
    if (showAddSheet) {
        AddFinancialEventSheet(
            sheetState = addSheetState,
            initialDate = state.selectedDate,
            onDismiss = { showAddSheet = false },
            onConfirm = { newEvent ->
                viewModel.addEvent(newEvent)
                showAddSheet = false
            }
        )
    }
}

@Composable
fun MonthlySummaryCard(
    monthTitle: String,
    eventCountText: String,
    totalPaymentsText: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Layered3DCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_summary_card"),
        backgroundColor = if (isDark) Color(0xFF141F32) else Color(0xFFEFF6FF),
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
        elevation = 2.dp,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Month Switcher Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Next Month (Chevron Right in RTL)
                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier.size(32.dp).testTag("calendar_next_month_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = "ماه بعد",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Month Title
                Text(
                    text = monthTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Previous Month (Chevron Left in RTL)
                IconButton(
                    onClick = onPreviousMonth,
                    modifier = Modifier.size(32.dp).testTag("calendar_prev_month_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = "ماه قبل",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                thickness = 1.dp
            )

            // Stats Row: Event Count + Total Monthly Payments
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Event Count
                Column {
                    Text(
                        text = "تعداد رویدادها",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = eventCountText,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Total Payments
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "مجموع پرداختی‌های ماه",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = totalPaymentsText,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp),
                        color = EmeraldPrimaryLight
                    )
                }
            }
        }
    }
}
