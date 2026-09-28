package com.example.calendar.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.example.calendar.domain.CalendarDateUtils
import com.example.calendar.domain.model.FinancialEvent
import com.example.calendar.domain.model.FinancialEventType
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.WarningAmberLight
import com.example.util.IranianPhoneUtils

@Composable
fun HomeUpcomingEventsCard(
    upcomingEvents: List<FinancialEvent>,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Layered3DCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_upcoming_events_card"),
        backgroundColor = if (isDark) Color(0xFF141E30) else Color(0xFFFFFFFF),
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
        elevation = 3.dp,
        onClick = onOpenCalendar,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Soft3DIcon(
                        imageRes = R.drawable.img_3d_calendar,
                        contentDescription = "تقویم مالی",
                        size = 38.dp,
                        accentColor = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "رویدادهای مالی نزدیک",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "سررسیدهای ۳ روز آینده",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // "مشاهده تقویم" Button
                FilledTonalButton(
                    onClick = onOpenCalendar,
                    shape = RoundedCornerShape(RadiusMD),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.22f else 0.12f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("view_calendar_button")
                ) {
                    Text(
                        text = "مشاهده تقویم",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                thickness = 1.dp
            )

            // Upcoming Items (up to 3 items)
            val itemsToShow = upcomingEvents.take(3)
            if (itemsToShow.isEmpty()) {
                Text(
                    text = "هیچ سررسیدی برای ۳ روز آینده وجود ندارد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsToShow.forEach { event ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isDark) Color(0xFF1B263B).copy(alpha = 0.5f) else Color(0xFFF8FAFC)
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Soft3DIcon(
                                    imageRes = event.type.icon3dRes,
                                    contentDescription = event.title,
                                    size = 28.dp,
                                    accentColor = event.type.primaryColor
                                )

                                Column {
                                    Text(
                                        text = event.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = CalendarDateUtils.toPersianDisplay(event.date),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (!event.time.isNullOrBlank()) {
                                            Text(
                                                text = "• ${event.displayTimePersian}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            if (event.formattedAmount != null) {
                                Text(
                                    text = event.formattedAmount ?: "",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
