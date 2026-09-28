package com.example.calendar.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.domain.CalendarDateUtils
import com.example.calendar.domain.model.FinancialEvent
import com.example.calendar.presentation.components.EventCard
import com.example.ui.components.Layered3DCard
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailsScreen(
    date: String,
    events: List<FinancialEvent>,
    onBackClick: () -> Unit,
    onAddEventClick: () -> Unit,
    onToggleStatus: (FinancialEvent) -> Unit,
    onDeleteEvent: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val displayDate = CalendarDateUtils.toPersianDisplay(date)
    val totalAmount = events.mapNotNull { it.amount }.sum()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("day_details_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "رویدادهای $displayDate",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${com.example.util.IranianPhoneUtils.convertDigitsToPersian(events.size.toString())} رویداد ثبت شده",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("day_details_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "بازگشت"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onAddEventClick,
                        modifier = Modifier.testTag("day_details_add_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "افزودن رویداد",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Summary Card for the Day
            item {
                Layered3DCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = if (isDark) Color(0xFF162032) else Color(0xFFF1F5F9),
                    elevation = 2.dp,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "مجموع پرداختی‌های این روز",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val formatted = java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(totalAmount)
                            Text(
                                text = "${com.example.util.IranianPhoneUtils.convertDigitsToPersian(formatted)} تومان",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Button(
                            onClick = onAddEventClick,
                            shape = RoundedCornerShape(RadiusMD),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("افزودن رویداد")
                        }
                    }
                }
            }

            // Events List
            if (events.isEmpty()) {
                item {
                    Layered3DCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        backgroundColor = if (isDark) Color(0xFF131A28) else Color(0xFFFAFAFA),
                        contentPadding = PaddingValues(32.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.EventBusy,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "رویدادی برای این روز ثبت نشده است",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "برای این روز قسط، یادآور، هزینه یا سرویس خودرو اضافه کنید تا سررسید آن را از دست ندهید.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            FilledTonalButton(
                                onClick = onAddEventClick,
                                shape = RoundedCornerShape(RadiusMD)
                            ) {
                                Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ثبت رویداد جدید")
                            }
                        }
                    }
                }
            } else {
                items(events, key = { it.id }) { event ->
                    EventCard(
                        event = event,
                        onClick = { /* Already in detail view */ },
                        onToggleStatus = { onToggleStatus(event) }
                    )
                }
            }
        }
    }
}
